package com.example.attendance.service;

import com.example.attendance.dto.AttendanceRecordResponse;
import com.example.attendance.dto.MonthlySummaryResponse;
import com.example.attendance.dto.UpdateRecordRequest;

import java.time.YearMonth;
import java.util.List;

public interface AttendanceService {

    AttendanceRecordResponse clockIn(Long employeeId);

    AttendanceRecordResponse clockOut(Long employeeId);

    AttendanceRecordResponse getTodayRecord(Long employeeId);

    List<AttendanceRecordResponse> getMonthlyRecords(Long employeeId, YearMonth yearMonth);

    MonthlySummaryResponse getMonthlySummary(Long employeeId, YearMonth yearMonth);

    AttendanceRecordResponse getRecord(Long recordId, Long requestingEmployeeId);

    AttendanceRecordResponse updateRecord(Long recordId, UpdateRecordRequest request,
                                          Long requestingEmployeeId);

    List<AttendanceRecordResponse> getAdminMonthlyRecords(Long employeeId, YearMonth yearMonth,
                                                          Long requestingEmployeeId);

    MonthlySummaryResponse getAdminMonthlySummary(Long employeeId, YearMonth yearMonth,
                                                   Long requestingEmployeeId);
}
