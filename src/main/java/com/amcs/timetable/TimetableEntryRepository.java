package com.amcs.timetable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {

    List<TimetableEntry> findByFacultyIdAndActiveTrue(Long facultyId);

    List<TimetableEntry> findBySectionIdAndActiveTrue(Long sectionId);

    @Query("SELECT t FROM TimetableEntry t WHERE t.faculty.id = :facultyId AND t.active = true AND t.dayOfWeek = :day ORDER BY t.startTime")
    List<TimetableEntry> findByFacultyAndDay(@Param("facultyId") Long facultyId, @Param("day") String day);

    @Query("SELECT t FROM TimetableEntry t WHERE t.active = true ORDER BY t.section.name, t.dayOfWeek, t.startTime")
    List<TimetableEntry> findAllActive();
}
