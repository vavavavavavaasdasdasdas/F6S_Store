package com.example.demodatn.repository.donhang;
import com.example.demodatn.entity.donhang.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {
    boolean existsByProductVariant_Id(Integer variantId);
    boolean existsByProductVariant_Product_Id(Integer productId);

    @org.springframework.data.jpa.repository.Query(value = "SELECT d.product_variant_id AS variantId,SUM(CAST(d.quantity AS bigint)) AS quantity FROM order_details d JOIN orders o ON o.id=d.order_id WHERE " + RevenueCondition.SQL + " GROUP BY d.product_variant_id", nativeQuery = true)
    java.util.List<java.util.Map<String,Object>> eligibleSoldQuantities();
}
