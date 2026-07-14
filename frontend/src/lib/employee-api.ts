import { apiFetch } from "@/lib/api-client";
import type {
  EmployeeResponse,
  CreateEmployeeRequest,
  UpdateEmployeeRequest,
} from "@/types/api";

export async function fetchEmployees(): Promise<EmployeeResponse[]> {
  return apiFetch<EmployeeResponse[]>("/admin/employees");
}

export async function fetchEmployee(id: number): Promise<EmployeeResponse> {
  return apiFetch<EmployeeResponse>(`/admin/employees/${id}`);
}

export async function createEmployee(
  request: CreateEmployeeRequest,
): Promise<EmployeeResponse> {
  return apiFetch<EmployeeResponse>("/admin/employees", {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export async function updateEmployee(
  id: number,
  request: UpdateEmployeeRequest,
): Promise<EmployeeResponse> {
  return apiFetch<EmployeeResponse>(`/admin/employees/${id}`, {
    method: "PUT",
    body: JSON.stringify(request),
  });
}

export async function deactivateEmployee(id: number): Promise<void> {
  return apiFetch<void>(`/admin/employees/${id}`, {
    method: "DELETE",
  });
}
