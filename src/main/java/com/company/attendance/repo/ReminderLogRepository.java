package com.company.attendance.repo;

import com.company.attendance.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;

public interface ReminderLogRepository extends JpaRepository<ReminderLog, Long> {
    boolean existsByEmployeeAndReminderDateAndType(Employee e, LocalDate d, String type);
}
