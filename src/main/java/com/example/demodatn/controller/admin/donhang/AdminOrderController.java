package com.example.demodatn.controller.admin.donhang;
import com.example.demodatn.entity.donhang.Order;
import com.example.demodatn.repository.donhang.InvoiceRepository;
import com.example.demodatn.repository.donhang.OrderRepository;
import com.example.demodatn.repository.khachhang.CustomerRepository;
import com.example.demodatn.service.donhang.OrderStatusService;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
import java.util.Locale;

@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {
    private final OrderRepository orderRepository;
    private final OrderStatusService orderService;
    private final InvoiceRepository invoices;
    private final CustomerRepository customers;
    private final com.example.demodatn.service.donhang.OrderConfirmationService confirmations;
    public static final java.util.Map<String,String> STATUS_LABELS=OrderStatusService.LABELS;

    @ModelAttribute public void labels(Model model) { model.addAttribute("statusLabels",STATUS_LABELS); }

    private final com.example.demodatn.util.Pagination pagination;

    @GetMapping
    public String list(@RequestParam(required=false) String status,
                       @RequestParam(defaultValue="") String q,
                       @RequestParam(defaultValue="") String customer,
                       @RequestParam(required=false) Integer customerId,
                       @RequestParam(required=false) String channel,
                       @RequestParam(required=false) String paymentStatus,
                       @RequestParam(required=false) String paymentMethod,
                       @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       @RequestParam(defaultValue="0") String page,
                       Model model) {
        Specification<Order> filter=(r,x,c)->c.conjunction();
        String st = (status != null && !status.isBlank()) ? status.trim() : null;
        String ch = (channel != null && !channel.isBlank()) ? channel.trim() : null;
        String ps = (paymentStatus != null && !paymentStatus.isBlank()) ? paymentStatus.trim() : null;
        String pm = (paymentMethod != null && !paymentMethod.isBlank()) ? paymentMethod.trim() : null;
        String custKw = customer == null ? "" : customer.trim();

        if(st != null) filter=filter.and((r,x,c)->c.equal(r.get("status"),st));
        if(!q.isBlank()) {
            String code=q.trim().toUpperCase(Locale.ROOT);
            filter=filter.and((r,x,c)->c.like(c.upper(r.get("orderCode")), "%"+code.replace("!","!!").replace("%","!%").replace("_","!_")+"%", '!'));
        }
        if (customerId != null) {
            filter = filter.and((r, x, c) -> c.equal(r.join("customer", JoinType.LEFT).get("id"), customerId));
        }
        if (!custKw.isEmpty()) {
            String pattern = "%" + custKw.toLowerCase(Locale.ROOT) + "%";
            filter = filter.and((r, x, c) -> {
                var custJoin = r.join("customer", JoinType.LEFT);
                return c.or(
                        c.like(c.lower(c.coalesce(r.get("receiverName"), "")), pattern),
                        c.like(c.lower(c.coalesce(r.get("phoneNumber"), "")), pattern),
                        c.like(c.lower(c.coalesce(custJoin.get("fullName"), "")), pattern),
                        c.like(c.lower(c.coalesce(custJoin.get("phone"), "")), pattern),
                        c.like(c.lower(c.coalesce(custJoin.get("customerCode"), "")), pattern)
                );
            });
        }
        if (ch != null) {
            filter = filter.and((r, x, c) -> c.equal(r.get("orderType"), ch));
        }
        if (ps != null) {
            filter = filter.and((r, x, c) -> c.equal(r.get("paymentStatus"), ps));
        }
        if (pm != null) {
            filter = filter.and((r, x, c) -> c.equal(r.get("paymentMethod"), pm));
        }
        if (from != null) {
            filter = filter.and((r, x, c) -> c.greaterThanOrEqualTo(r.get("createdAt"), from.atStartOfDay()));
        }
        if (to != null) {
            filter = filter.and((r, x, c) -> c.lessThan(r.get("createdAt"), to.plusDays(1).atStartOfDay()));
        }
        final var selection=filter;
        model.addAttribute("orders",pagination.load(page,Sort.by(Sort.Order.desc("createdAt"),Sort.Order.desc("id")),request->orderRepository.findAll(selection,request)));
        model.addAttribute("customers",customers.findAll());
        model.addAttribute("q",q);
        model.addAttribute("status",st);
        model.addAttribute("customer",customer);
        model.addAttribute("customerId",customerId);
        model.addAttribute("channel",ch);
        model.addAttribute("paymentStatus",ps);
        model.addAttribute("paymentMethod",pm);
        model.addAttribute("from",from);
        model.addAttribute("to",to);
        int activeFilterCount = 0;
        if (q != null && !q.trim().isEmpty()) activeFilterCount++;
        if (customerId != null) activeFilterCount++;
        if (!custKw.isEmpty()) activeFilterCount++;
        if (ch != null) activeFilterCount++;
        if (st != null) activeFilterCount++;
        if (ps != null) activeFilterCount++;
        if (pm != null) activeFilterCount++;
        if (from != null) activeFilterCount++;
        if (to != null) activeFilterCount++;
        model.addAttribute("activeFilterCount", activeFilterCount);
        return "admin/don-hang/danh-sach";
    }

    @GetMapping({"/{id}","/detail/{id}"})
    public String detail(@PathVariable Integer id,Model model) {
        var order=orderRepository.findById(id).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        model.addAttribute("order",order);
        model.addAttribute("confirmationMessage",confirmations.text(order));
        model.addAttribute("allowedStatuses",orderService.allowedStatuses(order));
        model.addAttribute("invoice",invoices.findByOrder_Id(id).orElse(null));
        return "admin/don-hang/chi-tiet";
    }

    @PostMapping({"/update-status/{id}","/{id}/status"})
    public String updateStatus(@PathVariable Integer id,@RequestParam String status,@RequestParam(required=false) String reason,@RequestParam(required=false) String carrier,@RequestParam(required=false) String trackingCode,RedirectAttributes flash) {
        try { orderService.transition(id,status,reason,carrier,trackingCode); flash.addFlashAttribute("message","Đã cập nhật đơn hàng. Đơn hủy được hoàn kho và ghi lịch sử."); }
        catch(IllegalArgumentException ex) { flash.addFlashAttribute("error",ex.getMessage()); }
        return "redirect:/admin/orders/"+id;
    }

    @PostMapping("/{id}/province")
    public String province(@PathVariable Integer id,@RequestParam(defaultValue="") String provinceId,RedirectAttributes flash) {
        try {orderService.updateProvince(id,Integer.valueOf(provinceId));flash.addFlashAttribute("message","Đã cập nhật tỉnh/thành và ghi lịch sử.");}
        catch(NumberFormatException ex){flash.addFlashAttribute("error","Vui lòng chọn Tỉnh/Thành phố hợp lệ trong danh sách.");}
        catch(IllegalArgumentException ex){flash.addFlashAttribute("error",ex.getMessage());}
        return "redirect:/admin/orders/"+id;
    }

    @PostMapping("/{id}/confirm-payment")
    public String payment(@PathVariable Integer id,RedirectAttributes flash) {
        try { orderService.confirmPayment(id); flash.addFlashAttribute("message","Đã xác nhận nhận chuyển khoản."); }
        catch(IllegalArgumentException ex) { flash.addFlashAttribute("error",ex.getMessage()); }
        return "redirect:/admin/orders/"+id;
    }
}
