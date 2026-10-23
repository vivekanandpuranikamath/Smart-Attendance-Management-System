package com.company.attendance.repo;

import com.company.attendance.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;

public interface LeaveRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByEmployeeOrderByCreatedAtDesc(Employee e);
    List<LeaveRequest> findByEmployeeInOrderByCreatedAtDesc(Collection<Employee> employees);
    List<LeaveRequest> findByEmployeeAndStatusNot(Employee e, Enums.RequestStatus status);
    boolean existsByEmployeeAndStatusAndFromDateLessThanEqualAndToDateGreaterThanEqual(
            Employee e, Enums.RequestStatus status, LocalDate d1, LocalDate d2);
}
