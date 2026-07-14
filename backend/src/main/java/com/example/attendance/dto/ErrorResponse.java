package com.example.attendance.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        int status,
        String message,
        List<FieldError> errors,
        LocalDateTime timestamp
) {
    public record FieldError(String field, String message) {}

    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(status, message, List.of(), LocalDateTime.now());
    }

    public static ErrorResponse of(int status, String message, List<FieldError> errors) {
        return new ErrorResponse(status, message, errors, LocalDateTime.now());
    }
}
