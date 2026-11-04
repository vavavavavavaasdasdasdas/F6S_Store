package com.example.demodatn.repository.donhang;

import com.example.demodatn.entity.donhang.OrderMailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderMailLogRepository extends JpaRepository<OrderMailLog, Long> {
    Optional<OrderMailLog> findByOrder_IdAndStatus(Integer orderId, String status);
    List<OrderMailLog> findByOrder_Id(Integer orderId);
    boolean existsByOrder_IdAndStatus(Integer orderId, String status);
}
