package com.example.demodatn.controller.admin.sanpham;

import com.example.demodatn.dto.sanpham.*;
import com.example.demodatn.entity.sanpham.ProductVariant;
import com.example.demodatn.exception.CatalogValidationException;
import com.example.demodatn.repository.sanpham.*;
import com.example.demodatn.service.sanpham.AdminCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Controller @RequiredArgsConstructor
@RequestMapping("/admin/products-crud/{productId}/variants")
public class AdminProductVariantController {
    private final AdminCatalogService catalog;
    private final SizeRepository sizes;
    private final ColorRepository colors;
    private final MaterialRepository materials;
    private final SoleRepository soles;

    private ProductVariant owned(Integer productId,Integer id) {
        var variant=catalog.variant(id);
        if(!variant.getProduct().getId().equals(productId))throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return variant;
    }
    @PostMapping("/inline-save") @ResponseBody
    public ResponseEntity<?> save(@PathVariable Integer productId,@Valid @ModelAttribute VariantForm form,BindingResult errors) {
        if(errors.hasErrors())return invalid(errors);
        if(form.getId()==null)return bad("id","Chọn biến thể cần sửa.",400);
        owned(productId,form.getId());form.setProductId(productId);
        if(form.getReason()==null || form.getReason().isBlank())form.setReason("Chỉnh trên trang sản phẩm");
        try {var saved=catalog.saveVariant(form);return ResponseEntity.ok(Map.of("id",saved.getId(),"message","Đã lưu biến thể."));}
        catch(CatalogValidationException ex){return bad(ex.getField(),ex.getMessage(),ex.getMessage().startsWith("Tồn kho đã thay đổi")?409:400);}
        catch(DataIntegrityViolationException ex){return bad("materialId","Bộ năm đã tồn tại hoặc dữ liệu vừa thay đổi.",409);}
    }
    @PostMapping("/batch") @ResponseBody
    public ResponseEntity<?> batch(@PathVariable Integer productId,@Valid @ModelAttribute VariantBatchForm form,BindingResult errors) {
        if(errors.hasErrors())return invalid(errors);
        form.setProductId(productId);
        try {var saved=catalog.createVariants(form);return ResponseEntity.ok(Map.of("count",saved.size(),"message","Đã thêm "+saved.size()+" biến thể."));}
        catch(CatalogValidationException ex){return bad(ex.getField(),ex.getMessage(),400);}
        catch(DataIntegrityViolationException ex){return bad("sizeIds","Có tổ hợp đã tồn tại hoặc dữ liệu vừa thay đổi. Không tạo biến thể nào.",409);}
    }
    @PostMapping("/{id}/status") @ResponseBody
    public ResponseEntity<?> status(@PathVariable Integer productId,@PathVariable Integer id,@RequestParam boolean selling) {
        owned(productId,id);catalog.variantStatus(id,selling);
        return ResponseEntity.ok(Map.of("id",id,"message",selling?"Đã bán lại biến thể.":"Đã ngừng bán biến thể. Tồn kho được giữ nguyên."));
    }
    @PostMapping("/{id}/delete") @ResponseBody
    public ResponseEntity<?> delete(@PathVariable Integer productId,@PathVariable Integer id) {
        try {catalog.deleteVariant(productId,id);return ResponseEntity.ok(Map.of("message","Đã xóa biến thể chưa có đơn hoặc hóa đơn chờ."));}
        catch(CatalogValidationException ex){return bad(ex.getField(),ex.getMessage(),409);}
        catch(DataIntegrityViolationException ex){return bad("id","Biến thể vừa có dữ liệu liên quan. Hãy dùng Ngừng bán.",409);}
    }
    @GetMapping("/row/{id}")
    public String row(@PathVariable Integer productId,@PathVariable Integer id,@RequestParam(defaultValue="1") int rowNumber,Model model) {
        var v=owned(productId,id);model.addAttribute("v",row(v));model.addAttribute("stt",Math.max(1,rowNumber));
        model.addAttribute("sizes",sizes.findAll());model.addAttribute("colors",colors.findAll());
        model.addAttribute("materials",materials.findAll());model.addAttribute("soles",soles.findAll());
        return "admin/san-pham/dong-bien-the :: row";
    }
    private Map<String,Object> row(ProductVariant v) {
        return Map.ofEntries(Map.entry("id",v.getId()),Map.entry("product_id",v.getProduct().getId()),
            Map.entry("size_id",v.getSize().getId()),Map.entry("size_value",v.getSize().getSizeValue()),
            Map.entry("mau_sac_id",v.getColor().getId()),Map.entry("color_name",v.getColor().getName()),
            Map.entry("chat_lieu_id",v.getMaterial().getId()),Map.entry("material_name",v.getMaterial().getName()),
            Map.entry("de_giay_id",v.getSole().getId()),Map.entry("sole_name",v.getSole().getName()),
            Map.entry("can_delete",catalog.canDeleteVariant(v.getId())),Map.entry("price",v.getPrice()),Map.entry("quantity",v.getQuantity()),Map.entry("status",v.getStatus()));
    }
    private ResponseEntity<?> invalid(BindingResult errors) {
        var fields=new LinkedHashMap<String,String>();errors.getFieldErrors().forEach(e->fields.putIfAbsent(e.getField(),e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of("errors",fields,"message","Kiểm tra các ô được đánh dấu."));
    }
    private ResponseEntity<?> bad(String field,String message,int status) {
        return ResponseEntity.status(status).body(Map.of("errors",Map.of(field,message),"message",message));
    }
}
