package com.company.attendance.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Returns the JSP shells; each JSP boots a React app that talks to the REST API. */
@Controller
public class PageController {
    @GetMapping("/") public String root() { return "redirect:/login"; }
    @GetMapping("/login") public String login() { return "login"; }
    @GetMapping("/admin") public String admin() { return "admin"; }
    @GetMapping("/manager") public String manager() { return "manager"; }
    @GetMapping("/employee") public String employee() { return "employee"; }
    @GetMapping("/scan") public String scan() { return "scan"; }
}
