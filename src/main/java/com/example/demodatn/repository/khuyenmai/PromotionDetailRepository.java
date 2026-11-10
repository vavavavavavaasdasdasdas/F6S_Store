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

    boolean existsByProductVariant_Product_Id(Integer productId);

    boolean existsByProductVariant_Id(Integer variantId);
}
