package com.example.attendance.dto;

import com.example.attendance.entity.Employee;
import com.example.attendance.entity.Role;

public record EmployeeResponse(
        Long id,
        String employeeCode,
        String name,
        String email,
        Role role,
        boolean active
) {
    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getName(),
                employee.getEmail(),
                employee.getRole(),
                employee.isActive()
        );
    }
}
