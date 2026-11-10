package com.example.demodatn.repository.khuyenmai;
import com.example.demodatn.entity.khuyenmai.PromotionDetail;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;

public interface PromotionDetailRepository extends JpaRepository<PromotionDetail, Integer> {
    @EntityGraph(attributePaths={"promotion","product","productVariant"})
    List<PromotionDetail> findByProduct_IdIn(Collection<Integer> productIds);

    List<PromotionDetail> findByPromotion_IdAndProduct_IdIn(Integer id, Collection<Integer> products);

    List<PromotionDetail> findByProductId(Integer productId);

    List<PromotionDetail> findByPromotion_Id(Integer promotionId);

    void deleteByPromotion_Id(Integer promotionId);

    @EntityGraph(attributePaths={"promotion","productVariant","productVariant.product"})
    List<PromotionDetail> findByProductVariant_IdIn(Collection<Integer> variantIds);

    @EntityGraph(attributePaths={"promotion","productVariant","productVariant.product"})
    List<PromotionDetail> findByProductVariant_Id(Integer variantId);

    List<PromotionDetail> findByPromotion_IdAndProductVariant_IdIn(Integer promotionId, Collection<Integer> variantIds);

    void deleteByPromotion_IdAndProductVariant_IdIn(Integer promotionId, Collection<Integer> variantIds);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM PromotionDetail d WHERE d.promotion.id = :promotionId AND d.productVariant IS NULL AND d.product.id IN :productIds")
    void deleteLegacyProductRows(@org.springframework.data.repository.query.Param("promotionId") Integer promotionId,
                                 @org.springframework.data.repository.query.Param("productIds") Collection<Integer> productIds);

    boolean existsByProductVariant_Product_Id(Integer productId);

    boolean existsByProductVariant_Id(Integer variantId);
}
