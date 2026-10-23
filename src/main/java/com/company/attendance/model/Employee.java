package com.company.attendance.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "employees")
@Getter @Setter @NoArgsConstructor
public class Employee {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "emp_code", nullable = false, unique = true, length = 30)
    private String empCode;
    @Column(nullable = false)
    private String fullName;
    @Column(nullable = false, unique = true)
    private String companyEmail;
    private String personalEmail;
    private String phone;
    @Column(length = 500)
    private String address;
    private String gender;
    private String emergencyContact;
    private String department;
    private String designation;
    private LocalDate dateOfBirth;
    private LocalDate joiningDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Enums.Role role = Enums.Role.EMPLOYEE;
    @Enumerated(EnumType.STRING) @Column(name = "shift_type", nullable = false, length = 20)
    private Enums.ShiftType shift = Enums.ShiftType.DAY;
    @Column(nullable = false)
    private String passwordHash;
    private boolean mustChangePassword = true;
    private boolean active = true;
    @Column(nullable = false, unique = true, length = 64)
    private String qrToken;
    @ManyToOne
    @JoinColumn(name = "manager_id")
    private Employee manager;
}
