package com.company.attendance.model;

public final class Enums {
    private Enums() {}
    public enum Role { ADMIN, MANAGER, EMPLOYEE }
    public enum ShiftType { DAY, NIGHT }
    public enum RequestStatus { PENDING, APPROVED, REJECTED }
    public enum LeaveType { SICK, PERSONAL }
    public enum ProjectStatus { ACTIVE, COMPLETED, FUTURE }
}
