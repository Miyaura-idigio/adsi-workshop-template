package com.example.attendance.service;

import com.example.attendance.dto.EmployeeResponse;
import com.example.attendance.dto.LoginRequest;
import com.example.attendance.dto.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    LoginResponse login(LoginRequest request, HttpServletRequest httpRequest);

    EmployeeResponse getCurrentUser(String email);
}
