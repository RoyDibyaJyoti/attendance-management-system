package com.amcs.application.service.importer.validator;

import com.amcs.application.port.out.AcademicPeriodRepositoryPort;
import com.amcs.application.port.out.FacultyRepositoryPort;
import com.amcs.application.port.out.LabGroupRepositoryPort;
import com.amcs.application.port.out.SectionRepositoryPort;
import com.amcs.application.port.out.SessionRepositoryPort;
import com.amcs.application.port.out.SubjectRepositoryPort;
import com.amcs.application.port.out.excel.ConversionResult;
import com.amcs.application.port.out.excel.ParsedRow;
import com.amcs.application.port.out.security.AuthenticatedActor;
import com.amcs.application.port.out.security.FacultyAssignmentRepositoryPort;
import com.amcs.application.service.importer.payload.StagedSessionPayload;
import com.amcs.domain.academic.AcademicPeriod;
import com.amcs.domain.academic.Subject;
import com.amcs.domain.attendance.Session;
import com.amcs.domain.attendance.SessionType;
import com.amcs.domain.importer.RowValidationError;
import com.amcs.infrastructure.excel.converter.CellTypeConverter;
import com.amcs.infrastructure.persistence.entity.FacultyEntity;
import com.amcs.infrastructure.persistence.entity.LabGroupEntity;
import com.amcs.infrastructure.persistence.entity.SectionEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class SessionRowValidator {

    private final SubjectRepositoryPort subjectRepository;
    private final SectionRepositoryPort sectionRepository;
    private final FacultyRepositoryPort facultyRepository;
    private final LabGroupRepositoryPort labGroupRepository;
    private final AcademicPeriodRepositoryPort academicPeriodRepository;
    private final SessionRepositoryPort sessionRepository;
    private final FacultyAssignmentRepositoryPort facultyAssignmentRepository;

    public SessionRowValidator(
        SubjectRepositoryPort subjectRepository,
        SectionRepositoryPort sectionRepository,
        FacultyRepositoryPort facultyRepository,
        LabGroupRepositoryPort labGroupRepository,
        AcademicPeriodRepositoryPort academicPeriodRepository,
        SessionRepositoryPort sessionRepository,
        FacultyAssignmentRepositoryPort facultyAssignmentRepository
    ) {
        this.subjectRepository = Objects.requireNonNull(subjectRepository, "subjectRepository");
        this.sectionRepository = Objects.requireNonNull(sectionRepository, "sectionRepository");
        this.facultyRepository = Objects.requireNonNull(facultyRepository, "facultyRepository");
        this.labGroupRepository = Objects.requireNonNull(labGroupRepository, "labGroupRepository");
        this.academicPeriodRepository = Objects.requireNonNull(academicPeriodRepository, "academicPeriodRepository");
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "sessionRepository");
        this.facultyAssignmentRepository = Objects.requireNonNull(facultyAssignmentRepository, "facultyAssignmentRepository");
    }

    public ValidationOutcome<StagedSessionPayload> validateRow(
        ParsedRow row,
        Set<String> seenSessions,
        AuthenticatedActor actor
    ) {
        int rowIndex = row.rowIndex();

        // Level 1: Syntactic
        String subjectCode = row.get("Subject Code");
        if (subjectCode.isEmpty()) {
            return ValidationOutcome.invalid(new RowValidationError(
                rowIndex, "Subject Code", "", "MISSING_REQUIRED_FIELD", "Subject Code is required"
            ));
        }

        String sectionName = row.get("Section Name");
        if (sectionName.isEmpty()) {
            return ValidationOutcome.invalid(new RowValidationError(
                rowIndex, "Section Name", "", "MISSING_REQUIRED_FIELD", "Section Name is required"
            ));
        }

        String facultyEmpId = row.get("Faculty Employee ID");
        if (facultyEmpId.isEmpty()) {
            return ValidationOutcome.invalid(new RowValidationError(
                rowIndex, "Faculty Employee ID", "", "MISSING_REQUIRED_FIELD", "Faculty Employee ID is required"
            ));
        }

        ConversionResult<LocalDate> dateResult = CellTypeConverter.asDate(
            row.get("Session Date"), "Session Date", rowIndex, true
        );
        if (!dateResult.isSuccess()) {
            return ValidationOutcome.invalid(dateResult.getError().get());
        }
        LocalDate sessionDate = dateResult.getValue().orElseThrow();

        ConversionResult<SessionType> typeResult = CellTypeConverter.asEnum(
            SessionType.class, row.get("Session Type"), "Session Type", rowIndex, true
        );
        if (!typeResult.isSuccess()) {
            return ValidationOutcome.invalid(typeResult.getError().get());
        }
        SessionType sessionType = typeResult.getValue().orElseThrow();

        ConversionResult<Integer> unitsResult = CellTypeConverter.asInteger(
            row.get("Planned Units"), "Planned Units", rowIndex, true
        );
        if (!unitsResult.isSuccess()) {
            return ValidationOutcome.invalid(unitsResult.getError().get());
        }
        int plannedUnits = unitsResult.getValue().orElseThrow();
        if (plannedUnits < 1) {
            return ValidationOutcome.invalid(new RowValidationError(
                rowIndex, "Planned Units", String.valueOf(plannedUnits), "INVALID_UNITS", "Planned units must be >= 1"
            ));
        }

        String labGroupName = row.get("Lab Group Name");

        // Level 2: Referential
        Optional<Subject> subjectOpt = subjectRepository.findByCode(subjectCode);
        if (subjectOpt.isEmpty()) {
            return ValidationOutcome.invalid(new RowValidationError(
                rowIndex, "Subject Code", subjectCode, "SUBJECT_NOT_FOUND", "Subject not found with code: '" + subjectCode + "'"
            ));
        }
        Subject subject = subjectOpt.get();

        Optional<SectionEntity> sectionOpt = sectionRepository.findAll().stream()
            .filter(s -> s.getName().equalsIgnoreCase(sectionName))
            .findFirst();
        if (sectionOpt.isEmpty()) {
            return ValidationOutcome.invalid(new RowValidationError(
                rowIndex, "Section Name", sectionName, "SECTION_NOT_FOUND", "Section not found with name: '" + sectionName + "'"
            ));
        }
        SectionEntity section = sectionOpt.get();

        Optional<AcademicPeriod> periodOpt = academicPeriodRepository.findById(section.getAcademicPeriodId());
        if (periodOpt.isEmpty()) {
            return ValidationOutcome.invalid(new RowValidationError(
                rowIndex, "Section Name", sectionName, "ACADEMIC_PERIOD_NOT_FOUND", "Academic period for section not found"
            ));
        }
        AcademicPeriod period = periodOpt.get();

        Optional<FacultyEntity> facultyOpt = facultyRepository.findByEmployeeId(facultyEmpId);
        if (facultyOpt.isEmpty()) {
            return ValidationOutcome.invalid(new RowValidationError(
                rowIndex, "Faculty Employee ID", facultyEmpId, "FACULTY_NOT_FOUND", "Faculty not found with ID: '" + facultyEmpId + "'"
            ));
        }
        FacultyEntity faculty = facultyOpt.get();

        UUID labGroupId = null;
        if (!labGroupName.isEmpty()) {
            Optional<LabGroupEntity> groupOpt = labGroupRepository.findGroupsBySection(section.getId()).stream()
                .filter(g -> g.getName().equalsIgnoreCase(labGroupName))
                .findFirst();
            if (groupOpt.isEmpty()) {
                return ValidationOutcome.invalid(new RowValidationError(
                    rowIndex, "Lab Group Name", labGroupName, "LAB_GROUP_NOT_FOUND", "Lab group not found with name: '" + labGroupName + "'"
                ));
            }
            labGroupId = groupOpt.get().getId();
        }

        // Level 3: Business rules & Authorization Scope
        if (!period.contains(sessionDate)) {
            return ValidationOutcome.invalid(new RowValidationError(
                rowIndex, "Session Date", sessionDate.toString(), "DATE_OUTSIDE_ACADEMIC_PERIOD",
                "Session date " + sessionDate + " is outside academic period (" + period.startDate() + " to " + period.endDate() + ")"
            ));
        }

        if (actor.isFaculty()) {
            UUID actorFacultyId = actor.facultyId().orElse(null);
            if (!faculty.getId().equals(actorFacultyId)) {
                return ValidationOutcome.invalid(new RowValidationError(
                    rowIndex, "Faculty Employee ID", facultyEmpId, "FACULTY_NOT_AUTHORIZED",
                    "Faculty cannot schedule sessions on behalf of another faculty member"
                ));
            }

            boolean assigned = facultyAssignmentRepository.isFacultyAssigned(
                faculty.getId(), subject.id(), section.getId(), section.getAcademicPeriodId(), sessionDate
            );
            if (!assigned) {
                return ValidationOutcome.invalid(new RowValidationError(
                    rowIndex, "Subject Code", subjectCode, "FACULTY_NOT_ASSIGNED",
                    "Faculty member is not assigned to teach subject '" + subjectCode + "' for section '" + sectionName + "'"
                ));
            }
        }

        // Duplicate check in file
        String sessionKey = subject.id() + ":" + section.getId() + ":" + sessionDate + ":" + sessionType + ":" + labGroupId;
        if (seenSessions.contains(sessionKey)) {
            return ValidationOutcome.invalid(new RowValidationError(
                rowIndex, "Session Date", sessionDate.toString(), "DUPLICATE_IN_FILE",
                "Duplicate session within file for subject " + subjectCode + " and section " + sectionName + " on " + sessionDate
            ));
        }
        seenSessions.add(sessionKey);

        // Duplicate check against database
        final UUID effectiveLabGroupId = labGroupId;
        boolean dbConflict = sessionRepository.findBySection(section.getId()).stream()
            .anyMatch(s -> s.subjectId().equals(subject.id())
                && s.sessionDate().equals(sessionDate)
                && s.sessionType() == sessionType
                && Objects.equals(s.labGroupId().orElse(null), effectiveLabGroupId));
        if (dbConflict) {
            return ValidationOutcome.invalid(new RowValidationError(
                rowIndex, "Session Date", sessionDate.toString(), "SESSION_ALREADY_EXISTS",
                "Session already exists in database for subject " + subjectCode + " and section " + sectionName + " on " + sessionDate
            ));
        }

        return ValidationOutcome.valid(new StagedSessionPayload(
            section.getAcademicPeriodId(), subject.id(), section.getId(), faculty.getId(), sessionDate, sessionType, plannedUnits, labGroupId
        ));
    }
}
