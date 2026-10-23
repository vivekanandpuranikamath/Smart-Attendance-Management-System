package com.company.attendance.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "reminder_logs",
       uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "reminder_date", "reminder_type"}))
@Getter @Setter @NoArgsConstructor
public class ReminderLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "employee_id")
    private Employee employee;
    @Column(name = "reminder_date", nullable = false)
    private LocalDate reminderDate;
    @Column(name = "reminder_type", nullable = false, length = 20)
    private String type;
}
