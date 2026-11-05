package com.example.demodatn.controller;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;
import java.util.List;

@ControllerAdvice(basePackages = "com.example.demodatn")
@lombok.RequiredArgsConstructor
public class AdminPageAdvice {
    private final com.example.demodatn.security.AdminAccess access;
    private final com.example.demodatn.service.hotro.SupportChatInboxService inbox;
    public record NavItem(String group, String title, String route, String key) {}
    public static final List<NavItem> NAV = List.of(
        new NavItem("", "Dashboard", "/admin/dashboard", "dashboard"),
        new NavItem("BÁN HÀNG", "Bán hàng tại quầy", "/admin/pos", "pos"),
        new NavItem("", "Quản lý hóa đơn", "/admin/invoices", "invoices"),
        new NavItem("QUẢN LÝ SẢN PHẨM", "Chi tiết sản phẩm", "/admin/products-crud", "products"),
        new NavItem("", "Thương hiệu", "/admin/categories", "categories"),
        new NavItem("", "Danh sách biến thể", "/admin/variants", "variants"),
        new NavItem("", "Kích cỡ", "/admin/sizes", "sizes"),
        new NavItem("", "Đế giày", "/admin/soles", "soles"),
        new NavItem("", "Chất liệu", "/admin/materials", "materials"),
        new NavItem("", "Màu sắc", "/admin/colors", "colors"),
        new NavItem("TÀI KHOẢN", "Khách hàng", "/admin/customers", "customers"),
        new NavItem("", "Nhân viên", "/admin/employees", "employees"),
        new NavItem("KHUYẾN MÃI", "Phiếu giảm giá", "/admin/vouchers", "vouchers"),
        new NavItem("", "Đợt giảm giá", "/admin/promotions", "promotions"),
        new NavItem("HỖ TRỢ KHÁCH HÀNG", "Chat với khách", "/admin/chat", "chat"),
        new NavItem("", "Đánh giá", "/admin/reviews", "reviews")
    );
    @ModelAttribute
    public void navigation(HttpServletRequest request, Model model) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!path.equals("/admin") && !path.startsWith("/admin/")) return;
        NavItem active = NAV.stream().filter(n -> path.equals(n.route()) || path.startsWith(n.route() + "/"))
                .findFirst().orElse(NAV.get(0));
        model.addAttribute("statusLabels",com.example.demodatn.service.donhang.OrderStatusService.LABELS);
        var authentication=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean full=access.isAdmin(authentication);
        model.addAttribute("adminFullAccess",full);model.addAttribute("adminHome",access.home(authentication));
        model.addAttribute("adminNav",NAV.stream().filter(n->access.allowed(authentication,"GET",n.route())).map(n->!full&&n.key().equals("account")?new NavItem("",n.title(),n.route(),n.key()):n).toList());

        String adminActive = path.startsWith("/admin/account") ? "account" : active.key();
        model.addAttribute("adminActive", adminActive);
        model.addAttribute("adminTitle", path.startsWith("/admin/account") ? "Hồ sơ của tôi" : active.title());

        // Group active checks for initial open state
        model.addAttribute("isBanHangActive", List.of("pos", "invoices").contains(adminActive));
        model.addAttribute("isSanPhamActive", List.of("products", "categories", "variants", "sizes", "soles", "materials", "colors").contains(adminActive));
        model.addAttribute("isSubSanPhamActive", List.of("products", "categories").contains(adminActive));
        model.addAttribute("isSubBienTheActive", List.of("variants", "sizes", "soles", "materials", "colors").contains(adminActive));
        model.addAttribute("isTaiKhoanActive", List.of("customers", "employees").contains(adminActive));
        model.addAttribute("isKhuyenMaiActive", List.of("vouchers", "promotions").contains(adminActive));
        model.addAttribute("isHoTroActive", List.of("chat", "reviews").contains(adminActive));

        // Group & item permission checks for both Admin and Staff
        boolean canDashboard = access.allowed(authentication, "GET", "/admin/dashboard");
        boolean canPos = access.allowed(authentication, "GET", "/admin/pos");
        boolean canInvoices = access.allowed(authentication, "GET", "/admin/invoices");
        boolean canProducts = access.allowed(authentication, "GET", "/admin/products-crud");
        boolean canVariants = access.allowed(authentication, "GET", "/admin/variants");
        boolean canCategories = access.allowed(authentication, "GET", "/admin/categories");
        boolean canSizes = access.allowed(authentication, "GET", "/admin/sizes");
        boolean canSoles = access.allowed(authentication, "GET", "/admin/soles");
        boolean canMaterials = access.allowed(authentication, "GET", "/admin/materials");
        boolean canColors = access.allowed(authentication, "GET", "/admin/colors");
        boolean canCustomers = access.allowed(authentication, "GET", "/admin/customers");
        boolean canEmployees = access.allowed(authentication, "GET", "/admin/employees");
        boolean canVouchers = access.allowed(authentication, "GET", "/admin/vouchers");
        boolean canPromotions = access.allowed(authentication, "GET", "/admin/promotions");
        boolean canChat = access.allowed(authentication, "GET", "/admin/chat");
        boolean canReviews = access.allowed(authentication, "GET", "/admin/reviews");

        model.addAttribute("canDashboard", canDashboard);
        model.addAttribute("canPos", canPos);
        model.addAttribute("canInvoices", canInvoices);
        model.addAttribute("canProducts", canProducts);
        model.addAttribute("canVariants", canVariants);
        model.addAttribute("canCategories", canCategories);
        model.addAttribute("canSizes", canSizes);
        model.addAttribute("canSoles", canSoles);
        model.addAttribute("canMaterials", canMaterials);
        model.addAttribute("canColors", canColors);
        model.addAttribute("canCustomers", canCustomers);
        model.addAttribute("canEmployees", canEmployees);
        model.addAttribute("canVouchers", canVouchers);
        model.addAttribute("canPromotions", canPromotions);
        model.addAttribute("canChat", canChat);
        model.addAttribute("canReviews", canReviews);

        model.addAttribute("hasGroupBanHang", canPos || canInvoices);
        model.addAttribute("hasGroupSanPham", canProducts || canCategories || canVariants || canSizes || canSoles || canMaterials || canColors);
        model.addAttribute("hasGroupTaiKhoan", canCustomers || canEmployees);
        model.addAttribute("hasGroupKhuyenMai", canVouchers || canPromotions);
        model.addAttribute("hasGroupHoTro", canChat || canReviews);

        try {
            Long unread = (Long) inbox.counts(new com.example.demodatn.service.hotro.SupportChatService.Actor(authentication.getName(),null)).get("unreadMessages");
            model.addAttribute("chatUnreadCount", unread != null ? unread : 0L);
        } catch (Exception ignored) {
            model.addAttribute("chatUnreadCount", 0L);
        }
    }
}
