package com.company.attendance.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "wfh_requests")
@Getter @Setter @NoArgsConstructor
public class WfhRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "employee_id")
    private Employee employee;
    @Column(nullable = false) private LocalDate wfhDate;
    @Column(length = 1000) private String reason;
    @Enumerated(EnumType.STRING) @Column(length = 20)
    private Enums.RequestStatus status = Enums.RequestStatus.PENDING;
    @Column(length = 1000) private String managerRemark;
    private LocalDateTime createdAt;
    private LocalDateTime decidedAt;
}
