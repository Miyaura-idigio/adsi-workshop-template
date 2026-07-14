import { apiFetch } from "@/lib/api-client";
import type {
  AttendanceRecordResponse,
  MonthlySummaryResponse,
  UpdateRecordRequest,
} from "@/types/api";

export function clockIn(): Promise<AttendanceRecordResponse> {
  return apiFetch("/attendance/clock-in", { method: "POST" });
}

export function clockOut(): Promise<AttendanceRecordResponse> {
  return apiFetch("/attendance/clock-out", { method: "POST" });
}

export function getTodayRecord(): Promise<AttendanceRecordResponse | null> {
  return apiFetch("/attendance/today");
}

export function getMonthlyRecords(
  yearMonth: string,
): Promise<AttendanceRecordResponse[]> {
  return apiFetch(`/attendance/records?yearMonth=${yearMonth}`);
}

export function getMonthlySummary(
  yearMonth: string,
): Promise<MonthlySummaryResponse> {
  return apiFetch(`/attendance/summary?yearMonth=${yearMonth}`);
}

export function updateRecord(
  id: number,
  request: UpdateRecordRequest,
): Promise<AttendanceRecordResponse> {
  return apiFetch(`/attendance/records/${id}`, {
    method: "PUT",
    body: JSON.stringify(request),
  });
}

export function getAdminRecords(
  employeeId: number,
  yearMonth: string,
): Promise<AttendanceRecordResponse[]> {
  return apiFetch(
    `/admin/attendance/records?employeeId=${employeeId}&yearMonth=${yearMonth}`,
  );
}

export function getAdminSummary(
  employeeId: number,
  yearMonth: string,
): Promise<MonthlySummaryResponse> {
  return apiFetch(
    `/admin/attendance/summary?employeeId=${employeeId}&yearMonth=${yearMonth}`,
  );
}
