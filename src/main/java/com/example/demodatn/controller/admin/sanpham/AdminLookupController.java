package com.example.demodatn.controller.admin.sanpham;
import com.example.demodatn.dto.sanpham.LookupForm;
import com.example.demodatn.entity.sanpham.Category;
import com.example.demodatn.entity.sanpham.Material;
import com.example.demodatn.exception.CatalogValidationException;
import com.example.demodatn.repository.sanpham.CategoryRepository;
import com.example.demodatn.repository.sanpham.MaterialRepository;
import com.example.demodatn.repository.sanpham.SizeRepository;
import com.example.demodatn.service.sanpham.AdminCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/{kind:categories|sizes|materials|danh-muc|size|chat-lieu}")
@RequiredArgsConstructor
public class AdminLookupController {
    private final CategoryRepository categories;
    private final SizeRepository sizes;
    private final MaterialRepository materials;
    private final AdminCatalogService catalog;
    private final com.example.demodatn.repository.sanpham.ProductVariantRepository variants;
    private final com.example.demodatn.util.Pagination pagination;

    private String normalizeKind(String kind) {
        if ("danh-muc".equals(kind)) return "categories";
        if ("size".equals(kind)) return "sizes";
        if ("chat-lieu".equals(kind)) return "materials";
        return kind;
    }

    @ModelAttribute
    public void common(@PathVariable String kind, Model model) {
        String k = normalizeKind(kind);
        model.addAttribute("kind", k);
        model.addAttribute("lookupTitle", "sizes".equals(k) ? "Kích cỡ" : ("materials".equals(k) ? "Chất liệu" : "Thương hiệu"));
    }

    @GetMapping
    public String list(@PathVariable String kind,
                       @RequestParam(defaultValue = "0") String page,
                       @RequestParam(required = false) String keyword,
                       Model model) {
        String k = normalizeKind(kind);
        String kw = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        model.addAttribute("keyword", kw);
        org.springframework.data.domain.Page<?> items;
        if ("sizes".equals(k)) {
            items = pagination.load(page, org.springframework.data.domain.Sort.by("id"),
                    p -> kw != null ? sizes.findBySizeValueContainingIgnoreCase(kw, p) : sizes.findAll(p));
        } else if ("materials".equals(k)) {
            items = pagination.load(page, org.springframework.data.domain.Sort.by("id"),
                    p -> kw != null ? materials.findByNameContainingIgnoreCase(kw, p) : materials.findAll(p));
        } else {
            items = pagination.load(page, org.springframework.data.domain.Sort.by("id"),
                    p -> kw != null ? categories.findByNameContainingIgnoreCase(kw, p) : categories.findAll(p));
        }
        if ("materials".equals(k)) model.addAttribute("materialUsage", variants.materialUsage().stream().collect(
                java.util.stream.Collectors.toMap(r -> ((Number)r.get("materialId")).intValue(), r -> ((Number)r.get("usageCount")).longValue())));
        model.addAttribute("items", items);
        return "admin/danh-muc-kich-co/danh-sach";
    }

    @GetMapping("/create")
    public String create(@PathVariable String kind, Model model) {
        model.addAttribute("lookup", new LookupForm());
        return "admin/danh-muc-kich-co/bieu-mau";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable String kind, @PathVariable Integer id, Model model) {
        String k = normalizeKind(kind);
        LookupForm form = new LookupForm();
        form.setId(id);
        if ("sizes".equals(k)) {
            form.setValue(sizes.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)).getSizeValue());
        } else if ("materials".equals(k)) {
            Material material = materials.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            form.setValue(material.getName());
            form.setStatus(material.getStatus());
        } else {
            Category category = categories.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            form.setValue(category.getName());
            form.setStatus(category.getStatus());
        }
        model.addAttribute("lookup", form);
        return "admin/danh-muc-kich-co/bieu-mau";
    }

    @PostMapping("/save")
    public String save(@PathVariable String kind,
                       @Valid @ModelAttribute("lookup") LookupForm form,
                       BindingResult result,
                       Model model,
                       RedirectAttributes flash) {
        String k = normalizeKind(kind);
        if (!result.hasErrors()) {
            try {
                catalog.saveLookup(k, form);
                String entityName = "sizes".equals(k) ? "kích cỡ" : ("materials".equals(k) ? "chất liệu" : "thương hiệu");
                flash.addFlashAttribute("message", "Đã lưu " + entityName + " thành công.");
                return "redirect:/admin/" + k;
            } catch (CatalogValidationException ex) {
                result.rejectValue(ex.getField(), "invalid", ex.getMessage());
            } catch (DataIntegrityViolationException ex) {
                String dupMsg = "sizes".equals(k) ? "Kích cỡ đã tồn tại." : ("materials".equals(k) ? "Tên chất liệu đã tồn tại." : "Tên thương hiệu đã tồn tại.");
                result.rejectValue("value", "duplicate", dupMsg);
            }
        }
        return "admin/danh-muc-kich-co/bieu-mau";
    }

    @PostMapping("/toggle-status/{id}")
    public String toggleStatus(@PathVariable String kind, @PathVariable Integer id, RedirectAttributes flash) {
        String k = normalizeKind(kind);
        if ("categories".equals(k)) {
            Category category = categories.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            boolean nextStatus = !Boolean.TRUE.equals(category.getStatus());
            category.setStatus(nextStatus);
            categories.saveAndFlush(category);
            flash.addFlashAttribute("message", "Đã chuyển thương hiệu '" + category.getName() + "' sang trạng thái: " + (nextStatus ? "Hiển thị" : "Ẩn") + ".");
        } else if ("materials".equals(k)) {
            Material material = materials.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            boolean nextStatus = !Boolean.TRUE.equals(material.getStatus());
            material.setStatus(nextStatus);
            materials.saveAndFlush(material);
            flash.addFlashAttribute("message", "Đã chuyển chất liệu '" + material.getName() + "' sang trạng thái: " + (nextStatus ? "Hiển thị" : "Ẩn") + ".");
        }
        return "redirect:/admin/" + k;
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable String kind, @PathVariable Integer id, RedirectAttributes flash) {
        String k = normalizeKind(kind);
        try {
            String message = catalog.deleteLookup(k, id);
            flash.addFlashAttribute(message.startsWith("Không") ? "error" : "message", message);
        } catch (DataIntegrityViolationException ex) {
            flash.addFlashAttribute("error", "Không thể xóa vì dữ liệu đang được tham chiếu.");
        }
        return "redirect:/admin/" + k;
    }
}
