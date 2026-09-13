package com.amcs.infrastructure.bootstrap;

import com.amcs.application.port.out.AcademicPeriodRepositoryPort;
import com.amcs.application.port.out.AttendancePolicyRepositoryPort;
import com.amcs.application.port.out.AttendanceRecordRepositoryPort;
import com.amcs.application.port.out.DepartmentRepositoryPort;
import com.amcs.application.port.out.EnrollmentRepositoryPort;
import com.amcs.application.port.out.FacultyRepositoryPort;
import com.amcs.application.port.out.SectionRepositoryPort;
import com.amcs.application.port.out.SessionRepositoryPort;
import com.amcs.application.port.out.StudentRepositoryPort;
import com.amcs.application.port.out.SubjectRepositoryPort;
import com.amcs.application.port.out.security.FacultyAssignmentRepositoryPort;
import com.amcs.application.port.out.security.UserAccount;
import com.amcs.application.port.out.security.UserAccountRepositoryPort;
import com.amcs.application.port.out.security.UserAccountStatus;
import com.amcs.application.port.out.security.UserRole;
import com.amcs.domain.academic.AcademicPeriod;
import com.amcs.domain.academic.CourseType;
import com.amcs.domain.academic.Subject;
import com.amcs.domain.attendance.AttendanceRecord;
import com.amcs.domain.attendance.AttendanceStatus;
import com.amcs.domain.attendance.Session;
import com.amcs.domain.attendance.SessionStatus;
import com.amcs.domain.attendance.SessionType;
import com.amcs.domain.enrollment.Enrollment;
import com.amcs.domain.policy.AttendancePolicy;
import com.amcs.domain.policy.MissingRecordStrategy;
import com.amcs.infrastructure.persistence.entity.DepartmentEntity;
import com.amcs.infrastructure.persistence.entity.FacultyEntity;
import com.amcs.infrastructure.persistence.entity.SectionEntity;
import com.amcs.infrastructure.persistence.entity.StudentEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Initializes baseline administrative credentials and demo university structure
 * on blank database startups. Enables immediate local browser testing and frontend operation.
 */
@Component
@ConditionalOnProperty(name = "amcs.bootstrap.enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseDataInitializer.class);

    private final UserAccountRepositoryPort userAccountPort;
    private final PasswordEncoder passwordEncoder;
    private final DepartmentRepositoryPort departmentPort;
    private final AcademicPeriodRepositoryPort periodPort;
    private final SectionRepositoryPort sectionPort;
    private final SubjectRepositoryPort subjectPort;
    private final FacultyRepositoryPort facultyPort;
    private final FacultyAssignmentRepositoryPort assignmentPort;
    private final StudentRepositoryPort studentPort;
    private final EnrollmentRepositoryPort enrollmentPort;
    private final AttendancePolicyRepositoryPort policyPort;
    private final SessionRepositoryPort sessionPort;
    private final AttendanceRecordRepositoryPort attendanceRecordPort;

    public DatabaseDataInitializer(
        UserAccountRepositoryPort userAccountPort,
        PasswordEncoder passwordEncoder,
        DepartmentRepositoryPort departmentPort,
        AcademicPeriodRepositoryPort periodPort,
        SectionRepositoryPort sectionPort,
        SubjectRepositoryPort subjectPort,
        FacultyRepositoryPort facultyPort,
        FacultyAssignmentRepositoryPort assignmentPort,
        StudentRepositoryPort studentPort,
        EnrollmentRepositoryPort enrollmentPort,
        AttendancePolicyRepositoryPort policyPort,
        SessionRepositoryPort sessionPort,
        AttendanceRecordRepositoryPort attendanceRecordPort
    ) {
        this.userAccountPort = userAccountPort;
        this.passwordEncoder = passwordEncoder;
        this.departmentPort = departmentPort;
        this.periodPort = periodPort;
        this.sectionPort = sectionPort;
        this.subjectPort = subjectPort;
        this.facultyPort = facultyPort;
        this.assignmentPort = assignmentPort;
        this.studentPort = studentPort;
        this.enrollmentPort = enrollmentPort;
        this.policyPort = policyPort;
        this.sessionPort = sessionPort;
        this.attendanceRecordPort = attendanceRecordPort;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userAccountPort.existsByUsername("admin")) {
            log.debug("AMCS database already initialized. Skipping bootstrap.");
            return;
        }

        log.info("Bootstrapping initial AMCS administrative and demo university dataset...");
        Instant now = Instant.now();

        // 1. Root Administrator
        UUID adminUserId = UUID.randomUUID();
        UserAccount adminAccount = new UserAccount(
            adminUserId,
            "admin",
            "admin@univ.edu",
            passwordEncoder.encode("AdminPassword123!"),
            UserRole.HOD_ADMIN,
            Optional.empty(),
            Optional.empty(),
            UserAccountStatus.ACTIVE,
            0,
            Optional.empty(),
            1,
            now,
            now
        );
        userAccountPort.save(adminAccount);
        log.info("Created root administrator account: admin (HOD_ADMIN)");

        // 2. Department
        UUID deptId = UUID.randomUUID();
        DepartmentEntity dept = new DepartmentEntity(deptId, "CSE", "Computer Science & Engineering");
        departmentPort.save(dept);

        // 3. Academic Period
        UUID periodId = UUID.randomUUID();
        AcademicPeriod period = new AcademicPeriod("Fall 2026", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 20));
        periodPort.save(periodId, period);

        // 4. Section
        UUID sectionId = UUID.randomUUID();
        SectionEntity section = new SectionEntity(sectionId, "CSE-A", deptId, periodId);
        sectionPort.save(section);

        // 5. Subjects
        UUID theorySubId = UUID.randomUUID();
        Subject theorySub = new Subject(theorySubId, "Operating Systems", "CS301", CourseType.THEORY, 3);
        subjectPort.save(theorySub, deptId);

        UUID labSubId = UUID.randomUUID();
        Subject labSub = new Subject(labSubId, "Database Systems Lab", "CS302L", CourseType.LABORATORY, 2);
        subjectPort.save(labSub, deptId);

        // 6. Faculty
        UUID facultyId = UUID.randomUUID();
        FacultyEntity faculty = new FacultyEntity(facultyId, "FAC001", "Dr. Alan Turing", "turing@univ.edu", deptId);
        facultyPort.save(faculty);

        UUID facultyUserId = UUID.randomUUID();
        UserAccount facultyAccount = new UserAccount(
            facultyUserId,
            "faculty1",
            "turing@univ.edu",
            passwordEncoder.encode("FacultyPassword123!"),
            UserRole.FACULTY,
            Optional.empty(),
            Optional.of(facultyId),
            UserAccountStatus.ACTIVE,
            0,
            Optional.empty(),
            1,
            now,
            now
        );
        userAccountPort.save(facultyAccount);
        log.info("Created demo faculty account: faculty1 (FACULTY)");

        // Assign faculty to section & subjects
        assignmentPort.save(new com.amcs.application.port.out.security.FacultyAssignment(
            UUID.randomUUID(), facultyId, theorySubId, sectionId, periodId, true, now));
        assignmentPort.save(new com.amcs.application.port.out.security.FacultyAssignment(
            UUID.randomUUID(), facultyId, labSubId, sectionId, periodId, true, now));

        // 7. Students & Enrollments
        UUID student1Id = UUID.randomUUID();
        StudentEntity student1 = new StudentEntity(student1Id, "REG2026001", "Alice Smith", "alice@univ.edu", deptId);
        studentPort.save(student1);

        UUID student1UserId = UUID.randomUUID();
        UserAccount studentAccount = new UserAccount(
            student1UserId,
            "student1",
            "alice@univ.edu",
            passwordEncoder.encode("StudentPassword123!"),
            UserRole.STUDENT,
            Optional.of(student1Id),
            Optional.empty(),
            UserAccountStatus.ACTIVE,
            0,
            Optional.empty(),
            1,
            now,
            now
        );
        userAccountPort.save(studentAccount);
        log.info("Created demo student account: student1 (STUDENT)");

        UUID student2Id = UUID.randomUUID();
        StudentEntity student2 = new StudentEntity(student2Id, "REG2026002", "Bob Jones", "bob@univ.edu", deptId);
        studentPort.save(student2);

        enrollmentPort.save(new Enrollment(student1Id, sectionId, LocalDate.of(2026, 8, 1), Optional.empty(), Optional.empty()));
        enrollmentPort.save(new Enrollment(student2Id, sectionId, LocalDate.of(2026, 8, 1), Optional.empty(), Optional.empty()));

        // 8. Standard Attendance Policy
        UUID policyId = UUID.randomUUID();
        AttendancePolicy policy = new AttendancePolicy(
            policyId,
            "Standard University Policy",
            1,
            new BigDecimal("75.00"),
            Map.of(
                AttendanceStatus.PRESENT, new BigDecimal("1.0"),
                AttendanceStatus.ABSENT, BigDecimal.ZERO,
                AttendanceStatus.DUTY_LEAVE, new BigDecimal("1.0"),
                AttendanceStatus.MEDICAL_LEAVE, new BigDecimal("1.0"),
                AttendanceStatus.ON_DUTY, new BigDecimal("1.0")
            ),
            MissingRecordStrategy.TREAT_AS_ABSENT,
            Optional.empty(),
            now,
            Optional.empty()
        );
        policyPort.save(policy);

        // 9. Initial Sessions & Attendance Records
        LocalDate today = LocalDate.now();
        LocalDate past1 = today.minusDays(2);
        LocalDate past2 = today.minusDays(1);

        // Session 1: Conducted
        UUID s1Id = UUID.randomUUID();
        Session session1 = new Session(s1Id, theorySubId, sectionId, facultyId, past1, SessionType.THEORY, 1, 1, SessionStatus.CONDUCTED, Optional.empty(), Optional.empty());
        sessionPort.save(session1, periodId);
        attendanceRecordPort.save(new AttendanceRecord(UUID.randomUUID(), s1Id, student1Id, AttendanceStatus.PRESENT));
        attendanceRecordPort.save(new AttendanceRecord(UUID.randomUUID(), s1Id, student2Id, AttendanceStatus.ABSENT));

        // Session 2: Conducted
        UUID s2Id = UUID.randomUUID();
        Session session2 = new Session(s2Id, theorySubId, sectionId, facultyId, past2, SessionType.THEORY, 1, 1, SessionStatus.CONDUCTED, Optional.empty(), Optional.empty());
        sessionPort.save(session2, periodId);
        attendanceRecordPort.save(new AttendanceRecord(UUID.randomUUID(), s2Id, student1Id, AttendanceStatus.PRESENT));
        attendanceRecordPort.save(new AttendanceRecord(UUID.randomUUID(), s2Id, student2Id, AttendanceStatus.PRESENT));

        // Session 3: Scheduled today for immediate roll-call testing
        UUID s3Id = UUID.randomUUID();
        Session session3 = new Session(s3Id, theorySubId, sectionId, facultyId, today, SessionType.THEORY, 1, 0, SessionStatus.SCHEDULED, Optional.empty(), Optional.empty());
        sessionPort.save(session3, periodId);

        log.info("AMCS university bootstrap completed successfully!");
    }
}
