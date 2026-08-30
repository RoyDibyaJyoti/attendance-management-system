package com.amcs.infrastructure.persistence;

import com.amcs.application.port.out.AcademicPeriodRepositoryPort;
import com.amcs.application.port.out.DepartmentRepositoryPort;
import com.amcs.application.port.out.EnrollmentRepositoryPort;
import com.amcs.application.port.out.ImportJobRepositoryPort;
import com.amcs.application.port.out.SectionRepositoryPort;
import com.amcs.application.port.out.StudentRepositoryPort;
import com.amcs.application.port.out.excel.ExcelWorkbookGeneratorPort;
import com.amcs.application.port.out.importer.ImportStagedRowRepositoryPort;
import com.amcs.application.port.out.security.UserRole;
import com.amcs.application.service.ImportApplicationService;
import com.amcs.application.service.importer.dto.ImportCommitResult;
import com.amcs.application.service.importer.dto.ImportSubmissionResult;
import com.amcs.domain.academic.AcademicPeriod;
import com.amcs.domain.importer.ImportJob;
import com.amcs.domain.importer.ImportMode;
import com.amcs.domain.importer.ImportStatus;
import com.amcs.domain.importer.ImportType;
import com.amcs.infrastructure.persistence.entity.DepartmentEntity;
import com.amcs.infrastructure.persistence.entity.SectionEntity;
import com.amcs.infrastructure.persistence.entity.StudentEntity;
import com.amcs.infrastructure.persistence.entity.UserAccountEntity;
import com.amcs.infrastructure.persistence.repository.SpringDataUserAccountRepository;
import com.amcs.infrastructure.security.principal.SecurityUserPrincipal;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Import Staging Pipeline PostgreSQL Integration Tests")
class ImportStagingPipelineIntegrationTest extends PostgresIntegrationTestBase {

    @Autowired
    private ImportApplicationService importApplicationService;

    @Autowired
    private ImportJobRepositoryPort importJobRepository;

    @Autowired
    private ImportStagedRowRepositoryPort stagedRowRepository;

    @Autowired
    private StudentRepositoryPort studentRepository;

    @Autowired
    private EnrollmentRepositoryPort enrollmentRepository;

    @Autowired
    private DepartmentRepositoryPort departmentRepository;

    @Autowired
    private AcademicPeriodRepositoryPort academicPeriodRepository;

    @Autowired
    private SectionRepositoryPort sectionRepository;

    @Autowired
    private SpringDataUserAccountRepository userAccountRepository;

    private UUID adminUserId;
    private UUID deptId;
    private UUID periodId;
    private UUID sectionId;
    private String deptCode;
    private String sectionName;

    @BeforeEach
    void setUpDatabase() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        deptCode = "DEPT_" + suffix;
        sectionName = "SEC_" + suffix;

        adminUserId = UUID.randomUUID();
        UserAccountEntity user = new UserAccountEntity(
            adminUserId,
            "import_admin_" + suffix,
            "admin_" + suffix + "@univ.edu",
            "$2a$12$DummyPasswordHashForImportTests1234567890",
            "HOD_ADMIN",
            null,
            null,
            "ACTIVE",
            0,
            null,
            1
        );
        userAccountRepository.save(user);

        SecurityUserPrincipal principal = new SecurityUserPrincipal(
            adminUserId,
            user.getUsername(),
            user.getEmail(),
            UserRole.HOD_ADMIN,
            Optional.empty(),
            Optional.empty(),
            1
        );
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        deptId = UUID.randomUUID();
        departmentRepository.save(new DepartmentEntity(deptId, deptCode, "Computer Science " + suffix));

        periodId = UUID.randomUUID();
        academicPeriodRepository.save(
            periodId,
            new AcademicPeriod("Fall 2026 " + suffix, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 31))
        );

        sectionId = UUID.randomUUID();
        sectionRepository.save(new SectionEntity(sectionId, sectionName, deptId, periodId));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private byte[] createStudentSpreadsheet(List<List<String>> dataRows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Students");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Registration Number");
            header.createCell(1).setCellValue("Full Name");
            header.createCell(2).setCellValue("Email");
            header.createCell(3).setCellValue("Department Code");
            header.createCell(4).setCellValue("Section Name");

            for (int i = 0; i < dataRows.size(); i++) {
                Row row = sheet.createRow(i + 1);
                List<String> values = dataRows.get(i);
                for (int c = 0; c < values.size(); c++) {
                    row.createCell(c).setCellValue(values.get(c));
                }
            }

            workbook.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate test spreadsheet", e);
        }
    }

    @Test
    @DisplayName("End-to-End: Submit, Stage Partial, and Commit creates entities in PostgreSQL")
    void shouldStagePartialAndCommitToPostgres() {
        String regNo1 = "REG_" + UUID.randomUUID().toString().substring(0, 8);
        String regNo2 = "REG_" + UUID.randomUUID().toString().substring(0, 8);

        byte[] xlsx = createStudentSpreadsheet(List.of(
            List.of(regNo1, "Alice Good", "alice_" + regNo1 + "@univ.edu", deptCode, sectionName),
            List.of(regNo2, "Bob BadEmail", "invalid_email_format", deptCode, sectionName)
        ));

        ImportSubmissionResult submission = importApplicationService.submitImport(
            new ByteArrayInputStream(xlsx),
            ImportType.STUDENTS,
            ImportMode.PARTIAL_COMMIT,
            "students_batch.xlsx"
        );

        assertThat(submission.status()).isEqualTo(ImportStatus.STAGED_PARTIAL);
        assertThat(submission.totalRows()).isEqualTo(2);
        assertThat(submission.validRows()).isEqualTo(1);
        assertThat(submission.invalidRows()).isEqualTo(1);
        assertThat(submission.isCommittable()).isTrue();

        // Verify PostgreSQL staging persistence
        long stagedCount = stagedRowRepository.countByJobId(submission.jobId());
        assertThat(stagedCount).isEqualTo(1);

        Optional<ImportJob> persistedJobOpt = importJobRepository.findById(submission.jobId());
        assertThat(persistedJobOpt).isPresent();
        assertThat(persistedJobOpt.get().getStatus()).isEqualTo(ImportStatus.STAGED_PARTIAL);
        assertThat(persistedJobOpt.get().getErrors()).hasSize(1);

        // Commit valid staged rows
        ImportCommitResult commitResult = importApplicationService.commitImport(submission.jobId());

        assertThat(commitResult.status()).isEqualTo(ImportStatus.COMMITTED);
        assertThat(commitResult.committedRows()).isEqualTo(1);

        // Verify entity persisted in PostgreSQL
        Optional<StudentEntity> studentInDb = studentRepository.findByRegistrationNumber(regNo1);
        assertThat(studentInDb).isPresent();
        assertThat(studentInDb.get().getName()).isEqualTo("Alice Good");

        // Verify enrollment persisted
        boolean isEnrolled = enrollmentRepository.findActiveEnrollment(studentInDb.get().getId(), sectionId).isPresent();
        assertThat(isEnrolled).isTrue();

        // Verify staged rows purged after commit
        assertThat(stagedRowRepository.countByJobId(submission.jobId())).isZero();
    }

    @Test
    @DisplayName("FAIL_FAST with errors rejects job and leaves 0 staged rows in PostgreSQL")
    void shouldRejectUnderFailFastAndPurgeStagedRows() {
        String regNo1 = "REG_" + UUID.randomUUID().toString().substring(0, 8);
        String regNo2 = "REG_" + UUID.randomUUID().toString().substring(0, 8);

        byte[] xlsx = createStudentSpreadsheet(List.of(
            List.of(regNo1, "Valid Student", "valid_" + regNo1 + "@univ.edu", deptCode, sectionName),
            List.of(regNo2, "Invalid Dept Student", "dept_bad@univ.edu", "NON_EXISTENT_DEPT", sectionName)
        ));

        ImportSubmissionResult submission = importApplicationService.submitImport(
            new ByteArrayInputStream(xlsx),
            ImportType.STUDENTS,
            ImportMode.FAIL_FAST,
            "students_fail_fast.xlsx"
        );

        assertThat(submission.status()).isEqualTo(ImportStatus.REJECTED);
        assertThat(submission.isCommittable()).isFalse();

        // 0 staged rows in PostgreSQL
        assertThat(stagedRowRepository.countByJobId(submission.jobId())).isZero();

        // Cannot commit rejected job
        assertThatThrownBy(() -> importApplicationService.commitImport(submission.jobId()))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Discarding staged job purges staged rows from PostgreSQL")
    void shouldDiscardStagedJobAndPurgeRows() {
        String regNo = "REG_" + UUID.randomUUID().toString().substring(0, 8);

        byte[] xlsx = createStudentSpreadsheet(List.of(
            List.of(regNo, "Clean Student", "clean_" + regNo + "@univ.edu", deptCode, sectionName)
        ));

        ImportSubmissionResult submission = importApplicationService.submitImport(
            new ByteArrayInputStream(xlsx),
            ImportType.STUDENTS,
            ImportMode.PARTIAL_COMMIT,
            "students_discard.xlsx"
        );

        assertThat(submission.status()).isEqualTo(ImportStatus.STAGED_CLEAN);
        assertThat(stagedRowRepository.countByJobId(submission.jobId())).isEqualTo(1);

        ImportSubmissionResult discarded = importApplicationService.discardImport(submission.jobId());

        assertThat(discarded.status()).isEqualTo(ImportStatus.DISCARDED);
        assertThat(stagedRowRepository.countByJobId(submission.jobId())).isZero();
    }
}
