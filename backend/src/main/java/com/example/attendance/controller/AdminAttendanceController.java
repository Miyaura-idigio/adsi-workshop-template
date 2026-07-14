package com.example.attendance.controller;

import com.example.attendance.dto.AttendanceRecordResponse;
import com.example.attendance.dto.MonthlySummaryResponse;
import com.example.attendance.service.AttendanceService;
import com.example.attendance.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/admin/attendance")
public class AdminAttendanceController {

    private final AttendanceService attendanceService;
    private final AuthService authService;

    public AdminAttendanceController(AttendanceService attendanceService, AuthService authService) {
        this.attendanceService = attendanceService;
        this.authService = authService;
    }

    @GetMapping("/records")
    public ResponseEntity<List<AttendanceRecordResponse>> records(
            @RequestParam Long employeeId,
            @RequestParam String yearMonth,
            @AuthenticationPrincipal UserDetails user) {
        Long requestingEmployeeId = authService.getCurrentUser(user.getUsername()).id();
        YearMonth ym = YearMonth.parse(yearMonth);
        return ResponseEntity.ok(attendanceService.getAdminMonthlyRecords(employeeId, ym, requestingEmployeeId));
    }

    @GetMapping("/summary")
    public ResponseEntity<MonthlySummaryResponse> summary(
            @RequestParam Long employeeId,
            @RequestParam String yearMonth,
            @AuthenticationPrincipal UserDetails user) {
        Long requestingEmployeeId = authService.getCurrentUser(user.getUsername()).id();
        YearMonth ym = YearMonth.parse(yearMonth);
        return ResponseEntity.ok(attendanceService.getAdminMonthlySummary(employeeId, ym, requestingEmployeeId));
    }
}
