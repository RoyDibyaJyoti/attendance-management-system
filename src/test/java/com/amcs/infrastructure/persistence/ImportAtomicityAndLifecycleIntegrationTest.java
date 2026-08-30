package com.amcs.infrastructure.persistence;

import com.amcs.application.exception.ResourceNotFoundException;
import com.amcs.application.port.out.AcademicPeriodRepositoryPort;
import com.amcs.application.port.out.ImportJobRepositoryPort;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Phase 5.7: Import Atomicity, Lifecycle Conflicts & Concurrency Integration Tests")
class ImportAtomicityAndLifecycleIntegrationTest extends PostgresIntegrationTestBase {

    @Autowired private ImportApplicationService importApplicationService;
    @Autowired private ImportJobRepositoryPort importJobRepository;
    @Autowired private ImportStagedRowRepositoryPort stagedRowRepository;
    @Autowired private SpringDataStudentRepository studentRepo;
    @Autowired private SpringDataDepartmentRepository departmentRepo;
    @Autowired private SpringDataSectionRepository sectionRepo;
    @Autowired private SpringDataAcademicPeriodRepository periodRepo;
    @Autowired private SpringDataUserAccountRepository userAccountRepo;

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
        adminAccount.setUsername("admin_life_" + UUID.randomUUID().toString().substring(0, 6));
        adminAccount.setEmail("admin_life_" + UUID.randomUUID().toString().substring(0, 6) + "@univ.edu");
        adminAccount.setPasswordHash("hash");
        adminAccount.setRole(UserRole.HOD_ADMIN.name());
        adminAccount.setStatus("ACTIVE");
        userAccountRepo.save(adminAccount);

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
        departmentRepo.save(new DepartmentEntity(deptId, deptCode, "Computer Science"));

        periodId = UUID.randomUUID();
        periodRepo.save(new AcademicPeriodEntity(
            periodId, "Fall 2026_" + UUID.randomUUID().toString().substring(0, 6), LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 20)
        ));

        sectionId = UUID.randomUUID();
        sectionName = "Sec-A_" + UUID.randomUUID().toString().substring(0, 6);
        sectionRepo.save(new SectionEntity(sectionId, sectionName, deptId, periodId));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Lifecycle: Non-existent job throws ResourceNotFoundException (404)")
    void nonExistentJobThrowsNotFound() {
        UUID bogusJobId = UUID.randomUUID();
        assertThatThrownBy(() -> importApplicationService.commitImport(bogusJobId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("not found");

        assertThatThrownBy(() -> importApplicationService.discardImport(bogusJobId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("Lifecycle: Discarding a staged job purges staged rows and marks DISCARDED")
    void discardStagedJobPurgesRows() throws Exception {
        byte[] file = createStudentXlsx("DiscardTest", "REG_DISC_" + UUID.randomUUID().toString().substring(0, 6));
        ImportSubmissionResult submission = importApplicationService.submitImport(
            new ByteArrayInputStream(file), ImportType.STUDENTS, ImportMode.FAIL_FAST, "discard.xlsx"
        );

        assertThat(stagedRowRepository.findByJobId(submission.jobId())).isNotEmpty();

        importApplicationService.discardImport(submission.jobId());

        assertThat(importJobRepository.findById(submission.jobId()).get().getStatus())
            .isEqualTo(ImportStatus.DISCARDED);
        assertThat(stagedRowRepository.findByJobId(submission.jobId())).isEmpty();

        // Cannot commit discarded job
        assertThatThrownBy(() -> importApplicationService.commitImport(submission.jobId()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("cannot be committed in status: DISCARDED");

        // Discarding already discarded job is idempotent
        ImportSubmissionResult secondDiscard = importApplicationService.discardImport(submission.jobId());
        assertThat(secondDiscard.status()).isEqualTo(ImportStatus.DISCARDED);
    }

    @Test
    @DisplayName("Lifecycle: Double commit or discard of committed job throws IllegalStateException (409)")
    void doubleCommitOrDiscardThrowsConflict() throws Exception {
        String reg = "REG_DBL_" + UUID.randomUUID().toString().substring(0, 6);
        byte[] file = createStudentXlsx("DoubleCommitTest", reg);
        ImportSubmissionResult submission = importApplicationService.submitImport(
            new ByteArrayInputStream(file), ImportType.STUDENTS, ImportMode.FAIL_FAST, "double.xlsx"
        );

        // First commit succeeds
        ImportCommitResult commitResult = importApplicationService.commitImport(submission.jobId());
        assertThat(commitResult.status()).isEqualTo(ImportStatus.COMMITTED);

        // Second commit fails
        assertThatThrownBy(() -> importApplicationService.commitImport(submission.jobId()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("cannot be committed in status: COMMITTED");

        // Discard of committed job fails
        assertThatThrownBy(() -> importApplicationService.discardImport(submission.jobId()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already committed");
    }

    @Test
    @DisplayName("Concurrency: Concurrent commit attempts execute safely with exactly one winner")
    void concurrentCommitExecutesSafely() throws Exception {
        String reg = "REG_RACE_" + UUID.randomUUID().toString().substring(0, 6);
        byte[] file = createStudentXlsx("RaceTest", reg);
        ImportSubmissionResult submission = importApplicationService.submitImport(
            new ByteArrayInputStream(file), ImportType.STUDENTS, ImportMode.FAIL_FAST, "race.xlsx"
        );

        UUID jobId = submission.jobId();
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    // Set auth in thread context
                    SecurityUserPrincipal p = new SecurityUserPrincipal(
                        adminUserId, "admin", "admin@univ.edu", UserRole.HOD_ADMIN,
                        Optional.empty(), Optional.empty(), 1
                    );
                    SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(p, null, p.getAuthorities())
                    );

                    startLatch.await();
                    importApplicationService.commitImport(jobId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        finishLatch.await();
        executor.shutdown();

        // Exactly one commit succeeds, the other fails due to lifecycle conflict or locking
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);

        // Permanent student record is not duplicated
        assertThat(studentRepo.findByRegistrationNumber(reg)).isPresent();
    }

    private byte[] createStudentXlsx(String name, String regNo) throws Exception {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Students");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Registration Number");
            header.createCell(1).setCellValue("Full Name");
            header.createCell(2).setCellValue("Email");
            header.createCell(3).setCellValue("Department Code");
            header.createCell(4).setCellValue("Section Name");

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(regNo);
            row.createCell(1).setCellValue(name);
            row.createCell(2).setCellValue(regNo + "@univ.edu");
            row.createCell(3).setCellValue(deptCode);
            row.createCell(4).setCellValue(sectionName);

            wb.write(baos);
            return baos.toByteArray();
        }
    }
}
