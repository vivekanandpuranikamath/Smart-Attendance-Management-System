package com.company.attendance.web;

import com.company.attendance.model.*;
import com.company.attendance.service.LeaveService;
import java.util.*;

/** Small JSON mappers: entities are never sent directly (no password hashes, no lazy-loading cycles). */
public final class Dto {
    private Dto() {}

    public static Map<String, Object> m(Object... kv) {
        Map<String, Object> r = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) r.put((String) kv[i], kv[i + 1]);
        return r;
    }

    public static Map<String, Object> emp(Employee e) {
        Employee mg = e.getManager();
        return m("id", e.getId(), "empCode", e.getEmpCode(), "fullName", e.getFullName(),
                "companyEmail", e.getCompanyEmail(), "personalEmail", e.getPersonalEmail(), "phone", e.getPhone(),
                "address", e.getAddress(), "gender", e.getGender(), "emergencyContact", e.getEmergencyContact(),
                "department", e.getDepartment(), "designation", e.getDesignation(),
                "dateOfBirth", e.getDateOfBirth(), "joiningDate", e.getJoiningDate(),
                "role", e.getRole(), "shift", e.getShift(), "active", e.isActive(),
                "mustChangePassword", e.isMustChangePassword(),
                "managerId", mg == null ? null : mg.getId(), "managerName", mg == null ? null : mg.getFullName());
    }

    public static Map<String, Object> leave(LeaveRequest l) {
        Employee e = l.getEmployee();
        return m("id", l.getId(), "employeeId", e.getId(), "empCode", e.getEmpCode(), "employeeName", e.getFullName(),
                "type", l.getType(), "fromDate", l.getFromDate(), "toDate", l.getToDate(),
                "days", LeaveService.workingDays(l.getFromDate(), l.getToDate(), null),
                "reason", l.getReason(), "status", l.getStatus(), "managerRemark", l.getManagerRemark(),
                "createdAt", l.getCreatedAt(), "decidedAt", l.getDecidedAt());
    }

    public static Map<String, Object> wfh(WfhRequest w) {
        Employee e = w.getEmployee();
        return m("id", w.getId(), "employeeId", e.getId(), "empCode", e.getEmpCode(), "employeeName", e.getFullName(),
                "date", w.getWfhDate(), "reason", w.getReason(), "status", w.getStatus(),
                "managerRemark", w.getManagerRemark(), "createdAt", w.getCreatedAt(), "decidedAt", w.getDecidedAt());
    }

    public static Map<String, Object> project(Project p) {
        List<Map<String, Object>> team = new ArrayList<>();
        for (Employee e : p.getMembers())
            team.add(m("id", e.getId(), "empCode", e.getEmpCode(), "fullName", e.getFullName(),
                    "designation", e.getDesignation(), "companyEmail", e.getCompanyEmail()));
        team.sort(Comparator.comparing(t -> String.valueOf(t.get("fullName"))));
        Employee mg = p.getManager();
        return m("id", p.getId(), "name", p.getName(), "description", p.getDescription(), "status", p.getStatus(),
                "startDate", p.getStartDate(), "endDate", p.getEndDate(),
                "managerName", mg == null ? null : mg.getFullName(), "team", team);
    }

    public static Map<String, Object> attendance(Attendance a) {
        return m("workDate", a.getWorkDate(), "checkIn", a.getCheckIn(), "checkOut", a.getCheckOut(),
                "workedHours", a.getWorkedHours(), "status", a.getStatus());
    }
}
