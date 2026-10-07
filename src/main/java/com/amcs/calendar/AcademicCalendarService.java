package com.amcs.calendar;

import com.amcs.holiday.Holiday;
import com.amcs.holiday.HolidayRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class AcademicCalendarService {

    private final HolidayRepository holidayRepository;

    public AcademicCalendarService(HolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;
    }

    public record DayStatus(
            @com.fasterxml.jackson.annotation.JsonProperty("isNonWorkingDay")
            boolean isNonWorkingDay,
            String holidayType, // WEEKLY, INSTITUTIONAL, DECLARED, or null
            String reason
    ) {
        @com.fasterxml.jackson.annotation.JsonProperty("nonWorkingDay")
        public boolean getNonWorkingDay() {
            return isNonWorkingDay;
        }
    }

    /**
     * Checks if a given date is the second Saturday of its month.
     * The first Saturday of any month is in range [1, 7].
     * The second Saturday is always in range [8, 14].
     */
    public static boolean isSecondSaturday(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY &&
                date.getDayOfMonth() >= 8 &&
                date.getDayOfMonth() <= 14;
    }

    public static boolean isSunday(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    public DayStatus getDayStatus(LocalDate date) {
        if (isSunday(date)) {
            return new DayStatus(true, "WEEKLY", "Sunday — Weekly Holiday");
        }
        if (isSecondSaturday(date)) {
            return new DayStatus(true, "INSTITUTIONAL", "Second Saturday — Institutional Holiday");
        }
        Optional<Holiday> holiday = holidayRepository.findByDate(date);
        if (holiday.isPresent()) {
            return new DayStatus(true, "DECLARED", "Official Holiday: " + holiday.get().getName());
        }
        return new DayStatus(false, null, null);
    }

    public boolean isNonWorkingDay(LocalDate date) {
        return getDayStatus(date).isNonWorkingDay();
    }
}
