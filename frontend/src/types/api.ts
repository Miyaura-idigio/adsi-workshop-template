export type Role = "EMPLOYEE" | "MANAGER" | "ADMIN";

export interface EmployeeResponse {
  id: number;
  employeeCode: string;
  name: string;
  email: string;
  role: Role;
  active: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  employee: EmployeeResponse;
}

export interface ErrorResponse {
  status: number;
  message: string;
  errors: FieldError[];
  timestamp: string;
}

export interface FieldError {
  field: string;
  message: string;
}

export interface AttendanceRecordResponse {
  id: number;
  employeeId: number;
  date: string;
  clockIn: string;
  clockOut: string | null;
  workingMinutes: number | null;
  overtimeMinutes: number | null;
}

export interface MonthlySummaryResponse {
  yearMonth: string;
  totalWorkingDays: number;
  totalActualMinutes: number;
  totalOvertimeMinutes: number;
}

export interface UpdateRecordRequest {
  clockIn: string;
  clockOut: string | null;
}
