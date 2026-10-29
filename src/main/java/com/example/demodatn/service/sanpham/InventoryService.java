package com.example.demodatn.service.sanpham;
import com.example.demodatn.service.sanpham.ProductSalePolicy;
import com.example.demodatn.dto.sanpham.InventoryForm;
import com.example.demodatn.entity.sanpham.InventoryLog;
import com.example.demodatn.entity.sanpham.ProductVariant;
import com.example.demodatn.exception.CatalogValidationException;
import com.example.demodatn.repository.sanpham.InventoryLogRepository;
import com.example.demodatn.repository.sanpham.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service @RequiredArgsConstructor
public class InventoryService {
    private final ProductVariantRepository variants;
    private final InventoryLogRepository logs;
    private final com.example.demodatn.repository.donhang.OrderRepository orders;
    private final jakarta.persistence.EntityManager entityManager;
    @org.springframework.beans.factory.annotation.Value("${shop.time-zone:Asia/Ho_Chi_Minh}") private String timeZone;

    public ProductVariant lock(Integer id) {
        ProductVariant variant = variants.findForUpdate(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy biến thể."));
        // A request may have read this entity before the sale transaction acquired its lock.
        entityManager.refresh(variant);
        entityManager.refresh(variant.getProduct(),jakarta.persistence.LockModeType.PESSIMISTIC_READ);
        return variant;
    }
    @Transactional
    public void adjust(InventoryForm form) {
        ProductVariant variant = lock(form.getVariantId());
        if (!"ADD".equals(form.getMode()) && !"SET".equals(form.getMode())) throw new CatalogValidationException("mode","Cách điều chỉnh không hợp lệ.");
        if (form.getQuantity() == null || form.getQuantity() < 0 || ("ADD".equals(form.getMode()) && form.getQuantity() == 0))
            throw new CatalogValidationException("quantity","Số lượng nhập thêm phải > 0; tồn kho mới phải ≥ 0.");
        if (!variant.getQuantity().equals(form.getExpectedQuantity()))
            throw new CatalogValidationException("quantity","Tồn kho đã thay đổi. Tải lại trang rồi điều chỉnh.");
        long target = "ADD".equals(form.getMode()) ? (long) variant.getQuantity() + form.getQuantity() : form.getQuantity();
        if (target == variant.getQuantity()) throw new CatalogValidationException("quantity","Tồn kho không thay đổi. Nhập số lượng khác để ghi nhận điều chỉnh.");
        change(variant, target, form.getReason());
    }
    @Transactional
    public void truKhoChoDon(com.example.demodatn.entity.donhang.Order order) {
        var lockedOrder = lockOrder(order);
        if (Boolean.TRUE.equals(lockedOrder.getDaTruKho())) return;
        for (var line : quantities(lockedOrder).entrySet()) {
            ProductVariant variant = lock(line.getKey());
            if (!ProductSalePolicy.isSelling(variant))
                throw new IllegalArgumentException("Sản phẩm đã ngừng bán: " + variant.getProduct().getName());
            int before = variant.getQuantity();
            int changed = entityManager.createNativeQuery("UPDATE product_variants SET quantity=quantity-:q WHERE id=:id AND quantity>=:q")
                    .setParameter("q",line.getValue()).setParameter("id",line.getKey()).executeUpdate();
            if (changed != 1)
                throw new IllegalArgumentException("Không đủ tồn kho: " + variant.getProduct().getName() + " / Size " + variant.getSize().getSizeValue()
                        + " / " + variant.getMaterial().getName() + ". Cần " + line.getValue() + ", còn " + before + ". Đơn chưa được xác nhận.");
            entityManager.refresh(variant);
        entityManager.refresh(variant.getProduct(),jakarta.persistence.LockModeType.PESSIMISTIC_READ);
            record(variant,before,variant.getQuantity(),"Trừ kho đơn " + lockedOrder.getOrderCode() + " · " + lockedOrder.getOrderType() + " / " + lockedOrder.getPaymentMethod());
        }
        lockedOrder.setDaTruKho(true);
    }
    @Transactional
    public void hoanKhoChoDon(com.example.demodatn.entity.donhang.Order order) {
        var lockedOrder = lockOrder(order);
        if (!Boolean.TRUE.equals(lockedOrder.getDaTruKho())) return;
        for (var line : quantities(lockedOrder).entrySet()) {
            ProductVariant variant = lock(line.getKey());
            int before = variant.getQuantity();
            int changed = entityManager.createNativeQuery("UPDATE product_variants SET quantity=quantity+:q WHERE id=:id AND quantity<=:maximum")
                    .setParameter("q",line.getValue()).setParameter("id",line.getKey())
                    .setParameter("maximum",Integer.MAX_VALUE-line.getValue()).executeUpdate();
            if (changed != 1) throw new IllegalArgumentException("Tồn kho sau hoàn vượt giới hạn hợp lệ.");
            entityManager.refresh(variant);
        entityManager.refresh(variant.getProduct(),jakarta.persistence.LockModeType.PESSIMISTIC_READ);
            record(variant,before,variant.getQuantity(),"Hoàn kho đơn " + lockedOrder.getOrderCode() + " · hủy hoặc giao hàng thất bại");
        }
        lockedOrder.setDaTruKho(false);
    }
    private com.example.demodatn.entity.donhang.Order lockOrder(com.example.demodatn.entity.donhang.Order order) {
        if(order==null || order.getId()==null)throw new IllegalArgumentException("Đơn hàng chưa được lưu.");
        var locked=orders.findForUpdate(order.getId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        // Refresh only the flag: refreshing the entire graph also reloads immutable sale snapshots.
        Object flag=entityManager.createNativeQuery("SELECT da_tru_kho FROM orders WHERE id=:id").setParameter("id",order.getId()).getSingleResult();
        locked.setDaTruKho(Boolean.TRUE.equals(flag));return locked;
    }
    private java.util.Map<Integer,Integer> quantities(com.example.demodatn.entity.donhang.Order order) {
        java.util.Map<Integer,Integer> result=new java.util.TreeMap<>();
        for(var detail:order.getOrderDetails()) {
            if(detail.getQuantity()==null || detail.getQuantity()<=0)throw new IllegalArgumentException("Số lượng đơn hàng không hợp lệ.");
            result.merge(detail.getProductVariant().getId(),detail.getQuantity(),Math::addExact);
        }
        if(result.isEmpty())throw new IllegalArgumentException("Đơn hàng không có sản phẩm.");
        return result;
    }
    // Called only with a managed locked variant, or a newly inserted variant, in a transaction.
    public void change(ProductVariant variant, long target, String reason) {
        if (target < 0 || target > Integer.MAX_VALUE) throw new CatalogValidationException("quantity","Tồn kho vượt giới hạn hợp lệ.");
        if (reason == null || reason.isBlank() || reason.trim().length() > 500) throw new CatalogValidationException("reason","Lý do phải có từ 1 đến 500 ký tự.");
        int before = variant.getQuantity();
        if (before == target) return;
        variant.setQuantity((int) target);
        variants.save(variant);
        record(variant,before,(int)target,reason);
    }
    // The newly inserted variant belongs to saveProduct's transaction, including zero stock.
    public void initialize(ProductVariant variant, int quantity) {
        if (quantity < 0) throw new CatalogValidationException("initialQuantity","Tồn kho ban đầu không được âm.");
        variant.setQuantity(quantity);
        variants.save(variant);
        record(variant,0,quantity,"Khởi tạo");
    }
    private void record(ProductVariant variant,int before,int target,String reason) {
        InventoryLog log = new InventoryLog();
        log.setVariant(variant);
        log.setQuantityBefore(before);
        log.setQuantityAfter((int) target);
        log.setQuantityChange((int) (target - before));
        log.setReason(reason.trim());
        var auth = SecurityContextHolder.getContext().getAuthentication();
        log.setActor(auth != null ? auth.getName() : "system");
        log.setCreatedAt(LocalDateTime.now(ZoneId.of(timeZone)));
        logs.save(log);
    }
}

