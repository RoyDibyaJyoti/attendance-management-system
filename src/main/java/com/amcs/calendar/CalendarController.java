package com.amcs.calendar;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/calendar")
public class CalendarController {

    private final AcademicCalendarService calendarService;

    public CalendarController(AcademicCalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @GetMapping("/status")
    public ResponseEntity<AcademicCalendarService.DayStatus> getDayStatus(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate target = (date != null) ? date : LocalDate.now();
        return ResponseEntity.ok(calendarService.getDayStatus(target));
    }
}
