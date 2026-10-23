package com.company.attendance.repo;

import com.company.attendance.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByManager(Employee manager);
    List<Project> findByMembers_Id(Long employeeId);
}
