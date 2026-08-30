package com.amcs.infrastructure.web.error;

public record ValidationErrorDetail(
    String field,
    Object rejectedValue,
    String message
) {}
