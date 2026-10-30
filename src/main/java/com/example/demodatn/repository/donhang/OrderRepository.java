package com.example.demodatn.repository.donhang;
import com.example.demodatn.entity.donhang.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Integer>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Order> {
    boolean existsByPaymentReference(String reference);
    org.springframework.data.domain.Page<Order> findByCustomer_IdAndOrderTypeOrderByCreatedAtDescIdDesc(Integer id,String type,org.springframework.data.domain.Pageable pageable);
    boolean existsByCustomer_Id(Integer id);
    boolean existsByEmployee_Id(Integer id);
    boolean existsByVoucher_Id(Integer id);
    org.springframework.data.domain.Page<Order> findByCustomer_IdOrderByCreatedAtDescIdDesc(Integer id,org.springframework.data.domain.Pageable pageable);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select o from Order o where o.id=:id")
    java.util.Optional<Order> findForUpdate(@org.springframework.data.repository.query.Param("id") Integer id);
    java.util.List<Order> findByStatusOrderByCreatedAtDesc(String status);
}
