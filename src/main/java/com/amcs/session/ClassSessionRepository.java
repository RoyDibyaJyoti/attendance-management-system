package com.amcs.session;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {

    Optional<ClassSession> findByTimetableEntryIdAndSessionDate(Long timetableEntryId, LocalDate sessionDate);

    @Query("SELECT cs FROM ClassSession cs WHERE cs.timetableEntry.faculty.id = :facultyId ORDER BY cs.sessionDate DESC, cs.timetableEntry.startTime DESC")
    List<ClassSession> findByFacultyId(@Param("facultyId") Long facultyId);

    List<ClassSession> findBySessionDate(LocalDate sessionDate);

    @Query("SELECT cs FROM ClassSession cs WHERE cs.timetableEntry.faculty.id = :facultyId AND cs.sessionDate = :sessionDate ORDER BY cs.timetableEntry.startTime")
    List<ClassSession> findByFacultyIdAndSessionDate(@Param("facultyId") Long facultyId, @Param("sessionDate") LocalDate sessionDate);

    @Query("SELECT cs FROM ClassSession cs WHERE cs.timetableEntry.section.id = :sectionId ORDER BY cs.sessionDate DESC, cs.timetableEntry.startTime DESC")
    List<ClassSession> findBySectionId(@Param("sectionId") Long sectionId);

    @Query("SELECT cs FROM ClassSession cs WHERE cs.timetableEntry.section.id = :sectionId AND cs.status = 'SUBMITTED' ORDER BY cs.sessionDate, cs.timetableEntry.startTime")
    List<ClassSession> findSubmittedBySection(@Param("sectionId") Long sectionId);

    @Query("""
        SELECT cs FROM ClassSession cs
        WHERE cs.timetableEntry.section.id = :sectionId
          AND cs.status = 'SUBMITTED'
          AND cs.sessionDate BETWEEN :startDate AND :endDate
        ORDER BY cs.sessionDate, cs.timetableEntry.startTime
        """)
    List<ClassSession> findSubmittedBySectionAndDateRange(
            @Param("sectionId") Long sectionId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
        SELECT cs FROM ClassSession cs
        WHERE cs.timetableEntry.section.id = :sectionId
          AND cs.status = 'SUBMITTED'
          AND MONTH(cs.sessionDate) = :month
          AND YEAR(cs.sessionDate) = :year
        ORDER BY cs.sessionDate, cs.timetableEntry.startTime
        """)
    List<ClassSession> findSubmittedBySectionAndMonth(
            @Param("sectionId") Long sectionId,
            @Param("month") int month,
            @Param("year") int year);

    @Query("""
        SELECT cs FROM ClassSession cs
        WHERE cs.timetableEntry.section.id = :sectionId
          AND cs.timetableEntry.subject.id = :subjectId
          AND cs.status = 'SUBMITTED'
        ORDER BY cs.sessionDate
        """)
    List<ClassSession> findSubmittedBySectionAndSubject(
            @Param("sectionId") Long sectionId,
            @Param("subjectId") Long subjectId);
}
