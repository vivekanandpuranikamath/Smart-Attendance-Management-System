package com.company.attendance.repo;

import com.company.attendance.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmpCode(String empCode);
    Optional<Employee> findByQrToken(String token);
    boolean existsByEmpCode(String empCode);
    boolean existsByCompanyEmail(String email);
    List<Employee> findByManager(Employee manager);
    List<Employee> findByActiveTrueAndRoleNot(Enums.Role role);
}
