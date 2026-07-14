package com.example.attendance.controller;

import com.example.attendance.dto.AttendanceRecordResponse;
import com.example.attendance.dto.EmployeeResponse;
import com.example.attendance.dto.MonthlySummaryResponse;
import com.example.attendance.entity.Role;
import com.example.attendance.security.SecurityConfig;
import com.example.attendance.service.AttendanceService;
import com.example.attendance.service.AuthService;
import com.example.attendance.service.ConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AttendanceController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AttendanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AttendanceService attendanceService;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("POST /attendance/clock-in: 正常打刻 → 200")
    @WithMockUser(username = "user@example.com")
    void clockIn_success_returns200() throws Exception {
        when(authService.getCurrentUser("user@example.com"))
                .thenReturn(new EmployeeResponse(1L, "EMP001", "テスト", "user@example.com", Role.EMPLOYEE, 1L));
        when(attendanceService.clockIn(1L))
                .thenReturn(new AttendanceRecordResponse(1L, 1L, LocalDate.now(),
                        LocalDateTime.now(), null, null, null));

        mockMvc.perform(post("/attendance/clock-in").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(1));
    }

    @Test
    @DisplayName("POST /attendance/clock-in: 重複打刻 → 409")
    @WithMockUser(username = "user@example.com")
    void clockIn_duplicate_returns409() throws Exception {
        when(authService.getCurrentUser("user@example.com"))
                .thenReturn(new EmployeeResponse(1L, "EMP001", "テスト", "user@example.com", Role.EMPLOYEE, 1L));
        when(attendanceService.clockIn(1L))
                .thenThrow(new ConflictException("本日は既に出勤打刻されています"));

        mockMvc.perform(post("/attendance/clock-in").with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("本日は既に出勤打刻されています"));
    }

    @Test
    @DisplayName("POST /attendance/clock-out: 正常退勤 → 200")
    @WithMockUser(username = "user@example.com")
    void clockOut_success_returns200() throws Exception {
        when(authService.getCurrentUser("user@example.com"))
                .thenReturn(new EmployeeResponse(1L, "EMP001", "テスト", "user@example.com", Role.EMPLOYEE, 1L));
        when(attendanceService.clockOut(1L))
                .thenReturn(new AttendanceRecordResponse(1L, 1L, LocalDate.now(),
                        LocalDateTime.now().minusHours(8), LocalDateTime.now(), 480, 0));

        mockMvc.perform(post("/attendance/clock-out").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clockOut").isNotEmpty());
    }

    @Test
    @DisplayName("GET /attendance/records: 月次一覧取得 → 200")
    @WithMockUser(username = "user@example.com")
    void records_success_returns200() throws Exception {
        when(authService.getCurrentUser("user@example.com"))
                .thenReturn(new EmployeeResponse(1L, "EMP001", "テスト", "user@example.com", Role.EMPLOYEE, 1L));
        when(attendanceService.getMonthlyRecords(1L, YearMonth.of(2026, 7)))
                .thenReturn(List.of());

        mockMvc.perform(get("/attendance/records").param("yearMonth", "2026-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /attendance/summary: 月次集計取得 → 200")
    @WithMockUser(username = "user@example.com")
    void summary_success_returns200() throws Exception {
        when(authService.getCurrentUser("user@example.com"))
                .thenReturn(new EmployeeResponse(1L, "EMP001", "テスト", "user@example.com", Role.EMPLOYEE, 1L));
        when(attendanceService.getMonthlySummary(1L, YearMonth.of(2026, 7)))
                .thenReturn(new MonthlySummaryResponse("2026-07", 10, 4800, 0));

        mockMvc.perform(get("/attendance/summary").param("yearMonth", "2026-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalWorkingDays").value(10));
    }

    @Test
    @DisplayName("GET /attendance/today: 当日記録あり → 200")
    @WithMockUser(username = "user@example.com")
    void today_exists_returns200() throws Exception {
        when(authService.getCurrentUser("user@example.com"))
                .thenReturn(new EmployeeResponse(1L, "EMP001", "テスト", "user@example.com", Role.EMPLOYEE, 1L));
        when(attendanceService.getTodayRecord(1L))
                .thenReturn(new AttendanceRecordResponse(1L, 1L, LocalDate.now(),
                        LocalDateTime.now().minusHours(3), null, null, null));

        mockMvc.perform(get("/attendance/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(1));
    }

    @Test
    @DisplayName("GET /attendance/today: 当日記録なし → 200 (null)")
    @WithMockUser(username = "user@example.com")
    void today_notExists_returns200Null() throws Exception {
        when(authService.getCurrentUser("user@example.com"))
                .thenReturn(new EmployeeResponse(1L, "EMP001", "テスト", "user@example.com", Role.EMPLOYEE, 1L));
        when(attendanceService.getTodayRecord(1L)).thenReturn(null);

        mockMvc.perform(get("/attendance/today"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /attendance/records/{id}: 正常取得 → 200")
    @WithMockUser(username = "user@example.com")
    void getRecord_success_returns200() throws Exception {
        when(authService.getCurrentUser("user@example.com"))
                .thenReturn(new EmployeeResponse(1L, "EMP001", "テスト", "user@example.com", Role.EMPLOYEE, 1L));
        when(attendanceService.getRecord(10L, 1L))
                .thenReturn(new AttendanceRecordResponse(10L, 1L, LocalDate.of(2026, 7, 13),
                        LocalDateTime.of(2026, 7, 13, 9, 0),
                        LocalDateTime.of(2026, 7, 13, 18, 0), 480, 0));

        mockMvc.perform(get("/attendance/records/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.employeeId").value(1));
    }

    @Test
    @DisplayName("PUT /attendance/records/{id}: 打刻修正 → 200")
    @WithMockUser(username = "user@example.com")
    void updateRecord_success_returns200() throws Exception {
        when(authService.getCurrentUser("user@example.com"))
                .thenReturn(new EmployeeResponse(1L, "EMP001", "テスト", "user@example.com", Role.EMPLOYEE, 1L));
        when(attendanceService.updateRecord(eq(10L), any(), eq(1L)))
                .thenReturn(new AttendanceRecordResponse(10L, 1L, LocalDate.of(2026, 7, 13),
                        LocalDateTime.of(2026, 7, 13, 8, 30),
                        LocalDateTime.of(2026, 7, 13, 17, 30), 480, 0));

        mockMvc.perform(put("/attendance/records/10").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clockIn\":\"2026-07-13T08:30:00\",\"clockOut\":\"2026-07-13T17:30:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @DisplayName("未認証 → 401")
    void unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/attendance/clock-in").with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
