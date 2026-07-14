package com.example.attendance.repository;

import com.example.attendance.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    List<Employee> findAllByOrderByEmployeeCodeAsc();

    @Query("SELECT MAX(e.employeeCode) FROM Employee e")
    Optional<String> findMaxEmployeeCode();
}
