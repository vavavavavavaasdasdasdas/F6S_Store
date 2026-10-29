package com.example.demodatn.service.sanpham;

import com.example.demodatn.entity.sanpham.Product;
import com.example.demodatn.entity.sanpham.ProductVariant;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

/** Current sale eligibility only. Historical orders, invoices and reports do not use this policy. */
public final class ProductSalePolicy {
    public static final String STOPPED = "Sản phẩm đã ngừng bán";
    private ProductSalePolicy() {}
    public static boolean isSelling(Product product) {
        return product != null && Boolean.TRUE.equals(product.getStatus());
    }
    public static boolean isSelling(ProductVariant variant) {
        return variant != null && isSelling(variant.getProduct()) && Boolean.TRUE.equals(variant.getStatus());
    }
    public static Predicate selling(Path<?> product, CriteriaBuilder cb) {
        return cb.isTrue(product.get("status"));
    }
}
