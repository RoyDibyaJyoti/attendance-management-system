package com.amcs.importer;

import com.amcs.auth.User;
import com.amcs.auth.UserRepository;
import com.amcs.department.Department;
import com.amcs.department.DepartmentRepository;
import com.amcs.faculty.Faculty;
import com.amcs.faculty.FacultyRepository;
import com.amcs.lab.LabBatch;
import com.amcs.lab.LabBatchRepository;
import com.amcs.section.Section;
import com.amcs.section.SectionRepository;
import com.amcs.subject.Subject;
import com.amcs.subject.SubjectRepository;
import com.amcs.term.AcademicTerm;
import com.amcs.term.AcademicTermRepository;
import com.amcs.timetable.TimetableEntry;
import com.amcs.timetable.TimetableEntryRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class ScheduleImportService {

    private final AcademicTermRepository termRepo;
    private final DepartmentRepository deptRepo;
    private final SectionRepository sectionRepo;
    private final SubjectRepository subjectRepo;
    private final FacultyRepository facultyRepo;
    private final LabBatchRepository labBatchRepo;
    private final TimetableEntryRepository timetableRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public ScheduleImportService(AcademicTermRepository termRepo,
                                 DepartmentRepository deptRepo,
                                 SectionRepository sectionRepo,
                                 SubjectRepository subjectRepo,
                                 FacultyRepository facultyRepo,
                                 LabBatchRepository labBatchRepo,
                                 TimetableEntryRepository timetableRepo,
                                 UserRepository userRepo,
                                 PasswordEncoder passwordEncoder) {
        this.termRepo = termRepo;
        this.deptRepo = deptRepo;
        this.sectionRepo = sectionRepo;
        this.subjectRepo = subjectRepo;
        this.facultyRepo = facultyRepo;
        this.labBatchRepo = labBatchRepo;
        this.timetableRepo = timetableRepo;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public ScheduleImportDto.ImportPreviewResponse preview(ScheduleImportDto.ImportRequest req) {
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        List<String> actions = new ArrayList<>();

        if (req == null) {
            errors.add("Import payload is empty or malformed.");
            return new ScheduleImportDto.ImportPreviewResponse(false, 0, 0, 0, 0, 0, warnings, errors, actions);
        }

        // 1. Academic Term
        AcademicTerm term = resolveTerm(req.academicTerm(), warnings, actions);
        if (term == null && req.academicTerm() == null) {
            errors.add("No academic term specified and no active term found in system.");
        }

        // 2. Departments
        Map<String, Department> deptMap = new HashMap<>();
        for (Department d : deptRepo.findAll()) {
            deptMap.put(d.getCode().toUpperCase(), d);
        }
        if (req.departments() != null) {
            for (var dDto : req.departments()) {
                if (dDto.code() == null || dDto.code().isBlank()) {
                    errors.add("Department definition missing 'code'.");
                    continue;
                }
                String code = dDto.code().trim().toUpperCase();
                if (!deptMap.containsKey(code)) {
                    actions.add(String.format("Will create Department %s (%s)", code, dDto.name() != null ? dDto.name() : code));
                } else {
                    warnings.add(String.format("Department %s already exists. Existing department will be reused.", code));
                }
            }
        }

        // 3. Sections
        Set<String> knownSectionNames = new HashSet<>();
        for (Section s : sectionRepo.findAll()) {
            knownSectionNames.add(s.getName().toUpperCase());
        }
        if (req.sections() != null) {
            for (var sDto : req.sections()) {
                if (sDto.name() == null || sDto.name().isBlank()) {
                    errors.add("Section definition missing 'name'.");
                    continue;
                }
                String sName = sDto.name().trim().toUpperCase();
                knownSectionNames.add(sName);
                String deptCode = sDto.departmentCode() != null ? sDto.departmentCode().trim().toUpperCase() : null;
                if (deptCode != null && !deptMap.containsKey(deptCode)) {
                    boolean deptInImport = req.departments() != null && req.departments().stream()
                            .anyMatch(d -> deptCode.equalsIgnoreCase(d.code()));
                    if (!deptInImport) {
                        errors.add(String.format("Section '%s' refers to department '%s', which does not exist.", sDto.name(), deptCode));
                    }
                }
                actions.add(String.format("Will configure Section '%s' (Department: %s, Year: %s)",
                        sDto.name(), deptCode != null ? deptCode : "Default", sDto.year() != null ? sDto.year() : 1));
            }
        }

        // 4. Faculty Resolution & Auto-profile warnings
        Map<String, Faculty> facultyByEmpId = new HashMap<>();
        List<Faculty> allFaculty = facultyRepo.findAll();
        for (Faculty f : allFaculty) {
            facultyByEmpId.put(f.getEmployeeId().toUpperCase(), f);
        }

        Set<String> importedEmpIds = new HashSet<>();
        if (req.faculty() != null) {
            for (var fDto : req.faculty()) {
                if (fDto.employeeId() == null || fDto.employeeId().isBlank()) {
                    errors.add(String.format("Faculty '%s' is missing required employeeId.", fDto.name()));
                    continue;
                }
                String empId = fDto.employeeId().trim().toUpperCase();
                importedEmpIds.add(empId);

                if (facultyByEmpId.containsKey(empId)) {
                    warnings.add(String.format("Faculty %s (%s) already exists. Profile will be reused.", fDto.name(), empId));
                } else {
                    // Check normalized name
                    Faculty fuzzy = findByNormalizedName(fDto.name(), allFaculty);
                    if (fuzzy != null) {
                        warnings.add(String.format("Warning: Imported faculty '%s' (%s) closely matches existing profile '%s' (%s).",
                                fDto.name(), empId, fuzzy.getUser().getFullName(), fuzzy.getEmployeeId()));
                    } else {
                        actions.add(String.format("Will automatically create Faculty profile for '%s' (%s). Login account will be created locked/pending requiring Admin credential setup.",
                                fDto.name(), empId));
                    }
                }
            }
        }

        // 5. Subjects
        Map<String, Subject> subjectMap = new HashMap<>();
        for (Subject s : subjectRepo.findAll()) {
            subjectMap.put(s.getCode().toUpperCase(), s);
        }
        if (req.subjects() != null) {
            for (var sDto : req.subjects()) {
                if (sDto.code() == null || sDto.code().isBlank()) {
                    errors.add(String.format("Subject '%s' missing code.", sDto.name()));
                    continue;
                }
                String code = sDto.code().trim().toUpperCase();
                if (subjectMap.containsKey(code)) {
                    warnings.add(String.format("Subject %s (%s) already exists. Will reuse existing subject.", sDto.name(), code));
                } else {
                    String type = (sDto.type() != null && sDto.type().equalsIgnoreCase("LAB")) ? "LAB" : "THEORY";
                    actions.add(String.format("Will create Subject %s (%s, %s, %d credits)",
                            sDto.name(), code, type, sDto.credits() != null ? sDto.credits() : 3));
                }
            }
        }

        // 6. Lab Batches
        if (req.labBatches() != null) {
            for (var bDto : req.labBatches()) {
                if (bDto.name() == null || bDto.name().isBlank()) {
                    errors.add("Lab batch definition missing 'name'.");
                }
                if (bDto.sectionName() == null || bDto.sectionName().isBlank()) {
                    errors.add(String.format("Lab batch '%s' missing 'sectionName'.", bDto.name()));
                }
            }
        }

        // 7. Timetable Entries Validation
        int entryCount = 0;
        if (req.timetableEntries() != null) {
            entryCount = req.timetableEntries().size();
            for (int i = 0; i < req.timetableEntries().size(); i++) {
                var te = req.timetableEntries().get(i);
                int idx = i + 1;

                if (te.sectionName() == null || te.sectionName().isBlank()) {
                    errors.add(String.format("Entry #%d: sectionName is required.", idx));
                } else if (!knownSectionNames.contains(te.sectionName().trim().toUpperCase())) {
                    errors.add(String.format("Entry #%d: section '%s' is not defined in system or import sections list.", idx, te.sectionName()));
                }

                if (te.subjectCode() == null || te.subjectCode().isBlank()) {
                    errors.add(String.format("Entry #%d: subjectCode is required.", idx));
                }

                // Day of week
                if (te.dayOfWeek() == null || te.dayOfWeek().isBlank()) {
                    errors.add(String.format("Entry #%d: dayOfWeek is required.", idx));
                } else {
                    String day = te.dayOfWeek().trim().toUpperCase();
                    if (day.equals("SUNDAY")) {
                        errors.add(String.format("Entry #%d: Sunday is a weekly holiday. Sunday timetable entries are not allowed.", idx));
                    } else {
                        try {
                            DayOfWeek.valueOf(day);
                        } catch (IllegalArgumentException e) {
                            errors.add(String.format("Entry #%d: invalid dayOfWeek '%s'.", idx, te.dayOfWeek()));
                        }
                    }
                }

                // Times
                LocalTime start = null;
                LocalTime end = null;
                try {
                    start = parseTime(te.startTime());
                    end = parseTime(te.endTime());
                    if (!end.isAfter(start)) {
                        errors.add(String.format("Entry #%d: endTime (%s) must be after startTime (%s).", idx, te.endTime(), te.startTime()));
                    }
                } catch (Exception e) {
                    errors.add(String.format("Entry #%d: invalid time format (%s - %s). Use HH:mm format.", idx, te.startTime(), te.endTime()));
                }

                // Lab vs Theory Batch Validation
                String subCode = te.subjectCode() != null ? te.subjectCode().trim().toUpperCase() : "";
                Subject existingSub = subjectMap.get(subCode);
                var importSub = req.subjects() != null ? req.subjects().stream()
                        .filter(s -> subCode.equalsIgnoreCase(s.code()))
                        .findFirst().orElse(null) : null;

                boolean isLab = (existingSub != null && "LAB".equalsIgnoreCase(existingSub.getType())) ||
                                (importSub != null && "LAB".equalsIgnoreCase(importSub.type())) ||
                                (subCode.endsWith("-LAB") || (te.labBatchName() != null && !te.labBatchName().isBlank()));

                if (isLab && (te.labBatchName() == null || te.labBatchName().isBlank())) {
                    errors.add(String.format("Entry #%d: LAB subject '%s' requires a labBatchName.", idx, subCode));
                }
                if (!isLab && te.labBatchName() != null && !te.labBatchName().isBlank()) {
                    warnings.add(String.format("Entry #%d: THEORY subject '%s' has labBatchName '%s' specified. It will be treated as LAB.", idx, subCode, te.labBatchName()));
                }

                // Faculty auto-creation notice from timetable entry if not declared in faculty list
                if ((te.facultyEmployeeId() != null && !te.facultyEmployeeId().isBlank()) ||
                    (te.facultyName() != null && !te.facultyName().isBlank())) {
                    String emp = te.facultyEmployeeId() != null ? te.facultyEmployeeId().trim().toUpperCase() : null;
                    if (emp != null && !facultyByEmpId.containsKey(emp) && !importedEmpIds.contains(emp)) {
                        importedEmpIds.add(emp);
                        actions.add(String.format("Entry #%d references new Faculty '%s' (%s). Profile will be auto-created (account locked/pending).",
                                idx, te.facultyName() != null ? te.facultyName() : emp, emp));
                    }
                }
            }

            // Conflict Check (faculty double-booking & section theory conflict)
            validateScheduleConflicts(req.timetableEntries(), errors, warnings);
        }

        int sectionCount = req.sections() != null ? req.sections().size() : 0;
        int facultyCount = importedEmpIds.size();
        int subjectCount = req.subjects() != null ? req.subjects().size() : 0;
        int labBatchCount = req.labBatches() != null ? req.labBatches().size() : 0;
        boolean canCommit = errors.isEmpty();

        return new ScheduleImportDto.ImportPreviewResponse(
                canCommit,
                sectionCount,
                facultyCount,
                subjectCount,
                labBatchCount,
                entryCount,
                warnings,
                errors,
                actions
        );
    }

    @Transactional
    public ScheduleImportDto.ImportCommitResponse commit(ScheduleImportDto.ImportRequest req) {
        // Run full preview validation first; reject if any error exists
        var preview = preview(req);
        if (!preview.canCommit()) {
            throw new IllegalArgumentException("Cannot commit schedule import: " + String.join("; ", preview.errors()));
        }

        // 1. Term
        List<String> dummyWarn = new ArrayList<>();
        List<String> dummyAct = new ArrayList<>();
        AcademicTerm term = resolveTerm(req.academicTerm(), dummyWarn, dummyAct);
        if (term == null) {
            term = new AcademicTerm();
            term.setName(req.academicTerm() != null && req.academicTerm().name() != null ? req.academicTerm().name() : "Academic Term " + java.time.LocalDate.now().getYear());
            term.setStartDate(java.time.LocalDate.of(java.time.LocalDate.now().getYear(), 7, 1));
            term.setEndDate(java.time.LocalDate.of(java.time.LocalDate.now().getYear(), 12, 31));
            term.setActive(true);
            term = termRepo.save(term);
        }

        // 2. Departments
        Map<String, Department> deptMap = new HashMap<>();
        for (Department d : deptRepo.findAll()) {
            deptMap.put(d.getCode().toUpperCase(), d);
        }
        if (req.departments() != null) {
            for (var dDto : req.departments()) {
                String code = dDto.code().trim().toUpperCase();
                if (!deptMap.containsKey(code)) {
                    Department d = new Department();
                    d.setCode(code);
                    d.setName(dDto.name() != null ? dDto.name() : code);
                    Department saved = deptRepo.save(d);
                    deptMap.put(code, saved);
                }
            }
        }
        Department defaultDept = deptMap.values().stream().findFirst().orElseGet(() -> {
            Department d = new Department();
            d.setCode("CSE");
            d.setName("Computer Science & Engineering");
            Department saved = deptRepo.save(d);
            deptMap.put("CSE", saved);
            return saved;
        });

        // 3. Sections
        int createdSections = 0;
        Map<String, Section> sectionMap = new HashMap<>();
        for (Section s : sectionRepo.findAll()) {
            sectionMap.put(s.getName().toUpperCase(), s);
        }
        if (req.sections() != null) {
            for (var sDto : req.sections()) {
                String sName = sDto.name().trim();
                String key = sName.toUpperCase();
                if (!sectionMap.containsKey(key)) {
                    String dCode = sDto.departmentCode() != null ? sDto.departmentCode().trim().toUpperCase() : null;
                    Department d = (dCode != null && deptMap.containsKey(dCode)) ? deptMap.get(dCode) : defaultDept;
                    Section s = new Section();
                    s.setName(sName);
                    s.setDepartment(d);
                    s.setTerm(term);
                    Section saved = sectionRepo.save(s);
                    sectionMap.put(key, saved);
                    createdSections++;
                }
            }
        }

        // 4. Faculty Profiles
        int createdFaculty = 0;
        Map<String, Faculty> facultyByEmpId = new HashMap<>();
        List<Faculty> allFaculty = facultyRepo.findAll();
        for (Faculty f : allFaculty) {
            facultyByEmpId.put(f.getEmployeeId().toUpperCase(), f);
        }

        if (req.faculty() != null) {
            for (var fDto : req.faculty()) {
                String empId = fDto.employeeId().trim().toUpperCase();
                if (!facultyByEmpId.containsKey(empId)) {
                    Faculty created = createFacultyProfile(fDto.name(), empId, fDto.departmentCode(), deptMap, defaultDept);
                    facultyByEmpId.put(empId, created);
                    createdFaculty++;
                }
            }
        }

        // 5. Subjects
        int createdSubjects = 0;
        Map<String, Subject> subjectMap = new HashMap<>();
        for (Subject s : subjectRepo.findAll()) {
            subjectMap.put(s.getCode().toUpperCase(), s);
        }
        if (req.subjects() != null) {
            for (var sDto : req.subjects()) {
                String code = sDto.code().trim().toUpperCase();
                if (!subjectMap.containsKey(code)) {
                    Subject s = new Subject();
                    s.setCode(code);
                    s.setName(sDto.name() != null ? sDto.name() : code);
                    s.setType((sDto.type() != null && sDto.type().equalsIgnoreCase("LAB")) ? "LAB" : "THEORY");
                    s.setCredits(sDto.credits() != null ? sDto.credits() : 3);
                    Subject saved = subjectRepo.save(s);
                    subjectMap.put(code, saved);
                    createdSubjects++;
                }
            }
        }

        // 6. Lab Batches
        int createdLabBatches = 0;
        Map<String, LabBatch> batchMap = new HashMap<>();
        for (LabBatch b : labBatchRepo.findAll()) {
            batchMap.put(b.getSection().getName().toUpperCase() + "_" + b.getName().toUpperCase(), b);
        }
        if (req.labBatches() != null) {
            for (var bDto : req.labBatches()) {
                String secKey = bDto.sectionName().trim().toUpperCase();
                Section sec = sectionMap.get(secKey);
                if (sec != null) {
                    String batchKey = secKey + "_" + bDto.name().trim().toUpperCase();
                    if (!batchMap.containsKey(batchKey)) {
                        LabBatch b = new LabBatch();
                        b.setName(bDto.name().trim());
                        b.setSection(sec);
                        LabBatch saved = labBatchRepo.save(b);
                        batchMap.put(batchKey, saved);
                        createdLabBatches++;
                    }
                }
            }
        }

        // 7. Timetable Entries
        int createdEntries = 0;
        if (req.timetableEntries() != null) {
            for (var teDto : req.timetableEntries()) {
                String secKey = teDto.sectionName().trim().toUpperCase();
                Section sec = sectionMap.get(secKey);

                String subKey = teDto.subjectCode().trim().toUpperCase();
                Subject sub = subjectMap.get(subKey);
                if (sub == null) {
                    // Auto-create subject if missing
                    sub = new Subject();
                    sub.setCode(subKey);
                    sub.setName(subKey);
                    sub.setType(teDto.labBatchName() != null ? "LAB" : "THEORY");
                    sub.setCredits(3);
                    sub = subjectRepo.save(sub);
                    subjectMap.put(subKey, sub);
                    createdSubjects++;
                }

                // Faculty resolution
                String emp = teDto.facultyEmployeeId() != null ? teDto.facultyEmployeeId().trim().toUpperCase() : null;
                Faculty fac = emp != null ? facultyByEmpId.get(emp) : null;
                if (fac == null && teDto.facultyName() != null) {
                    fac = findByNormalizedName(teDto.facultyName(), facultyByEmpId.values().stream().toList());
                }
                if (fac == null) {
                    // Auto-create faculty profile
                    String genEmpId = emp != null ? emp : "FAC-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                    String name = teDto.facultyName() != null ? teDto.facultyName() : genEmpId;
                    fac = createFacultyProfile(name, genEmpId, sec.getDepartment().getCode(), deptMap, defaultDept);
                    facultyByEmpId.put(genEmpId.toUpperCase(), fac);
                    createdFaculty++;
                }

                // Lab batch resolution
                LabBatch labBatch = null;
                if (teDto.labBatchName() != null && !teDto.labBatchName().isBlank()) {
                    String bKey = secKey + "_" + teDto.labBatchName().trim().toUpperCase();
                    labBatch = batchMap.get(bKey);
                    if (labBatch == null) {
                        labBatch = new LabBatch();
                        labBatch.setName(teDto.labBatchName().trim());
                        labBatch.setSection(sec);
                        labBatch = labBatchRepo.save(labBatch);
                        batchMap.put(bKey, labBatch);
                        createdLabBatches++;
                    }
                }

                TimetableEntry te = new TimetableEntry();
                te.setTerm(term);
                te.setSection(sec);
                te.setSubject(sub);
                te.setFaculty(fac);
                te.setDayOfWeek(teDto.dayOfWeek().trim().toUpperCase());
                te.setStartTime(parseTime(teDto.startTime()));
                te.setEndTime(parseTime(teDto.endTime()));
                te.setRoom(teDto.room() != null ? teDto.room().trim() : "Room TBA");
                te.setLabBatch(labBatch);
                te.setActive(true);
                timetableRepo.save(te);
                createdEntries++;
            }
        }

        String msg = String.format("Successfully imported schedule: %d sections, %d faculty, %d subjects, %d lab batches, %d timetable entries committed.",
                createdSections, createdFaculty, createdSubjects, createdLabBatches, createdEntries);

        return new ScheduleImportDto.ImportCommitResponse(
                true,
                msg,
                createdSections,
                createdFaculty,
                createdSubjects,
                createdLabBatches,
                createdEntries
        );
    }

    private Faculty createFacultyProfile(String name, String employeeId, String deptCode,
                                         Map<String, Department> deptMap, Department defaultDept) {
        Department dept = (deptCode != null && deptMap.containsKey(deptCode.toUpperCase()))
                ? deptMap.get(deptCode.toUpperCase())
                : defaultDept;

        // Create locked User profile (no active login, unusable random password hash)
        String username = employeeId.toLowerCase().replaceAll("[^a-z0-9_]", "_");
        if (userRepo.findByUsername(username).isPresent()) {
            username = username + "_" + System.currentTimeMillis() % 10000;
        }

        User u = new User();
        u.setUsername(username);
        u.setFullName(name);
        u.setRole("FACULTY");
        u.setActive(false); // Locked until Admin sets credentials
        u.setPassword(passwordEncoder.encode(UUID.randomUUID().toString() + "_LOCKED_ACCOUNT"));
        User savedUser = userRepo.save(u);

        Faculty f = new Faculty();
        f.setEmployeeId(employeeId);
        f.setUser(savedUser);
        f.setDepartment(dept);
        f.setActive(true);
        return facultyRepo.save(f);
    }

    private AcademicTerm resolveTerm(ScheduleImportDto.AcademicTermDto termDto, List<String> warnings, List<String> actions) {
        if (termDto != null && termDto.name() != null) {
            String name = termDto.name().trim();
            var existing = termRepo.findByName(name);
            if (existing.isPresent()) {
                warnings.add(String.format("Using existing academic term: %s", name));
                return existing.get();
            }
            actions.add(String.format("Will create academic term: %s", name));
            return null;
        }
        var active = termRepo.findByActiveTrue().orElse(null);
        if (active != null) {
            warnings.add(String.format("No term specified in import. Binding to active term '%s'.", active.getName()));
            return active;
        }
        return null;
    }

    private Faculty findByNormalizedName(String rawName, List<Faculty> facultyList) {
        if (rawName == null || rawName.isBlank()) return null;
        String normalizedTarget = normalizeName(rawName);
        for (Faculty f : facultyList) {
            if (f.getUser() != null && f.getUser().getFullName() != null) {
                if (normalizeName(f.getUser().getFullName()).equals(normalizedTarget)) {
                    return f;
                }
            }
        }
        return null;
    }

    private String normalizeName(String name) {
        return name.toLowerCase()
                .replaceAll("\\b(dr|prof|mr|mrs|ms|shri)\\b", "")
                .replaceAll("[^a-z0-9]", "")
                .trim();
    }

    private LocalTime parseTime(String timeStr) {
        if (timeStr == null) throw new IllegalArgumentException("Time cannot be null");
        String trimmed = timeStr.trim();
        if (trimmed.length() == 5) {
            return LocalTime.parse(trimmed + ":00");
        }
        return LocalTime.parse(trimmed);
    }

    private void validateScheduleConflicts(List<ScheduleImportDto.TimetableEntryDto> entries,
                                           List<String> errors,
                                           List<String> warnings) {
        for (int i = 0; i < entries.size(); i++) {
            var e1 = entries.get(i);
            LocalTime s1, end1;
            try {
                s1 = parseTime(e1.startTime());
                end1 = parseTime(e1.endTime());
            } catch (Exception ex) {
                continue;
            }

            for (int j = i + 1; j < entries.size(); j++) {
                var e2 = entries.get(j);
                if (!e1.dayOfWeek().equalsIgnoreCase(e2.dayOfWeek())) continue;

                LocalTime s2, end2;
                try {
                    s2 = parseTime(e2.startTime());
                    end2 = parseTime(e2.endTime());
                } catch (Exception ex) {
                    continue;
                }

                // Check time overlap: (start1 < end2) && (start2 < end1)
                boolean overlaps = s1.isBefore(end2) && s2.isBefore(end1);
                if (!overlaps) continue;

                // 1. Same Faculty Conflict
                String f1 = e1.facultyEmployeeId() != null ? e1.facultyEmployeeId().trim().toUpperCase() : e1.facultyName();
                String f2 = e2.facultyEmployeeId() != null ? e2.facultyEmployeeId().trim().toUpperCase() : e2.facultyName();
                if (f1 != null && f1.equalsIgnoreCase(f2)) {
                    errors.add(String.format("Faculty conflict: %s is assigned to two overlapping classes on %s (%s - %s vs %s - %s) in sections '%s' and '%s'.",
                            f1, e1.dayOfWeek(), e1.startTime(), e1.endTime(), e2.startTime(), e2.endTime(), e1.sectionName(), e2.sectionName()));
                }

                // 2. Same Section Conflict
                if (e1.sectionName().equalsIgnoreCase(e2.sectionName())) {
                    boolean isCombinedLab = e1.labBatchName() != null && e2.labBatchName() != null &&
                            !e1.labBatchName().equalsIgnoreCase(e2.labBatchName());
                    if (!isCombinedLab) {
                        // If same lab batch or theory, this is a conflicting overlap!
                        errors.add(String.format("Section schedule conflict in section '%s' on %s (%s - %s): '%s' conflicts with '%s'.",
                                e1.sectionName(), e1.dayOfWeek(), e1.startTime(), e1.endTime(), e1.subjectCode(), e2.subjectCode()));
                    }
                }
            }
        }
    }
}
