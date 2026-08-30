package com.amcs.application.dto.labgroup;

import java.time.LocalDate;
import java.util.UUID;

public record LabGroupMembershipResponse(
    UUID id,
    UUID studentId,
    UUID labGroupId,
    LocalDate effectiveStart,
    LocalDate effectiveEnd
) {}
