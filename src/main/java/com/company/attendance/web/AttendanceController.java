package com.company.attendance.web;

import com.company.attendance.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/** Open endpoint used by the /scan page: the secret QR token itself is the credential. */
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {
    private final AttendanceService attendance;

    @PostMapping("/scan")
    public Map<String, Object> scan(@RequestBody Map<String, String> b) {
        return attendance.scan(b.get("token"));
    }
}
