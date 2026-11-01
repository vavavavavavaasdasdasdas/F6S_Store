package com.example.demodatn.repository.sanpham;
import com.example.demodatn.entity.sanpham.ProductReview;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import java.util.*;
public interface ProductReviewRepository extends JpaRepository<ProductReview,Integer>,JpaSpecificationExecutor<ProductReview> {
 Optional<ProductReview> findByCustomer_IdAndProduct_Id(Integer customerId,Integer productId);
 @EntityGraph(attributePaths={"customer","product"}) Page<ProductReview> findByProduct_IdAndVisibleTrue(Integer productId,Pageable page);
 @Override @EntityGraph(attributePaths={"customer","product"}) Page<ProductReview> findAll(Specification<ProductReview> spec,Pageable page);
 @Query("select r.product.id,avg(r.stars),count(r.id) from ProductReview r where r.visible=true group by r.product.id") List<Object[]> summaries();
 @Query("select r.stars,count(r.id) from ProductReview r where r.product.id=:id and r.visible=true group by r.stars") List<Object[]> distribution(Integer id);
}
