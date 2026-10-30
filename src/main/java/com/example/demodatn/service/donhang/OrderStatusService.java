package com.example.demodatn.service.donhang;
import com.example.demodatn.service.sanpham.ProductSalePolicy;
import com.example.demodatn.entity.donhang.Order;
import com.example.demodatn.repository.donhang.OrderRepository;
import com.example.demodatn.service.sanpham.InventoryService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class OrderStatusService {
    public static final Map<String,String> LABELS=Map.of(
        "PENDING","Chờ xử lý","CONFIRMED","Đã xác nhận","PREPARING","Đang chuẩn bị",
        "SHIPPING","Đang giao","DELIVERED","Hoàn tất","CANCELLED","Đã hủy",
        "DELIVERY_FAILED","Giao hàng thất bại");
    private final OrderRepository orders;
    private final InventoryService inventory;
    private final EntityManager entityManager;
    private final Clock clock;
    private final DeliveryService delivery;
    private final com.example.demodatn.repository.donhang.OrderStatusHistoryRepository history;
    private static String actor() {
        var auth=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String value=auth==null || !auth.isAuthenticated()?"Hệ thống":auth.getName();
        return value.substring(0,Math.min(100,value.length()));
    }
    private static String text(String value,int max) {
        if(value==null || value.isBlank())return null;
        value=value.trim();if(value.length()>max)throw new IllegalArgumentException("Thông tin vận chuyển/lý do vượt độ dài cho phép.");return value;
    }
    private void record(Order order,String note,String carrier,String trackingCode) {
        var row=new com.example.demodatn.entity.donhang.OrderStatusHistory();row.setOrder(order);row.setStatus(order.getStatus());
        row.setChangedAt(LocalDateTime.ofInstant(clock.instant(),DeliveryService.SHOP_ZONE));row.setActor(actor());
        row.setNote(text(note,1000));row.setCarrier(text(carrier,100));row.setTrackingCode(text(trackingCode,100));history.save(row);
    }
    @Value("${shop.order.bank-transfer-timeout-hours:24}") private long timeoutHours;

    public List<String> allowedStatuses(Order order) {
        return switch(order.getStatus()) {
            case "PENDING" -> List.of("CONFIRMED","CANCELLED");
            case "CONFIRMED" -> List.of("PREPARING","CANCELLED");
            case "PREPARING" -> List.of("SHIPPING","CANCELLED");
            case "SHIPPING" -> List.of("DELIVERED","DELIVERY_FAILED");
            default -> List.of();
        };
    }
    public static boolean bankTransfer(Order order) {
        return "TRANSFER".equals(order.getPaymentMethod()) || "BANK_TRANSFER".equals(order.getPaymentMethod());
    }
    private Order locked(Integer id) {
        Order order=orders.findForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        entityManager.refresh(order);
        return order;
    }

    @Transactional
    public void place(Order order) {
        if(order.getId()!=null)throw new IllegalArgumentException("Đơn hàng đã được tạo.");
        boolean pos="POS".equals(order.getOrderType());
        if(!pos && !"COD".equals(order.getPaymentMethod()) && !bankTransfer(order))
            throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ.");
        order.setStatus(pos?"CONFIRMED":"PENDING");
        order.setPaymentStatus(pos?"PAID":"UNPAID");
        order.setCreatedAt(LocalDateTime.ofInstant(clock.instant(),DeliveryService.SHOP_ZONE));order.setUpdatedAt(order.getCreatedAt());
        orders.saveAndFlush(order);
        record(order,"Đã tạo đơn hàng.",null,null);
        order.setOrderCode(String.format("DH%06d",order.getId()));orders.flush();
        if(pos || bankTransfer(order))inventory.truKhoChoDon(order);
        else {
            // COD checks availability without reserving. Confirmation checks it again atomically.
            Map<Integer,Integer> quantities=new TreeMap<>();
            for(var detail:order.getOrderDetails())quantities.merge(detail.getProductVariant().getId(),detail.getQuantity(),Math::addExact);
            for(var line:quantities.entrySet()) {
                var variant=inventory.lock(line.getKey());
                if(!ProductSalePolicy.isSelling(variant) || variant.getQuantity()<line.getValue())
                    throw new IllegalArgumentException("Không đủ tồn kho hoặc sản phẩm ngừng bán: "+variant.getProduct().getName()+" / Kích cỡ "+variant.getSize().getSizeValue()+" / "+variant.getMaterial().getName());
            }
        }
    }

    @Transactional
    public void updateStatus(Integer id,String status) { change(locked(id),status); }

    @Transactional
    public void transition(Integer id,String status,String reason,String carrier,String trackingCode) {
        Order order=locked(id);
        if("ONLINE".equals(order.getOrderType()) && Set.of("CANCELLED","DELIVERY_FAILED").contains(status) && !status.equals(order.getStatus()) && (reason==null || reason.isBlank()))
            throw new IllegalArgumentException("Vui lòng nhập lý do hủy đơn hoặc giao hàng thất bại.");
        change(order,status,reason,carrier,trackingCode);
    }
    private void change(Order order,String status) { change(order,status,null,null,null); }
    private void change(Order order,String status,String reason,String carrier,String trackingCode) {
        if(!LABELS.containsKey(status))throw new IllegalArgumentException("Trạng thái đơn hàng không hợp lệ.");
        if(status.equals(order.getStatus()))return;
        if(!allowedStatuses(order).contains(status))throw new IllegalArgumentException("Không thể chuyển từ "+LABELS.getOrDefault(order.getStatus(),order.getStatus())+" sang "+LABELS.get(status)+".");
        if("DELIVERED".equals(status) && bankTransfer(order) && !"PAID".equals(order.getPaymentStatus()))
            throw new IllegalArgumentException("Cần xác nhận đã nhận chuyển khoản trước khi hoàn tất.");
        reason=text(reason,1000);carrier=text(carrier,100);trackingCode=text(trackingCode,100);
        if(!"SHIPPING".equals(status) && (carrier!=null || trackingCode!=null))throw new IllegalArgumentException("Chỉ nhập vận đơn khi giao cho đơn vị vận chuyển.");
        if("CONFIRMED".equals(status)) {
            inventory.truKhoChoDon(order);
            String note=delivery.snapshotEstimate(order,clock.instant());
            if(note!=null)reason=reason==null?note:reason+" "+note;
        }
        if(Set.of("CANCELLED","DELIVERY_FAILED").contains(status))inventory.hoanKhoChoDon(order);
        if("DELIVERED".equals(status)){order.setPaymentStatus("PAID");order.setDeliveredOn(clock.instant().atZone(DeliveryService.SHOP_ZONE).toLocalDate());}
        order.setStatus(status);order.setUpdatedAt(LocalDateTime.ofInstant(clock.instant(),DeliveryService.SHOP_ZONE));
        record(order,reason,carrier,trackingCode);
    }

    @Transactional
    public void cancelByCustomer(Integer id,String username) {
        Order order=locked(id);
        if(!"ONLINE".equals(order.getOrderType()) || order.getCustomer()==null || order.getCustomer().getUser()==null
                || !order.getCustomer().getUser().getUsername().equals(username))
            throw new org.springframework.security.access.AccessDeniedException("Không có quyền hủy đơn hàng này.");
        change(order,"CANCELLED","Khách hàng yêu cầu hủy đơn.",null,null);
    }

    @Transactional
    public void updateProvince(Integer id,Integer provinceId) {
        Order order=locked(id);
        if(!"ONLINE".equals(order.getOrderType()) || !Set.of("PENDING","CONFIRMED","PREPARING").contains(order.getStatus()))
            throw new IllegalArgumentException("Chỉ sửa tỉnh/thành trước khi giao cho đơn vị vận chuyển.");
        var province=delivery.province(provinceId);
        if(order.getProvince()!=null && order.getProvince().getId().equals(provinceId))return;
        String before=order.getProvinceName()==null?"chưa xác định":order.getProvinceName();
        order.setProvince(province);order.setProvinceName(province.getName());
        String note="Đổi tỉnh/thành từ "+before+" sang "+province.getName()+".";
        if(!"PENDING".equals(order.getStatus())){String late=delivery.snapshotEstimate(order,clock.instant());if(late!=null)note+=" "+late;}
        order.setUpdatedAt(LocalDateTime.ofInstant(clock.instant(),DeliveryService.SHOP_ZONE));record(order,note,null,null);
    }

    @Transactional
    public void confirmPayment(Integer id) {
        Order order=locked(id);
        if(Set.of("CANCELLED","DELIVERY_FAILED").contains(order.getStatus()) || !bankTransfer(order))
            throw new IllegalArgumentException("Chỉ xác nhận chuyển khoản cho đơn chưa hủy hoặc thất bại.");
        order.setPaymentStatus("PAID");order.setUpdatedAt(LocalDateTime.now(clock));
    }

    @Transactional
    public boolean cancelExpiredTransfer(Integer id) {
        Order order=locked(id);
        if(!"ONLINE".equals(order.getOrderType()) || !"PENDING".equals(order.getStatus()) || !"UNPAID".equals(order.getPaymentStatus()) || !bankTransfer(order)
                || order.getCreatedAt().isAfter(LocalDateTime.now(clock).minusHours(Math.max(1,timeoutHours))))return false;
        change(order,"CANCELLED","Hết hạn thanh toán chuyển khoản.",null,null);return true;
    }
}
