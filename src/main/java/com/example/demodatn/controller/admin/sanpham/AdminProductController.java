package com.example.demodatn.controller.admin.sanpham;
import com.example.demodatn.dto.sanpham.AdminProductForm;
import com.example.demodatn.entity.sanpham.Product;
import com.example.demodatn.exception.CatalogValidationException;
import com.example.demodatn.repository.sanpham.AdminCatalogQuery;
import com.example.demodatn.repository.sanpham.CategoryRepository;
import com.example.demodatn.service.sanpham.AdminCatalogService;
import com.example.demodatn.service.sanpham.ProductGalleryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;

@Controller @RequestMapping("/admin/products-crud") @RequiredArgsConstructor
public class AdminProductController {
    private final AdminCatalogService catalog;
    private final AdminCatalogQuery query;
    private final CategoryRepository categories;
    private final ProductGalleryService gallery;
    private final com.example.demodatn.repository.sanpham.MaterialRepository materials;
    private final com.example.demodatn.repository.sanpham.SizeRepository sizes;
    private final com.example.demodatn.repository.sanpham.SoleRepository soles;
    private final com.example.demodatn.repository.sanpham.ProductVariantRepository variants;
    private final com.example.demodatn.util.Pagination pagination;
    @org.springframework.beans.factory.annotation.Value("${shop.upload-max-bytes:5242880}") private long maxBytes;

    @GetMapping
    public String list(@RequestParam(defaultValue="") String keyword, @RequestParam(required=false) Integer categoryId,
                       @RequestParam(required=false) Integer materialId, @RequestParam(required=false) Boolean status, @RequestParam(defaultValue="newest") String sort,
                       @RequestParam(defaultValue="0") String page, @RequestParam(required=false) Integer soleId,
                       @RequestParam(required=false) BigDecimal minPrice, @RequestParam(required=false) BigDecimal maxPrice, Model model) {
        model.addAttribute("productPage",query.products(keyword,categoryId,status,sort,page,materialId,soleId,minPrice,maxPrice));
        model.addAttribute("soles",soles.findAll());model.addAttribute("soleId",soleId);
        model.addAttribute("categories",categories.findAll());
        model.addAttribute("materials",materials.findAll()); model.addAttribute("materialId",materialId);
        model.addAttribute("keyword",keyword); model.addAttribute("categoryId",categoryId);
        model.addAttribute("status",status); model.addAttribute("sort",sort);
        model.addAttribute("minPrice",minPrice); model.addAttribute("maxPrice",maxPrice);
        int activeFilterCount = 0;
        if (keyword != null && !keyword.trim().isEmpty()) activeFilterCount++;
        if (categoryId != null) activeFilterCount++;
        if (soleId != null) activeFilterCount++;
        if (materialId != null) activeFilterCount++;
        if (status != null) activeFilterCount++;
        if (minPrice != null) activeFilterCount++;
        if (maxPrice != null) activeFilterCount++;
        model.addAttribute("activeFilterCount", activeFilterCount);
        return "admin/san-pham/danh-sach";
    }
    @GetMapping("/create")
    public String create(Model model) { model.addAttribute("product",new AdminProductForm()); return form(model); }
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Integer id, Model model) {
        Product product=catalog.product(id);
        AdminProductForm form=new AdminProductForm();
        form.setId(id); form.setCode(product.getCode()); form.setName(product.getName());
        form.setDescription(product.getDescription()); form.setImageUrl(product.getImageUrl());
        form.setSoleId(product.getSole()==null?null:product.getSole().getId());
        form.setCategoryId(product.getCategory()==null?null:product.getCategory().getId()); form.setStatus(product.getStatus());
        model.addAttribute("product",form); return form(model);
    }
    @GetMapping("/view/{id}")
    public String view(@PathVariable Integer id,@RequestParam(defaultValue="0") String page,Model model) {
        model.addAttribute("product",catalog.product(id));
        var result=query.variantsPage(id,"all",0,page);model.addAttribute("variants",result.getContent());model.addAttribute("variantPage",result); return "admin/san-pham/chi-tiet";
    }
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("product") AdminProductForm product, BindingResult result, Model model, RedirectAttributes flash) {
        boolean hasInitialSizes = (product.getInitialSizeIds()!=null && !product.getInitialSizeIds().isEmpty())
                || (product.getNewInitialSizeValue()!=null && !product.getNewInitialSizeValue().isBlank());
        if (product.getId()==null && hasInitialSizes) {
            boolean hasMaterial = product.getInitialMaterialId()!=null
                    || (product.getNewInitialMaterialName()!=null && !product.getNewInitialMaterialName().isBlank());
            if (!hasMaterial) result.rejectValue("initialMaterialId","required","Chọn chất liệu khi chọn kích cỡ ban đầu.");
            if (product.getInitialPrice()==null) result.rejectValue("initialPrice","required","Nhập giá khi chọn kích cỡ ban đầu.");
            if (product.getInitialQuantity()==null) result.rejectValue("initialQuantity","required","Nhập tồn kho ban đầu từ 0 trở lên.");
        }
        if (!result.hasErrors()) {
            try {
                Product saved=catalog.saveProduct(product);
                flash.addFlashAttribute("message","Đã lưu sản phẩm và các biến thể ban đầu (nếu có).");
                return "redirect:/admin/products-crud/view/"+saved.getId();
            } catch(CatalogValidationException ex) { result.rejectValue(ex.getField(),"invalid",ex.getMessage()); }
              catch(DataIntegrityViolationException ex) { result.rejectValue("code","duplicate","Mã sản phẩm đã tồn tại hoặc dữ liệu liên quan vừa thay đổi."); }
        }
        if(product.getId()!=null) product.setImageUrl(catalog.product(product.getId()).getImageUrl());
        return form(model);
    }
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Integer id, RedirectAttributes flash) {
        try { flash.addFlashAttribute("message",catalog.deleteProduct(id)); }
        catch(DataIntegrityViolationException ex) { flash.addFlashAttribute("error","Không thể xóa: sản phẩm vừa phát sinh dữ liệu liên quan."); }
        return "redirect:/admin/products-crud";
    }
    private String form(Model model) {
        AdminProductForm form=(AdminProductForm)model.getAttribute("product");
        model.addAttribute("productImages", form.getId()==null ? java.util.List.of() : gallery.list(form.getId()));
        model.addAttribute("categories",categories.findAll());
        model.addAttribute("materials",materials.findByStatusTrueOrderByNameAscIdAsc());
        var choices=new java.util.ArrayList<>(soles.findByStatusTrueOrderByNameAscIdAsc());
        if(form.getId()!=null) {
            var current=catalog.product(form.getId()).getSole();
            if(current!=null&&!Boolean.TRUE.equals(current.getStatus()))choices.add(current);
        }
        model.addAttribute("soles",choices);
        model.addAttribute("sizes",sizes.findAll());
        model.addAttribute("existingMaterials",form.getId()==null ? java.util.List.of() : variants.materialNames(form.getId()));
        model.addAttribute("uploadMaxBytes",maxBytes);
        return "admin/san-pham/bieu-mau";
    }
}
