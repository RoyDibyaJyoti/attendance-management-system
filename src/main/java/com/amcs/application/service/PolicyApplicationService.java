package com.amcs.application.service;

import com.amcs.application.dto.policy.AttendancePolicyResponse;
import com.amcs.application.dto.policy.CreateAttendancePolicyRequest;
import com.amcs.application.dto.policy.CreateOverallAttendancePolicyRequest;
import com.amcs.application.dto.policy.OverallAttendancePolicyResponse;
import com.amcs.application.exception.ResourceNotFoundException;
import com.amcs.application.port.out.AttendancePolicyRepositoryPort;
import com.amcs.application.port.out.OverallAttendancePolicyRepositoryPort;
import com.amcs.domain.attendance.AttendanceStatus;
import com.amcs.domain.policy.AttendancePolicy;
import com.amcs.domain.policy.MissingRecordStrategy;
import com.amcs.infrastructure.persistence.entity.OverallAttendancePolicyEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PolicyApplicationService {

    private final AttendancePolicyRepositoryPort policyPort;
    private final OverallAttendancePolicyRepositoryPort overallPolicyPort;
    private final com.amcs.application.security.ApplicationAuthorizationService authorizationService;

    public PolicyApplicationService(
        AttendancePolicyRepositoryPort policyPort,
        OverallAttendancePolicyRepositoryPort overallPolicyPort,
        com.amcs.application.security.ApplicationAuthorizationService authorizationService
    ) {
        this.policyPort = Objects.requireNonNull(policyPort, "policyPort");
        this.overallPolicyPort = Objects.requireNonNull(overallPolicyPort, "overallPolicyPort");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService");
    }

    @Transactional
    public AttendancePolicyResponse createPolicy(CreateAttendancePolicyRequest request) {
        authorizationService.requireAdminOnly("create attendance policies");
        List<AttendancePolicy> existing = policyPort.findAllVersions(request.name().trim());
        int nextVersion = existing.isEmpty() ? 1 : existing.getFirst().version() + 1;

        Map<AttendanceStatus, BigDecimal> contributions = new EnumMap<>(AttendanceStatus.class);
        for (var entry : request.statusContributions().entrySet()) {
            contributions.put(AttendanceStatus.valueOf(entry.getKey()), entry.getValue());
        }

        UUID policyId = UUID.randomUUID();
        AttendancePolicy domain = new AttendancePolicy(
            policyId,
            request.name().trim(),
            nextVersion,
            request.minimumThresholdPercentage(),
            contributions,
            MissingRecordStrategy.valueOf(request.missingRecordStrategy()),
            Optional.empty(),
            request.effectiveFrom(),
            Optional.ofNullable(request.effectiveTo())
        );

        AttendancePolicy saved = policyPort.save(domain);
        return toResponse(saved);
    }

    public AttendancePolicyResponse getPolicyById(UUID id) {
        return policyPort.findById(id)
            .map(this::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Policy not found with ID: " + id));
    }

    public List<AttendancePolicyResponse> listPolicyVersions(String name) {
        return policyPort.findAllVersions(name).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public OverallAttendancePolicyResponse createOverallPolicy(CreateOverallAttendancePolicyRequest request) {
        if (authorizationService != null) {
            authorizationService.requireAdminOnly("create overall attendance policies");
        }
        List<OverallAttendancePolicyEntity> existing = overallPolicyPort.findAllVersions(request.name().trim());
        int nextVersion = existing.isEmpty() ? 1 : existing.getFirst().getVersion() + 1;

        UUID id = UUID.randomUUID();
        OverallAttendancePolicyEntity entity = new OverallAttendancePolicyEntity(
            id,
            request.name().trim(),
            nextVersion,
            request.aggregationStrategy(),
            request.minimumThresholdPercentage(),
            request.effectiveFrom(),
            request.effectiveTo(),
            true
        );

        OverallAttendancePolicyEntity saved = overallPolicyPort.save(entity);
        return new OverallAttendancePolicyResponse(
            saved.getId(), saved.getName(), saved.getVersion(), saved.getAggregationStrategy(),
            saved.getMinimumThresholdPercentage(), saved.getEffectiveFrom(), saved.getEffectiveTo(), saved.isActive());
    }

    private AttendancePolicyResponse toResponse(AttendancePolicy policy) {
        Map<String, BigDecimal> stringContributions = new HashMap<>();
        for (var entry : policy.statusContributions().entrySet()) {
            stringContributions.put(entry.getKey().name(), entry.getValue());
        }

        return new AttendancePolicyResponse(
            policy.id(),
            policy.name(),
            policy.version(),
            policy.minimumThresholdPercentage(),
            stringContributions,
            policy.missingRecordStrategy().name(),
            policy.effectiveFrom(),
            policy.effectiveTo().orElse(null),
            true
        );
    }
}
