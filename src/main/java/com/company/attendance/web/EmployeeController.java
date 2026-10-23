package com.company.attendance.web;

import com.company.attendance.model.*;
import com.company.attendance.repo.*;
import com.company.attendance.service.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/employee")
@RequiredArgsConstructor
public class EmployeeController {
    private final CurrentUser current;
    private final ProjectRepository projects;
    private final LeaveRepository leaves;
    private final WfhRepository wfhs;
    private final AttendanceRepository attendanceRepo;
    private final AttendanceService attendance;
    private final LeaveService leaveService;
    private final ZoneId zone;

    public record LeaveForm(Enums.LeaveType type, LocalDate fromDate, LocalDate toDate, String reason) {}
    public record WfhForm(LocalDate date, String reason) {}

    @GetMapping("/profile")
    public Map<String, Object> profile(HttpSession s) {
        Employee e = current.get(s);
        Map<String, Object> r = Dto.emp(e);
        r.put("shiftTiming", attendance.shiftLabel(e));
        return r;
    }

    @GetMapping("/projects")
    public List<Map<String, Object>> myProjects(HttpSession s) {
        return projects.findByMembers_Id(current.get(s).getId()).stream().map(Dto::project).toList();
    }

    @GetMapping("/attendance")
    public Map<String, Object> myAttendance(HttpSession s) {
        Employee e = current.get(s);
        return Dto.m("today", attendance.todayState(e),
                "history", attendanceRepo.findTop30ByEmployeeOrderByWorkDateDesc(e).stream().map(Dto::attendance).toList());
    }

    @GetMapping("/leaves")
    public Map<String, Object> myLeaves(HttpSession s) {
        Employee e = current.get(s);
        return Dto.m("balance", leaveService.balance(e),
                "requests", leaves.findByEmployeeOrderByCreatedAtDesc(e).stream().map(Dto::leave).toList());
    }

    @PostMapping("/leaves")
    public Map<String, Object> applyLeave(@RequestBody LeaveForm f, HttpSession s) {
        return Dto.leave(leaveService.apply(current.get(s), f.type(), f.fromDate(), f.toDate(), f.reason()));
    }

    @GetMapping("/wfh")
    public List<Map<String, Object>> myWfh(HttpSession s) {
        return wfhs.findByEmployeeOrderByCreatedAtDesc(current.get(s)).stream().map(Dto::wfh).toList();
    }

    @PostMapping("/wfh")
    public Map<String, Object> applyWfh(@RequestBody WfhForm f, HttpSession s) {
        Employee e = current.get(s);
        if (f.date() == null) throw new IllegalArgumentException("Please choose the WFH date");
        if (f.reason() == null || f.reason().isBlank()) throw new IllegalArgumentException("Please give a reason");
        if (f.date().isBefore(LocalDate.now(zone))) throw new IllegalArgumentException("WFH date cannot be in the past");
        if (LeaveService.weekend(f.date())) throw new IllegalArgumentException("Weekends are company holidays");
        if (wfhs.existsByEmployeeAndWfhDateAndStatusNot(e, f.date(), Enums.RequestStatus.REJECTED))
            throw new IllegalArgumentException("You already have a WFH request for that date");
        WfhRequest w = new WfhRequest();
        w.setEmployee(e); w.setWfhDate(f.date()); w.setReason(f.reason().trim()); w.setCreatedAt(LocalDateTime.now(zone));
        return Dto.wfh(wfhs.save(w));
    }
}
