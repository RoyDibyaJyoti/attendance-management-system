package com.amcs.domain.academic;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents a bounded academic period — e.g., a semester or term.
 *
 * <p>Sessions and enrollment records are filtered against this period's date range
 * when calculating attendance. Both {@code startDate} and {@code endDate} are inclusive.
 *
 * <p>REQUIRES INSTITUTIONAL CONFIRMATION (RIC-RP-001): The granularity of reporting
 * periods (semester vs. term vs. year vs. custom) must be confirmed by the institution.
 * This class is deliberately generic to support any date-bounded period.
 */
public record AcademicPeriod(String name, LocalDate startDate, LocalDate endDate) {

    public AcademicPeriod {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(startDate, "startDate must not be null");
        Objects.requireNonNull(endDate, "endDate must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                "endDate [%s] must not be before startDate [%s]".formatted(endDate, startDate));
        }
    }

    /**
     * Returns {@code true} if the given date falls within this period (inclusive on both ends).
     */
    public boolean contains(LocalDate date) {
        Objects.requireNonNull(date, "date must not be null");
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }
}
