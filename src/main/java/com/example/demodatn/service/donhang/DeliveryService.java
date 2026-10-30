package com.example.demodatn.service.donhang;
import com.example.demodatn.entity.donhang.*;
import com.example.demodatn.repository.donhang.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service @RequiredArgsConstructor
public class DeliveryService {
 private final ProvinceRepository provinces;
 private final ShippingRegionRepository regions;
 public static final ZoneId SHOP_ZONE=ZoneId.of("Asia/Ho_Chi_Minh");
 public record Estimate(LocalDate from,LocalDate to,boolean lateConfirmation) {}
 public Province province(Integer id) {
  if(id==null)throw new IllegalArgumentException("Vui lòng chọn Tỉnh/Thành phố.");
  return provinces.findById(id).orElseThrow(()->new IllegalArgumentException("Tỉnh/Thành phố không hợp lệ."));
 }
 public static Estimate calculate(LocalDateTime placed,Instant confirmation,int min,int max) {
  if(min<1 || max>30 || min>max)throw new IllegalArgumentException("Số ngày giao hàng phải từ 1 đến 30, ngày tối thiểu không lớn hơn tối đa.");
  LocalDate confirmed=confirmation.atZone(SHOP_ZONE).toLocalDate(),base=placed.toLocalDate();
  boolean late=!base.plusDays(min).isAfter(confirmed);if(late)base=confirmed;
  return new Estimate(base.plusDays(min),base.plusDays(max),late);
 }
 public String snapshotEstimate(Order order,Instant confirmation) {
  if(!"ONLINE".equals(order.getOrderType()) || order.getProvince()==null)return null;
  var rule=order.getProvince().getRegion();var e=calculate(order.getCreatedAt(),confirmation,rule.getMinDays(),rule.getMaxDays());
  order.setEstimatedFrom(e.from());order.setEstimatedTo(e.to());
  return e.lateConfirmation()?"Xác nhận muộn: tính dự kiến từ ngày xác nhận thay cho ngày đặt.":null;
 }
 @Transactional public void updateRegion(String code,int min,int max) {
  calculate(LocalDateTime.now(),Instant.now(),min,max);
  var rule=regions.findById(code).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
  rule.setMinDays(min);rule.setMaxDays(max);
 }
 public String estimateText(Order o) {
  if(!"ONLINE".equals(o.getOrderType()) || Set.of("CANCELLED","DELIVERY_FAILED").contains(o.getStatus()))return null;
  var date=DateTimeFormatter.ofPattern("dd/MM/yyyy");
  if("DELIVERED".equals(o.getStatus()))return o.getDeliveredOn()==null?"Đã giao, chưa có ngày giao thực tế":"Đã giao ngày "+date.format(o.getDeliveredOn());
  if(o.getProvince()==null)return "Chưa xác định tỉnh/thành";
  if(o.getEstimatedFrom()==null || o.getEstimatedTo()==null)return "PENDING".equals(o.getStatus())?"Sẽ có sau khi cửa hàng xác nhận đơn":"Chưa có ngày giao dự kiến từ dữ liệu cũ";
  return o.getEstimatedFrom().equals(o.getEstimatedTo())?"Dự kiến giao: "+date.format(o.getEstimatedFrom()):"Dự kiến giao: từ "+DateTimeFormatter.ofPattern("dd/MM").format(o.getEstimatedFrom())+" đến "+date.format(o.getEstimatedTo());
 }
}
