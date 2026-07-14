package com.example.attendance.service;

import com.example.attendance.dto.AttendanceRecordResponse;
import com.example.attendance.dto.MonthlySummaryResponse;
import com.example.attendance.dto.UpdateRecordRequest;
import com.example.attendance.entity.AttendanceRecord;
import com.example.attendance.entity.Employee;
import com.example.attendance.entity.Role;
import com.example.attendance.repository.AttendanceRecordRepository;
import com.example.attendance.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceImplTest {

    @Mock
    private AttendanceRecordRepository attendanceRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    private AttendanceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AttendanceServiceImpl(attendanceRepository, employeeRepository);
    }

    @Nested
    @DisplayName("clockIn")
    class ClockIn {

        @Test
        @DisplayName("未打刻の場合、出勤記録が作成される")
        void clockIn_notYetClockedIn_createsRecord() {
            when(attendanceRepository.findByEmployeeIdAndDate(1L, LocalDate.now()))
                    .thenReturn(Optional.empty());
            when(attendanceRepository.save(any(AttendanceRecord.class)))
                    .thenAnswer(inv -> {
                        AttendanceRecord r = inv.getArgument(0);
                        r.setId(1L);
                        return r;
                    });

            AttendanceRecordResponse result = service.clockIn(1L);

            assertThat(result.employeeId()).isEqualTo(1L);
            assertThat(result.date()).isEqualTo(LocalDate.now());
            assertThat(result.clockIn()).isNotNull();
            assertThat(result.clockOut()).isNull();
        }

        @Test
        @DisplayName("当日打刻済みの場合、409エラー")
        void clockIn_alreadyClockedIn_throwsConflict() {
            AttendanceRecord existing = AttendanceRecord.builder()
                    .id(1L).employeeId(1L).date(LocalDate.now())
                    .clockIn(LocalDateTime.now().minusHours(2))
                    .build();
            when(attendanceRepository.findByEmployeeIdAndDate(1L, LocalDate.now()))
                    .thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.clockIn(1L))
                    .isInstanceOf(ConflictException.class);
        }
    }

    @Nested
    @DisplayName("clockOut")
    class ClockOut {

        @Test
        @DisplayName("出勤済み・未退勤の場合、退勤記録が更新される")
        void clockOut_clockedIn_updatesRecord() {
            AttendanceRecord existing = AttendanceRecord.builder()
                    .id(1L).employeeId(1L).date(LocalDate.now())
                    .clockIn(LocalDateTime.now().minusHours(8))
                    .clockOut(null)
                    .build();
            when(attendanceRepository.findByEmployeeIdAndDate(1L, LocalDate.now()))
                    .thenReturn(Optional.of(existing));
            when(attendanceRepository.save(any(AttendanceRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            AttendanceRecordResponse result = service.clockOut(1L);

            assertThat(result.clockOut()).isNotNull();
        }

        @Test
        @DisplayName("未出勤の場合、409エラー")
        void clockOut_notClockedIn_throwsConflict() {
            when(attendanceRepository.findByEmployeeIdAndDate(1L, LocalDate.now()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.clockOut(1L))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("退勤済みの場合、409エラー")
        void clockOut_alreadyClockedOut_throwsConflict() {
            AttendanceRecord existing = AttendanceRecord.builder()
                    .id(1L).employeeId(1L).date(LocalDate.now())
                    .clockIn(LocalDateTime.now().minusHours(8))
                    .clockOut(LocalDateTime.now())
                    .build();
            when(attendanceRepository.findByEmployeeIdAndDate(1L, LocalDate.now()))
                    .thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.clockOut(1L))
                    .isInstanceOf(ConflictException.class);
        }
    }

    @Nested
    @DisplayName("updateRecord")
    class UpdateRecord {

        @Test
        @DisplayName("本人が修正する場合、成功")
        void updateRecord_byOwner_succeeds() {
            AttendanceRecord existing = AttendanceRecord.builder()
                    .id(10L).employeeId(1L).date(LocalDate.now().minusDays(1))
                    .clockIn(LocalDateTime.of(2026, 7, 13, 9, 0))
                    .clockOut(LocalDateTime.of(2026, 7, 13, 17, 0))
                    .build();
            when(attendanceRepository.findById(10L)).thenReturn(Optional.of(existing));

            Employee owner = Employee.builder().id(1L).role(Role.EMPLOYEE).build();
            when(employeeRepository.findById(1L)).thenReturn(Optional.of(owner));
            when(attendanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdateRecordRequest request = new UpdateRecordRequest(
                    LocalDateTime.of(2026, 7, 13, 8, 30),
                    LocalDateTime.of(2026, 7, 13, 17, 30));

            AttendanceRecordResponse result = service.updateRecord(10L, request, 1L);

            assertThat(result.clockIn()).isEqualTo(LocalDateTime.of(2026, 7, 13, 8, 30));
            assertThat(result.clockOut()).isEqualTo(LocalDateTime.of(2026, 7, 13, 17, 30));
        }

        @Test
        @DisplayName("ADMINが修正する場合、成功")
        void updateRecord_byAdmin_succeeds() {
            AttendanceRecord existing = AttendanceRecord.builder()
                    .id(10L).employeeId(2L).date(LocalDate.of(2026, 7, 13))
                    .clockIn(LocalDateTime.of(2026, 7, 13, 9, 0))
                    .clockOut(LocalDateTime.of(2026, 7, 13, 17, 0))
                    .build();
            when(attendanceRepository.findById(10L)).thenReturn(Optional.of(existing));

            Employee admin = Employee.builder().id(1L).role(Role.ADMIN).build();
            when(employeeRepository.findById(1L)).thenReturn(Optional.of(admin));
            when(attendanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdateRecordRequest request = new UpdateRecordRequest(
                    LocalDateTime.of(2026, 7, 13, 8, 0),
                    LocalDateTime.of(2026, 7, 13, 18, 0));

            AttendanceRecordResponse result = service.updateRecord(10L, request, 1L);

            assertThat(result.clockIn()).isEqualTo(LocalDateTime.of(2026, 7, 13, 8, 0));
        }

        @Test
        @DisplayName("他人（非管理者）が修正する場合、403エラー")
        void updateRecord_byOtherEmployee_throwsForbidden() {
            AttendanceRecord existing = AttendanceRecord.builder()
                    .id(10L).employeeId(2L).date(LocalDate.of(2026, 7, 13))
                    .clockIn(LocalDateTime.of(2026, 7, 13, 9, 0))
                    .build();
            when(attendanceRepository.findById(10L)).thenReturn(Optional.of(existing));

            Employee other = Employee.builder().id(3L).role(Role.EMPLOYEE).build();
            when(employeeRepository.findById(3L)).thenReturn(Optional.of(other));

            UpdateRecordRequest request = new UpdateRecordRequest(
                    LocalDateTime.of(2026, 7, 13, 8, 0), null);

            assertThatThrownBy(() -> service.updateRecord(10L, request, 3L))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("MANAGERが部下の打刻を修正する場合、成功")
        void updateRecord_byManagerForSubordinate_succeeds() {
            AttendanceRecord existing = AttendanceRecord.builder()
                    .id(10L).employeeId(2L).date(LocalDate.of(2026, 7, 13))
                    .clockIn(LocalDateTime.of(2026, 7, 13, 9, 0))
                    .clockOut(LocalDateTime.of(2026, 7, 13, 17, 0))
                    .build();
            when(attendanceRepository.findById(10L)).thenReturn(Optional.of(existing));

            Employee manager = Employee.builder().id(5L).role(Role.MANAGER).build();
            when(employeeRepository.findById(5L)).thenReturn(Optional.of(manager));

            Employee subordinate = Employee.builder().id(2L).role(Role.EMPLOYEE).managerId(5L).build();
            when(employeeRepository.findById(2L)).thenReturn(Optional.of(subordinate));
            when(attendanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdateRecordRequest request = new UpdateRecordRequest(
                    LocalDateTime.of(2026, 7, 13, 8, 30),
                    LocalDateTime.of(2026, 7, 13, 17, 30));

            AttendanceRecordResponse result = service.updateRecord(10L, request, 5L);

            assertThat(result.clockIn()).isEqualTo(LocalDateTime.of(2026, 7, 13, 8, 30));
        }
    }

    @Nested
    @DisplayName("getTodayRecord")
    class GetTodayRecord {

        @Test
        @DisplayName("当日の出勤記録がある場合、レコードを返す")
        void getTodayRecord_exists_returnsRecord() {
            AttendanceRecord existing = AttendanceRecord.builder()
                    .id(1L).employeeId(1L).date(LocalDate.now())
                    .clockIn(LocalDateTime.now().minusHours(3))
                    .build();
            when(attendanceRepository.findByEmployeeIdAndDate(1L, LocalDate.now()))
                    .thenReturn(Optional.of(existing));

            AttendanceRecordResponse result = service.getTodayRecord(1L);

            assertThat(result).isNotNull();
            assertThat(result.employeeId()).isEqualTo(1L);
            assertThat(result.date()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("当日の出勤記録がない場合、nullを返す")
        void getTodayRecord_notExists_returnsNull() {
            when(attendanceRepository.findByEmployeeIdAndDate(1L, LocalDate.now()))
                    .thenReturn(Optional.empty());

            AttendanceRecordResponse result = service.getTodayRecord(1L);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("getRecord")
    class GetRecord {

        @Test
        @DisplayName("本人のレコードを取得できる")
        void getRecord_byOwner_returnsRecord() {
            AttendanceRecord record = AttendanceRecord.builder()
                    .id(10L).employeeId(1L).date(LocalDate.of(2026, 7, 10))
                    .clockIn(LocalDateTime.of(2026, 7, 10, 9, 0))
                    .clockOut(LocalDateTime.of(2026, 7, 10, 18, 0))
                    .build();
            when(attendanceRepository.findById(10L)).thenReturn(Optional.of(record));

            Employee owner = Employee.builder().id(1L).role(Role.EMPLOYEE).build();
            when(employeeRepository.findById(1L)).thenReturn(Optional.of(owner));

            AttendanceRecordResponse result = service.getRecord(10L, 1L);

            assertThat(result.id()).isEqualTo(10L);
            assertThat(result.employeeId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("他人（非管理者）がレコードを取得する場合、403エラー")
        void getRecord_byOtherEmployee_throwsForbidden() {
            AttendanceRecord record = AttendanceRecord.builder()
                    .id(10L).employeeId(2L).date(LocalDate.of(2026, 7, 10))
                    .clockIn(LocalDateTime.of(2026, 7, 10, 9, 0))
                    .build();
            when(attendanceRepository.findById(10L)).thenReturn(Optional.of(record));

            Employee other = Employee.builder().id(3L).role(Role.EMPLOYEE).build();
            when(employeeRepository.findById(3L)).thenReturn(Optional.of(other));

            assertThatThrownBy(() -> service.getRecord(10L, 3L))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("存在しないレコードIDの場合、404エラー")
        void getRecord_notFound_throwsNotFound() {
            when(attendanceRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getRecord(999L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getMonthlyRecords")
    class GetMonthlyRecords {

        @Test
        @DisplayName("指定月のレコード一覧を返す")
        void getMonthlyRecords_returnsRecordsForMonth() {
            YearMonth ym = YearMonth.of(2026, 7);
            List<AttendanceRecord> records = List.of(
                    AttendanceRecord.builder().id(1L).employeeId(1L)
                            .date(LocalDate.of(2026, 7, 1))
                            .clockIn(LocalDateTime.of(2026, 7, 1, 9, 0))
                            .clockOut(LocalDateTime.of(2026, 7, 1, 18, 0))
                            .build(),
                    AttendanceRecord.builder().id(2L).employeeId(1L)
                            .date(LocalDate.of(2026, 7, 2))
                            .clockIn(LocalDateTime.of(2026, 7, 2, 9, 0))
                            .clockOut(LocalDateTime.of(2026, 7, 2, 17, 0))
                            .build()
            );
            when(attendanceRepository.findByEmployeeIdAndDateBetweenOrderByDateAsc(
                    1L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                    .thenReturn(records);

            List<AttendanceRecordResponse> result = service.getMonthlyRecords(1L, ym);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).date()).isEqualTo(LocalDate.of(2026, 7, 1));
        }
    }

    @Nested
    @DisplayName("getMonthlySummary")
    class GetMonthlySummary {

        @Test
        @DisplayName("月次集計が正しく計算される")
        void getMonthlySummary_calculatesCorrectly() {
            YearMonth ym = YearMonth.of(2026, 7);
            List<AttendanceRecord> records = List.of(
                    AttendanceRecord.builder().id(1L).employeeId(1L)
                            .date(LocalDate.of(2026, 7, 1))
                            .clockIn(LocalDateTime.of(2026, 7, 1, 9, 0))
                            .clockOut(LocalDateTime.of(2026, 7, 1, 18, 0)) // 9h → 休憩60m → 実480m → 残業0
                            .build(),
                    AttendanceRecord.builder().id(2L).employeeId(1L)
                            .date(LocalDate.of(2026, 7, 2))
                            .clockIn(LocalDateTime.of(2026, 7, 2, 9, 0))
                            .clockOut(LocalDateTime.of(2026, 7, 2, 20, 0)) // 11h=660m → 休憩60m → 実600m → 残業120m
                            .build()
            );
            when(attendanceRepository.findByEmployeeIdAndDateBetweenOrderByDateAsc(
                    1L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                    .thenReturn(records);

            MonthlySummaryResponse result = service.getMonthlySummary(1L, ym);

            assertThat(result.yearMonth()).isEqualTo("2026-07");
            assertThat(result.totalWorkingDays()).isEqualTo(2);
            assertThat(result.totalActualMinutes()).isEqualTo(1080); // 480 + 600
            assertThat(result.totalOvertimeMinutes()).isEqualTo(120); // 0 + 120
        }
    }
}
