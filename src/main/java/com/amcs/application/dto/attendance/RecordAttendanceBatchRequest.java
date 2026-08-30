package com.amcs.application.dto.attendance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record RecordAttendanceBatchRequest(
    @NotEmpty(message = "Attendance records list must not be empty")
    @Valid
    List<AttendanceRecordItemDto> records
) {}
