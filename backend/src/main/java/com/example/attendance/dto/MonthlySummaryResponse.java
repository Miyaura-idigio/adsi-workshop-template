package com.example.attendance.dto;

import java.time.YearMonth;

public record MonthlySummaryResponse(
        String yearMonth,
        int totalWorkingDays,
        int totalActualMinutes,
        int totalOvertimeMinutes
) {}
