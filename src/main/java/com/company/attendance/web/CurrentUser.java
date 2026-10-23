package com.company.attendance.web;

import com.company.attendance.model.Employee;
import com.company.attendance.repo.EmployeeRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class CurrentUser {
    private final EmployeeRepository employees;

    public Employee get(HttpSession s) {
        Object id = s.getAttribute("uid");
        if (id == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        return employees.findById((Long) id).filter(Employee::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in"));
    }
}
