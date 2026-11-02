package com.example.demodatn.controller.admin.thongke;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Compatibility redirects for legacy statistics and reports URLs.
 * All statistics content is now unified into /admin/dashboard (Giai đoạn 16).
 */
@Controller
public class AdminStatisticsController {

    @GetMapping({"/admin/statistics", "/admin/statistics/**", "/admin/reports", "/admin/reports/**"})
    public String redirectOld(HttpServletRequest request) {
        String qs = request.getQueryString();
        return "redirect:/admin/dashboard" + (qs == null || qs.isBlank() ? "" : "?" + qs);
    }
}
