package com.example.demodatn.controller.admin.donhang;
import com.example.demodatn.entity.donhang.Invoice;
import com.example.demodatn.repository.donhang.InvoiceRepository;
import com.example.demodatn.repository.khachhang.CustomerRepository;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
import java.util.Locale;

@Controller
@RequestMapping("/admin/invoices")
@RequiredArgsConstructor
public class AdminInvoiceController {
    private final InvoiceRepository invoices;
    private final CustomerRepository customers;
    private final com.example.demodatn.repository.donhang.ProvinceRepository provinces;
    private final com.example.demodatn.service.donhang.OrderConfirmationService confirmations;
    private final com.example.demodatn.service.donhang.OrderStatusService orderStatus;
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
                       Model model) {
        Specification<Invoice> filter=(root,query,cb)->cb.conjunction();
        String ch = (channel != null && !channel.isBlank()) ? channel.trim() : null;
        if(!q.isBlank()) {
            String code=q.trim().toUpperCase(Locale.ROOT);
            String pattern="%"+code.replace("!","!!").replace("%","!%").replace("_","!_")+"%";
            filter=filter.and((r,x,c)->c.or(c.like(c.upper(r.get("invoiceCode")),pattern,'!'),c.like(c.upper(r.get("order").get("orderCode")),pattern,'!')));
        }
        if(!customer.isBlank()){
            String pattern="%"+customer.trim().toLowerCase(Locale.ROOT)+"%";
            filter=filter.and((r,x,c)->{
                var ord = r.join("order", JoinType.LEFT);
                var custJoin = ord.join("customer", JoinType.LEFT);
                return c.or(
                    c.like(c.lower(c.coalesce(r.get("customerName"),"")),pattern),
                    c.like(c.lower(c.coalesce(r.get("customerPhone"),"")),pattern),
                    c.like(c.lower(c.coalesce(ord.get("receiverName"),"")),pattern),
                    c.like(c.lower(c.coalesce(ord.get("phoneNumber"),"")),pattern),
                    c.like(c.lower(c.coalesce(custJoin.get("fullName"),"")),pattern),
                    c.like(c.lower(c.coalesce(custJoin.get("phone"),"")),pattern),
                    c.like(c.lower(c.coalesce(custJoin.get("customerCode"),"")),pattern)
                );
            });
        }
        if(customerId!=null)filter=filter.and((r,x,c)->c.equal(r.join("order",JoinType.LEFT).join("customer",JoinType.LEFT).get("id"),customerId));
        for(var entry:java.util.Map.of("orderSource",source,"status",status,"paymentStatus",paymentStatus,"paymentMethod",paymentMethod).entrySet())if(!entry.getValue().isBlank())filter=filter.and((r,x,c)->c.equal(r.get("order").get(entry.getKey()),entry.getValue()));
        if(ch != null) filter=filter.and((r,x,c)->c.equal(r.get("order").get("orderType"), ch));
        if(from!=null) filter=filter.and((r,x,c)->c.greaterThanOrEqualTo(r.get("issuedAt"),from.atStartOfDay()));
        if(to!=null) filter=filter.and((r,x,c)->c.lessThan(r.get("issuedAt"),to.plusDays(1).atStartOfDay()));
        if(from!=null && to!=null && to.isBefore(from)) model.addAttribute("error","Ngày kết thúc phải từ ngày bắt đầu.");
        final var selection=filter;
        model.addAttribute("invoices",pagination.load(page,Sort.by(Sort.Order.desc("issuedAt"),Sort.Order.desc("id")),request->invoices.findAll(selection,request)));
        model.addAttribute("customers",customers.findAll());
        model.addAttribute("q",q);model.addAttribute("customer",customer);model.addAttribute("customerId",customerId);model.addAttribute("source",source);model.addAttribute("status",status);model.addAttribute("paymentStatus",paymentStatus);model.addAttribute("paymentMethod",paymentMethod);model.addAttribute("orderSources",com.example.demodatn.entity.donhang.Order.SOURCE_LABELS);
        model.addAttribute("from",from);
        model.addAttribute("to",to);
        model.addAttribute("channel",ch);
        int activeFilterCount = 0;
        if (q != null && !q.trim().isEmpty()) activeFilterCount++;
        if (ch != null) activeFilterCount++;
        if(!customer.isBlank())activeFilterCount++;if(customerId!=null)activeFilterCount++;
        for(String value:java.util.List.of(source,status,paymentStatus,paymentMethod))if(!value.isBlank())activeFilterCount++;
        if (from != null) activeFilterCount++;
        if (to != null) activeFilterCount++;
        model.addAttribute("activeFilterCount", activeFilterCount);
        return "admin/hoa-don/danh-sach";
    }

    @GetMapping("/order/{orderId}")
    public String byOrder(@PathVariable Integer orderId){return "redirect:/admin/invoices/"+invoices.findByOrder_Id(orderId).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND)).getId();}
    @GetMapping({"/{id}","/detail/{id}"})
    public String detail(@PathVariable Integer id,Model model) {
        var invoice=invoice(id);model.addAttribute("invoice",invoice);model.addAttribute("order",invoice.getOrder());model.addAttribute("allowedStatuses",orderStatus.allowedStatuses(invoice.getOrder()));
        model.addAttribute("provinces",provinces.findAllByOrderByNameAsc());
        model.addAttribute("confirmationMessage",confirmations.text(invoice.getOrder()));model.addAttribute("shopName",shopName);model.addAttribute("shopAddress",shopAddress);model.addAttribute("shopPhone",shopPhone);
        return "admin/hoa-don/chi-tiet";
    }
    private Invoice invoice(Integer id){return invoices.findById(id).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));}
    @PostMapping({"/{id}/status","/update-status/{id}"})
    public String status(@PathVariable Integer id,@RequestParam String status,@RequestParam(required=false) String reason,@RequestParam(required=false) String carrier,@RequestParam(required=false) String trackingCode,org.springframework.web.servlet.mvc.support.RedirectAttributes flash){
        try{orderStatus.transition(invoice(id).getOrder().getId(),status,reason,carrier,trackingCode);flash.addFlashAttribute("message","Đã cập nhật trạng thái đơn hàng. Đơn hủy hoặc giao thất bại được hoàn kho và ghi lịch sử.");}
        catch(IllegalArgumentException ex){flash.addFlashAttribute("error",ex.getMessage());}
        return "redirect:/admin/invoices/"+id;
    }
    @PostMapping("/{id}/confirm-payment")
    public String payment(@PathVariable Integer id,org.springframework.web.servlet.mvc.support.RedirectAttributes flash){
        try{orderStatus.confirmPayment(invoice(id).getOrder().getId());flash.addFlashAttribute("message","Đã xác nhận nhận chuyển khoản.");}
        catch(IllegalArgumentException ex){flash.addFlashAttribute("error",ex.getMessage());}
        return "redirect:/admin/invoices/"+id;
    }
    @PostMapping("/{id}/province")
    public String province(@PathVariable Integer id,@RequestParam(defaultValue="") String provinceId,org.springframework.web.servlet.mvc.support.RedirectAttributes flash){
        try{orderStatus.updateProvince(invoice(id).getOrder().getId(),Integer.valueOf(provinceId));flash.addFlashAttribute("message","Đã cập nhật tỉnh/thành và ghi lịch sử.");}
        catch(NumberFormatException ex){flash.addFlashAttribute("error","Vui lòng chọn Tỉnh/Thành phố hợp lệ trong danh sách.");}
        catch(IllegalArgumentException ex){flash.addFlashAttribute("error",ex.getMessage());}
        return "redirect:/admin/invoices/"+id;
    }
}
