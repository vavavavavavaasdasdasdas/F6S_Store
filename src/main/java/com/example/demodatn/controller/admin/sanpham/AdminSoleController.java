package com.example.demodatn.controller.admin.sanpham;

import com.example.demodatn.dto.sanpham.LookupForm;
import com.example.demodatn.entity.sanpham.Product;
import com.example.demodatn.entity.sanpham.Sole;
import com.example.demodatn.repository.sanpham.*;
import com.example.demodatn.service.sanpham.SoleService;
import com.example.demodatn.exception.CatalogValidationException;
import com.example.demodatn.util.Pagination;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.ArrayList;
import java.util.List;

@Controller @RequestMapping("/admin/soles") @RequiredArgsConstructor
public class AdminSoleController {
    private final SoleRepository soles;
    private final ProductRepository products;
    private final SoleService service;
    private final Pagination pagination;

    @ModelAttribute public void common(Model model) {
        model.addAttribute("kind","soles");model.addAttribute("lookupTitle","Đế giày");
    }
    @GetMapping public String list(@RequestParam(defaultValue="") String keyword,
                                   @RequestParam(required=false) Boolean status,
                                   @RequestParam(required=false) Boolean inUse,
                                   @RequestParam(defaultValue="0") String page,
                                   Model model) {
        String kw = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        Specification<Sole> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (kw != null) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + kw.toLowerCase() + "%"));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (inUse != null) {
                var sq = query.subquery(Integer.class);
                var p = sq.from(Product.class);
                sq.select(cb.literal(1)).where(cb.equal(p.get("sole").get("id"), root.get("id")));
                predicates.add(inUse ? cb.exists(sq) : cb.not(cb.exists(sq)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        var items=pagination.load(page,Sort.by("id"),p->soles.findAll(spec, p));
        var usage=new java.util.HashMap<Integer,Long>();
        products.soleUsage().forEach(r->usage.put(((Number)r.get("soleId")).intValue(),((Number)r.get("usageCount")).longValue()));
        model.addAttribute("items",items);
        model.addAttribute("keyword",kw);
        model.addAttribute("status",status);
        model.addAttribute("inUse",inUse);
        int activeFilterCount = 0;
        if (kw != null) activeFilterCount++;
        if (status != null) activeFilterCount++;
        if (inUse != null) activeFilterCount++;
        model.addAttribute("activeFilterCount", activeFilterCount);
        model.addAttribute("soleUsage",usage);
        return "admin/danh-muc-kich-co/danh-sach";
    }
    @GetMapping("/create") public String create(Model model) {
        model.addAttribute("lookup",new LookupForm());return "admin/danh-muc-kich-co/bieu-mau";
    }
    @GetMapping("/edit/{id}") public String edit(@PathVariable Integer id,Model model) {
        var sole=service.get(id);var form=new LookupForm();form.setId(id);form.setValue(sole.getName());form.setStatus(sole.getStatus());
        model.addAttribute("lookup",form);return "admin/danh-muc-kich-co/bieu-mau";
    }
    @PostMapping("/save") public String save(@Valid @ModelAttribute("lookup") LookupForm form,BindingResult errors,RedirectAttributes flash) {
        if(!errors.hasErrors())try {
            service.save(form);flash.addFlashAttribute("message","Đã lưu đế giày.");return "redirect:/admin/soles";
        } catch(CatalogValidationException ex){errors.rejectValue(ex.getField(),"invalid",ex.getMessage());}
          catch(DataIntegrityViolationException ex){errors.rejectValue("value","duplicate","Tên đế giày đã tồn tại hoặc dữ liệu vừa thay đổi.");}
        return "admin/danh-muc-kich-co/bieu-mau";
    }
    @PostMapping("/toggle-status/{id}") public String toggle(@PathVariable Integer id,RedirectAttributes flash) {
        service.toggle(id);flash.addFlashAttribute("message","Đã đổi trạng thái hiển thị của đế giày.");return "redirect:/admin/soles";
    }
    @PostMapping("/delete/{id}") public String delete(@PathVariable Integer id,RedirectAttributes flash) {
        try{String message=service.delete(id);flash.addFlashAttribute(message.startsWith("Không")?"error":"message",message);}
        catch(DataIntegrityViolationException ex){flash.addFlashAttribute("error","Không thể xóa đế giày vì sản phẩm vừa tham chiếu đến dữ liệu này.");}
        return "redirect:/admin/soles";
    }
}
