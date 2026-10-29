package com.example.demodatn.controller.admin.sanpham;
import com.example.demodatn.dto.sanpham.VariantForm;
import com.example.demodatn.entity.sanpham.ProductVariant;
import com.example.demodatn.exception.CatalogValidationException;
import com.example.demodatn.repository.sanpham.AdminCatalogQuery;
import com.example.demodatn.repository.sanpham.MaterialRepository;
import com.example.demodatn.repository.sanpham.ProductRepository;
import com.example.demodatn.repository.sanpham.SizeRepository;
import com.example.demodatn.service.sanpham.AdminCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.dao.DataIntegrityViolationException;

@Controller @RequiredArgsConstructor
public class AdminVariantController {
    private final AdminCatalogService catalog;
    private final AdminCatalogQuery query;
    private final ProductRepository products;
    private final SizeRepository sizes;
    private final MaterialRepository materials;
    @GetMapping("/admin/variants")
    public String list(@RequestParam(required=false) Integer productId, @RequestParam(defaultValue="0") String page,
                       @RequestParam(required=false) Integer materialId, @RequestParam(required=false) Integer sizeId,
                       @RequestParam(required=false) Boolean status, @RequestParam(defaultValue="") String keyword, Model model) {
        return listFor(productId,page,materialId,sizeId,status,keyword,model);
    }
    @GetMapping("/admin/products-crud/{productId}/variants")
    public String productVariants(@PathVariable Integer productId, @RequestParam(defaultValue="0") String page,
                                  @RequestParam(required=false) Integer materialId, @RequestParam(required=false) Integer sizeId,
                                  @RequestParam(required=false) Boolean status, @RequestParam(defaultValue="") String keyword, Model model) {
        return listFor(productId,page,materialId,sizeId,status,keyword,model);
    }
    private String listFor(Integer productId, String page, Integer materialId, Integer sizeId, Boolean status, String keyword, Model model) {
        var result=query.variantsPage(productId,"all",0,page,materialId,sizeId,status,keyword,null);
        model.addAttribute("variants",result.getContent()); model.addAttribute("dataPage",result);
        model.addAttribute("materials",materials.findAll()); model.addAttribute("materialId",materialId);
        model.addAttribute("sizes",sizes.findAll()); model.addAttribute("sizeId",sizeId);
        model.addAttribute("status",status); model.addAttribute("keyword",keyword);
        model.addAttribute("selectedProduct",productId==null?null:catalog.product(productId));
        model.addAttribute("products",products.findAll()); model.addAttribute("productId",productId);
        int activeFilterCount = 0;
        if (keyword != null && !keyword.trim().isEmpty()) activeFilterCount++;
        if (productId != null) activeFilterCount++;
        if (sizeId != null) activeFilterCount++;
        if (materialId != null) activeFilterCount++;
        if (status != null) activeFilterCount++;
        model.addAttribute("activeFilterCount", activeFilterCount);
        return "admin/bien-the/danh-sach";
    }
    @GetMapping("/admin/variants/create")
    public String create(@RequestParam(required=false) Integer productId, Model model) {
        VariantForm form=new VariantForm(); form.setProductId(productId); model.addAttribute("variant",form);
        return form(model);
    }
    @GetMapping("/admin/variants/edit/{id}")
    public String edit(@PathVariable Integer id, Model model) {
        ProductVariant variant=catalog.variant(id); VariantForm form=new VariantForm();
        form.setId(id); form.setProductId(variant.getProduct().getId()); form.setSizeId(variant.getSize().getId()); form.setMaterialId(variant.getMaterial().getId());
        form.setPrice(variant.getPrice()); form.setQuantity(variant.getQuantity()); form.setExpectedQuantity(variant.getQuantity()); form.setStatus(variant.getStatus());
        model.addAttribute("variant",form); return form(model);
    }
    @PostMapping("/admin/variants/save")
    public String save(@Valid @ModelAttribute("variant") VariantForm form, BindingResult result, Model model, RedirectAttributes flash) {
        if(!result.hasErrors()) {
            try {
                ProductVariant saved=catalog.saveVariant(form);
                flash.addFlashAttribute("message","Đã lưu biến thể. Mọi thay đổi tồn kho đã được ghi lịch sử.");
                return "redirect:/admin/products-crud/"+saved.getProduct().getId()+"/variants";
            } catch(CatalogValidationException ex) { result.rejectValue(ex.getField(),"invalid",ex.getMessage()); }
              catch(DataIntegrityViolationException ex) { result.rejectValue("materialId","duplicate","Bộ ba sản phẩm, kích cỡ và chất liệu đã tồn tại hoặc dữ liệu vừa thay đổi."); }
        }
        return form(model);
    }
    @PostMapping("/admin/variants/status/{id}")
    public String status(@PathVariable Integer id,@RequestParam boolean selling,RedirectAttributes flash) {
        Integer productId=catalog.variantStatus(id,selling);
        flash.addFlashAttribute("message",selling?"Đã bán lại biến thể. Tồn kho được giữ nguyên.":"Đã ngừng bán biến thể. Tồn kho và lịch sử được giữ nguyên.");
        return "redirect:/admin/products-crud/"+productId+"/variants";
    }
    private String form(Model model) {
        model.addAttribute("products",products.findAll()); model.addAttribute("sizes",sizes.findAll()); model.addAttribute("materials",materials.findByStatusTrueOrderByNameAscIdAsc());
        VariantForm current=(VariantForm)model.getAttribute("variant");
        if(current.getId()!=null) {
            var existing=catalog.variant(current.getId()).getMaterial();
            if(!Boolean.TRUE.equals(existing.getStatus())) {
                var options=new java.util.ArrayList<>(materials.findByStatusTrueOrderByNameAscIdAsc());
                options.add(existing); model.addAttribute("materials",options);
            }
        }
        return "admin/bien-the/bieu-mau";
    }
}
