package com.example.demodatn.controller.admin.thongke;

import com.example.demodatn.repository.sanpham.CategoryRepository;
import com.example.demodatn.service.thongke.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Set;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final DashboardService dashboardService;
    private final CategoryRepository categories;
    @Value("${shop.time-zone:Asia/Ho_Chi_Minh}") private String timeZone;

    @GetMapping(value = {"", "/", "/dashboard"})
    public String dashboard(@RequestParam(required = false) Integer days,
                            @RequestParam(defaultValue = "") String period,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                            @RequestParam(defaultValue = "ALL") String channel,
                            @RequestParam(required = false) Integer categoryId,
                            @RequestParam(defaultValue = "0") String page,
                            @RequestParam(required = false) String channelPage,
                            @RequestParam(required = false) String statusPage,
                            @RequestParam(required = false) String quantityPage,
                            @RequestParam(required = false) String revenuePage,
                            @RequestParam(required = false) String customerPage,
                            @RequestParam(required = false) String reviewPage,
                            @RequestParam(required = false) String favoritePage,
                            @RequestParam(required = false) String lowStockPage,
                            Model model) {
        LocalDate today = LocalDate.now(ZoneId.of(timeZone));
        String ch = (channel == null || channel.isBlank()) ? "ALL" : channel.trim();
        int validDays = (days != null && days == 30) ? 30 : 7;
        LocalDate rawEnd = to != null ? to : today;
        LocalDate rawStart;
        if (from != null) {
            rawStart = from;
        } else if ("today".equalsIgnoreCase(period)) {
            rawStart = rawEnd;
        } else if ("month".equalsIgnoreCase(period)) {
            rawStart = rawEnd.withDayOfMonth(1);
        } else {
            rawStart = rawEnd.minusDays(validDays - 1);
        }

        String error = rawEnd.isBefore(rawStart) ? "Ngày kết thúc phải từ ngày bắt đầu." :
                ChronoUnit.DAYS.between(rawStart, rawEnd) > 3660 ? "Vui lòng chọn khoảng ngày không quá 10 năm." :
                !Set.of("ALL", "ONLINE", "POS").contains(ch) ? "Kênh bán không hợp lệ." : null;
        model.addAttribute("filterError", error);

        var dash = dashboardService.load(
                days, period, from, to, ch, categoryId,
                channelPage != null ? channelPage : page,
                statusPage != null ? statusPage : page,
                quantityPage != null ? quantityPage : page,
                revenuePage != null ? revenuePage : page,
                customerPage != null ? customerPage : page,
                reviewPage != null ? reviewPage : page,
                favoritePage != null ? favoritePage : page,
                lowStockPage != null ? lowStockPage : page
        );

        model.addAttribute("dashboard", dash);
        if (error == null) {
            model.addAttribute("stats", dash);
        }
        model.addAttribute("from", rawStart);
        model.addAttribute("to", rawEnd);
        model.addAttribute("channel", ch);
        model.addAttribute("categories", categories.findAll());
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("timeZone", timeZone);

        int activeFilterCount = 1; // Date range filter is always active
        if (!"ALL".equals(ch)) activeFilterCount++;
        if (categoryId != null) activeFilterCount++;
        model.addAttribute("activeFilterCount", activeFilterCount);
        return "admin/bang-dieu-khien";
    }

    @GetMapping("/products")
    public String products(Model model, jakarta.servlet.http.HttpServletRequest request) {
        return "redirect:/admin/products-crud" + (request.getQueryString() == null ? "" : "?" + request.getQueryString());
    }

    @GetMapping(value = {"/san-pham", "/san-pham/**"})
    public String sanPham(jakarta.servlet.http.HttpServletRequest request) {
        return "redirect:/admin/products-crud" + (request.getQueryString() == null ? "" : "?" + request.getQueryString());
    }

    @GetMapping(value = {"/variant", "/variant/**"})
    public String variant(jakarta.servlet.http.HttpServletRequest request) {
        return "redirect:/admin/variants" + (request.getQueryString() == null ? "" : "?" + request.getQueryString());
    }

    @GetMapping(value = {"/danh-muc", "/danh-muc/**"})
    public String danhMuc(jakarta.servlet.http.HttpServletRequest request) {
        return "redirect:/admin/categories" + (request.getQueryString() == null ? "" : "?" + request.getQueryString());
    }

    @GetMapping(value = {"/size", "/size/**"})
    public String size(jakarta.servlet.http.HttpServletRequest request) {
        return "redirect:/admin/sizes" + (request.getQueryString() == null ? "" : "?" + request.getQueryString());
    }

    @GetMapping(value = {"/chat-lieu", "/chat-lieu/**"})
    public String chatLieu(jakarta.servlet.http.HttpServletRequest request) {
        return "redirect:/admin/materials" + (request.getQueryString() == null ? "" : "?" + request.getQueryString());
    }

    @GetMapping(value = {"/ton-kho", "/ton-kho/**"})
    public String tonKho(jakarta.servlet.http.HttpServletRequest request) {
        return "redirect:/admin/inventory" + (request.getQueryString() == null ? "" : "?" + request.getQueryString());
    }
}
