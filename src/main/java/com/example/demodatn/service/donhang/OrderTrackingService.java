package com.example.demodatn.service.donhang;
import com.example.demodatn.entity.donhang.*;
import com.example.demodatn.repository.donhang.OrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
@Service @RequiredArgsConstructor
public class OrderTrackingService {
 private final OrderStatusHistoryRepository history;
 public record Step(String status,String label,LocalDateTime at,String actor,boolean current) {}
 private static final List<String> STEPS=List.of("PENDING","CONFIRMED","PREPARING","SHIPPING","DELIVERED");
 public static String label(String s){return switch(s){case "PENDING"->"Đã đặt";case "PREPARING"->"Đang chuẩn bị hàng";case "DELIVERED"->"Đã giao";default->OrderStatusService.LABELS.getOrDefault(s,s);};}
 @Transactional(readOnly=true) public List<OrderStatusHistory> history(Order order){return history.findByOrder_IdOrderByChangedAtAscIdAsc(order.getId());}
 @Transactional(readOnly=true) public List<Step> steps(Order order){var rows=history(order);return STEPS.stream().map(s->{var entry=rows.stream().filter(h->s.equals(h.getStatus())).findFirst().orElse(null);return new Step(s,label(s),entry==null?null:entry.getChangedAt(),entry==null?null:entry.getActor(),s.equals(order.getStatus()));}).toList();}
}
