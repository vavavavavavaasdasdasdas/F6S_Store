package com.example.demodatn.repository.donhang;
import com.example.demodatn.entity.donhang.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory,Integer> { java.util.List<OrderStatusHistory> findByOrder_IdOrderByChangedAtAscIdAsc(Integer orderId); }
