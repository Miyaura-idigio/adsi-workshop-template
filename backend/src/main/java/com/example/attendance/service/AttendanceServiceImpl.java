package com.example.attendance.service;

import com.example.attendance.domain.WorkDuration;
import com.example.attendance.dto.AttendanceRecordResponse;
import com.example.attendance.dto.MonthlySummaryResponse;
import com.example.attendance.dto.UpdateRecordRequest;
import com.example.attendance.entity.AttendanceRecord;
import com.example.attendance.entity.Employee;
import com.example.attendance.entity.Role;
import com.example.attendance.repository.AttendanceRecordRepository;
import com.example.attendance.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRecordRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    public AttendanceServiceImpl(AttendanceRecordRepository attendanceRepository,
                                 EmployeeRepository employeeRepository) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public AttendanceRecordResponse clockIn(Long employeeId) {
        LocalDate today = LocalDate.now();
        attendanceRepository.findByEmployeeIdAndDate(employeeId, today)
                .ifPresent(r -> {
                    throw new ConflictException("本日は既に出勤打刻されています");
                });

        AttendanceRecord record = AttendanceRecord.builder()
                .employeeId(employeeId)
                .date(today)
                .clockIn(LocalDateTime.now())
                .build();

        AttendanceRecord saved = attendanceRepository.save(record);
        return AttendanceRecordResponse.from(saved);
    }

    @Override
    public AttendanceRecordResponse clockOut(Long employeeId) {
        LocalDate today = LocalDate.now();
        AttendanceRecord record = attendanceRepository.findByEmployeeIdAndDate(employeeId, today)
                .orElseThrow(() -> new ConflictException("本日の出勤打刻がありません"));

        if (record.getClockOut() != null) {
            throw new ConflictException("本日は既に退勤打刻されています");
        }

        record.setClockOut(LocalDateTime.now());
        AttendanceRecord saved = attendanceRepository.save(record);
        return AttendanceRecordResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceRecordResponse getTodayRecord(Long employeeId) {
        LocalDate today = LocalDate.now();
        return attendanceRepository.findByEmployeeIdAndDate(employeeId, today)
                .map(AttendanceRecordResponse::from)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceRecordResponse> getMonthlyRecords(Long employeeId, YearMonth yearMonth) {
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        return attendanceRepository.findByEmployeeIdAndDateBetweenOrderByDateAsc(employeeId, start, end)
                .stream()
                .map(AttendanceRecordResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlySummaryResponse getMonthlySummary(Long employeeId, YearMonth yearMonth) {
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        List<AttendanceRecord> records = attendanceRepository
                .findByEmployeeIdAndDateBetweenOrderByDateAsc(employeeId, start, end);

        int totalWorkingDays = 0;
        int totalActualMinutes = 0;
        int totalOvertimeMinutes = 0;

        for (AttendanceRecord record : records) {
            if (record.getClockOut() != null) {
                totalWorkingDays++;
                WorkDuration duration = new WorkDuration(record.getClockIn(), record.getClockOut());
                totalActualMinutes += duration.actualMinutes();
                totalOvertimeMinutes += duration.overtimeMinutes();
            }
        }

        return new MonthlySummaryResponse(
                yearMonth.toString(),
                totalWorkingDays,
                totalActualMinutes,
                totalOvertimeMinutes
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceRecordResponse getRecord(Long recordId, Long requestingEmployeeId) {
        AttendanceRecord record = attendanceRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("勤怠記録が見つかりません"));

        Employee requester = employeeRepository.findById(requestingEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("社員が見つかりません"));

        if (!canAccessRecord(record, requester)) {
            throw new ForbiddenException("この勤怠記録を閲覧する権限がありません");
        }

        return AttendanceRecordResponse.from(record);
    }

    @Override
    public AttendanceRecordResponse updateRecord(Long recordId, UpdateRecordRequest request,
                                                  Long requestingEmployeeId) {
        AttendanceRecord record = attendanceRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("勤怠記録が見つかりません"));

        Employee requester = employeeRepository.findById(requestingEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("社員が見つかりません"));

        if (!canUpdateRecord(record, requester)) {
            throw new ForbiddenException("この勤怠記録を修正する権限がありません");
        }

        if (request.clockOut() != null && request.clockOut().isBefore(request.clockIn())) {
            throw new IllegalArgumentException("退勤時刻は出勤時刻より後である必要があります");
        }

        record.setClockIn(request.clockIn());
        record.setClockOut(request.clockOut());

        AttendanceRecord saved = attendanceRepository.save(record);
        return AttendanceRecordResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceRecordResponse> getAdminMonthlyRecords(Long employeeId, YearMonth yearMonth,
                                                                  Long requestingEmployeeId) {
        verifyAdminAccess(employeeId, requestingEmployeeId);
        return getMonthlyRecords(employeeId, yearMonth);
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlySummaryResponse getAdminMonthlySummary(Long employeeId, YearMonth yearMonth,
                                                          Long requestingEmployeeId) {
        verifyAdminAccess(employeeId, requestingEmployeeId);
        return getMonthlySummary(employeeId, yearMonth);
    }

    private void verifyAdminAccess(Long targetEmployeeId, Long requestingEmployeeId) {
        Employee requester = employeeRepository.findById(requestingEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("社員が見つかりません"));

        if (requester.getRole() == Role.ADMIN) {
            return;
        }
        if (requester.getRole() == Role.MANAGER) {
            Employee target = employeeRepository.findById(targetEmployeeId)
                    .orElseThrow(() -> new ResourceNotFoundException("対象社員が見つかりません"));
            if (requester.getId().equals(target.getManagerId())) {
                return;
            }
        }
        throw new ForbiddenException("この社員の勤怠情報を閲覧する権限がありません");
    }

    private boolean canAccessRecord(AttendanceRecord record, Employee requester) {
        return canUpdateRecord(record, requester);
    }

    private boolean canUpdateRecord(AttendanceRecord record, Employee requester) {
        if (record.getEmployeeId().equals(requester.getId())) {
            return true;
        }
        if (requester.getRole() == Role.ADMIN) {
            return true;
        }
        if (requester.getRole() == Role.MANAGER) {
            Employee recordOwner = employeeRepository.findById(record.getEmployeeId())
                    .orElseThrow(() -> new ResourceNotFoundException("社員が見つかりません"));
            return requester.getId().equals(recordOwner.getManagerId());
        }
        return false;
    }
}
