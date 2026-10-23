package com.company.attendance.web;

import com.company.attendance.model.*;
import com.company.attendance.repo.*;
import com.company.attendance.service.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;

/** Everything a manager sees/changes lives in the same tables the employee pages read, so both stay in sync. */
@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
public class ManagerController {
    private final CurrentUser current;
    private final EmployeeRepository employees;
    private final ProjectRepository projects;
    private final LeaveRepository leaves;
    private final WfhRepository wfhs;
    private final AttendanceRepository attendanceRepo;
    private final AttendanceService attendance;
    private final MailService mail;
    private final ZoneId zone;

    public record ProjectForm(String name, String description, Enums.ProjectStatus status,
                              LocalDate startDate, LocalDate endDate, List<Long> memberIds) {}
    public record TeamForm(String phone, String address, String department, String designation, Enums.ShiftType shift) {}

    // ---------- attendance ----------
    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(HttpSession s) {
        Employee mgr = current.get(s);
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Integer> c = new LinkedHashMap<>();
        for (String k : List.of("ACTIVE", "CHECKED_OUT", "ON_LEAVE", "WFH", "NOT_CHECKED_IN", "WEEKEND")) c.put(k, 0);
        for (Employee e : employees.findByManager(mgr)) {
            if (!e.isActive()) continue;
            Map<String, Object> st = attendance.todayState(e);
            c.merge((String) st.get("state"), 1, Integer::sum);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", e.getId()); row.put("empCode", e.getEmpCode()); row.put("fullName", e.getFullName());
            row.put("shift", e.getShift()); row.put("designation", e.getDesignation());
            row.putAll(st);
            rows.add(row);
        }
        long pendingLeaves = leaves.findByEmployeeInOrderByCreatedAtDesc(employees.findByManager(mgr)).stream()
                .filter(l -> l.getStatus() == Enums.RequestStatus.PENDING).count();
        long pendingWfh = wfhs.findByEmployeeInOrderByCreatedAtDesc(employees.findByManager(mgr)).stream()
                .filter(w -> w.getStatus() == Enums.RequestStatus.PENDING).count();
        return Dto.m("teamSize", rows.size(), "counts", c, "pendingLeaves", pendingLeaves, "pendingWfh", pendingWfh, "rows", rows);
    }

    @GetMapping("/attendance")
    public List<Map<String, Object>> byDate(@RequestParam String date, HttpSession s) {
        LocalDate d = LocalDate.parse(date);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Employee e : employees.findByManager(current.get(s))) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("empCode", e.getEmpCode()); row.put("fullName", e.getFullName()); row.put("shift", e.getShift());
            Optional<Attendance> a = attendanceRepo.findByEmployeeAndWorkDate(e, d);
            row.put("checkIn", a.map(Attendance::getCheckIn).orElse(null));
            row.put("checkOut", a.map(Attendance::getCheckOut).orElse(null));
            row.put("workedHours", a.map(Attendance::getWorkedHours).orElse(null));
            String status = a.map(Attendance::getStatus).orElse(null);
            if (status == null) status = attendance.onApprovedLeave(e, d) ? "ON_LEAVE" : attendance.onApprovedWfh(e, d) ? "WFH"
                    : LeaveService.weekend(d) ? "WEEKEND" : "ABSENT";
            row.put("status", status);
            rows.add(row);
        }
        return rows;
    }

    // ---------- leave applications ----------
    @GetMapping("/leaves")
    public List<Map<String, Object>> leaves(HttpSession s) {
        List<Employee> team = employees.findByManager(current.get(s));
        if (team.isEmpty()) return List.of();
        return leaves.findByEmployeeInOrderByCreatedAtDesc(team).stream().map(Dto::leave).toList();
    }

    @PutMapping("/leaves/{id}")
    public Map<String, Object> decideLeave(@PathVariable Long id, @RequestBody Map<String, Object> b, HttpSession s) {
        Employee mgr = current.get(s);
        LeaveRequest l = leaves.findById(id).orElseThrow(() -> new IllegalArgumentException("Leave request not found"));
        ownTeam(mgr, l.getEmployee());
        if (l.getStatus() != Enums.RequestStatus.PENDING) throw new IllegalArgumentException("Already decided");
        boolean approve = Boolean.TRUE.equals(b.get("approve"));
        String remark = remark(b, approve);
        l.setStatus(approve ? Enums.RequestStatus.APPROVED : Enums.RequestStatus.REJECTED);
        l.setManagerRemark(remark); l.setDecidedAt(LocalDateTime.now(zone));
        leaves.save(l);
        mail.send(l.getEmployee().getCompanyEmail(), "Your leave request was " + l.getStatus().name().toLowerCase(),
                "Leave " + l.getFromDate() + " to " + l.getToDate() + " - " + l.getStatus() + (remark == null ? "" : "\nManager remark: " + remark));
        return Dto.leave(l);
    }

    // ---------- work from home ----------
    @GetMapping("/wfh")
    public List<Map<String, Object>> wfh(HttpSession s) {
        List<Employee> team = employees.findByManager(current.get(s));
        if (team.isEmpty()) return List.of();
        return wfhs.findByEmployeeInOrderByCreatedAtDesc(team).stream().map(Dto::wfh).toList();
    }

    @PutMapping("/wfh/{id}")
    public Map<String, Object> decideWfh(@PathVariable Long id, @RequestBody Map<String, Object> b, HttpSession s) {
        Employee mgr = current.get(s);
        WfhRequest w = wfhs.findById(id).orElseThrow(() -> new IllegalArgumentException("WFH request not found"));
        ownTeam(mgr, w.getEmployee());
        if (w.getStatus() != Enums.RequestStatus.PENDING) throw new IllegalArgumentException("Already decided");
        boolean approve = Boolean.TRUE.equals(b.get("approve"));
        String remark = remark(b, approve);
        w.setStatus(approve ? Enums.RequestStatus.APPROVED : Enums.RequestStatus.REJECTED);
        w.setManagerRemark(remark); w.setDecidedAt(LocalDateTime.now(zone));
        wfhs.save(w);
        mail.send(w.getEmployee().getCompanyEmail(), "Your WFH request was " + w.getStatus().name().toLowerCase(),
                "Work from home on " + w.getWfhDate() + " - " + w.getStatus() + (remark == null ? "" : "\nManager remark: " + remark));
        return Dto.wfh(w);
    }

    // ---------- projects (active / completed / future) ----------
    @GetMapping("/projects")
    public List<Map<String, Object>> projects(HttpSession s) {
        return projects.findByManager(current.get(s)).stream().map(Dto::project).toList();
    }

    @PostMapping("/projects")
    public Map<String, Object> createProject(@RequestBody ProjectForm f, HttpSession s) {
        Employee mgr = current.get(s);
        Project p = new Project();
        p.setManager(mgr);
        fill(p, f, mgr);
        return Dto.project(projects.save(p));
    }

    @PutMapping("/projects/{id}")
    public Map<String, Object> updateProject(@PathVariable Long id, @RequestBody ProjectForm f, HttpSession s) {
        Employee mgr = current.get(s);
        Project p = projects.findById(id).orElseThrow(() -> new IllegalArgumentException("Project not found"));
        if (p.getManager() == null || !p.getManager().getId().equals(mgr.getId())) throw new IllegalArgumentException("Not your project");
        fill(p, f, mgr);
        return Dto.project(projects.save(p));
    }

    // ---------- team (manager edits are visible to the employee instantly) ----------
    @GetMapping("/team")
    public List<Map<String, Object>> team(HttpSession s) {
        return employees.findByManager(current.get(s)).stream().map(Dto::emp).toList();
    }

    @PutMapping("/team/{id}")
    public Map<String, Object> updateTeam(@PathVariable Long id, @RequestBody TeamForm f, HttpSession s) {
        Employee e = employees.findById(id).orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        ownTeam(current.get(s), e);
        if (f.phone() != null) e.setPhone(f.phone());
        if (f.address() != null) e.setAddress(f.address());
        if (f.department() != null) e.setDepartment(f.department());
        if (f.designation() != null) e.setDesignation(f.designation());
        if (f.shift() != null) e.setShift(f.shift());
        return Dto.emp(employees.save(e));
    }

    // ---------- helpers ----------
    private void fill(Project p, ProjectForm f, Employee mgr) {
        if (f.name() == null || f.name().isBlank()) throw new IllegalArgumentException("Project name is required");
        if (f.startDate() != null && f.endDate() != null && f.endDate().isBefore(f.startDate()))
            throw new IllegalArgumentException("End date cannot be before start date");
        p.setName(f.name().trim()); p.setDescription(f.description());
        p.setStatus(f.status() == null ? Enums.ProjectStatus.ACTIVE : f.status());
        p.setStartDate(f.startDate()); p.setEndDate(f.endDate());
        p.getMembers().clear();
        if (f.memberIds() != null)
            for (Long id : f.memberIds()) {
                Employee e = employees.findById(id).orElseThrow(() -> new IllegalArgumentException("Employee not found"));
                ownTeam(mgr, e);
                p.getMembers().add(e);
            }
    }

    private static void ownTeam(Employee mgr, Employee e) {
        if (e.getManager() == null || !e.getManager().getId().equals(mgr.getId()))
            throw new IllegalArgumentException(e.getFullName() + " is not in your team");
    }

    private static String remark(Map<String, Object> b, boolean approve) {
        String r = b.get("remark") == null ? null : b.get("remark").toString().trim();
        if (!approve && (r == null || r.isEmpty())) throw new IllegalArgumentException("Please give a reason for rejecting");
        return (r == null || r.isEmpty()) ? null : r;
    }
}
