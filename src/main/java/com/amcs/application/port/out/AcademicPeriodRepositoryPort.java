package com.amcs.application.port.out;

import com.amcs.domain.academic.AcademicPeriod;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound repository port for AcademicPeriod domain entity.
 */
public interface AcademicPeriodRepositoryPort {
    AcademicPeriod save(UUID id, AcademicPeriod period);
    Optional<AcademicPeriod> findById(UUID id);
    Optional<AcademicPeriod> findByName(String name);
    List<AcademicPeriod> findAll();
}
