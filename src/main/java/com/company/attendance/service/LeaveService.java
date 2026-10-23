package com.company.attendance.service;

import com.company.attendance.model.*;
import com.company.attendance.repo.LeaveRepository;
import com.company.attendance.web.Dto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

/** Every employee gets 1 SICK + 1 PERSONAL leave day per month (weekends are company holidays). */
@Service
@RequiredArgsConstructor
public class LeaveService {
    public static final int MONTHLY_QUOTA = 1;
    private final LeaveRepository leaves;
    private final ZoneId zone;

    public static boolean weekend(LocalDate d) {
        return d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
    /** working days between from..to that fall in month ym (ym == null: whole range) */
    public static long workingDays(LocalDate from, LocalDate to, YearMonth ym) {
        long n = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1))
            if (!weekend(d) && (ym == null || YearMonth.from(d).equals(ym))) n++;
        return n;
    }

    private long used(Employee e, Enums.LeaveType t, YearMonth ym) {
        return leaves.findByEmployeeAndStatusNot(e, Enums.RequestStatus.REJECTED).stream()
                .filter(l -> l.getType() == t)
                .mapToLong(l -> workingDays(l.getFromDate(), l.getToDate(), ym)).sum();
    }

    public Map<String, Object> balance(Employee e) {
        YearMonth ym = YearMonth.now(zone);
        Map<String, Object> r = Dto.m("month", ym.toString());
        for (Enums.LeaveType t : Enums.LeaveType.values()) {
            long u = used(e, t, ym);
            r.put(t.name().toLowerCase(), Dto.m("quota", MONTHLY_QUOTA, "used", u, "remaining", Math.max(0, MONTHLY_QUOTA - u)));
        }
        return r;
    }

    public LeaveRequest apply(Employee e, Enums.LeaveType type, LocalDate from, LocalDate to, String reason) {
        if (type == null || from == null || to == null) throw new IllegalArgumentException("Leave type, from date and to date are required");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Please give a reason for the leave");
        if (to.isBefore(from)) throw new IllegalArgumentException("'To' date cannot be before 'From' date");
        if (workingDays(from, to, null) == 0) throw new IllegalArgumentException("Selected dates are weekends - already company holidays");
        for (YearMonth ym = YearMonth.from(from); !ym.isAfter(YearMonth.from(to)); ym = ym.plusMonths(1)) {
            long req = workingDays(from, to, ym);
            if (req > 0 && used(e, type, ym) + req > MONTHLY_QUOTA)
                throw new IllegalArgumentException("Only " + MONTHLY_QUOTA + " " + type.name().toLowerCase()
                        + " leave per month is allowed (" + ym + " quota exceeded)");
        }
        LeaveRequest l = new LeaveRequest();
        l.setEmployee(e); l.setType(type); l.setFromDate(from); l.setToDate(to); l.setReason(reason.trim());
        l.setCreatedAt(LocalDateTime.now(zone));
        return leaves.save(l);
    }
}
