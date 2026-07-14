package com.example.attendance.service;

import com.example.attendance.dto.CreateEmployeeRequest;
import com.example.attendance.dto.EmployeeResponse;
import com.example.attendance.dto.UpdateEmployeeRequest;
import com.example.attendance.entity.Employee;
import com.example.attendance.entity.Role;
import com.example.attendance.exception.DuplicateEmailException;
import com.example.attendance.exception.ResourceNotFoundException;
import com.example.attendance.repository.EmployeeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    @Test
    @DisplayName("create: 正常登録でemployeeCodeが自動採番される")
    void create_validRequest_generatesEmployeeCode() {
        // Arrange
        var request = new CreateEmployeeRequest("田中太郎", "tanaka@example.com", "password123", Role.EMPLOYEE);
        when(employeeRepository.existsByEmail("tanaka@example.com")).thenReturn(false);
        when(employeeRepository.findMaxEmployeeCode()).thenReturn(Optional.of("EMP003"));
        when(passwordEncoder.encode("password123")).thenReturn("$2a$encoded");
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee e = invocation.getArgument(0);
            e.setId(4L);
            return e;
        });

        // Act
        EmployeeResponse response = employeeService.create(request);

        // Assert
        assertThat(response.employeeCode()).isEqualTo("EMP004");
        assertThat(response.name()).isEqualTo("田中太郎");
        assertThat(response.email()).isEqualTo("tanaka@example.com");
        assertThat(response.role()).isEqualTo(Role.EMPLOYEE);
    }

    @Test
    @DisplayName("create: 最初の社員登録でEMP001が採番される")
    void create_firstEmployee_generatesEMP001() {
        // Arrange
        var request = new CreateEmployeeRequest("初めての社員", "first@example.com", "password123", Role.EMPLOYEE);
        when(employeeRepository.existsByEmail("first@example.com")).thenReturn(false);
        when(employeeRepository.findMaxEmployeeCode()).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("$2a$encoded");
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee e = invocation.getArgument(0);
            e.setId(1L);
            return e;
        });

        // Act
        EmployeeResponse response = employeeService.create(request);

        // Assert
        assertThat(response.employeeCode()).isEqualTo("EMP001");
    }

    @Test
    @DisplayName("create: メール重複で409エラー")
    void create_duplicateEmail_throwsDuplicateEmailException() {
        // Arrange
        var request = new CreateEmployeeRequest("田中太郎", "existing@example.com", "password123", Role.EMPLOYEE);
        when(employeeRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("このメールアドレスは既に使用されています");
    }

    @Test
    @DisplayName("create: パスワードがBCryptハッシュ化される")
    void create_validRequest_hashesPassword() {
        // Arrange
        var request = new CreateEmployeeRequest("田中太郎", "tanaka@example.com", "password123", Role.EMPLOYEE);
        when(employeeRepository.existsByEmail("tanaka@example.com")).thenReturn(false);
        when(employeeRepository.findMaxEmployeeCode()).thenReturn(Optional.of("EMP001"));
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashedpassword");
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        employeeService.create(request);

        // Assert
        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("$2a$10$hashedpassword");
    }

    @Test
    @DisplayName("update: 正常更新")
    void update_validRequest_updatesEmployee() {
        // Arrange
        var employee = Employee.builder()
                .id(1L).employeeCode("EMP001").name("旧名前")
                .email("old@example.com").role(Role.EMPLOYEE).active(true).version(0L)
                .build();
        var request = new UpdateEmployeeRequest("新名前", "new@example.com", Role.ADMIN, 0L);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByEmailAndIdNot("new@example.com", 1L)).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        EmployeeResponse response = employeeService.update(1L, request);

        // Assert
        assertThat(response.name()).isEqualTo("新名前");
        assertThat(response.email()).isEqualTo("new@example.com");
        assertThat(response.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("update: 存在しないIDで404エラー")
    void update_nonExistingId_throwsResourceNotFoundException() {
        // Arrange
        var request = new UpdateEmployeeRequest("名前", "email@example.com", Role.EMPLOYEE, 0L);
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> employeeService.update(999L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("update: メール重複（他社員と）で409エラー")
    void update_duplicateEmailWithOther_throwsDuplicateEmailException() {
        // Arrange
        var employee = Employee.builder()
                .id(1L).employeeCode("EMP001").name("名前")
                .email("me@example.com").role(Role.EMPLOYEE).active(true).version(0L)
                .build();
        var request = new UpdateEmployeeRequest("名前", "other@example.com", Role.EMPLOYEE, 0L);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByEmailAndIdNot("other@example.com", 1L)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> employeeService.update(1L, request))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    @DisplayName("deactivate: active=falseに更新")
    void deactivate_existingId_setsActiveFalse() {
        // Arrange
        var employee = Employee.builder()
                .id(1L).employeeCode("EMP001").name("名前")
                .email("e@example.com").role(Role.EMPLOYEE).active(true).version(0L)
                .build();
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        employeeService.deactivate(1L);

        // Assert
        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
    }

    @Test
    @DisplayName("deactivate: 存在しないIDで404エラー")
    void deactivate_nonExistingId_throwsResourceNotFoundException() {
        // Arrange
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> employeeService.deactivate(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("findAll: active=trueのみ返す")
    void findAll_returnsOnlyActiveEmployees() {
        // Arrange
        var emp1 = Employee.builder()
                .id(1L).employeeCode("EMP001").name("社員1")
                .email("emp1@example.com").role(Role.EMPLOYEE).active(true)
                .build();
        var emp2 = Employee.builder()
                .id(2L).employeeCode("EMP002").name("社員2")
                .email("emp2@example.com").role(Role.ADMIN).active(true)
                .build();
        when(employeeRepository.findByActiveTrueOrderByEmployeeCodeAsc()).thenReturn(List.of(emp1, emp2));

        // Act
        List<EmployeeResponse> result = employeeService.findAll();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).employeeCode()).isEqualTo("EMP001");
    }

    @Test
    @DisplayName("findById: 存在するIDでEmployee返却")
    void findById_existingId_returnsEmployee() {
        // Arrange
        var employee = Employee.builder()
                .id(1L).employeeCode("EMP001").name("社員1")
                .email("emp1@example.com").role(Role.EMPLOYEE).active(true)
                .build();
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        // Act
        EmployeeResponse result = employeeService.findById(1L);

        // Assert
        assertThat(result.name()).isEqualTo("社員1");
    }

    @Test
    @DisplayName("findById: 存在しないIDで404エラー")
    void findById_nonExistingId_throwsResourceNotFoundException() {
        // Arrange
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> employeeService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
