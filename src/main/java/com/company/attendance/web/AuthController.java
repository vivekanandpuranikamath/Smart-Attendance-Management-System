package com.company.attendance.web;

import com.company.attendance.model.*;
import com.company.attendance.repo.EmployeeRepository;
import com.company.attendance.service.AttendanceService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final EmployeeRepository employees;
    private final PasswordEncoder encoder;
    private final CurrentUser current;
    private final AttendanceService attendance;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> b, HttpSession s) {
        String bad = "Invalid employee ID or password";
        Employee e = employees.findByEmpCode(b.getOrDefault("empCode", "").trim().toUpperCase())
                .filter(Employee::isActive).orElseThrow(() -> new IllegalArgumentException(bad));
        if (!encoder.matches(b.getOrDefault("password", ""), e.getPasswordHash())) throw new IllegalArgumentException(bad);
        s.setAttribute("uid", e.getId());
        s.setAttribute("role", e.getRole().name());
        String page = e.getRole() == Enums.Role.ADMIN ? "/admin" : e.getRole() == Enums.Role.MANAGER ? "/manager" : "/employee";
        return Dto.m("redirect", page, "user", Dto.emp(e));
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpSession s) { s.invalidate(); return Dto.m("ok", true); }

    @GetMapping("/me")
    public Map<String, Object> me(HttpSession s) {
        Employee e = current.get(s);
        Map<String, Object> r = Dto.emp(e);
        r.put("shiftTiming", attendance.shiftLabel(e));
        return r;
    }

    @PostMapping("/change-password")
    public Map<String, Object> change(@RequestBody Map<String, String> b, HttpSession s) {
        Employee e = current.get(s);
        if (!encoder.matches(b.getOrDefault("currentPassword", ""), e.getPasswordHash()))
            throw new IllegalArgumentException("Current password is wrong");
        String np = b.getOrDefault("newPassword", "");
        if (np.length() < 8) throw new IllegalArgumentException("New password must be at least 8 characters");
        e.setPasswordHash(encoder.encode(np));
        e.setMustChangePassword(false);
        employees.save(e);
        return Dto.m("ok", true);
    }
}
