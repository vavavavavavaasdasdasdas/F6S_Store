package com.example.demodatn.controller.admin.khuyenmai;
import com.example.demodatn.dto.khuyenmai.VoucherDto;
import com.example.demodatn.entity.khuyenmai.Voucher;
import com.example.demodatn.exception.DiscountValidationException;
import com.example.demodatn.repository.khachhang.CustomerRepository;
import com.example.demodatn.repository.khuyenmai.VoucherRepository;
import com.example.demodatn.service.khuyenmai.DiscountAdminService;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Controller @RequestMapping("/admin/vouchers") @RequiredArgsConstructor
public class AdminVoucherController {
    private final VoucherRepository vouchers;
    private final CustomerRepository customers;
    private final DiscountAdminService discounts;
    private final com.example.demodatn.util.Pagination pagination;

    @GetMapping
    public String list(@RequestParam(defaultValue="") String keyword,
                       @RequestParam(required=false) String discountType,
                       @RequestParam(required=false) String state,
                       @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       @RequestParam(defaultValue="0") String page,
                       Model model) {
        String kw = keyword == null ? "" : keyword.trim();
        String type = (discountType != null && !discountType.isBlank()) ? discountType.trim() : null;
        String st = (state != null && !state.isBlank()) ? state.trim() : null;
        LocalDateTime now = LocalDateTime.now();

        Specification<Voucher> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!kw.isEmpty()) {
                String pattern = "%" + kw.toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("name")), pattern)
                ));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("discountType"), type));
            }
            if (st != null) {
                switch (st) {
                    case "RUNNING" -> {
                        predicates.add(cb.isTrue(root.get("status")));
                        predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), now));
                        predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), now));
                        predicates.add(cb.or(
                                cb.isNull(root.get("usageLimit")),
                                cb.lessThan(cb.coalesce(root.get("usedCount"), 0), root.get("usageLimit"))
                        ));
                    }
                    case "UPCOMING" -> {
                        predicates.add(cb.isTrue(root.get("status")));
                        predicates.add(cb.greaterThan(root.get("startDate"), now));
                    }
                    case "EXPIRED" -> predicates.add(cb.lessThan(root.get("endDate"), now));
                    case "DEPLETED" -> {
                        predicates.add(cb.isNotNull(root.get("usageLimit")));
                        predicates.add(cb.greaterThanOrEqualTo(cb.coalesce(root.get("usedCount"), 0), root.get("usageLimit")));
                    }
                    case "INACTIVE" -> predicates.add(cb.isFalse(root.get("status")));
                }
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), from.atStartOfDay()));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("startDate"), to.plusDays(1).atStartOfDay()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        var result=pagination.load(page,Sort.by("id").descending(),p -> vouchers.findAll(spec, p));
        model.addAttribute("vouchers",result.getContent());
        model.addAttribute("dataPage",result);
        model.addAttribute("keyword",keyword);
        model.addAttribute("discountType",type);
        model.addAttribute("state",st);
        model.addAttribute("from",from);
        model.addAttribute("to",to);
        int activeFilterCount = 0;
        if (!kw.isEmpty()) activeFilterCount++;
        if (type != null) activeFilterCount++;
        if (st != null) activeFilterCount++;
        if (from != null) activeFilterCount++;
        if (to != null) activeFilterCount++;
        model.addAttribute("activeFilterCount", activeFilterCount);
        return "admin/voucher/danh-sach";
    }

    private String form(Model model) { model.addAttribute("customers",customers.findAll());return "admin/voucher/bieu-mau"; }
    @GetMapping("/create") public String create(Model model) { model.addAttribute("voucher",new VoucherDto());return form(model); }
    @GetMapping("/edit/{id}") public String edit(@PathVariable Integer id,Model model) {
        Voucher v=discounts.voucher(id);VoucherDto f=new VoucherDto();BeanUtils.copyProperties(v,f);
        if(v.getCustomer()!=null) f.setCustomerId(v.getCustomer().getId());model.addAttribute("voucher",f);model.addAttribute("usedCount",v.getUsedCount());return form(model);
    }
    @PostMapping("/save") public String save(@Valid @ModelAttribute("voucher") VoucherDto f,BindingResult errors,Model model,RedirectAttributes flash) {
        if(!errors.hasErrors()) try { discounts.saveVoucher(f);flash.addFlashAttribute("message","Đã lưu phiếu giảm giá.");return "redirect:/admin/vouchers"; }
        catch(DiscountValidationException ex) { errors.rejectValue(ex.getField(),"invalid",ex.getMessage()); }
        catch(DataIntegrityViolationException ex) { errors.rejectValue("code","duplicate","Mã phiếu giảm giá đã tồn tại hoặc dữ liệu đã thay đổi. Kiểm tra rồi lưu lại."); }
        return form(model);
    }
    @PostMapping("/delete/{id}") public String delete(@PathVariable Integer id,RedirectAttributes flash) { flash.addFlashAttribute("message",discounts.deleteVoucher(id));return "redirect:/admin/vouchers"; }
}
