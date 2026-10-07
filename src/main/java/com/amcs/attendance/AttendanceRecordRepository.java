package com.amcs.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    List<AttendanceRecord> findBySessionId(Long sessionId);

    List<AttendanceRecord> findBySessionIdIn(List<Long> sessionIds);

    Optional<AttendanceRecord> findBySessionIdAndStudentId(Long sessionId, Long studentId);

    boolean existsBySessionId(Long sessionId);

    void deleteBySessionId(Long sessionId);

    @Query("SELECT ar FROM AttendanceRecord ar WHERE ar.session.timetableEntry.section.id = :sectionId AND ar.session.status = 'SUBMITTED'")
    List<AttendanceRecord> findSubmittedBySectionId(@Param("sectionId") Long sectionId);

    @Query("""
        SELECT ar FROM AttendanceRecord ar
        WHERE ar.session.timetableEntry.section.id = :sectionId
          AND ar.session.status = 'SUBMITTED'
          AND ar.session.sessionDate BETWEEN :startDate AND :endDate
        """)
    List<AttendanceRecord> findSubmittedBySectionIdAndDateRange(
            @Param("sectionId") Long sectionId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
