package com.example.demodatn.repository.sanpham;
import com.example.demodatn.entity.sanpham.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths={"product","product.category","size","material","color","sole"})
    java.util.List<ProductVariant> findByProduct_IdInAndStatusTrueAndProduct_StatusTrue(java.util.Collection<Integer> ids);
    boolean existsByProduct_IdAndSize_IdAndMaterial_IdAndIdNot(Integer productId, Integer sizeId, Integer materialId, Integer id);
    boolean existsByProduct_IdAndSize_IdAndMaterial_IdAndColor_IdAndSole_IdAndIdNot(Integer productId,Integer sizeId,Integer materialId,Integer colorId,Integer soleId,Integer id);
    boolean existsByColor_Id(Integer colorId);
    boolean existsBySole_Id(Integer soleId);
    @org.springframework.data.jpa.repository.Query("select v.color.id as colorId, count(v) as usageCount from ProductVariant v group by v.color.id")
    java.util.List<java.util.Map<String,Object>> colorUsage();
    @org.springframework.data.jpa.repository.Query("select v.sole.id as soleId, count(v) as usageCount from ProductVariant v group by v.sole.id")
    java.util.List<java.util.Map<String,Object>> soleUsage();
    boolean existsByMaterial_Id(Integer materialId);
    @org.springframework.data.jpa.repository.Query("select v.material.id as materialId, count(v) as usageCount from ProductVariant v group by v.material.id")
    java.util.List<java.util.Map<String,Object>> materialUsage();
    @org.springframework.data.jpa.repository.Query("select distinct v.material.name from ProductVariant v where v.product.id=:productId order by v.material.name")
    java.util.List<String> materialNames(@org.springframework.data.repository.query.Param("productId") Integer productId);
    boolean existsBySize_Id(Integer sizeId);
    boolean existsByProduct_Id(Integer productId);
    java.util.List<ProductVariant> findByProduct_IdAndStatusTrue(Integer productId);
    java.util.List<ProductVariant> findByStatusTrueAndProduct_StatusTrue();
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select v from ProductVariant v where v.id=:id")
    java.util.Optional<ProductVariant> findForUpdate(@org.springframework.data.repository.query.Param("id") Integer id);
    java.util.List<ProductVariant> findByProduct_Id(Integer productId);
}
