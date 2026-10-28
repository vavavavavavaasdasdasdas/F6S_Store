package com.example.demodatn.repository.khuyenmai;
import com.example.demodatn.entity.khuyenmai.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Integer>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Voucher> {
    Optional<Voucher> findByCode(String code);
    Optional<Voucher> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Integer id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select v from Voucher v where lower(v.code)=lower(:code)")
    Optional<Voucher> findByCodeForUpdate(@org.springframework.data.repository.query.Param("code") String code);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select v from Voucher v where v.id=:id")
    Optional<Voucher> findForUpdate(@org.springframework.data.repository.query.Param("id") Integer id);
    boolean existsByCustomer_Id(Integer id);
}
