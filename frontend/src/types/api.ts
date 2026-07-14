export type Role = "EMPLOYEE" | "ADMIN";

export interface EmployeeResponse {
  id: number;
  employeeCode: string;
  name: string;
  email: string;
  role: Role;
  version: number;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  employee: EmployeeResponse;
}

export interface CreateEmployeeRequest {
  name: string;
  email: string;
  password: string;
  role: Role;
}

export interface UpdateEmployeeRequest {
  name: string;
  email: string;
  role: Role;
  version: number;
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
