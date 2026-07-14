package com.example.attendance.controller;

import com.example.attendance.dto.AttendanceRecordResponse;
import com.example.attendance.dto.MonthlySummaryResponse;
import com.example.attendance.dto.UpdateRecordRequest;
import com.example.attendance.service.AttendanceService;
import com.example.attendance.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/attendance")
@Validated
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AuthService authService;

    public AttendanceController(AttendanceService attendanceService, AuthService authService) {
        this.attendanceService = attendanceService;
        this.authService = authService;
    }

    @PostMapping("/clock-in")
    public ResponseEntity<AttendanceRecordResponse> clockIn(@AuthenticationPrincipal UserDetails user) {
        Long employeeId = getEmployeeId(user);
        return ResponseEntity.ok(attendanceService.clockIn(employeeId));
    }

    @PostMapping("/clock-out")
    public ResponseEntity<AttendanceRecordResponse> clockOut(@AuthenticationPrincipal UserDetails user) {
        Long employeeId = getEmployeeId(user);
        return ResponseEntity.ok(attendanceService.clockOut(employeeId));
    }

    @GetMapping("/today")
    public ResponseEntity<AttendanceRecordResponse> today(@AuthenticationPrincipal UserDetails user) {
        Long employeeId = getEmployeeId(user);
        AttendanceRecordResponse record = attendanceService.getTodayRecord(employeeId);
        if (record == null) {
            return ResponseEntity.ok(null);
        }
        return ResponseEntity.ok(record);
    }

    @GetMapping("/records")
    public ResponseEntity<List<AttendanceRecordResponse>> records(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam @Pattern(regexp = "\\d{4}-\\d{2}", message = "yearMonth は yyyy-MM 形式で指定してください") String yearMonth) {
        Long employeeId = getEmployeeId(user);
        YearMonth ym = YearMonth.parse(yearMonth);
        return ResponseEntity.ok(attendanceService.getMonthlyRecords(employeeId, ym));
    }

    @GetMapping("/summary")
    public ResponseEntity<MonthlySummaryResponse> summary(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam @Pattern(regexp = "\\d{4}-\\d{2}", message = "yearMonth は yyyy-MM 形式で指定してください") String yearMonth) {
        Long employeeId = getEmployeeId(user);
        YearMonth ym = YearMonth.parse(yearMonth);
        return ResponseEntity.ok(attendanceService.getMonthlySummary(employeeId, ym));
    }

    @GetMapping("/records/{id}")
    public ResponseEntity<AttendanceRecordResponse> getRecord(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        Long employeeId = getEmployeeId(user);
        return ResponseEntity.ok(attendanceService.getRecord(id, employeeId));
    }

    @PutMapping("/records/{id}")
    public ResponseEntity<AttendanceRecordResponse> updateRecord(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRecordRequest request,
            @AuthenticationPrincipal UserDetails user) {
        Long employeeId = getEmployeeId(user);
        return ResponseEntity.ok(attendanceService.updateRecord(id, request, employeeId));
    }

    private Long getEmployeeId(UserDetails user) {
        return authService.getCurrentUser(user.getUsername()).id();
    }
}
