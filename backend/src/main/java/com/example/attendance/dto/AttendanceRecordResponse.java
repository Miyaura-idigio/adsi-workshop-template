package com.example.attendance.dto;

import com.example.attendance.domain.WorkDuration;
import com.example.attendance.entity.AttendanceRecord;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AttendanceRecordResponse(
        Long id,
        Long employeeId,
        LocalDate date,
        LocalDateTime clockIn,
        LocalDateTime clockOut,
        Integer workingMinutes,
        Integer overtimeMinutes
) {
    public static AttendanceRecordResponse from(AttendanceRecord record) {
        Integer working = null;
        Integer overtime = null;
        if (record.getClockOut() != null) {
            WorkDuration duration = new WorkDuration(record.getClockIn(), record.getClockOut());
            working = duration.actualMinutes();
            overtime = duration.overtimeMinutes();
        }
        return new AttendanceRecordResponse(
                record.getId(),
                record.getEmployeeId(),
                record.getDate(),
                record.getClockIn(),
                record.getClockOut(),
                working,
                overtime
        );
    }
}
