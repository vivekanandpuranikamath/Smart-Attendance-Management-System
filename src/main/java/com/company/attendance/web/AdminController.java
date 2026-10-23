package com.company.attendance.web;

import com.company.attendance.model.*;
import com.company.attendance.repo.EmployeeRepository;
import com.company.attendance.service.MailService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final EmployeeRepository employees;
    private final PasswordEncoder encoder;
    private final MailService mail;
    private final CurrentUser current;

    public record EmployeeForm(String empCode, String fullName, String companyEmail, String personalEmail, String phone,
                               String address, String gender, String emergencyContact, String department, String designation,
                               LocalDate dateOfBirth, LocalDate joiningDate, Enums.Role role, Enums.ShiftType shift,
                               Long managerId, String tempPassword) {}

    @GetMapping("/employees")
    public List<Map<String, Object>> list() {
        return employees.findAll().stream().sorted(Comparator.comparing(Employee::getEmpCode)).map(Dto::emp).toList();
    }

    @PostMapping("/employees")
    public Map<String, Object> create(@RequestBody EmployeeForm f) {
        req(f.empCode(), "Employee ID"); req(f.fullName(), "Full name"); req(f.companyEmail(), "Company e-mail");
        String code = f.empCode().trim().toUpperCase();
        if (employees.existsByEmpCode(code)) throw new IllegalArgumentException("Employee ID " + code + " already exists");
        if (employees.existsByCompanyEmail(f.companyEmail().trim())) throw new IllegalArgumentException("Company e-mail already used");
        String temp = (f.tempPassword() == null || f.tempPassword().isBlank()) ? randomPassword() : f.tempPassword().trim();
        if (temp.length() < 6) throw new IllegalArgumentException("Temporary password must be at least 6 characters");
        Employee e = new Employee();
        e.setEmpCode(code);
        apply(e, f);
        e.setPasswordHash(encoder.encode(temp));
        e.setMustChangePassword(true);
        e.setQrToken(UUID.randomUUID().toString().replace("-", ""));
        employees.save(e);
        mail.send(e.getCompanyEmail(), "Welcome - your attendance portal login",
                "Hi " + e.getFullName() + ",\n\nYour account is ready.\nEmployee ID: " + code + "\nTemporary password: " + temp
                + "\n\nPlease change your password after the first login. Your attendance QR will be handed over by the admin.");
        return Dto.m("employee", Dto.emp(e), "tempPassword", temp);
    }

    @PutMapping("/employees/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody EmployeeForm f) {
        Employee e = employees.findById(id).orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        req(f.fullName(), "Full name"); req(f.companyEmail(), "Company e-mail");
        if (!e.getCompanyEmail().equalsIgnoreCase(f.companyEmail().trim()) && employees.existsByCompanyEmail(f.companyEmail().trim()))
            throw new IllegalArgumentException("Company e-mail already used");
        apply(e, f);
        employees.save(e);
        return Dto.emp(e);
    }

    @PostMapping("/employees/{id}/toggle")
    public Map<String, Object> toggle(@PathVariable Long id, HttpSession s) {
        Employee e = employees.findById(id).orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        if (e.getId().equals(current.get(s).getId())) throw new IllegalArgumentException("You cannot deactivate yourself");
        e.setActive(!e.isActive());
        employees.save(e);
        return Dto.emp(e);
    }

    @PostMapping("/employees/{id}/reset-password")
    public Map<String, Object> reset(@PathVariable Long id) {
        Employee e = employees.findById(id).orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        String temp = randomPassword();
        e.setPasswordHash(encoder.encode(temp));
        e.setMustChangePassword(true);
        employees.save(e);
        mail.send(e.getCompanyEmail(), "Your password was reset", "Hi " + e.getFullName() + ",\nTemporary password: " + temp);
        return Dto.m("tempPassword", temp);
    }

    /** QR image the admin prints / hands over to the employee. Content = secret attendance token. */
    @GetMapping(value = "/employees/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] qr(@PathVariable Long id) throws Exception {
        Employee e = employees.findById(id).orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        BitMatrix matrix = new QRCodeWriter().encode(e.getQrToken(), BarcodeFormat.QR_CODE, 320, 320);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }

    private void apply(Employee e, EmployeeForm f) {
        e.setFullName(f.fullName().trim());
        e.setCompanyEmail(f.companyEmail().trim());
        e.setPersonalEmail(f.personalEmail()); e.setPhone(f.phone()); e.setAddress(f.address()); e.setGender(f.gender());
        e.setEmergencyContact(f.emergencyContact()); e.setDepartment(f.department()); e.setDesignation(f.designation());
        e.setDateOfBirth(f.dateOfBirth()); e.setJoiningDate(f.joiningDate());
        e.setRole(f.role() == null ? Enums.Role.EMPLOYEE : f.role());
        e.setShift(f.shift() == null ? Enums.ShiftType.DAY : f.shift());
        Employee mgr = null;
        if (f.managerId() != null && !f.managerId().equals(e.getId())) {
            mgr = employees.findById(f.managerId()).filter(m -> m.getRole() == Enums.Role.MANAGER)
                    .orElseThrow(() -> new IllegalArgumentException("Selected manager is invalid"));
        }
        e.setManager(e.getRole() == Enums.Role.EMPLOYEE ? mgr : null);
    }

    private static void req(String v, String name) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(name + " is required");
    }

    private static String randomPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder("Tmp@");
        for (int i = 0; i < 6; i++) sb.append(chars.charAt(r.nextInt(chars.length())));
        return sb.toString();
    }
}
