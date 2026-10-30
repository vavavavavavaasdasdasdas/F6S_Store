package com.example.demodatn.repository.sanpham;
import com.example.demodatn.entity.sanpham.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Integer>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Product> {
    @org.springframework.data.jpa.repository.Query("select p from Product p where p.status=true and exists (select v.id from ProductVariant v where v.product=p and v.status=true and (:materialId is null or v.material.id=:materialId) and (:soleId is null or v.sole.id=:soleId) and (:colorId is null or v.color.id=:colorId))")
    java.util.List<Product> findSellingMatchingAttributes(@org.springframework.data.repository.query.Param("materialId") Integer materialId,@org.springframework.data.repository.query.Param("soleId") Integer soleId,@org.springframework.data.repository.query.Param("colorId") Integer colorId);

    long countBySole_Id(Integer soleId);
    @org.springframework.data.jpa.repository.Query("select p.sole.id as soleId,count(p) as usageCount from Product p where p.sole is not null group by p.sole.id")
    java.util.List<java.util.Map<String,Object>> soleUsage();
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Product p where p.id=:id")
    java.util.Optional<Product> lockById(@org.springframework.data.repository.query.Param("id") Integer id);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Integer id);
    boolean existsByCategory_Id(Integer categoryId);
    java.util.List<Product> findByStatusTrue();
}
