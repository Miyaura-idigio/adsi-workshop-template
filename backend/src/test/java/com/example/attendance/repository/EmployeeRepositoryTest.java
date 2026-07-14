package com.example.attendance.repository;

import com.example.attendance.entity.Employee;
import com.example.attendance.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    @DisplayName("メールアドレスで社員を検索できる")
    void findByEmail_existingEmail_returnsEmployee() {
        // Arrange
        var employee = Employee.builder()
                .employeeCode("EMP100")
                .name("テスト太郎")
                .email("test@example.com")
                .password("$2a$10$encodedpassword")
                .role(Role.EMPLOYEE)
                .active(true)
                .version(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        employeeRepository.save(employee);

        // Act
        Optional<Employee> result = employeeRepository.findByEmail("test@example.com");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("テスト太郎");
    }

    @Test
    @DisplayName("存在しないメールアドレスでは空を返す")
    void findByEmail_nonExistingEmail_returnsEmpty() {
        Optional<Employee> result = employeeRepository.findByEmail("nonexist@example.com");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("メールアドレスの存在確認ができる")
    void existsByEmail_existingEmail_returnsTrue() {
        // Arrange
        var employee = Employee.builder()
                .employeeCode("EMP101")
                .name("テスト次郎")
                .email("exists@example.com")
                .password("$2a$10$encodedpassword")
                .role(Role.EMPLOYEE)
                .active(true)
                .version(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        employeeRepository.save(employee);

        // Act & Assert
        assertThat(employeeRepository.existsByEmail("exists@example.com")).isTrue();
        assertThat(employeeRepository.existsByEmail("no@example.com")).isFalse();
    }
}
