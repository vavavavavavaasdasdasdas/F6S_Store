package com.example.demodatn.controller.admin.sanpham;
import com.example.demodatn.dto.sanpham.InventoryForm;
import com.example.demodatn.entity.sanpham.ProductVariant;
import com.example.demodatn.exception.CatalogValidationException;
import com.example.demodatn.repository.sanpham.AdminCatalogQuery;
import com.example.demodatn.repository.sanpham.CategoryRepository;
import com.example.demodatn.repository.sanpham.MaterialRepository;
import com.example.demodatn.repository.sanpham.SizeRepository;
import com.example.demodatn.service.sanpham.AdminCatalogService;
import com.example.demodatn.service.sanpham.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;

@Controller @RequestMapping("/admin/inventory") @RequiredArgsConstructor
public class AdminInventoryController {
    private final AdminCatalogQuery query;
    private final AdminCatalogService catalog;
    private final InventoryService inventory;
    private final MaterialRepository materials;
    private final CategoryRepository categories;
    private final SizeRepository sizes;
    @Value("${shop.low-stock-threshold:5}") private int threshold;
    @GetMapping
    public String list(@RequestParam(defaultValue="all") String stock,
                       @RequestParam(required=false) Integer variantId,
                       @RequestParam(defaultValue="0") String page,
                       @RequestParam(defaultValue="0") String historyPage,
                       @RequestParam(required=false) Integer materialId,
                       @RequestParam(defaultValue="") String keyword,
                       @RequestParam(required=false) Integer categoryId,
                       @RequestParam(required=false) Integer sizeId,
                       @RequestParam(defaultValue="") String historyKeyword,
                       @RequestParam(defaultValue="all") String changeType,
                       @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate historyFrom,
                       @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate historyTo,
                       @RequestParam(defaultValue="") String actor,
                       Model model) {
        var result=query.variantsPage(null,stock,Math.max(0,threshold),page,materialId,sizeId,null,keyword,categoryId);
        model.addAttribute("materials",materials.findAll()); model.addAttribute("materialId",materialId);
        model.addAttribute("categories",categories.findAll()); model.addAttribute("categoryId",categoryId);
        model.addAttribute("sizes",sizes.findAll()); model.addAttribute("sizeId",sizeId);
        model.addAttribute("keyword",keyword);
        model.addAttribute("variants",result.getContent()); model.addAttribute("dataPage",result); model.addAttribute("stock",stock);
        model.addAttribute("threshold",Math.max(0,threshold));
        int activeFilterCount = 0;
        if (keyword != null && !keyword.trim().isEmpty()) activeFilterCount++;
        if (categoryId != null) activeFilterCount++;
        if (materialId != null) activeFilterCount++;
        if (sizeId != null) activeFilterCount++;
        if (stock != null && !"all".equalsIgnoreCase(stock)) activeFilterCount++;
        model.addAttribute("activeFilterCount", activeFilterCount);

        var logs=query.history(variantId,historyPage,materialId,changeType,historyFrom,historyTo,actor,historyKeyword);
        model.addAttribute("history",logs.getContent());model.addAttribute("historyPage",logs);
        model.addAttribute("historyVariant",variantId);
        model.addAttribute("historyKeyword",historyKeyword);
        model.addAttribute("changeType",changeType);
        model.addAttribute("historyFrom",historyFrom);
        model.addAttribute("historyTo",historyTo);
        model.addAttribute("actor",actor);
        model.addAttribute("actors",query.distinctHistoryActors());
        int historyFilterCount = 0;
        if (variantId != null) historyFilterCount++;
        if (historyKeyword != null && !historyKeyword.trim().isEmpty()) historyFilterCount++;
        if (changeType != null && !changeType.isBlank() && !"all".equalsIgnoreCase(changeType)) historyFilterCount++;
        if (historyFrom != null) historyFilterCount++;
        if (historyTo != null) historyFilterCount++;
        if (actor != null && !actor.trim().isEmpty()) historyFilterCount++;
        model.addAttribute("historyFilterCount", historyFilterCount);
        return "admin/ton-kho/danh-sach";
    }
    @GetMapping("/adjust/{id}")
    public String adjust(@PathVariable Integer id,Model model) {
        ProductVariant variant=catalog.variant(id); InventoryForm form=new InventoryForm();
        form.setVariantId(id); form.setExpectedQuantity(variant.getQuantity()); model.addAttribute("adjustment",form);
        return form(id,model);
    }
    @PostMapping("/adjust")
    public String save(@Valid @ModelAttribute("adjustment") InventoryForm form,BindingResult result,Model model,RedirectAttributes flash) {
        if (form.getVariantId() == null)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Thiếu biến thể cần điều chỉnh.");
        if(!result.hasErrors()) {
            try {
                inventory.adjust(form); flash.addFlashAttribute("message","Đã điều chỉnh tồn kho và ghi lịch sử.");
                return "redirect:/admin/inventory?variantId="+form.getVariantId();
            } catch(CatalogValidationException ex) { result.rejectValue(ex.getField(),"invalid",ex.getMessage()); }
        }
        return form(form.getVariantId(),model);
    }
    private String form(Integer id,Model model) { model.addAttribute("item",catalog.variant(id)); return "admin/ton-kho/bieu-mau"; }
}
