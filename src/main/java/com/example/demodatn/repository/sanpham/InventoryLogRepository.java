package com.example.demodatn.repository.sanpham;
import com.example.demodatn.entity.sanpham.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
public interface InventoryLogRepository extends JpaRepository<InventoryLog,Long> {
    void deleteByVariant_Id(Integer variantId);
    boolean existsByVariant_Product_Id(Integer productId);
}

