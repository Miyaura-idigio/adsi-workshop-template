package com.example.attendance.service;

import com.example.attendance.dto.CreateEmployeeRequest;
import com.example.attendance.dto.EmployeeResponse;
import com.example.attendance.dto.UpdateEmployeeRequest;
import com.example.attendance.entity.Employee;
import com.example.attendance.exception.DuplicateEmailException;
import com.example.attendance.exception.ResourceNotFoundException;
import com.example.attendance.repository.EmployeeRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository,
                               PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<EmployeeResponse> findAll() {
        return employeeRepository.findByActiveTrueOrderByEmployeeCodeAsc().stream()
                .map(EmployeeResponse::from)
                .toList();
    }

    @Override
    public EmployeeResponse findById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("社員が見つかりません (ID: " + id + ")"));
        return EmployeeResponse.from(employee);
    }

    @Override
    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest request) {
        if (employeeRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException("このメールアドレスは既に使用されています");
        }

        String employeeCode = generateNextEmployeeCode();

        Employee employee = Employee.builder()
                .employeeCode(employeeCode)
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();

        Employee saved = employeeRepository.save(employee);
        return EmployeeResponse.from(saved);
    }

    @Override
    @Transactional
    public EmployeeResponse update(Long id, UpdateEmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("社員が見つかりません (ID: " + id + ")"));

        if (!employee.getVersion().equals(request.version())) {
            throw new org.springframework.orm.ObjectOptimisticLockingFailureException(Employee.class, id);
        }

        if (employeeRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new DuplicateEmailException("このメールアドレスは既に使用されています");
        }

        employee.setName(request.name());
        employee.setEmail(request.email());
        employee.setRole(request.role());

        Employee saved = employeeRepository.save(employee);
        return EmployeeResponse.from(saved);
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("社員が見つかりません (ID: " + id + ")"));

        employee.setActive(false);
        employeeRepository.save(employee);
    }

    private String generateNextEmployeeCode() {
        return employeeRepository.findMaxEmployeeCode()
                .map(maxCode -> {
                    int number = Integer.parseInt(maxCode.substring(3));
                    return String.format("EMP%03d", number + 1);
                })
                .orElse("EMP001");
    }
}
