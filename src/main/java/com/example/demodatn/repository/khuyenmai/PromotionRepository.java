package com.example.demodatn.repository.khuyenmai;
import com.example.demodatn.entity.khuyenmai.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionRepository extends JpaRepository<Promotion, Integer>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Promotion> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Promotion p where p.id=:id")
    java.util.Optional<Promotion> findForUpdate(@org.springframework.data.repository.query.Param("id") Integer id);
}
