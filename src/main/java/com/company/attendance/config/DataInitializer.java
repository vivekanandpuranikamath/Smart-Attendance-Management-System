package com.company.attendance.config;

import com.company.attendance.model.*;
import com.company.attendance.repo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.*;

/** Seeds demo data on first start so every page can be tried immediately. */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    private final EmployeeRepository employees;
    private final ProjectRepository projects;
    private final PasswordEncoder encoder;

    @Override
    public void run(String... args) {
        if (employees.count() > 0) return;
        Employee admin = make("ADMIN001", "System Admin", "admin@company.com", Enums.Role.ADMIN, Enums.ShiftType.DAY, null, "Admin@123", "Administration", "HR Admin");
        Employee mgr = make("MGR001", "Meera Manager", "meera.manager@company.com", Enums.Role.MANAGER, Enums.ShiftType.DAY, null, "Manager@123", "Engineering", "Engineering Manager");
        Employee e1 = make("EMP001", "Arjun Rao", "arjun.rao@company.com", Enums.Role.EMPLOYEE, Enums.ShiftType.DAY, mgr, "Employee@123", "Engineering", "Software Engineer");
        Employee e2 = make("EMP002", "Nisha Iyer", "nisha.iyer@company.com", Enums.Role.EMPLOYEE, Enums.ShiftType.NIGHT, mgr, "Employee@123", "Engineering", "QA Engineer");
        project("Customer Portal", "Self-service portal for customers.", Enums.ProjectStatus.ACTIVE, LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(3), mgr, e1, e2);
        project("Billing Migration", "Migrate billing to the new platform.", Enums.ProjectStatus.COMPLETED, LocalDate.now().minusMonths(8), LocalDate.now().minusMonths(1), mgr, e1);
        project("Mobile App v2", "Planned rewrite of the mobile app.", Enums.ProjectStatus.FUTURE, LocalDate.now().plusMonths(2), LocalDate.now().plusMonths(8), mgr, e1, e2);
        log.info("Demo users created: ADMIN001/Admin@123, MGR001/Manager@123, EMP001 & EMP002 / Employee@123");
    }

    private Employee make(String code, String name, String email, Enums.Role role, Enums.ShiftType shift,
                          Employee mgr, String pw, String dept, String desig) {
        Employee e = new Employee();
        e.setEmpCode(code); e.setFullName(name); e.setCompanyEmail(email); e.setRole(role); e.setShift(shift);
        e.setManager(mgr); e.setDepartment(dept); e.setDesignation(desig); e.setJoiningDate(LocalDate.now().minusYears(1));
        e.setPhone("9000000000"); e.setGender("-"); e.setAddress("Bengaluru, India");
        e.setPasswordHash(encoder.encode(pw)); e.setMustChangePassword(role != Enums.Role.ADMIN);
        e.setQrToken(UUID.randomUUID().toString().replace("-", ""));
        return employees.save(e);
    }

    private void project(String name, String desc, Enums.ProjectStatus st, LocalDate from, LocalDate to, Employee mgr, Employee... members) {
        Project p = new Project();
        p.setName(name); p.setDescription(desc); p.setStatus(st); p.setStartDate(from); p.setEndDate(to); p.setManager(mgr);
        p.getMembers().addAll(Arrays.asList(members));
        projects.save(p);
    }
}
