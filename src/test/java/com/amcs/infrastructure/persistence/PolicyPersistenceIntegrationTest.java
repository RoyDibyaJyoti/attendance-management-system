package com.amcs.infrastructure.persistence;

import com.amcs.domain.attendance.AttendanceStatus;
import com.amcs.domain.policy.AttendancePolicy;
import com.amcs.domain.policy.MissingRecordStrategy;
import com.amcs.infrastructure.persistence.adapter.AttendancePolicyPersistenceAdapter;
import com.amcs.infrastructure.persistence.entity.AttendancePolicyEntity;
import com.amcs.infrastructure.persistence.repository.SpringDataAttendancePolicyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Policy Persistence Integration Tests")
class PolicyPersistenceIntegrationTest extends PostgresIntegrationTestBase {

    @Autowired
    private AttendancePolicyPersistenceAdapter policyAdapter;

    @Autowired
    private SpringDataAttendancePolicyRepository policyRepository;

    @Test
    @Transactional
    @DisplayName("PO-01: Save and retrieve policy through adapter with full JSONB contributions")
    void saveAndRetrievePolicyThroughAdapter() {
        UUID policyId = UUID.randomUUID();
        String policyName = "Standard_75_" + UUID.randomUUID().toString().substring(0, 6);

        Map<AttendanceStatus, BigDecimal> contributions = Map.of(
            AttendanceStatus.PRESENT, BigDecimal.ONE,
            AttendanceStatus.ABSENT, BigDecimal.ZERO,
            AttendanceStatus.DUTY_LEAVE, BigDecimal.ONE,
            AttendanceStatus.MEDICAL_LEAVE, new BigDecimal("0.50"),
            AttendanceStatus.ON_DUTY, BigDecimal.ONE
        );

        AttendancePolicy domainPolicy = new AttendancePolicy(
            policyId, policyName, 1, new BigDecimal("75.00"),
            contributions, MissingRecordStrategy.MARK_AS_INCOMPLETE,
            Optional.empty(), Instant.now(), Optional.empty());

        AttendancePolicy saved = policyAdapter.save(domainPolicy);
        assertThat(saved.id()).isEqualTo(policyId);

        Optional<AttendancePolicy> retrieved = policyAdapter.findById(policyId);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().name()).isEqualTo(policyName);
        assertThat(retrieved.get().version()).isEqualTo(1);
        assertThat(retrieved.get().minimumThresholdPercentage()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(retrieved.get().missingRecordStrategy()).isEqualTo(MissingRecordStrategy.MARK_AS_INCOMPLETE);
        assertThat(retrieved.get().getContribution(AttendanceStatus.MEDICAL_LEAVE)).isEqualByComparingTo(new BigDecimal("0.50"));
    }

    @Test
    @DisplayName("PO-02: Duplicate policy (name, version) is rejected by database unique constraint")
    void duplicatePolicyVersion_rejected() {
        String policyName = "Unique_Version_Policy_" + UUID.randomUUID().toString().substring(0, 6);

        AttendancePolicyEntity p1 = new AttendancePolicyEntity(
            UUID.randomUUID(), policyName, 1, new BigDecimal("75.00"),
            "{\"PRESENT\":\"1.0\",\"ABSENT\":\"0.0\"}", "TREAT_AS_ABSENT",
            Instant.now(), null, true);
        policyRepository.saveAndFlush(p1);

        AttendancePolicyEntity p2 = new AttendancePolicyEntity(
            UUID.randomUUID(), policyName, 1, new BigDecimal("80.00"),
            "{\"PRESENT\":\"1.0\",\"ABSENT\":\"0.0\"}", "TREAT_AS_ABSENT",
            Instant.now(), null, true);

        assertThatThrownBy(() -> policyRepository.saveAndFlush(p2))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
