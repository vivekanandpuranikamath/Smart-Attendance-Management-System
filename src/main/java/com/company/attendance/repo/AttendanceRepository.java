package com.company.attendance.repo;

import com.company.attendance.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Optional<Attendance> findByEmployeeAndWorkDate(Employee e, LocalDate date);
    Optional<Attendance> findFirstByEmployeeAndCheckOutIsNullOrderByCheckInDesc(Employee e);
    List<Attendance> findTop30ByEmployeeOrderByWorkDateDesc(Employee e);
}
