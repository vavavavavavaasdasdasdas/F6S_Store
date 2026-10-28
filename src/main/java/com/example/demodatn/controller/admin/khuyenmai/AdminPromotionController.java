package com.example.demodatn.controller.admin.khuyenmai;
import com.example.demodatn.dto.khuyenmai.PromotionDto;
import com.example.demodatn.entity.khuyenmai.Promotion;
import com.example.demodatn.entity.khuyenmai.PromotionDetail;
import com.example.demodatn.entity.sanpham.Product;
import com.example.demodatn.exception.DiscountValidationException;
import com.example.demodatn.repository.khuyenmai.PromotionDetailRepository;
import com.example.demodatn.repository.khuyenmai.PromotionRepository;
import com.example.demodatn.repository.sanpham.CategoryRepository;
import com.example.demodatn.repository.sanpham.ProductRepository;
import com.example.demodatn.service.khuyenmai.DiscountAdminService;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
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
import java.util.stream.Collectors;

@Controller @RequestMapping("/admin/promotions") @RequiredArgsConstructor
public class AdminPromotionController {
    private final PromotionRepository promotions;
    private final PromotionDetailRepository details;
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final DiscountAdminService discounts;
    private final com.example.demodatn.util.Pagination pagination;

    @GetMapping
    public String list(@RequestParam(defaultValue="") String keyword,
                       @RequestParam(required=false) String state,
                       @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       @RequestParam(defaultValue="0") String page,
                       Model model) {
        String kw = keyword == null ? "" : keyword.trim();
        String st = (state != null && !state.isBlank()) ? state.trim() : null;
        LocalDateTime now = LocalDateTime.now();

        Specification<Promotion> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!kw.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + kw.toLowerCase(Locale.ROOT) + "%"));
            }
            if (st != null) {
                switch (st) {
                    case "RUNNING" -> {
                        predicates.add(cb.isTrue(root.get("status")));
                        predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), now));
                        predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), now));
                    }
                    case "UPCOMING" -> {
                        predicates.add(cb.isTrue(root.get("status")));
                        predicates.add(cb.greaterThan(root.get("startDate"), now));
                    }
                    case "EXPIRED" -> predicates.add(cb.lessThan(root.get("endDate"), now));
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

        var result=pagination.load(page,Sort.by("id").descending(),p -> promotions.findAll(spec, p));
        model.addAttribute("promotions",result.getContent());
        model.addAttribute("dataPage",result);
        model.addAttribute("keyword",keyword);
        model.addAttribute("state",st);
        model.addAttribute("from",from);
        model.addAttribute("to",to);
        int activeFilterCount = 0;
        if (!kw.isEmpty()) activeFilterCount++;
        if (st != null) activeFilterCount++;
        if (from != null) activeFilterCount++;
        if (to != null) activeFilterCount++;
        model.addAttribute("activeFilterCount", activeFilterCount);
        return "admin/khuyen-mai/danh-sach";
    }
    @GetMapping("/create") public String create(Model model) { model.addAttribute("promotion",new PromotionDto());return "admin/khuyen-mai/bieu-mau"; }
    @GetMapping("/edit/{id}") public String edit(@PathVariable Integer id,Model model) { PromotionDto f=new PromotionDto();BeanUtils.copyProperties(discounts.promotion(id),f);model.addAttribute("promotion",f);return "admin/khuyen-mai/bieu-mau"; }
    @PostMapping("/save") public String save(@Valid @ModelAttribute("promotion") PromotionDto f,BindingResult errors,RedirectAttributes flash) {
        if(!errors.hasErrors()) try { Promotion p=discounts.savePromotion(f);flash.addFlashAttribute("message","Đã lưu đợt giảm giá. Chọn sản phẩm áp dụng.");return "redirect:/admin/promotions/"+p.getId()+"/products"; }
        catch(DiscountValidationException ex) { errors.rejectValue(ex.getField(),"invalid",ex.getMessage()); }
        return "admin/khuyen-mai/bieu-mau";
    }
    @GetMapping("/{id}/products")
    public String selection(@PathVariable Integer id,
                            @RequestParam(defaultValue="") String q,
                            @RequestParam(required=false) Integer categoryId,
                            @RequestParam(required=false) Boolean applied,
                            @RequestParam(defaultValue="0") String page,
                            Model model) {
        model.addAttribute("promotion",discounts.promotion(id));
        Specification<Product> filter=(r,x,c)->{
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(c.or(c.like(c.lower(r.get("name")),like(q),'!'),c.like(c.lower(r.get("code")),like(q),'!')));
            if (categoryId != null) {
                predicates.add(c.equal(r.get("category").get("id"), categoryId));
            }
            if (applied != null) {
                var sq = x.subquery(Integer.class);
                var d = sq.from(PromotionDetail.class);
                sq.select(c.literal(1)).where(
                        c.equal(d.get("promotion").get("id"), id),
                        c.equal(d.get("product").get("id"), r.get("id"))
                );
                predicates.add(applied ? c.exists(sq) : c.not(c.exists(sq)));
            }
            return c.and(predicates.toArray(new Predicate[0]));
        };
        var result=pagination.load(page,Sort.by("name","id"),request->products.findAll(filter,request));
        model.addAttribute("products",result.getContent());
        model.addAttribute("productPage",result);
        model.addAttribute("q",q);
        model.addAttribute("categories",categories.findAll());
        model.addAttribute("categoryId",categoryId);
        model.addAttribute("applied",applied);
        int activeFilterCount = 0;
        if (q != null && !q.trim().isEmpty()) activeFilterCount++;
        if (categoryId != null) activeFilterCount++;
        if (applied != null) activeFilterCount++;
        model.addAttribute("activeFilterCount", activeFilterCount);
        model.addAttribute("selected",details.findByPromotion_IdAndProduct_IdIn(id,result.getContent().stream().map(Product::getId).toList()).stream().map(d->d.getProduct().getId()).collect(Collectors.toSet()));
        return "admin/khuyen-mai/san-pham-ap-dung";
    }
    @PostMapping("/{id}/products")
    public String select(@PathVariable Integer id,
                         @RequestParam(required=false) List<Integer> productIds,
                         @RequestParam(required=false) List<Integer> visibleIds,
                         @RequestParam(defaultValue="0") String page,
                         @RequestParam(defaultValue="") String q,
                         @RequestParam(required=false) Integer categoryId,
                         @RequestParam(required=false) Boolean applied,
                         RedirectAttributes flash) {
        try { discounts.selectProductPage(id,visibleIds==null?List.of():visibleIds,productIds==null?List.of():productIds);flash.addFlashAttribute("message","Đã lưu sản phẩm áp dụng."); }
        catch(IllegalArgumentException ex) { flash.addFlashAttribute("error",ex.getMessage()); }
        String redirect = "redirect:/admin/promotions/"+id+"/products?page="+java.net.URLEncoder.encode(page,java.nio.charset.StandardCharsets.UTF_8)+"&q="+java.net.URLEncoder.encode(q,java.nio.charset.StandardCharsets.UTF_8);
        if (categoryId != null) redirect += "&categoryId=" + categoryId;
        if (applied != null) redirect += "&applied=" + applied;
        return redirect;
    }
    private String like(String q){return "%"+q.trim().toLowerCase(java.util.Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_").replace("[","![")+"%";}
    @PostMapping("/delete/{id}") public String delete(@PathVariable Integer id,RedirectAttributes flash) { discounts.deletePromotion(id);flash.addFlashAttribute("message","Đã xóa đợt giảm giá; giá đơn đã đặt được giữ nguyên.");return "redirect:/admin/promotions"; }
}
