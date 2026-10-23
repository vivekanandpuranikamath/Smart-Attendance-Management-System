package com.company.attendance.service;

import com.company.attendance.model.*;
import com.company.attendance.repo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 * Sends the "scan your attendance QR" e-mail to the employee's COMPANY e-mail:
 *  PRE_SHIFT   - N minutes BEFORE the shift starts (employees with no approved leave / approved WFH)
 *  MISSED_SCAN - shortly AFTER the shift started if the employee still has not scanned and has no leave/WFH
 * Runs every 5 minutes, one mail per employee per type per day. Weekends are skipped.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderScheduler {
    private final EmployeeRepository employees;
    private final ReminderLogRepository logs;
    private final AttendanceService attendance;
    private final MailService mail;
    private final ZoneId zone;
    @Value("${app.reminder.minutes-before:30}") private int before;
    @Value("${app.reminder.late-after-minutes:15}") private int lateAfter;

    @Scheduled(cron = "0 */5 * * * *", zone = "${app.timezone:Asia/Kolkata}")
    public void run() {
        LocalDateTime now = LocalDateTime.now(zone);
        for (Enums.ShiftType shift : Enums.ShiftType.values()) {
            LocalDateTime start = now.toLocalDate().atTime(attendance.shiftStart(shift));
            if (LeaveService.weekend(start.toLocalDate())) continue;
            if (!now.isBefore(start.minusMinutes(before)) && now.isBefore(start)) notifyShift(shift, start, "PRE_SHIFT");
            else if (!now.isBefore(start.plusMinutes(lateAfter)) && now.isBefore(start.plusMinutes(lateAfter + 30)))
                notifyShift(shift, start, "MISSED_SCAN");
        }
    }

    void notifyShift(Enums.ShiftType shift, LocalDateTime start, String type) {
        LocalDate day = start.toLocalDate();
        String time = start.format(DateTimeFormatter.ofPattern("hh:mm a"));
        for (Employee e : employees.findByActiveTrueAndRoleNot(Enums.Role.ADMIN)) {
            if (e.getShift() != shift) continue;
            if (attendance.hasAttendance(e, day) || attendance.onApprovedLeave(e, day) || attendance.onApprovedWfh(e, day)) continue;
            if (logs.existsByEmployeeAndReminderDateAndType(e, day, type)) continue;
            String subject, body;
            if (type.equals("PRE_SHIFT")) {
                subject = "Reminder: your " + shift.name().toLowerCase() + " shift starts at " + time;
                body = "Hi " + e.getFullName() + ",\n\nYour shift starts at " + time + " today. You have no leave or work-from-home "
                     + "approved for today, so please scan your attendance QR as soon as you reach the office.\n\n"
                     + "If you cannot come, apply for leave / WFH in the portal.\n\nSmart Attendance System";
            } else {
                subject = "Action needed: attendance not recorded for today";
                body = "Hi " + e.getFullName() + ",\n\nYour shift started at " + time + " but no attendance scan was found, and you have no leave "
                     + "or WFH approved. If you are in the office, please scan your QR now (or tell your manager). "
                     + "Otherwise today will be marked absent.\n\nSmart Attendance System";
            }
            mail.send(e.getCompanyEmail(), subject, body);
            ReminderLog l = new ReminderLog();
            l.setEmployee(e); l.setReminderDate(day); l.setType(type);
            logs.save(l);
        }
    }
}
