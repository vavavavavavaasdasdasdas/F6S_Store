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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    private final com.example.demodatn.repository.sanpham.ColorRepository colors;
    @org.springframework.beans.factory.annotation.Value("${shop.low-stock-threshold:10}") private int lowStockThreshold;

    @GetMapping
    public String list(@RequestParam(defaultValue="") String keyword,@RequestParam(required=false) Integer categoryId,
                       @RequestParam(required=false) Boolean status,@RequestParam(required=false) String stockStatus,
                       @RequestParam(defaultValue="newest") String sort,
                       @RequestParam(defaultValue="0") String page,Model model) {
        if(!java.util.Set.of("newest","name","code").contains(sort))sort="newest";
        model.addAttribute("productPage",query.productsWithStock(keyword,categoryId,status,sort,page,stockStatus,lowStockThreshold));
        model.addAttribute("categories",categories.findAll());model.addAttribute("keyword",keyword);
        model.addAttribute("categoryId",categoryId);model.addAttribute("status",status);model.addAttribute("stockStatus",stockStatus);
        model.addAttribute("sort",sort);model.addAttribute("lowStockThreshold",lowStockThreshold);
        boolean hasStockFilter = stockStatus != null && !stockStatus.isBlank() && !"ALL".equalsIgnoreCase(stockStatus);
        model.addAttribute("activeFilterCount",(keyword.isBlank()?0:1)+(categoryId==null?0:1)+(status==null?0:1)+(hasStockFilter?1:0));
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
    public String view(@PathVariable Integer id,@RequestParam(defaultValue="0") String page,
                       @RequestParam(required=false) Integer sizeId,@RequestParam(required=false) Integer colorId,
                       @RequestParam(required=false) Integer materialId,@RequestParam(required=false) Integer soleId,
                       @RequestParam(defaultValue="all") String stock,
                       @RequestParam(required=false) Boolean variantStatus,Model model) {
        model.addAttribute("product",catalog.product(id));
        var all=query.variantsPage(id,"all",0,0);
        model.addAttribute("variantCount",all.getTotalElements());
        model.addAttribute("totalStock",query.productStock(id));
        model.addAttribute("productImages",gallery.list(id));
        var result=query.variantsPage(id,stock,lowStockThreshold,page,materialId,sizeId,variantStatus,null,null,colorId,soleId);
        model.addAttribute("variants",result.getContent());model.addAttribute("variantPage",result);
        model.addAttribute("sizes",sizes.findAll());model.addAttribute("colors",colors.findAll());
        model.addAttribute("materials",materials.findAll());model.addAttribute("soles",soles.findAll());
        var existingVariants = variants.findByProduct_Id(id);
        var fixedMaterial = !existingVariants.isEmpty() ? existingVariants.get(0).getMaterial() : null;
        var prod = catalog.product(id);
        var fixedSole = (prod.getSole() != null) ? prod.getSole() : (!existingVariants.isEmpty() ? existingVariants.get(0).getSole() : null);
        model.addAttribute("fixedMaterial", fixedMaterial);
        model.addAttribute("fixedSole", fixedSole);
        model.addAttribute("sizeId",sizeId);model.addAttribute("colorId",colorId);model.addAttribute("materialId",materialId);
        model.addAttribute("soleId",soleId);model.addAttribute("variantStatus",variantStatus);
        model.addAttribute("stock",stock);model.addAttribute("threshold",lowStockThreshold);
        return "admin/san-pham/chi-tiet";
    }
    @GetMapping("/{id}/variants")
    public String productVariants(@PathVariable Integer id, jakarta.servlet.http.HttpServletRequest request) {
        String qs = request.getQueryString();
        if (qs != null && !qs.isBlank()) {
            return "redirect:/admin/products-crud/view/" + id + "?" + qs;
        }
        var builder = org.springframework.web.util.UriComponentsBuilder.fromPath("/admin/products-crud/view/" + id);
        request.getParameterMap().forEach((k, values) -> {
            if (!"_csrf".equals(k) && values != null) {
                for (String v : values) {
                    builder.queryParam(k, v);
                }
            }
        });
        return "redirect:" + builder.build().encode().toUriString();
    }
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("product") AdminProductForm product, BindingResult result,
                       jakarta.servlet.http.HttpServletRequest request, Model model, RedirectAttributes flash) {
        boolean hasInitialSizes = (product.getInitialSizeIds()!=null && !product.getInitialSizeIds().isEmpty())
                || (product.getNewInitialSizeValue()!=null && !product.getNewInitialSizeValue().isBlank());
        if (product.getId()==null) {
            String[] rawMat = request.getParameterValues("initialMaterialId");
            String[] rawSole = request.getParameterValues("initialSoleId");
            if (rawMat != null && (rawMat.length > 1 || (rawMat.length == 1 && rawMat[0] != null && rawMat[0].contains(",")))) {
                if (!result.hasFieldErrors("initialMaterialId")) {
                    result.rejectValue("initialMaterialId", "multiple", "Chỉ được chọn đúng 1 chất liệu đang hoạt động trong danh sách.");
                }
            } else if (product.getNewInitialMaterialName() != null && !product.getNewInitialMaterialName().isBlank()) {
                if (!result.hasFieldErrors("initialMaterialId")) {
                    result.rejectValue("initialMaterialId", "creatable", "Không cho phép gõ tạo mới chất liệu trên biểu mẫu sản phẩm.");
                }
            } else if (!result.hasFieldErrors("initialMaterialId")) {
                if (product.getInitialMaterialId() == null) {
                    result.rejectValue("initialMaterialId", "required", "Chọn chất liệu khi chọn kích cỡ ban đầu. Vui lòng chọn đúng 1 chất liệu đang hoạt động.");
                } else if (materials.findById(product.getInitialMaterialId()).filter(m -> Boolean.TRUE.equals(m.getStatus())).isEmpty()) {
                    result.rejectValue("initialMaterialId", "invalid", "Chất liệu không tồn tại hoặc đang ẩn.");
                }
            }
            if (rawSole != null && (rawSole.length > 1 || (rawSole.length == 1 && rawSole[0] != null && rawSole[0].contains(",")))) {
                if (!result.hasFieldErrors("initialSoleId")) {
                    result.rejectValue("initialSoleId", "multiple", "Chỉ được chọn đúng 1 đế giày đang hoạt động trong danh sách.");
                }
            } else if ((product.getNewInitialSoleName() != null && !product.getNewInitialSoleName().isBlank())
                    || (product.getNewSoleName() != null && !product.getNewSoleName().isBlank())) {
                if (!result.hasFieldErrors("initialSoleId")) {
                    result.rejectValue("initialSoleId", "creatable", "Không cho phép gõ tạo mới đế giày trên biểu mẫu sản phẩm.");
                }
            } else if (!result.hasFieldErrors("initialSoleId")) {
                if (product.getInitialSoleId() == null) {
                    result.rejectValue("initialSoleId", "required", "Vui lòng chọn đúng 1 đế giày đang hoạt động.");
                } else if (soles.findById(product.getInitialSoleId()).filter(s -> Boolean.TRUE.equals(s.getStatus())).isEmpty()) {
                    result.rejectValue("initialSoleId", "invalid", "Đế giày không tồn tại hoặc đang ẩn.");
                }
            }
            if (hasInitialSizes) {
                if (product.getInitialPrice()==null) result.rejectValue("initialPrice","required","Nhập giá khi chọn kích cỡ ban đầu.");
                if (product.getInitialQuantity()==null) result.rejectValue("initialQuantity","required","Nhập tồn kho ban đầu từ 0 trở lên.");
            }
        }
        if (!result.hasErrors()) {
            try {
                Product saved=catalog.saveProduct(product);
                flash.addFlashAttribute("message",product.getId()==null?"Đã tạo sản phẩm thành công.":"Đã lưu sản phẩm.");
                if(product.getId()==null)flash.addFlashAttribute("createdProductId",saved.getId());
                return product.getId()==null?"redirect:/admin/products-crud":"redirect:/admin/products-crud/view/"+saved.getId();
            } catch(CatalogValidationException ex) { result.rejectValue(ex.getField(),"invalid",ex.getMessage()); }
              catch(DataIntegrityViolationException ex) { result.rejectValue("code","duplicate","Mã sản phẩm đã tồn tại hoặc dữ liệu liên quan vừa thay đổi."); }
        }
        if(product.getId()!=null) {var stored=catalog.product(product.getId());product.setImageUrl(stored.getImageUrl());product.setCode(stored.getCode());} else product.setCode(null);
        return form(model);
    }
    @PostMapping("/status/{id}")
    public Object status(@PathVariable Integer id,@RequestParam boolean selling,
                         jakarta.servlet.http.HttpServletRequest request,RedirectAttributes flash) {
        catalog.productStatus(id,selling);
        String msg = selling
                ? "Đã bán lại sản phẩm. Trạng thái từng biến thể được giữ nguyên."
                : "Đã ngừng bán sản phẩm. Tồn kho, biến thể và lịch sử được giữ nguyên.";
        String xhr = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        if ("XMLHttpRequest".equalsIgnoreCase(xhr) || (accept != null && accept.contains("application/json"))) {
            return ResponseEntity.ok(java.util.Map.of(
                    "id", id,
                    "selling", selling,
                    "active", selling,
                    "stateLabel", selling ? "Đang bán" : "Ngừng bán",
                    "message", msg
            ));
        }
        flash.addFlashAttribute("message",msg);
        return "redirect:/admin/products-crud/view/"+id;
    }
    @RequestMapping(value = "/delete/{id}", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE})
    public void delete(@PathVariable Integer id) {
        throw new ResponseStatusException(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Không được xóa sản phẩm. Hãy dùng công tắc bật/tắt trạng thái bán.");
    }
    private String form(Model model) {
        AdminProductForm form=(AdminProductForm)model.getAttribute("product");
        model.addAttribute("productImages", form.getId()==null ? java.util.List.of() : gallery.list(form.getId()));
        model.addAttribute("categories",categories.findAll());
        model.addAttribute("materials",materials.findByStatusTrueOrderByNameAscIdAsc());
        model.addAttribute("colors",colors.findByStatusTrueOrderByNameAscIdAsc());
        var choices=new java.util.ArrayList<>(soles.findByStatusTrueOrderByNameAscIdAsc());
        model.addAttribute("soles",choices);
        model.addAttribute("sizes",sizes.findAll());
        model.addAttribute("existingMaterials",form.getId()==null ? java.util.List.of() : variants.materialNames(form.getId()));
        model.addAttribute("uploadMaxBytes",maxBytes);
        return "admin/san-pham/bieu-mau";
    }
}
