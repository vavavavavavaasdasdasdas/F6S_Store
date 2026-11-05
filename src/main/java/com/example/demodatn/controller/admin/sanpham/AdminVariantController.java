package com.example.demodatn.controller.admin.sanpham;

import com.example.demodatn.dto.sanpham.InventoryForm;
import com.example.demodatn.dto.sanpham.VariantForm;
import com.example.demodatn.entity.sanpham.ProductVariant;
import com.example.demodatn.exception.CatalogValidationException;
import com.example.demodatn.repository.sanpham.AdminCatalogQuery;
import com.example.demodatn.repository.sanpham.CategoryRepository;
import com.example.demodatn.repository.sanpham.ColorRepository;
import com.example.demodatn.repository.sanpham.MaterialRepository;
import com.example.demodatn.repository.sanpham.ProductRepository;
import com.example.demodatn.repository.sanpham.SizeRepository;
import com.example.demodatn.repository.sanpham.SoleRepository;
import com.example.demodatn.service.sanpham.AdminCatalogService;
import com.example.demodatn.service.sanpham.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller @RequiredArgsConstructor
public class AdminVariantController {
    private final AdminCatalogService catalog;
    private final AdminCatalogQuery query;
    private final InventoryService inventory;
    private final ProductRepository products;
    private final SizeRepository sizes;
    private final MaterialRepository materials;
    private final ColorRepository colors;
    private final SoleRepository soles;
    private final CategoryRepository categories;
    @Value("${shop.low-stock-threshold:5}") private int threshold;

    @GetMapping("/admin/variants")
    public String list(@RequestParam(required=false) Integer productId, @RequestParam(defaultValue="0") String page,
                       @RequestParam(required=false) Integer materialId, @RequestParam(required=false) Integer sizeId,
                       @RequestParam(required=false) Integer colorId, @RequestParam(required=false) Integer soleId,
                       @RequestParam(required=false) Integer categoryId,
                       @RequestParam(defaultValue="all") String stock,
                       @RequestParam(required=false) Integer variantId,
                       @RequestParam(required=false) Boolean status, @RequestParam(defaultValue="") String keyword, Model model) {
        return listFor(productId, page, materialId, sizeId, colorId, soleId, categoryId, stock, variantId, status, keyword, model);
    }

    private String listFor(Integer productId, String page, Integer materialId, Integer sizeId, Integer colorId, Integer soleId,
                           Integer categoryId, String stock, Integer variantId, Boolean status, String keyword, Model model) {
        String effectiveKeyword = keyword;
        if ((effectiveKeyword == null || effectiveKeyword.isBlank()) && variantId != null) {
            try {
                var v = catalog.variant(variantId);
                if (v != null) effectiveKeyword = v.getCode();
            } catch (Exception ignored) {}
        }
        var result = query.variantsPage(productId, stock, Math.max(0, threshold), page, materialId, sizeId, status, effectiveKeyword, categoryId, colorId, soleId);
        model.addAttribute("variants", result.getContent());
        model.addAttribute("dataPage", result);
        model.addAttribute("colors", colors.findAll());
        model.addAttribute("colorId", colorId);
        model.addAttribute("soles", soles.findAll());
        model.addAttribute("soleId", soleId);
        model.addAttribute("materials", materials.findAll());
        model.addAttribute("materialId", materialId);
        model.addAttribute("sizes", sizes.findAll());
        model.addAttribute("sizeId", sizeId);
        model.addAttribute("categories", categories.findAll());
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("status", status);
        model.addAttribute("keyword", keyword);
        model.addAttribute("stock", stock);
        model.addAttribute("threshold", Math.max(0, threshold));
        model.addAttribute("variantId", variantId);
        model.addAttribute("selectedProduct", productId == null ? null : catalog.product(productId));
        model.addAttribute("products", products.findAll());
        model.addAttribute("productId", productId);

        int activeFilterCount = 0;
        if (colorId != null) activeFilterCount++;
        if (soleId != null) activeFilterCount++;
        if (keyword != null && !keyword.trim().isEmpty()) activeFilterCount++;
        if (productId != null) activeFilterCount++;
        if (categoryId != null) activeFilterCount++;
        if (sizeId != null) activeFilterCount++;
        if (materialId != null) activeFilterCount++;
        if (status != null) activeFilterCount++;
        if (stock != null && !"all".equalsIgnoreCase(stock)) activeFilterCount++;
        model.addAttribute("activeFilterCount", activeFilterCount);
        return "admin/bien-the/danh-sach";
    }

    @GetMapping("/admin/variants/create")
    public String create(@RequestParam(required=false) Integer productId, Model model) {
        VariantForm form = new VariantForm();
        form.setProductId(productId);
        model.addAttribute("variant", form);
        return form(model);
    }

    @GetMapping("/admin/variants/edit/{id}")
    public String edit(@PathVariable Integer id, Model model) {
        ProductVariant variant = catalog.variant(id);
        VariantForm form = new VariantForm();
        form.setId(id);
        form.setCode(variant.getCode());
        form.setProductId(variant.getProduct().getId());
        form.setSizeId(variant.getSize().getId());
        form.setMaterialId(variant.getMaterial().getId());
        form.setColorId(variant.getColor().getId());
        form.setSoleId(variant.getSole().getId());
        form.setPrice(variant.getPrice());
        form.setQuantity(variant.getQuantity());
        form.setExpectedQuantity(variant.getQuantity());
        form.setStatus(variant.getStatus());
        model.addAttribute("variant", form);
        return form(model);
    }

    @PostMapping("/admin/variants/save")
    public String save(@Valid @ModelAttribute("variant") VariantForm form, BindingResult result, Model model, RedirectAttributes flash) {
        if (!result.hasErrors()) {
            try {
                ProductVariant saved = catalog.saveVariant(form);
                flash.addFlashAttribute("message", "Đã lưu biến thể. Mọi thay đổi tồn kho đã được ghi lịch sử.");
                return "redirect:/admin/products-crud/view/" + saved.getProduct().getId();
            } catch (CatalogValidationException ex) {
                result.rejectValue(ex.getField(), "invalid", ex.getMessage());
            } catch (DataIntegrityViolationException ex) {
                result.rejectValue("materialId", "duplicate", "Bộ năm sản phẩm, kích cỡ, chất liệu, màu sắc và đế giày đã tồn tại hoặc dữ liệu vừa thay đổi.");
            }
        }
        return form(model);
    }

    @PostMapping({"/admin/variants/status/{id}", "/admin/variants/toggle-status/{id}"})
    public Object status(@PathVariable Integer id, @RequestParam boolean selling,
                         jakarta.servlet.http.HttpServletRequest request, RedirectAttributes flash) {
        Integer productId = catalog.variantStatus(id, selling);
        String msg = selling ? "Đã bán lại biến thể. Tồn kho được giữ nguyên." : "Đã ngừng bán biến thể. Tồn kho và lịch sử được giữ nguyên.";
        String xhr = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        if ("XMLHttpRequest".equalsIgnoreCase(xhr) || (accept != null && accept.contains("application/json"))) {
            return ResponseEntity.ok(Map.of(
                    "id", id,
                    "selling", selling,
                    "active", selling,
                    "stateLabel", selling ? "Đang bán" : "Ngừng bán",
                    "message", msg
            ));
        }
        flash.addFlashAttribute("message", msg);
        return "redirect:/admin/products-crud/view/" + productId;
    }

    @PostMapping("/admin/variants/delete/{id}")
    public void delete(@PathVariable Integer id) {
        throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED,
                "Chức năng xóa đã chuyển thành tắt hoạt động.");
    }

    @GetMapping("/admin/variants/adjust/{id}")
    public String adjust(@PathVariable Integer id, Model model) {
        ProductVariant variant = catalog.variant(id);
        InventoryForm form = new InventoryForm();
        form.setVariantId(id);
        form.setExpectedQuantity(variant.getQuantity());
        model.addAttribute("adjustment", form);
        model.addAttribute("item", variant);
        return "admin/bien-the/dieu-chinh";
    }

    @PostMapping("/admin/variants/adjust")
    public Object saveAdjust(@Valid @ModelAttribute("adjustment") InventoryForm form, BindingResult result,
                             Model model, RedirectAttributes flash, jakarta.servlet.http.HttpServletRequest request) {
        if (form.getVariantId() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Thiếu biến thể cần điều chỉnh.");
        }
        String xhr = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(xhr) || (accept != null && accept.contains("application/json"));

        if (!result.hasErrors()) {
            try {
                inventory.adjust(form);
                var variant = catalog.variant(form.getVariantId());
                String msg = "Đã điều chỉnh tồn kho và ghi lịch sử.";
                if (isAjax) {
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "id", form.getVariantId(),
                            "newQuantity", variant.getQuantity(),
                            "message", msg
                    ));
                }
                flash.addFlashAttribute("message", msg);
                return "redirect:/admin/variants?variantId=" + form.getVariantId();
            } catch (CatalogValidationException ex) {
                result.rejectValue(ex.getField(), "invalid", ex.getMessage());
                if (isAjax) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "error", ex.getMessage(),
                            "field", ex.getField()
                    ));
                }
            }
        } else if (isAjax) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Vui lòng kiểm tra lại thông tin điều chỉnh.",
                    "field", "quantity"
            ));
        }
        model.addAttribute("item", catalog.variant(form.getVariantId()));
        return "admin/bien-the/dieu-chinh";
    }

    @GetMapping("/admin/variants/{id}/history")
    @ResponseBody
    public ResponseEntity<?> history(@PathVariable Integer id,
                                     @RequestParam(defaultValue="0") String page) {
        var logs = query.history(id, page);
        var variant = catalog.variant(id);
        return ResponseEntity.ok(Map.of(
                "variantId", id,
                "variantCode", variant.getCode(),
                "productName", variant.getProduct().getName(),
                "content", logs.getContent(),
                "totalElements", logs.getTotalElements(),
                "totalPages", logs.getTotalPages(),
                "number", logs.getNumber()
        ));
    }

    private String form(Model model) {
        model.addAttribute("products", products.findAll());
        model.addAttribute("sizes", sizes.findAll());
        model.addAttribute("materials", materials.findByStatusTrueOrderByNameAscIdAsc());
        model.addAttribute("colors", new java.util.ArrayList<>(colors.findByStatusTrueOrderByNameAscIdAsc()));
        model.addAttribute("soles", new java.util.ArrayList<>(soles.findByStatusTrueOrderByNameAscIdAsc()));
        VariantForm current = (VariantForm) model.getAttribute("variant");
        if (current.getId() != null) {
            var existingVariant = catalog.variant(current.getId());
            if (!Boolean.TRUE.equals(existingVariant.getColor().getStatus()))
                ((java.util.List<com.example.demodatn.entity.sanpham.Color>) model.getAttribute("colors")).add(existingVariant.getColor());
            if (!Boolean.TRUE.equals(existingVariant.getSole().getStatus()))
                ((java.util.List<com.example.demodatn.entity.sanpham.Sole>) model.getAttribute("soles")).add(existingVariant.getSole());
            var existing = catalog.variant(current.getId()).getMaterial();
            if (!Boolean.TRUE.equals(existing.getStatus())) {
                var options = new java.util.ArrayList<>(materials.findByStatusTrueOrderByNameAscIdAsc());
                options.add(existing);
                model.addAttribute("materials", options);
            }
        }
        return "admin/bien-the/bieu-mau";
    }
}
