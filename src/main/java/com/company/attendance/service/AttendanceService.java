package com.company.attendance.service;

import com.company.attendance.model.*;
import com.company.attendance.repo.*;
import com.company.attendance.web.Dto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AttendanceService {
    private final AttendanceRepository attendance;
    private final EmployeeRepository employees;
    private final LeaveRepository leaves;
    private final WfhRepository wfhs;
    private final ZoneId zone;
    @Value("${app.shift.day.start:09:00}") private String dayStart;
    @Value("${app.shift.night.start:21:00}") private String nightStart;

    public LocalTime shiftStart(Enums.ShiftType t) { return LocalTime.parse(t == Enums.ShiftType.DAY ? dayStart : nightStart); }
    public String shiftLabel(Employee e) { LocalTime s = shiftStart(e.getShift()); return s + " - " + s.plusHours(9) + " (" + e.getShift() + " shift)"; }

    /** Night shift people who scan after midnight still belong to the previous day's shift. */
    public LocalDate shiftDate(Employee e, LocalDateTime now) {
        return (e.getShift() == Enums.ShiftType.NIGHT && now.toLocalTime().isBefore(LocalTime.NOON))
                ? now.toLocalDate().minusDays(1) : now.toLocalDate();
    }
    public boolean onApprovedLeave(Employee e, LocalDate d) {
        return leaves.existsByEmployeeAndStatusAndFromDateLessThanEqualAndToDateGreaterThanEqual(e, Enums.RequestStatus.APPROVED, d, d);
    }
    public boolean onApprovedWfh(Employee e, LocalDate d) {
        return wfhs.existsByEmployeeAndWfhDateAndStatus(e, d, Enums.RequestStatus.APPROVED);
    }
    public boolean hasAttendance(Employee e, LocalDate d) {
        return attendance.findByEmployeeAndWorkDate(e, d).isPresent();
    }

    /** One QR scan toggles: first scan = check-in, second scan = check-out. */
    @Transactional
    public Map<String, Object> scan(String token) {
        Employee e = employees.findByQrToken(token == null ? "" : token.trim()).filter(Employee::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or inactive QR code"));
        LocalDateTime now = LocalDateTime.now(zone);
        Optional<Attendance> open = attendance.findFirstByEmployeeAndCheckOutIsNullOrderByCheckInDesc(e)
                .filter(a -> Duration.between(a.getCheckIn(), now).toHours() < 20);
        if (open.isPresent()) {
            Attendance a = open.get();
            if (Duration.between(a.getCheckIn(), now).getSeconds() < 60)
                throw new IllegalArgumentException(e.getFullName() + " is already checked in");
            double hours = Math.round(Duration.between(a.getCheckIn(), now).toMinutes() / 6.0) / 10.0;
            a.setCheckOut(now); a.setWorkedHours(hours); a.setStatus(hours >= 8 ? "PRESENT" : "SHORT_HOURS");
            attendance.save(a);
            return Dto.m("action", "CHECK_OUT", "employee", e.getFullName(), "empCode", e.getEmpCode(), "time", now,
                    "workedHours", hours, "message", "Checked out. Worked " + hours + " h" + (hours < 8 ? " (below the 8 h minimum)" : ""));
        }
        LocalDate wd = shiftDate(e, now);
        if (hasAttendance(e, wd)) throw new IllegalArgumentException("Attendance for today is already completed");
        Attendance a = new Attendance();
        a.setEmployee(e); a.setWorkDate(wd); a.setCheckIn(now); a.setStatus("IN_PROGRESS");
        attendance.save(a);
        return Dto.m("action", "CHECK_IN", "employee", e.getFullName(), "empCode", e.getEmpCode(), "time", now,
                "message", "Checked in. Have a productive day!");
    }

    /** Live state used by the manager dashboard and the employee page. */
    public Map<String, Object> todayState(Employee e) {
        LocalDateTime now = LocalDateTime.now(zone);
        LocalDate today = now.toLocalDate();
        Optional<Attendance> open = attendance.findFirstByEmployeeAndCheckOutIsNullOrderByCheckInDesc(e)
                .filter(a -> Duration.between(a.getCheckIn(), now).toHours() < 20);
        Attendance a = open.orElseGet(() -> attendance.findByEmployeeAndWorkDate(e, shiftDate(e, now)).orElse(null));
        String state;
        if (open.isPresent()) state = "ACTIVE";
        else if (a != null) state = "CHECKED_OUT";
        else if (onApprovedLeave(e, today)) state = "ON_LEAVE";
        else if (onApprovedWfh(e, today)) state = "WFH";
        else if (LeaveService.weekend(today)) state = "WEEKEND";
        else state = "NOT_CHECKED_IN";
        return Dto.m("state", state, "checkIn", a == null ? null : a.getCheckIn(),
                "checkOut", a == null ? null : a.getCheckOut(), "workedHours", a == null ? null : a.getWorkedHours());
    }
}
