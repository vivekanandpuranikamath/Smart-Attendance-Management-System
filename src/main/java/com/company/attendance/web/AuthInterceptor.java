package com.company.attendance.web;

import jakarta.servlet.http.*;
import org.springframework.web.servlet.HandlerInterceptor;

/** Session + role guard for the three dashboards and their REST APIs. */
public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        String path = req.getRequestURI().substring(req.getContextPath().length());
        if (path.startsWith("/api/auth") || path.startsWith("/api/attendance")) return true;   // login + QR scan are open
        boolean api = path.startsWith("/api/");
        HttpSession s = req.getSession(false);
        String role = s == null ? null : (String) s.getAttribute("role");
        boolean ok = role != null;
        if (ok) {
            if (path.startsWith("/admin") || path.startsWith("/api/admin")) ok = role.equals("ADMIN");
            else if (path.startsWith("/manager") || path.startsWith("/api/manager")) ok = role.equals("MANAGER");
            else if (path.startsWith("/employee") || path.startsWith("/api/employee"))
                ok = role.equals("EMPLOYEE") || (api && role.equals("MANAGER"));
        }
        if (ok) return true;
        if (api) {
            res.setStatus(role == null ? 401 : 403);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"" + (role == null ? "Not logged in" : "Access denied") + "\"}");
        } else {
            res.sendRedirect(req.getContextPath() + "/login");
        }
        return false;
    }
}
