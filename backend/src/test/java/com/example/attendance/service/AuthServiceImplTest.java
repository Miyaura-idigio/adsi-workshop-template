package com.example.attendance.service;

import com.example.attendance.dto.LoginRequest;
import com.example.attendance.dto.LoginResponse;
import com.example.attendance.entity.Employee;
import com.example.attendance.entity.Role;
import com.example.attendance.repository.EmployeeRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("正しい認証情報でログインするとEmployeeResponseが返る")
    void login_validCredentials_returnsLoginResponse() {
        // Arrange
        var request = new LoginRequest("admin@example.com", "admin1234");
        var employee = Employee.builder()
                .id(1L)
                .employeeCode("EMP001")
                .name("管理者")
                .email("admin@example.com")
                .role(Role.ADMIN)
                .active(true)
                .build();

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(employeeRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(employee));

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(httpRequest.getSession(true)).thenReturn(session);

        // Act
        LoginResponse response = authService.login(request, httpRequest);

        // Assert
        assertThat(response.employee().name()).isEqualTo("管理者");
        assertThat(response.employee().role()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("不正な認証情報でログインするとBadCredentialsExceptionが発生する")
    void login_invalidCredentials_throwsBadCredentials() {
        // Arrange
        var request = new LoginRequest("admin@example.com", "wrongpassword");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request, httpRequest))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("getCurrentUserで現在のユーザー情報を取得できる")
    void getCurrentUser_existingEmail_returnsEmployeeResponse() {
        // Arrange
        var employee = Employee.builder()
                .id(1L)
                .employeeCode("EMP001")
                .name("管理者")
                .email("admin@example.com")
                .role(Role.ADMIN)
                .active(true)
                .build();
        when(employeeRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(employee));

        // Act
        var result = authService.getCurrentUser("admin@example.com");

        // Assert
        assertThat(result.name()).isEqualTo("管理者");
        assertThat(result.email()).isEqualTo("admin@example.com");
    }
}
