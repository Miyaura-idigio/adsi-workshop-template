package com.example.attendance.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record UpdateRecordRequest(
        @NotNull(message = "出勤時刻は必須です")
        LocalDateTime clockIn,
        LocalDateTime clockOut
) {}
