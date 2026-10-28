package com.example.demodatn.controller.admin.thongke;
import com.example.demodatn.service.thongke.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final DashboardService dashboardService;
    @GetMapping(value = {"", "/", "/dashboard"})
    public String dashboard(@RequestParam(defaultValue = "7") int days, Model model) {
        model.addAttribute("dashboard", dashboardService.load(days == 30 ? 30 : 7));
        return "admin/bang-dieu-khien";
    }

    @GetMapping("/products")
    public String products(Model model,jakarta.servlet.http.HttpServletRequest request) {
        return "redirect:/admin/products-crud"+(request.getQueryString()==null?"":"?"+request.getQueryString());
    }

    @GetMapping(value = {"/san-pham", "/san-pham/**"})
    public String sanPham(jakarta.servlet.http.HttpServletRequest request) {
        return "redirect:/admin/products-crud"+(request.getQueryString()==null?"":"?"+request.getQueryString());
    }

    @GetMapping(value = {"/variant", "/variant/**"})
    public String variant(jakarta.servlet.http.HttpServletRequest request) {
        return "redirect:/admin/variants"+(request.getQueryString()==null?"":"?"+request.getQueryString());
    }

    @GetMapping(value = {"/danh-muc", "/danh-muc/**"})
    public String danhMuc(jakarta.servlet.http.HttpServletRequest request) {
        return "redirect:/admin/categories"+(request.getQueryString()==null?"":"?"+request.getQueryString());
    }

    @GetMapping(value = {"/size", "/size/**"})
    public String size(jakarta.servlet.http.HttpServletRequest request) {
        return "redirect:/admin/sizes"+(request.getQueryString()==null?"":"?"+request.getQueryString());
    }

    @GetMapping(value = {"/chat-lieu", "/chat-lieu/**"})
    public String chatLieu(jakarta.servlet.http.HttpServletRequest request) {
        return "redirect:/admin/materials"+(request.getQueryString()==null?"":"?"+request.getQueryString());
    }

    @GetMapping(value = {"/ton-kho", "/ton-kho/**"})
    public String tonKho(jakarta.servlet.http.HttpServletRequest request) {
        return "redirect:/admin/inventory"+(request.getQueryString()==null?"":"?"+request.getQueryString());
    }
}
