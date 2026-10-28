package com.example.demodatn.repository.sanpham;
import com.example.demodatn.entity.sanpham.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Integer>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Product> {
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
