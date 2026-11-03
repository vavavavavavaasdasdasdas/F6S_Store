package com.example.demodatn.service.donhang;
import com.example.demodatn.service.sanpham.ProductSalePolicy;
import com.example.demodatn.entity.sanpham.Product;
import com.example.demodatn.entity.sanpham.ProductVariant;
import com.example.demodatn.repository.sanpham.ProductRepository;
import com.example.demodatn.util.Pagination;
import jakarta.persistence.criteria.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import lombok.RequiredArgsConstructor;
import java.util.*;

@Service @RequiredArgsConstructor
public class PosCatalogQuery {
    private final ProductRepository products;
    private final Pagination pagination;
    private final JdbcTemplate jdbc;
    private final com.example.demodatn.repository.sanpham.ProductVariantRepository variantsRepo;

    public Page<ProductVariant> variants(String q, Integer categoryId, Integer colorId, Integer materialId, Integer soleId, Integer sizeId, String priceRange, int page) {
        org.springframework.data.jpa.domain.Specification<ProductVariant> filter = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.isTrue(root.get("status")));
            preds.add(cb.isTrue(root.get("product").get("status")));
            if (q != null && !q.trim().isEmpty()) {
                String pattern = like(q);
                preds.add(cb.or(
                    cb.like(cb.lower(root.get("product").get("name")), pattern, '!'),
                    cb.like(cb.lower(root.get("product").get("code")), pattern, '!'),
                    cb.like(cb.lower(root.get("code")), pattern, '!')
                ));
            }
            if (categoryId != null) preds.add(cb.equal(root.get("product").get("category").get("id"), categoryId));
            if (colorId != null) preds.add(cb.equal(root.get("color").get("id"), colorId));
            if (materialId != null) preds.add(cb.equal(root.get("material").get("id"), materialId));
            if (soleId != null) preds.add(cb.equal(root.get("sole").get("id"), soleId));
            if (sizeId != null) preds.add(cb.equal(root.get("size").get("id"), sizeId));
            if (priceRange != null && !priceRange.isBlank()) {
                switch (priceRange) {
                    case "UNDER_500" -> preds.add(cb.lessThan(root.get("price"), new java.math.BigDecimal("500000")));
                    case "500_1000" -> {
                        preds.add(cb.greaterThanOrEqualTo(root.get("price"), new java.math.BigDecimal("500000")));
                        preds.add(cb.lessThanOrEqualTo(root.get("price"), new java.math.BigDecimal("1000000")));
                    }
                    case "1000_2000" -> {
                        preds.add(cb.greaterThanOrEqualTo(root.get("price"), new java.math.BigDecimal("1000000")));
                        preds.add(cb.lessThanOrEqualTo(root.get("price"), new java.math.BigDecimal("2000000")));
                    }
                    case "OVER_2000" -> preds.add(cb.greaterThan(root.get("price"), new java.math.BigDecimal("2000000")));
                }
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
        return variantsRepo.findAll(filter, PageRequest.of(Math.max(0, page), 12, Sort.by("product.name", "id")));
    }
    public Page<Product> products(String q,Integer category,Object page) {
        return products(q, category, null, null, page);
    }
    public Page<Product> products(String q,Integer category,Integer materialId,Integer sizeId,Object page) {
        org.springframework.data.jpa.domain.Specification<Product> filter=(root,query,cb)->{
            var variants=query.subquery(Integer.class);var v=variants.from(ProductVariant.class);
            List<Predicate> variantPreds = new ArrayList<>();
            variantPreds.add(cb.equal(v.get("product"),root));
            variantPreds.add(cb.isTrue(v.get("status")));
            if (materialId != null) variantPreds.add(cb.equal(v.get("material").get("id"), materialId));
            if (sizeId != null) variantPreds.add(cb.equal(v.get("size").get("id"), sizeId));
            variants.select(v.get("id")).where(variantPreds.toArray(new Predicate[0]));
            var text=cb.or(cb.like(cb.lower(root.get("name")),like(q),'!'),cb.like(cb.lower(root.get("code")),like(q),'!'));
            var active=cb.and(ProductSalePolicy.selling(root,cb),cb.exists(variants),text);
            return category==null?active:cb.and(active,cb.equal(root.get("category").get("id"),category));
        };
        return pagination.load(page,Sort.by("name","id"),request->products.findAll(filter,request));
    }
    public Page<Map<String,Object>> customers(String q,Object page) {
        if(q==null || q.trim().length()<2)return Page.empty(PageRequest.of(0,12));
        String from=" FROM customers WHERE status=1 AND (LOWER(full_name) LIKE ? ESCAPE '!' OR phone LIKE ? ESCAPE '!' OR LOWER(customer_code) LIKE ? ESCAPE '!')";
        return pagination.sql(jdbc,"SELECT id,customer_code,full_name,phone"+from+" ORDER BY full_name,id","SELECT COUNT_BIG(*)"+from,List.of(like(q),like(q),like(q)),page);
    }
    private String like(String q){return "%"+q.trim().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_").replace("[","![")+"%";}
}
