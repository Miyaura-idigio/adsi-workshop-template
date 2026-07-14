package com.example.attendance.controller;

import com.example.attendance.dto.AttendanceRecordResponse;
import com.example.attendance.dto.EmployeeResponse;
import com.example.attendance.dto.MonthlySummaryResponse;
import com.example.attendance.entity.Role;
import com.example.attendance.security.SecurityConfig;
import com.example.attendance.service.AttendanceService;
import com.example.attendance.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.YearMonth;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminAttendanceController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class AdminAttendanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AttendanceService attendanceService;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("ADMIN: 社員勤怠一覧取得 → 200")
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void records_asAdmin_returns200() throws Exception {
        when(authService.getCurrentUser("admin@example.com"))
                .thenReturn(new EmployeeResponse(1L, "ADM001", "管理者", "admin@example.com", Role.ADMIN, 1L));
        when(attendanceService.getAdminMonthlyRecords(2L, YearMonth.of(2026, 7), 1L))
                .thenReturn(List.of());

        mockMvc.perform(get("/admin/attendance/records")
                        .param("employeeId", "2")
                        .param("yearMonth", "2026-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("MANAGER: 社員勤怠一覧取得 → 200")
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void records_asManager_returns200() throws Exception {
        when(authService.getCurrentUser("manager@example.com"))
                .thenReturn(new EmployeeResponse(5L, "MGR001", "マネージャー", "manager@example.com", Role.MANAGER, 1L));
        when(attendanceService.getAdminMonthlyRecords(2L, YearMonth.of(2026, 7), 5L))
                .thenReturn(List.of());

        mockMvc.perform(get("/admin/attendance/records")
                        .param("employeeId", "2")
                        .param("yearMonth", "2026-07"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("EMPLOYEE: 社員勤怠一覧へアクセス → 403")
    @WithMockUser(roles = "EMPLOYEE")
    void records_asEmployee_returns403() throws Exception {
        mockMvc.perform(get("/admin/attendance/records")
                        .param("employeeId", "2")
                        .param("yearMonth", "2026-07"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("未認証 → 401")
    void records_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/admin/attendance/records")
                        .param("employeeId", "2")
                        .param("yearMonth", "2026-07"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ADMIN: 月次集計取得 → 200")
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void summary_asAdmin_returns200() throws Exception {
        when(authService.getCurrentUser("admin@example.com"))
                .thenReturn(new EmployeeResponse(1L, "ADM001", "管理者", "admin@example.com", Role.ADMIN, 1L));
        when(attendanceService.getAdminMonthlySummary(2L, YearMonth.of(2026, 7), 1L))
                .thenReturn(new MonthlySummaryResponse("2026-07", 10, 4800, 0));

        mockMvc.perform(get("/admin/attendance/summary")
                        .param("employeeId", "2")
                        .param("yearMonth", "2026-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalWorkingDays").value(10));
    }
}
