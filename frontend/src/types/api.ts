export type Role = "EMPLOYEE" | "ADMIN";

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
