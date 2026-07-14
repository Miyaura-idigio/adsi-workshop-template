package com.example.attendance.domain;

import java.time.Duration;
import java.time.LocalDateTime;

public record WorkDuration(LocalDateTime clockIn, LocalDateTime clockOut) {

    private static final int STANDARD_WORK_MINUTES = 480;
    private static final int BREAK_THRESHOLD_6H = 360;
    private static final int BREAK_THRESHOLD_8H = 480;
    private static final int BREAK_MINUTES_45 = 45;
    private static final int BREAK_MINUTES_60 = 60;

    public int totalMinutes() {
        if (clockOut == null) {
            return 0;
        }
        return (int) Duration.between(clockIn, clockOut).toMinutes();
    }

    public int breakMinutes() {
        int total = totalMinutes();
        if (total > BREAK_THRESHOLD_8H) {
            return BREAK_MINUTES_60;
        } else if (total > BREAK_THRESHOLD_6H) {
            return BREAK_MINUTES_45;
        }
        return 0;
    }

    public int actualMinutes() {
        return Math.max(0, totalMinutes() - breakMinutes());
    }

    public int overtimeMinutes() {
        return Math.max(0, actualMinutes() - STANDARD_WORK_MINUTES);
    }
}
