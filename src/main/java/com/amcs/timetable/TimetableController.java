package com.amcs.timetable;

import com.amcs.faculty.FacultyRepository;
import com.amcs.lab.LabBatchRepository;
import com.amcs.section.SectionRepository;
import com.amcs.subject.SubjectRepository;
import com.amcs.term.AcademicTermRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/timetable")
public class TimetableController {

    private final TimetableEntryRepository timetableRepo;
    private final AcademicTermRepository termRepo;
    private final SectionRepository sectionRepo;
    private final SubjectRepository subjectRepo;
    private final FacultyRepository facultyRepo;
    private final LabBatchRepository labBatchRepo;

    public TimetableController(TimetableEntryRepository timetableRepo, AcademicTermRepository termRepo,
                               SectionRepository sectionRepo, SubjectRepository subjectRepo,
                               FacultyRepository facultyRepo, LabBatchRepository labBatchRepo) {
        this.timetableRepo = timetableRepo;
        this.termRepo = termRepo;
        this.sectionRepo = sectionRepo;
        this.subjectRepo = subjectRepo;
        this.facultyRepo = facultyRepo;
        this.labBatchRepo = labBatchRepo;
    }

    record TimetableRequest(
        @NotNull Long termId,
        @NotNull Long sectionId,
        @NotNull Long subjectId,
        @NotNull Long facultyId,
        @NotBlank String dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        String room,
        Long labBatchId
    ) {}
    record ErrorBody(String message) {}

    @GetMapping
    public List<TimetableEntry> list(@RequestParam(required = false) Long sectionId,
                                      @RequestParam(required = false) Long facultyId) {
        if (sectionId != null) return timetableRepo.findBySectionIdAndActiveTrue(sectionId);
        if (facultyId != null) return timetableRepo.findByFacultyIdAndActiveTrue(facultyId);
        return timetableRepo.findAllActive();
    }

    /** Faculty-specific: today's classes */
    @GetMapping("/my")
    public ResponseEntity<?> myTimetable(@AuthenticationPrincipal String username) {
        var faculty = facultyRepo.findByUserUsername(username).orElse(null);
        if (faculty == null) return ResponseEntity.status(403).body(new ErrorBody("Not a faculty account"));
        return ResponseEntity.ok(timetableRepo.findByFacultyIdAndActiveTrue(faculty.getId()));
    }

    /** Faculty: today's classes */
    @GetMapping("/today")
    public ResponseEntity<?> todayClasses(@AuthenticationPrincipal String username) {
        var faculty = facultyRepo.findByUserUsername(username).orElse(null);
        if (faculty == null) return ResponseEntity.status(403).body(new ErrorBody("Not a faculty account"));
        String today = LocalDate.now().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toUpperCase();
        return ResponseEntity.ok(timetableRepo.findByFacultyAndDay(faculty.getId(), today));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> create(@Valid @RequestBody TimetableRequest req) {
        if ("SUNDAY".equalsIgnoreCase(req.dayOfWeek())) {
            return ResponseEntity.badRequest().body(new ErrorBody("Sunday is a weekly holiday. Normal timetable creation for Sunday is not allowed."));
        }
        if (!req.endTime().isAfter(req.startTime())) {
            return ResponseEntity.badRequest().body(new ErrorBody("End time must be after start time"));
        }
        var term = termRepo.findById(req.termId()).orElse(null);
        if (term == null) return ResponseEntity.badRequest().body(new ErrorBody("Term not found"));
        var section = sectionRepo.findById(req.sectionId()).orElse(null);
        if (section == null) return ResponseEntity.badRequest().body(new ErrorBody("Section not found"));
        var subject = subjectRepo.findById(req.subjectId()).orElse(null);
        if (subject == null) return ResponseEntity.badRequest().body(new ErrorBody("Subject not found"));
        var faculty = facultyRepo.findById(req.facultyId()).orElse(null);
        if (faculty == null) return ResponseEntity.badRequest().body(new ErrorBody("Faculty not found"));

        TimetableEntry te = new TimetableEntry();
        te.setTerm(term);
        te.setSection(section);
        te.setSubject(subject);
        te.setFaculty(faculty);
        te.setDayOfWeek(req.dayOfWeek().toUpperCase());
        te.setStartTime(req.startTime());
        te.setEndTime(req.endTime());
        te.setRoom(req.room());

        if ("LAB".equalsIgnoreCase(subject.getType())) {
            if (req.labBatchId() == null) {
                return ResponseEntity.badRequest().body(new ErrorBody("Lab batch is required for LAB subject"));
            }
            var batch = labBatchRepo.findById(req.labBatchId()).orElse(null);
            if (batch == null || !batch.getSection().getId().equals(section.getId())) {
                return ResponseEntity.badRequest().body(new ErrorBody("Selected lab batch does not belong to section"));
            }
            te.setLabBatch(batch);
        } else {
            te.setLabBatch(null);
        }

        return ResponseEntity.ok(timetableRepo.save(te));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody TimetableRequest req) {
        if ("SUNDAY".equalsIgnoreCase(req.dayOfWeek())) {
            return ResponseEntity.badRequest().body(new ErrorBody("Sunday is a weekly holiday. Normal timetable creation for Sunday is not allowed."));
        }
        if (!req.endTime().isAfter(req.startTime())) {
            return ResponseEntity.badRequest().body(new ErrorBody("End time must be after start time"));
        }
        return timetableRepo.findById(id).map(te -> {
            var term = termRepo.findById(req.termId()).orElse(null);
            if (term == null) return ResponseEntity.badRequest().body(new ErrorBody("Term not found"));
            var section = sectionRepo.findById(req.sectionId()).orElse(null);
            if (section == null) return ResponseEntity.badRequest().body(new ErrorBody("Section not found"));
            var subject = subjectRepo.findById(req.subjectId()).orElse(null);
            if (subject == null) return ResponseEntity.badRequest().body(new ErrorBody("Subject not found"));
            var faculty = facultyRepo.findById(req.facultyId()).orElse(null);
            if (faculty == null) return ResponseEntity.badRequest().body(new ErrorBody("Faculty not found"));

            te.setTerm(term);
            te.setSection(section);
            te.setSubject(subject);
            te.setFaculty(faculty);
            te.setDayOfWeek(req.dayOfWeek().toUpperCase());
            te.setStartTime(req.startTime());
            te.setEndTime(req.endTime());
            te.setRoom(req.room());

            if ("LAB".equalsIgnoreCase(subject.getType())) {
                if (req.labBatchId() == null) {
                    return ResponseEntity.badRequest().body(new ErrorBody("Lab batch is required for LAB subject"));
                }
                var batch = labBatchRepo.findById(req.labBatchId()).orElse(null);
                if (batch == null || !batch.getSection().getId().equals(section.getId())) {
                    return ResponseEntity.badRequest().body(new ErrorBody("Selected lab batch does not belong to section"));
                }
                te.setLabBatch(batch);
            } else {
                te.setLabBatch(null);
            }

            return ResponseEntity.ok(timetableRepo.save(te));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return timetableRepo.findById(id).map(te -> {
            te.setActive(false);
            timetableRepo.save(te);
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
