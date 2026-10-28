package com.example.demodatn.service.sanpham;
import com.example.demodatn.dto.sanpham.AdminProductForm;
import com.example.demodatn.dto.sanpham.LookupForm;
import com.example.demodatn.dto.sanpham.VariantForm;
import com.example.demodatn.entity.sanpham.Category;
import com.example.demodatn.entity.sanpham.Material;
import com.example.demodatn.entity.sanpham.Product;
import com.example.demodatn.entity.sanpham.ProductVariant;
import com.example.demodatn.entity.sanpham.Size;
import com.example.demodatn.exception.CatalogValidationException;
import com.example.demodatn.repository.donhang.OrderDetailRepository;
import com.example.demodatn.repository.khuyenmai.PromotionDetailRepository;
import com.example.demodatn.repository.sanpham.CategoryRepository;
import com.example.demodatn.repository.sanpham.InventoryLogRepository;
import com.example.demodatn.repository.sanpham.MaterialRepository;
import com.example.demodatn.repository.sanpham.ProductRepository;
import com.example.demodatn.repository.sanpham.ProductVariantRepository;
import com.example.demodatn.repository.sanpham.SizeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service @RequiredArgsConstructor
public class AdminCatalogService {
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final CategoryRepository categories;
    private final SizeRepository sizes;
    private final MaterialRepository materials;
    private final com.example.demodatn.repository.sanpham.SoleRepository soles;
    private final OrderDetailRepository details;
    private final PromotionDetailRepository promotions;
    private final InventoryLogRepository inventoryLogs;
    private final InventoryService inventory;
    private final ProductImageStorage images;
    private final ProductGalleryService gallery;

    public Product product(Integer id) {
        return products.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy sản phẩm."));
    }
    public ProductVariant variant(Integer id) {
        return variants.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy biến thể."));
    }
    @Transactional
    public Product saveProduct(AdminProductForm form) {
        boolean initialize = form.getId() == null && form.getInitialSizeIds() != null && !form.getInitialSizeIds().isEmpty();
        Material initialMaterial = null;
        java.util.List<Size> initialSizes = new java.util.ArrayList<>();
        if (initialize) {
            if (form.getInitialMaterialId() == null)
                throw new CatalogValidationException("initialMaterialId","Chọn chất liệu khi chọn kích cỡ ban đầu.");
            initialMaterial = materials.findById(form.getInitialMaterialId()).filter(m -> Boolean.TRUE.equals(m.getStatus()))
                    .orElseThrow(() -> new CatalogValidationException("initialMaterialId","Chọn chất liệu đang hiển thị."));
            if (form.getInitialPrice() == null || form.getInitialPrice().signum() <= 0 || form.getInitialPrice().scale() > 2
                    || form.getInitialPrice().precision() - form.getInitialPrice().scale() > 16)
                throw new CatalogValidationException("initialPrice","Nhập giá lớn hơn 0, tối đa 16 chữ số và 2 số thập phân.");
            if (form.getInitialQuantity() == null || form.getInitialQuantity() < 0)
                throw new CatalogValidationException("initialQuantity","Nhập tồn kho ban đầu từ 0 trở lên.");
            for (Integer sizeId : new java.util.LinkedHashSet<>(form.getInitialSizeIds())) {
                if (sizeId == null) throw new CatalogValidationException("initialSizeIds","Kích cỡ không hợp lệ.");
                initialSizes.add(sizes.findById(sizeId).orElseThrow(() -> new CatalogValidationException("initialSizeIds","Kích cỡ không tồn tại.")));
            }
        }
        String code = form.getCode().trim();
        if (products.existsByCodeIgnoreCaseAndIdNot(code, form.getId() == null ? -1 : form.getId()))
            throw new CatalogValidationException("code","Mã sản phẩm đã tồn tại.");
        Category category = categories.findById(form.getCategoryId()).orElseThrow(() -> new CatalogValidationException("categoryId","Thương hiệu không tồn tại."));
        Product product = form.getId() == null ? new Product() : products.lockById(form.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        com.example.demodatn.entity.sanpham.Sole sole=null;
        if(form.getSoleId()!=null) {
            sole=soles.findById(form.getSoleId()).orElseThrow(()->new CatalogValidationException("soleId","Đế giày không tồn tại."));
            if(!Boolean.TRUE.equals(sole.getStatus()) && (product.getSole()==null || !form.getSoleId().equals(product.getSole().getId())))
                throw new CatalogValidationException("soleId","Chọn đế giày đang hiển thị.");
        }
        product.setSole(sole);
        product.setCode(code); product.setName(form.getName().trim()); product.setDescription(form.getDescription());
        product.setCategory(category); product.setStatus(form.getStatus());
        products.saveAndFlush(product);
        gallery.save(product,form);
        if (initialize) {
            for (Size size : initialSizes) {
                ProductVariant variant = new ProductVariant();
                variant.setProduct(product); variant.setSize(size); variant.setMaterial(initialMaterial);
                variant.setPrice(form.getInitialPrice()); variant.setStatus(true);
                variants.saveAndFlush(variant);
                inventory.initialize(variant,form.getInitialQuantity());
            }
        }
        return products.saveAndFlush(product);
    }
    @Transactional
    public String deleteProduct(Integer id) {
        Product product = product(id);
        if (details.existsByProductVariant_Product_Id(id)) {
            product.setStatus(false);
            return "Sản phẩm đã có trong đơn hàng: đã chuyển sang ngừng bán, không xóa dữ liệu đơn cũ.";
        }
        if (variants.existsByProduct_Id(id) || inventoryLogs.existsByVariant_Product_Id(id) || !promotions.findByProductId(id).isEmpty()) {
            product.setStatus(false);
            return "Sản phẩm có biến thể/lịch sử liên quan: đã chuyển sang ngừng bán để bảo toàn tồn kho và lịch sử.";
        }
        gallery.removeForProduct(id);
        products.delete(product);
        products.flush();
        return "Đã xóa sản phẩm chưa có dữ liệu liên quan.";
    }
    @Transactional
    public ProductVariant saveVariant(VariantForm form) {
        Product product = product(form.getProductId());
        Size size = sizes.findById(form.getSizeId()).orElseThrow(() -> new CatalogValidationException("sizeId","Kích cỡ không tồn tại."));
        ProductVariant current = form.getId() == null ? null : inventory.lock(form.getId());
        Material material = materials.findById(form.getMaterialId()).filter(m -> Boolean.TRUE.equals(m.getStatus())
                || (current != null && current.getMaterial().getId().equals(m.getId())))
                .orElseThrow(() -> new CatalogValidationException("materialId","Chọn chất liệu đang hiển thị."));
        if (variants.existsByProduct_IdAndSize_IdAndMaterial_IdAndIdNot(product.getId(),size.getId(),material.getId(),form.getId()==null ? -1 : form.getId()))
            throw new CatalogValidationException("materialId","Bộ ba sản phẩm, kích cỡ và chất liệu đã tồn tại.");
        ProductVariant variant = form.getId() == null ? new ProductVariant() : current;
        if (form.getId() != null && !variant.getProduct().getId().equals(product.getId()))
            throw new CatalogValidationException("productId","Không thể chuyển biến thể sang sản phẩm khác.");
        int before = variant.getQuantity();
        if (form.getId() != null && !java.util.Objects.equals(form.getExpectedQuantity(),before))
            throw new CatalogValidationException("quantity","Tồn kho đã thay đổi. Tải lại trang trước khi lưu.");
        if (form.getId() != null && before != form.getQuantity() && (form.getReason()==null || form.getReason().isBlank()))
            throw new CatalogValidationException("reason","Nhập lý do khi thay đổi số lượng.");
        variant.setProduct(product); variant.setSize(size); variant.setMaterial(material); variant.setPrice(form.getPrice()); variant.setStatus(form.getStatus());
        variants.saveAndFlush(variant);
        inventory.change(variant, form.getQuantity(), form.getId() == null ? "Khởi tạo biến thể" : (form.getReason()==null || form.getReason().isBlank() ? "Lưu biến thể không đổi tồn" : form.getReason()));
        return variant;
    }
    @Transactional
    public void saveLookup(String kind, LookupForm form) {
        String value = form.getValue() != null ? form.getValue().trim() : "";
        if (value.isBlank()) {
            if ("sizes".equals(kind)) throw new CatalogValidationException("value", "Vui lòng nhập kích cỡ kích cỡ.");
            if ("materials".equals(kind)) throw new CatalogValidationException("value", "Vui lòng nhập tên chất liệu.");
            throw new CatalogValidationException("value", "Vui lòng nhập tên thương hiệu.");
        }
        if ("sizes".equals(kind)) {
            if (value.length() > 10) throw new CatalogValidationException("value", "Kích cỡ kích cỡ tối đa 10 ký tự.");
            if (sizes.existsBySizeValueIgnoreCaseAndIdNot(value, form.getId() == null ? -1 : form.getId()))
                throw new CatalogValidationException("value", "Kích cỡ kích cỡ đã tồn tại.");
            Size size = form.getId() == null ? new Size() : sizes.findById(form.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            size.setSizeValue(value); sizes.saveAndFlush(size);
        } else if ("materials".equals(kind)) {
            if (value.length() > 100) throw new CatalogValidationException("value", "Tên chất liệu tối đa 100 ký tự.");
            if (materials.existsByNameIgnoreCaseAndIdNot(value, form.getId() == null ? -1 : form.getId()))
                throw new CatalogValidationException("value", "Tên chất liệu đã tồn tại.");
            Material material = form.getId() == null ? new Material() : materials.findById(form.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            material.setName(value); material.setStatus(Boolean.TRUE.equals(form.getStatus())); materials.saveAndFlush(material);
        } else {
            if (value.length() > 100) throw new CatalogValidationException("value", "Tên thương hiệu tối đa 100 ký tự.");
            if (categories.existsByNameIgnoreCaseAndIdNot(value, form.getId() == null ? -1 : form.getId()))
                throw new CatalogValidationException("value", "Tên thương hiệu đã tồn tại.");
            Category category = form.getId() == null ? new Category() : categories.findById(form.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            category.setName(value); category.setStatus(Boolean.TRUE.equals(form.getStatus())); categories.saveAndFlush(category);
        }
    }
    @Transactional
    public String deleteLookup(String kind, Integer id) {
        if ("sizes".equals(kind)) {
            if (variants.existsBySize_Id(id)) return "Không thể xóa kích cỡ đang được biến thể tham chiếu.";
            sizes.deleteById(id); sizes.flush(); return "Đã xóa kích cỡ.";
        }
        if ("materials".equals(kind)) {
            if (variants.existsByMaterial_Id(id)) return "Không thể xóa chất liệu đang được biến thể tham chiếu.";
            materials.deleteById(id); materials.flush(); return "Đã xóa chất liệu.";
        }
        if (products.existsByCategory_Id(id)) return "Không thể xóa thương hiệu đang được sản phẩm tham chiếu.";
        categories.deleteById(id); categories.flush(); return "Đã xóa thương hiệu.";
    }
}

