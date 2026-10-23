package com.company.attendance.repo;

import com.company.attendance.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;

public interface WfhRepository extends JpaRepository<WfhRequest, Long> {
    List<WfhRequest> findByEmployeeOrderByCreatedAtDesc(Employee e);
    List<WfhRequest> findByEmployeeInOrderByCreatedAtDesc(Collection<Employee> employees);
    boolean existsByEmployeeAndWfhDateAndStatus(Employee e, LocalDate d, Enums.RequestStatus s);
    boolean existsByEmployeeAndWfhDateAndStatusNot(Employee e, LocalDate d, Enums.RequestStatus s);
}
