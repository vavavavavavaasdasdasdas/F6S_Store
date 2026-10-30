package com.example.demodatn.controller;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;
import java.util.List;

@ControllerAdvice(basePackages = "com.example.demodatn")
public class AdminPageAdvice {
    public record NavItem(String group, String title, String route, String key) {}
    public static final List<NavItem> NAV = List.of(
        new NavItem("", "Bảng điều khiển", "/admin/dashboard", "dashboard"),
        new NavItem("BÁN HÀNG", "Bán hàng tại quầy", "/admin/pos", "pos"),
        new NavItem("", "Đơn hàng", "/admin/orders", "orders"),
        new NavItem("", "Hóa đơn", "/admin/invoices", "invoices"),
        new NavItem("SẢN PHẨM", "Sản phẩm", "/admin/products-crud", "products"),
        new NavItem("", "Biến thể", "/admin/variants", "variants"),
        new NavItem("", "Thương hiệu", "/admin/categories", "categories"),
        new NavItem("", "Kích cỡ", "/admin/sizes", "sizes"),
        new NavItem("", "Đế giày", "/admin/soles", "soles"),
        new NavItem("", "Màu sắc", "/admin/colors", "colors"),
        new NavItem("", "Chất liệu", "/admin/materials", "materials"),
        new NavItem("", "Tồn kho", "/admin/inventory", "inventory"),
        new NavItem("TÀI KHOẢN", "Khách hàng", "/admin/customers", "customers"),
        new NavItem("", "Nhân viên", "/admin/employees", "employees"),
        new NavItem("KHUYẾN MÃI", "Phiếu giảm giá", "/admin/vouchers", "vouchers"),
        new NavItem("", "Đợt giảm giá", "/admin/promotions", "promotions"),
        new NavItem("THỐNG KÊ", "Doanh thu", "/admin/statistics/revenue", "revenue"),
        new NavItem("", "Sản phẩm bán chạy", "/admin/statistics/best-sellers", "best-sellers"),
        new NavItem("HỆ THỐNG", "Thời gian giao hàng", "/admin/shipping-times", "shipping-times"),
        new NavItem("", "Hồ sơ của tôi", "/admin/account", "account")
    );
    @ModelAttribute
    public void navigation(HttpServletRequest request, Model model) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!path.equals("/admin") && !path.startsWith("/admin/")) return;
        NavItem active = NAV.stream().filter(n -> path.equals(n.route()) || path.startsWith(n.route() + "/"))
                .findFirst().orElse(NAV.get(0));
        model.addAttribute("statusLabels",com.example.demodatn.service.donhang.OrderStatusService.LABELS);
        model.addAttribute("adminNav", NAV);
        model.addAttribute("adminActive", active.key());
        model.addAttribute("adminTitle", active.title());
    }
}

