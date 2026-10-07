package com.amcs.importer;

import java.util.List;

public class ScheduleImportDto {

    public record AcademicTermDto(
            String name,
            String semester
    ) {}

    public record DepartmentDto(
            String code,
            String name
    ) {}

    public record SectionDto(
            String name,
            String departmentCode,
            Integer year
    ) {}

    public record FacultyDto(
            String name,
            String employeeId,
            String departmentCode,
            String email
    ) {}

    public record SubjectDto(
            String code,
            String name,
            String type, // THEORY | LAB
            Integer credits
    ) {}

    public record LabBatchDto(
            String name,
            String sectionName
    ) {}

    public record TimetableEntryDto(
            String sectionName,
            String subjectCode,
            String facultyEmployeeId,
            String facultyName,
            String dayOfWeek,
            String startTime,
            String endTime,
            String room,
            String labBatchName
    ) {}

    public record ImportRequest(
            AcademicTermDto academicTerm,
            List<DepartmentDto> departments,
            List<SectionDto> sections,
            List<FacultyDto> faculty,
            List<SubjectDto> subjects,
            List<LabBatchDto> labBatches,
            List<TimetableEntryDto> timetableEntries
    ) {}

    public record ImportPreviewResponse(
            boolean canCommit,
            int sectionCount,
            int facultyCount,
            int subjectCount,
            int labBatchCount,
            int timetableEntryCount,
            List<String> warnings,
            List<String> errors,
            List<String> actions
    ) {}

    public record ImportCommitResponse(
            boolean success,
            String message,
            int createdSections,
            int createdFaculty,
            int createdSubjects,
            int createdLabBatches,
            int createdTimetableEntries
    ) {}
}
