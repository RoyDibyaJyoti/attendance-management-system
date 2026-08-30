package com.amcs.infrastructure.persistence.mapper;

import com.amcs.domain.attendance.AttendanceStatus;
import com.amcs.domain.policy.AttendancePolicy;
import com.amcs.domain.policy.MissingRecordStrategy;
import com.amcs.infrastructure.persistence.entity.AttendancePolicyEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

@Component
public class PolicyPersistenceMapper {

    private final ObjectMapper objectMapper;

    public PolicyPersistenceMapper() {
        this.objectMapper = new ObjectMapper();
    }

    public AttendancePolicy toDomain(AttendancePolicyEntity entity) {
        if (entity == null) return null;

        Map<AttendanceStatus, BigDecimal> contributions;
        try {
            Map<String, String> rawMap = objectMapper.readValue(
                entity.getStatusContributionsJson(),
                new TypeReference<>() {}
            );
            contributions = new EnumMap<>(AttendanceStatus.class);
            for (var entry : rawMap.entrySet()) {
                contributions.put(
                    AttendanceStatus.valueOf(entry.getKey()),
                    new BigDecimal(entry.getValue())
                );
            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse status_contributions_json for policy: " + entity.getId(), e);
        }

        MissingRecordStrategy missingStrategy = entity.getMissingRecordStrategy() != null
            ? MissingRecordStrategy.valueOf(entity.getMissingRecordStrategy())
            : MissingRecordStrategy.TREAT_AS_ABSENT;

        return new AttendancePolicy(
            entity.getId(),
            entity.getName(),
            entity.getVersion(),
            entity.getMinimumThresholdPercentage(),
            contributions,
            missingStrategy,
            Optional.empty(),
            entity.getEffectiveFrom(),
            Optional.ofNullable(entity.getEffectiveTo())
        );
    }

    public AttendancePolicyEntity toEntity(AttendancePolicy domain, boolean active) {
        if (domain == null) return null;

        Map<String, String> rawMap = new java.util.LinkedHashMap<>();
        for (var entry : domain.statusContributions().entrySet()) {
            rawMap.put(entry.getKey().name(), entry.getValue().toPlainString());
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(rawMap);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize statusContributions to JSON", e);
        }

        return new AttendancePolicyEntity(
            domain.id(),
            domain.name(),
            domain.version(),
            domain.minimumThresholdPercentage(),
            json,
            domain.missingRecordStrategy().name(),
            domain.effectiveFrom(),
            domain.effectiveTo().orElse(null),
            active
        );
    }
}
