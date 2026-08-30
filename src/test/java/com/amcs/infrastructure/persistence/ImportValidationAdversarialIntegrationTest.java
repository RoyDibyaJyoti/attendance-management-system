package com.amcs.infrastructure.persistence;

import com.amcs.application.port.out.DepartmentRepositoryPort;
import com.amcs.application.port.out.EnrollmentRepositoryPort;
import com.amcs.application.port.out.ImportJobRepositoryPort;
import com.amcs.application.port.out.SectionRepositoryPort;
import com.amcs.application.port.out.StudentRepositoryPort;
import com.amcs.application.port.out.importer.ImportStagedRowRepositoryPort;
import com.amcs.application.port.out.security.UserRole;
import com.amcs.application.service.ImportApplicationService;
import com.amcs.application.service.importer.dto.ImportCommitResult;
import com.amcs.application.service.importer.dto.ImportSubmissionResult;
import com.amcs.domain.importer.ImportMode;
import com.amcs.domain.importer.ImportStatus;
import com.amcs.domain.importer.ImportType;
import com.amcs.infrastructure.persistence.entity.AcademicPeriodEntity;
import com.amcs.infrastructure.persistence.entity.DepartmentEntity;
import com.amcs.infrastructure.persistence.entity.SectionEntity;
import com.amcs.infrastructure.persistence.entity.StudentEntity;
import com.amcs.infrastructure.persistence.entity.UserAccountEntity;
import com.amcs.infrastructure.persistence.repository.SpringDataAcademicPeriodRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataDepartmentRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataSectionRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataStudentRepository;
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

@DisplayName("Phase 5.7: Import Validation & Business Rules Adversarial Integration Tests")
class ImportValidationAdversarialIntegrationTest extends PostgresIntegrationTestBase {

    @Autowired private ImportApplicationService importApplicationService;
    @Autowired private ImportJobRepositoryPort importJobRepository;
    @Autowired private ImportStagedRowRepositoryPort stagedRowRepository;
    @Autowired private SpringDataStudentRepository springDataStudentRepo;
    @Autowired private SpringDataDepartmentRepository springDataDepartmentRepo;
    @Autowired private SpringDataSectionRepository springDataSectionRepo;
    @Autowired private SpringDataAcademicPeriodRepository springDataAcademicPeriodRepo;
    @Autowired private SpringDataUserAccountRepository userAccountRepository;

    private UUID adminUserId;
    private UUID deptId;
    private String deptCode;
    private UUID periodId;
    private UUID sectionId;
    private String sectionName;

    @BeforeEach
    void setUp() {
        adminUserId = UUID.randomUUID();
        UserAccountEntity adminAccount = new UserAccountEntity();
        adminAccount.setId(adminUserId);
        adminAccount.setUsername("admin_adv_" + UUID.randomUUID().toString().substring(0, 6));
        adminAccount.setEmail("admin_adv_" + UUID.randomUUID().toString().substring(0, 6) + "@univ.edu");
        adminAccount.setPasswordHash("hash");
        adminAccount.setRole(UserRole.HOD_ADMIN.name());
        adminAccount.setStatus("ACTIVE");
        userAccountRepository.save(adminAccount);

        SecurityUserPrincipal principal = new SecurityUserPrincipal(
            adminUserId,
            adminAccount.getUsername(),
            adminAccount.getEmail(),
            UserRole.HOD_ADMIN,
            Optional.empty(),
            Optional.empty(),
            1
        );
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            principal, null, principal.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        deptId = UUID.randomUUID();
        deptCode = "CSE_" + UUID.randomUUID().toString().substring(0, 6);
        springDataDepartmentRepo.save(new DepartmentEntity(deptId, deptCode, "Computer Science"));

        periodId = UUID.randomUUID();
        springDataAcademicPeriodRepo.save(new AcademicPeriodEntity(
            periodId, "Fall 2026_" + UUID.randomUUID().toString().substring(0, 6), LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 20)
        ));

        sectionId = UUID.randomUUID();
        sectionName = "Sec-A_" + UUID.randomUUID().toString().substring(0, 6);
        springDataSectionRepo.save(new SectionEntity(sectionId, sectionName, deptId, periodId));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("FAIL_FAST: Intra-file duplicate registration numbers reject the entire import")
    void failFastRejectsIntraFileDuplicates() throws Exception {
        String duplicateReg = "REG_DUP_" + UUID.randomUUID().toString().substring(0, 6);
        byte[] fileBytes = createStudentXlsx(List.of(
            new StudentRowData("Alice", duplicateReg, "alice@univ.edu", deptCode, sectionName),
            new StudentRowData("Bob", duplicateReg, "bob@univ.edu", deptCode, sectionName)
        ));

        ImportSubmissionResult result = importApplicationService.submitImport(
            new ByteArrayInputStream(fileBytes), ImportType.STUDENTS, ImportMode.FAIL_FAST, "students_dup.xlsx"
        );

        assertThat(result.status()).isEqualTo(ImportStatus.REJECTED);
        assertThat(result.invalidRows()).isGreaterThanOrEqualTo(1);

        // Verify staged rows are purged in FAIL_FAST
        int stagedCount = stagedRowRepository.findByJobId(result.jobId()).size();
        assertThat(stagedCount).isEqualTo(0);

        // Verify commit is rejected
        assertThatThrownBy(() -> importApplicationService.commitImport(result.jobId()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("cannot be committed in status: REJECTED");

        // Verify no permanent student entity was persisted
        assertThat(springDataStudentRepo.findByRegistrationNumber(duplicateReg)).isEmpty();
    }

    @Test
    @DisplayName("FAIL_FAST: Duplicate registration number against database rejects the entire import")
    void failFastRejectsDatabaseDuplicate() throws Exception {
        String existingReg = "REG_EXISTING_" + UUID.randomUUID().toString().substring(0, 6);
        springDataStudentRepo.save(new StudentEntity(
            UUID.randomUUID(), existingReg, "Existing Student", existingReg + "@univ.edu", deptId
        ));

        byte[] fileBytes = createStudentXlsx(List.of(
            new StudentRowData("New Student", existingReg, "new@univ.edu", deptCode, sectionName)
        ));

        ImportSubmissionResult result = importApplicationService.submitImport(
            new ByteArrayInputStream(fileBytes), ImportType.STUDENTS, ImportMode.FAIL_FAST, "students_db_dup.xlsx"
        );

        assertThat(result.status()).isEqualTo(ImportStatus.REJECTED);
        assertThat(result.errors()).isNotEmpty();
        assertThat(result.errors().get(0).errorMessage()).contains("already exists with registration number");
    }

    @Test
    @DisplayName("PARTIAL_COMMIT: Commits only valid rows while recording diagnostics for invalid rows")
    void partialCommitStagesValidRowsAndPersistsThem() throws Exception {
        String validReg1 = "REG_VALID_1_" + UUID.randomUUID().toString().substring(0, 6);
        String invalidEmail = "not_an_email";
        String validReg2 = "REG_VALID_2_" + UUID.randomUUID().toString().substring(0, 6);

        byte[] fileBytes = createStudentXlsx(List.of(
            new StudentRowData("Student One", validReg1, "one@univ.edu", deptCode, sectionName),
            new StudentRowData("Invalid Student", "REG_INV", invalidEmail, deptCode, sectionName),
            new StudentRowData("Student Two", validReg2, "two@univ.edu", deptCode, sectionName)
        ));

        ImportSubmissionResult submission = importApplicationService.submitImport(
            new ByteArrayInputStream(fileBytes), ImportType.STUDENTS, ImportMode.PARTIAL_COMMIT, "students_partial.xlsx"
        );

        assertThat(submission.status()).isEqualTo(ImportStatus.STAGED_PARTIAL);
        assertThat(submission.validRows()).isEqualTo(2);
        assertThat(submission.invalidRows()).isEqualTo(1);

        // Commit the valid rows
        ImportCommitResult commit = importApplicationService.commitImport(submission.jobId());
        assertThat(commit.committedRows()).isEqualTo(2);
        assertThat(commit.status()).isEqualTo(ImportStatus.COMMITTED);

        // Verify only the 2 valid students exist permanently in the database
        assertThat(springDataStudentRepo.findByRegistrationNumber(validReg1)).isPresent();
        assertThat(springDataStudentRepo.findByRegistrationNumber(validReg2)).isPresent();
    }

    @Test
    @DisplayName("FAIL_FAST: Rejects import referencing non-existent department code")
    void rejectsNonexistentDepartment() throws Exception {
        String bogusDept = "NONEXISTENT_DEPT";
        String regNo = "REG_BOGUS_" + UUID.randomUUID().toString().substring(0, 6);

        byte[] fileBytes = createStudentXlsx(List.of(
            new StudentRowData("Bogus Student", regNo, "bogus@univ.edu", bogusDept, sectionName)
        ));

        ImportSubmissionResult result = importApplicationService.submitImport(
            new ByteArrayInputStream(fileBytes), ImportType.STUDENTS, ImportMode.FAIL_FAST, "bogus_dept.xlsx"
        );

        assertThat(result.status()).isEqualTo(ImportStatus.REJECTED);
        assertThat(result.errors()).isNotEmpty();
        assertThat(result.errors().get(0).errorMessage()).contains("Department not found");
        assertThat(springDataStudentRepo.findByRegistrationNumber(regNo)).isEmpty();
    }

    private record StudentRowData(String name, String regNo, String email, String dept, String section) {}

    private byte[] createStudentXlsx(List<StudentRowData> rows) throws Exception {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Students");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Registration Number");
            header.createCell(1).setCellValue("Full Name");
            header.createCell(2).setCellValue("Email");
            header.createCell(3).setCellValue("Department Code");
            header.createCell(4).setCellValue("Section Name");

            int r = 1;
            for (StudentRowData data : rows) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(data.regNo());
                row.createCell(1).setCellValue(data.name());
                row.createCell(2).setCellValue(data.email());
                row.createCell(3).setCellValue(data.dept());
                row.createCell(4).setCellValue(data.section());
            }
            wb.write(baos);
            return baos.toByteArray();
        }
    }
}
