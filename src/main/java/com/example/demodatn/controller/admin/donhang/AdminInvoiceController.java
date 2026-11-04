package com.example.demodatn.controller.admin.donhang;

import com.example.demodatn.entity.donhang.Invoice;
import com.example.demodatn.repository.donhang.InvoiceRepository;
import com.example.demodatn.repository.donhang.OrderMailLogRepository;
import com.example.demodatn.repository.khachhang.CustomerRepository;
import com.example.demodatn.service.donhang.OrderEmailService;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/admin/invoices")
@RequiredArgsConstructor
public class AdminInvoiceController {
    public record StatusTab(String key, String label, long count) {}

    private final InvoiceRepository invoices;
    private final CustomerRepository customers;
    private final com.example.demodatn.repository.donhang.ProvinceRepository provinces;
    private final com.example.demodatn.service.donhang.OrderConfirmationService confirmations;
    private final com.example.demodatn.service.donhang.OrderStatusService orderStatus;
    private final com.example.demodatn.service.donhang.OrderTrackingService orderTrackingService;
    private final OrderMailLogRepository mailLogs;
    private final OrderEmailService orderEmailService;

    @org.springframework.beans.factory.annotation.Value("${shop.name:SHOP GIÀY}") private String shopName;
    @org.springframework.beans.factory.annotation.Value("${shop.address:}") private String shopAddress;
    @org.springframework.beans.factory.annotation.Value("${shop.phone:}") private String shopPhone;

    private final com.example.demodatn.util.Pagination pagination;

    @GetMapping
    public String list(@RequestParam(defaultValue="") String q,
                       @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       @RequestParam(required=false) String channel,
                       @RequestParam(defaultValue="") String customer,
                       @RequestParam(required=false) Integer customerId,
                       @RequestParam(defaultValue="") String source,
                       @RequestParam(defaultValue="") String status,
                       @RequestParam(defaultValue="") String paymentStatus,
                       @RequestParam(defaultValue="") String paymentMethod,
                       @RequestParam(defaultValue="0") String page,
                       jakarta.servlet.http.HttpServletRequest request,
                       Model model) {
        Specification<Invoice> baseFilter = (root, query, cb) -> cb.conjunction();
        String ch = (channel != null && !channel.isBlank()) ? channel.trim() : null;
        String ps = (paymentStatus != null && !paymentStatus.isBlank()) ? paymentStatus.trim() : null;
        String pm = (paymentMethod != null && !paymentMethod.isBlank()) ? paymentMethod.trim() : null;
        String kw = (q != null && !q.isBlank()) ? q.trim() : null;
        String cust = (customer != null && !customer.isBlank()) ? customer.trim() : null;

        if (kw != null) {
            String upper = "%" + kw.toUpperCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            String lower = "%" + kw.toLowerCase(Locale.ROOT) + "%";
            baseFilter = baseFilter.and((r, x, c) -> {
                var ord = r.join("order", JoinType.LEFT);
                var custJoin = ord.join("customer", JoinType.LEFT);
                return c.or(
                    c.like(c.upper(r.get("invoiceCode")), upper, '!'),
                    c.like(c.upper(ord.get("orderCode")), upper, '!'),
                    c.like(c.lower(c.coalesce(r.get("customerName"), "")), lower),
                    c.like(c.lower(c.coalesce(r.get("customerPhone"), "")), lower),
                    c.like(c.lower(c.coalesce(ord.get("receiverName"), "")), lower),
                    c.like(c.lower(c.coalesce(ord.get("phoneNumber"), "")), lower),
                    c.like(c.lower(c.coalesce(custJoin.get("fullName"), "")), lower),
                    c.like(c.lower(c.coalesce(custJoin.get("phone"), "")), lower),
                    c.like(c.lower(c.coalesce(custJoin.get("customerCode"), "")), lower)
                );
            });
        }
        if (cust != null) {
            String lower = "%" + cust.toLowerCase(Locale.ROOT) + "%";
            baseFilter = baseFilter.and((r, x, c) -> {
                var ord = r.join("order", JoinType.LEFT);
                var custJoin = ord.join("customer", JoinType.LEFT);
                return c.or(
                    c.like(c.lower(c.coalesce(r.get("customerName"), "")), lower),
                    c.like(c.lower(c.coalesce(r.get("customerPhone"), "")), lower),
                    c.like(c.lower(c.coalesce(ord.get("receiverName"), "")), lower),
                    c.like(c.lower(c.coalesce(ord.get("phoneNumber"), "")), lower),
                    c.like(c.lower(c.coalesce(custJoin.get("fullName"), "")), lower),
                    c.like(c.lower(c.coalesce(custJoin.get("phone"), "")), lower),
                    c.like(c.lower(c.coalesce(custJoin.get("customerCode"), "")), lower)
                );
            });
        }
        if (customerId != null) {
            baseFilter = baseFilter.and((r, x, c) -> c.equal(r.join("order", JoinType.LEFT).join("customer", JoinType.LEFT).get("id"), customerId));
        }
        if (source != null && !source.isBlank()) {
            baseFilter = baseFilter.and((r, x, c) -> c.equal(r.get("order").get("orderSource"), source.trim()));
        }
        if (ch != null) {
            baseFilter = baseFilter.and((r, x, c) -> c.equal(r.get("order").get("orderType"), ch));
        }
        if (ps != null) {
            baseFilter = baseFilter.and((r, x, c) -> c.equal(r.get("order").get("paymentStatus"), ps));
        }
        if (pm != null) {
            if ("TRANSFER".equalsIgnoreCase(pm) || "BANK_TRANSFER".equalsIgnoreCase(pm)) {
                baseFilter = baseFilter.and((r, x, c) -> r.get("order").get("paymentMethod").in("TRANSFER", "BANK_TRANSFER"));
            } else {
                baseFilter = baseFilter.and((r, x, c) -> c.equal(r.get("order").get("paymentMethod"), pm));
            }
        }
        if (from != null) {
            baseFilter = baseFilter.and((r, x, c) -> c.greaterThanOrEqualTo(r.get("issuedAt"), from.atStartOfDay()));
        }
        if (to != null) {
            baseFilter = baseFilter.and((r, x, c) -> c.lessThan(r.get("issuedAt"), to.plusDays(1).atStartOfDay()));
        }
        if (from != null && to != null && to.isBefore(from)) {
            model.addAttribute("error", "Ngày kết thúc phải từ ngày bắt đầu.");
        }

        List<String[]> tabDefs = List.of(
            new String[]{"", "Tất cả"},
            new String[]{"PENDING", "Chờ xử lý"},
            new String[]{"CONFIRMED", "Đã xác nhận"},
            new String[]{"PREPARING", "Đang chuẩn bị"},
            new String[]{"SHIPPING", "Đang giao"},
            new String[]{"DELIVERED", "Đã giao/Hoàn tất"},
            new String[]{"CANCELLED", "Đã hủy"},
            new String[]{"DELIVERY_FAILED", "Giao thất bại"}
        );
        List<StatusTab> statusTabs = new ArrayList<>();
        for (String[] def : tabDefs) {
            String key = def[0];
            String label = def[1];
            Specification<Invoice> tabSpec = baseFilter;
            if (!key.isEmpty()) {
                tabSpec = tabSpec.and((r, x, c) -> c.equal(r.get("order").get("status"), key));
            }
            long count = invoices.count(tabSpec);
            statusTabs.add(new StatusTab(key, label, count));
        }
        model.addAttribute("statusTabs", statusTabs);

        Specification<Invoice> selection = baseFilter;
        String st = (status != null && !status.isBlank()) ? status.trim() : null;
        if (st != null) {
            selection = selection.and((r, x, c) -> c.equal(r.get("order").get("status"), st));
        }
        final var finalSelection = selection;

        model.addAttribute("invoices", pagination.load(page, Sort.by(Sort.Order.desc("issuedAt"), Sort.Order.desc("id")), req -> invoices.findAll(finalSelection, req)));
        model.addAttribute("customers", customers.findAll());
        model.addAttribute("q", q);
        model.addAttribute("customer", customer);
        model.addAttribute("customerId", customerId);
        model.addAttribute("source", source);
        model.addAttribute("status", st);
        model.addAttribute("paymentStatus", ps);
        model.addAttribute("paymentMethod", pm);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("channel", ch);
        String qs = request.getQueryString();
        model.addAttribute("currentUrl", "/admin/invoices" + (qs != null && !qs.isBlank() ? "?" + qs : ""));

        int activeFilterCount = 0;
        if (kw != null) activeFilterCount++;
        if (ch != null) activeFilterCount++;
        if (cust != null) activeFilterCount++;
        if (customerId != null) activeFilterCount++;
        if (st != null) activeFilterCount++;
        if (ps != null) activeFilterCount++;
        if (pm != null) activeFilterCount++;
        if (from != null) activeFilterCount++;
        if (to != null) activeFilterCount++;
        model.addAttribute("activeFilterCount", activeFilterCount);

        return "admin/hoa-don/danh-sach";
    }

    @GetMapping("/order/{orderId}")
    public String byOrder(@PathVariable Integer orderId) {
        return "redirect:/admin/invoices/" + invoices.findByOrder_Id(orderId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND))
                .getId();
    }

    @GetMapping({"/{id}", "/detail/{id}"})
    public String detail(@PathVariable Integer id, Model model) {
        var invoice = invoice(id);
        var order = invoice.getOrder();
        model.addAttribute("invoice", invoice);
        model.addAttribute("order", order);
        model.addAttribute("allowedStatuses", orderStatus.allowedStatuses(order));
        model.addAttribute("nextLinearStatus", orderStatus.nextLinearStatus(order));
        model.addAttribute("nextLinearStatusLabel", orderStatus.nextLinearStatusLabel(orderStatus.nextLinearStatus(order)));
        model.addAttribute("provinces", provinces.findAllByOrderByNameAsc());
        model.addAttribute("confirmationMessage", confirmations.text(order));
        model.addAttribute("shopName", shopName);
        model.addAttribute("shopAddress", shopAddress);
        model.addAttribute("shopPhone", shopPhone);

        var histories = orderTrackingService.history(order);
        String latestCarrier = histories.stream().map(h -> h.getCarrier()).filter(c -> c != null && !c.isBlank()).reduce((first, second) -> second).orElse("");
        String latestTrackingCode = histories.stream().map(h -> h.getTrackingCode()).filter(c -> c != null && !c.isBlank()).reduce((first, second) -> second).orElse("");
        model.addAttribute("latestCarrier", latestCarrier);
        model.addAttribute("latestTrackingCode", latestTrackingCode);

        var mailLogList = mailLogs.findByOrder_Id(order.getId());
        Map<String, com.example.demodatn.entity.donhang.OrderMailLog> mailLogMap = new HashMap<>();
        for (var ml : mailLogList) {
            mailLogMap.put(ml.getStatus(), ml);
        }
        model.addAttribute("mailLogMap", mailLogMap);

        return "admin/hoa-don/chi-tiet";
    }

    private Invoice invoice(Integer id) {
        return invoices.findById(id).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
    }

    @PostMapping({"/{id}/status", "/update-status/{id}"})
    public String status(@PathVariable Integer id,
                         @RequestParam String status,
                         @RequestParam(required=false) String reason,
                         @RequestParam(required=false) String carrier,
                         @RequestParam(required=false) String trackingCode,
                         @RequestParam(required=false) String returnTo,
                         org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        try {
            orderStatus.transition(invoice(id).getOrder().getId(), status, reason, carrier, trackingCode);
            flash.addFlashAttribute("message", "Đã cập nhật trạng thái đơn hàng. Đơn hủy hoặc giao thất bại được hoàn kho và ghi lịch sử.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        if (returnTo != null && returnTo.startsWith("/admin/invoices")) {
            return "redirect:" + returnTo;
        }
        return "redirect:/admin/invoices/" + id;
    }

    @PostMapping("/{id}/confirm-payment")
    public String payment(@PathVariable Integer id,
                          @RequestParam(required=false) String returnTo,
                          org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        try {
            orderStatus.confirmPayment(invoice(id).getOrder().getId());
            flash.addFlashAttribute("message", "Đã xác nhận nhận chuyển khoản.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        if (returnTo != null && returnTo.startsWith("/admin/invoices")) {
            return "redirect:" + returnTo;
        }
        return "redirect:/admin/invoices/" + id;
    }

    @PostMapping("/{id}/resend-mail")
    public String resendMail(@PathVariable Integer id,
                             @RequestParam String status,
                             org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        var invoice = invoice(id);
        var order = invoice.getOrder();
        try {
            orderEmailService.resend(order.getId(), status);
            flash.addFlashAttribute("message", "Đã gửi lại thông báo trạng thái " + status + " tới email.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        } catch (Exception ex) {
            flash.addFlashAttribute("error", "Lỗi gửi lại thông báo: " + ex.getMessage());
        }
        return "redirect:/admin/invoices/" + id;
    }

    @PostMapping("/{id}/province")
    public String province(@PathVariable Integer id,
                           @RequestParam(defaultValue="") String provinceId,
                           org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        try {
            orderStatus.updateProvince(invoice(id).getOrder().getId(), Integer.valueOf(provinceId));
            flash.addFlashAttribute("message", "Đã cập nhật tỉnh/thành và ghi lịch sử.");
        } catch (NumberFormatException ex) {
            flash.addFlashAttribute("error", "Vui lòng chọn Tỉnh/Thành phố hợp lệ trong danh sách.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/invoices/" + id;
    }
}
