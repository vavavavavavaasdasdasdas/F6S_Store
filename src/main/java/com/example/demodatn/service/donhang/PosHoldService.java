package com.example.demodatn.service.donhang;
import com.example.demodatn.dto.donhang.CartItemDto;
import com.example.demodatn.dto.donhang.PosCheckoutRequest;
import com.example.demodatn.entity.donhang.Order;
import com.example.demodatn.entity.donhang.PosHeldItem;
import com.example.demodatn.entity.donhang.PosHeldOrder;
import com.example.demodatn.repository.donhang.PosHeldOrderRepository;
import com.example.demodatn.repository.khachhang.CustomerRepository;
import com.example.demodatn.repository.nhanvien.EmployeeRepository;
import com.example.demodatn.repository.nhanvien.UserRepository;
import com.example.demodatn.repository.sanpham.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;

@Service @RequiredArgsConstructor
public class PosHoldService {
    public static final String LIMIT_MESSAGE="Đã đủ 5 hóa đơn chờ. Hãy thanh toán hoặc xóa bớt một hóa đơn.";
    private final PosHeldOrderRepository holds;
    private final UserRepository users;
    private final EmployeeRepository employees;
    private final CustomerRepository customers;
    private final ProductVariantRepository variants;
    private final PricingService pricing;
    private final OrderService orders;
    private final PosOnlineOrderService onlineOrders;
    private final Clock clock;
    private final tools.jackson.databind.ObjectMapper mapper;
    @Value("${shop.pos.hold.expiry-mode:END_OF_DAY}") private String expiryMode="END_OF_DAY";

    public LocalDateTime now(){return LocalDateTime.now(clock);}
    public static LocalDateTime expiry(Clock clock,String mode) {
        var created=ZonedDateTime.now(clock);
        return switch(mode) {
            case "END_OF_DAY" -> created.toLocalDate().plusDays(1).atStartOfDay(clock.getZone()).toLocalDateTime();
            case "HOURS_24" -> created.plusHours(24).toLocalDateTime();
            default -> throw new IllegalArgumentException("Chế độ hết hạn hóa đơn chờ không hợp lệ.");
        };
    }

    @Transactional(readOnly=true)
    public List<HoldSummary> list(String username){return list(username,"POS");}
    @Transactional(readOnly=true)
    public List<HoldSummary> list(String username,String mode) {
        return holds.findByStaffUser_UsernameAndExpiresAtAfterOrderByCreatedAtAsc(username,now()).stream()
                .filter(h->mode.equals(h.getOrderType())&&h.getExpiresAt().isAfter(now())).map(h->new HoldSummary(h.getId(),h.getCode(),
                    h.getItems().stream().mapToInt(PosHeldItem::getQuantity).sum(),
                    h.getItems().stream().map(i->pricing.calculatePrice(i.getVariant()).getFinalPrice().multiply(BigDecimal.valueOf(i.getQuantity()))).reduce(BigDecimal.ZERO,BigDecimal::add),
                    h.getExpiresAt().atZone(clock.getZone()).toInstant().toEpochMilli(),
                    h.getItems().stream().map(i -> i.getVariant().getMaterial().getName()).distinct().sorted().collect(java.util.stream.Collectors.joining(", ")),
                    h.getItems().stream().map(i -> i.getVariant().getColor().getName()).distinct().sorted().collect(java.util.stream.Collectors.joining(", ")),
                    h.getItems().stream().map(i -> i.getVariant().getSole().getName()).distinct().sorted().collect(java.util.stream.Collectors.joining(", ")))).toList();
    }
    public record HoldSummary(Long id,String code,int itemCount,BigDecimal subtotal,long expiresEpoch,String materialNames,String colorNames,String soleNames) {}
    public record Loaded(Long id,String code,List<CartItemDto> cart,PosCheckoutRequest request,com.example.demodatn.dto.donhang.PosOnlineOrderForm online) {}

    private PosHeldOrder locked(String username,Long id) {
        return holds.lockActive(id,username,now()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Hóa đơn chờ không tồn tại hoặc đã hết hạn."));
    }

    private PosHeldOrder locked(String username,Long id,String mode){var h=locked(username,id);if(!mode.equals(h.getOrderType()))throw new ResponseStatusException(HttpStatus.NOT_FOUND);return h;}

    @Transactional
    public Long save(String username,List<CartItemDto> cart,PosCheckoutRequest request,Long currentId){return save(username,cart,request,currentId,"POS",null);}
    @Transactional
    public Long save(String username,List<CartItemDto> cart,PosCheckoutRequest request,Long currentId,String mode,com.example.demodatn.dto.donhang.PosOnlineOrderForm online) {
        if(!Set.of("POS","ONLINE").contains(mode))throw new IllegalArgumentException("Loại hóa đơn chờ không hợp lệ.");
        var user=users.lockByUsername(username).orElseThrow();
        Set<String> validMethods = (request.isDelivery() || "ONLINE".equals(mode))
                ? Set.of("COD","BANK_TRANSFER","TRANSFER","CASH")
                : Set.of("CASH","TRANSFER","COMBINED");
        String pMethod = Objects.toString(request.getPaymentMethod(),"");
        if (pMethod.isBlank()) {
            pMethod = request.isDelivery() ? "COD" : "CASH";
            request.setPaymentMethod(pMethod);
        }
        if(!validMethods.contains(pMethod))
            throw new IllegalArgumentException(request.isDelivery() ? "Chọn COD hoặc Chuyển khoản." : "Chọn Tiền mặt, Chuyển khoản hoặc Kết hợp.");
        if(!"ONLINE".equals(mode)&&request.getNote()!=null && request.getNote().length()>500)throw new IllegalArgumentException("Ghi chú tối đa 500 ký tự.");
        if(request.getVoucherCode()!=null && request.getVoucherCode().length()>50)throw new IllegalArgumentException("Mã phiếu giảm giá tối đa 50 ký tự.");
        PosHeldOrder hold;
        if(currentId==null) {
            if(holds.countByStaffUser_IdAndOrderTypeAndExpiresAtAfter(user.getId(),mode,now())>=5)throw new IllegalArgumentException(LIMIT_MESSAGE);
            hold=new PosHeldOrder();hold.setStaffUser(user);hold.setCode("HC-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT));
            hold.setCreatedAt(now());hold.setExpiresAt(expiry(clock,expiryMode));
        } else hold=locked(username,currentId,mode);
        hold.setOrderType(mode);
        hold.setLaGiaoHang(request.isDelivery());
        if (request.isDelivery()) {
            Map<String, Object> deliveryMap = new HashMap<>();
            deliveryMap.put("receiverName", request.getReceiverName());
            deliveryMap.put("receiverPhone", request.getReceiverPhone());
            deliveryMap.put("receiverEmail", request.getReceiverEmail());
            deliveryMap.put("provinceId", request.getProvinceId());
            deliveryMap.put("wardId", request.getWardId());
            deliveryMap.put("streetAddress", request.getStreetAddress());
            deliveryMap.put("latitude", request.getLatitude());
            deliveryMap.put("longitude", request.getLongitude());
            deliveryMap.put("coordinateSource", request.getCoordinateSource());
            deliveryMap.put("confirmNow", request.isConfirmNow());
            try {
                hold.setThongTinGiaoHang(mapper.writeValueAsString(deliveryMap));
            } catch (tools.jackson.core.JacksonException ex) {
                // ignore
            }
        } else {
            hold.setThongTinGiaoHang(null);
        }
        try{var snapshot=new com.example.demodatn.dto.donhang.PosOnlineOrderForm();if(online!=null)org.springframework.beans.BeanUtils.copyProperties(online,snapshot);snapshot.setCheckoutToken(null);hold.setDeliveryData(online==null?null:mapper.writeValueAsString(snapshot));}catch(tools.jackson.core.JacksonException ex){throw new IllegalArgumentException("Không thể lưu thông tin giao hàng.");}
        var customer=request.getCustomerId()==null ? null : customers.findById(request.getCustomerId()).orElseThrow(()->new IllegalArgumentException("Khách hàng không tồn tại."));
        if(customer!=null && !Boolean.TRUE.equals(customer.getStatus()))throw new IllegalArgumentException("Khách hàng đã bị vô hiệu hóa.");
        hold.setCustomer(customer);hold.setCustomerLabel(customer==null ? "Khách lẻ" : customer.getFullName());
        hold.setVoucherCode(request.getVoucherCode());hold.setPaymentMethod(request.getPaymentMethod());hold.setNote("ONLINE".equals(mode)?null:request.getNote());
        hold.getItems().clear();
        if(hold.getId()!=null)holds.flush();
        for(var line:cart) {
            if(line.getQuantity()==null || line.getQuantity()<=0)throw new IllegalArgumentException("Số lượng phải lớn hơn 0.");
            var item=new PosHeldItem();item.setHeld(hold);item.setQuantity(line.getQuantity());
            item.setVariant(variants.findById(line.getVariantId()).orElseThrow(()->new IllegalArgumentException("Sản phẩm không tồn tại.")));
            hold.getItems().add(item);
        }
        return holds.saveAndFlush(hold).getId();
    }

    @Transactional
    public Loaded open(String username,Long id){return open(username,id,"POS");}
    @Transactional
    public Loaded open(String username,Long id,String mode) {
        var h=locked(username,id,mode);var request=new PosCheckoutRequest();
        request.setCustomerId(h.getCustomer()==null ? null : h.getCustomer().getId());
        request.setVoucherCode(h.getVoucherCode());request.setPaymentMethod(h.getPaymentMethod());request.setNote(h.getNote());
        request.setDelivery(h.isLaGiaoHang());
        if (h.getThongTinGiaoHang() != null) {
            try {
                var map = mapper.readValue(h.getThongTinGiaoHang(), Map.class);
                request.setReceiverName((String) map.get("receiverName"));
                request.setReceiverPhone((String) map.get("receiverPhone"));
                request.setReceiverEmail((String) map.get("receiverEmail"));
                if (map.get("provinceId") != null) request.setProvinceId(Integer.valueOf(map.get("provinceId").toString()));
                if (map.get("wardId") != null) request.setWardId(Integer.valueOf(map.get("wardId").toString()));
                request.setStreetAddress((String) map.get("streetAddress"));
                if (map.get("latitude") != null) request.setLatitude(new BigDecimal(map.get("latitude").toString()));
                if (map.get("longitude") != null) request.setLongitude(new BigDecimal(map.get("longitude").toString()));
                request.setCoordinateSource((String) map.get("coordinateSource"));
                if (map.get("confirmNow") != null) request.setConfirmNow(Boolean.parseBoolean(map.get("confirmNow").toString()));
            } catch (Exception ignored) {}
        }
        var cart=h.getItems().stream().map(i->{var v=i.getVariant();return new CartItemDto(v.getProduct().getId(),v.getId(),v.getProduct().getName(),v.getSize().getSizeValue(),v.getMaterial().getName(),v.getColor().getName(),v.getSole().getName(),pricing.calculatePrice(v).getFinalPrice(),i.getQuantity());}).toList();
        com.example.demodatn.dto.donhang.PosOnlineOrderForm online=null;
        if(h.getDeliveryData()!=null)try{online=mapper.readValue(h.getDeliveryData(),com.example.demodatn.dto.donhang.PosOnlineOrderForm.class);}catch(tools.jackson.core.JacksonException ex){throw new IllegalArgumentException("Không đọc được địa chỉ hóa đơn chờ.");}
        return new Loaded(h.getId(),h.getCode(),new ArrayList<>(cart),request,online);
    }

    @Transactional
    public Loaded switchTo(String username,Long id,List<CartItemDto> current,PosCheckoutRequest request,Long currentId,boolean saveCurrent){return switchTo(username,id,current,request,currentId,saveCurrent,"POS",null);}
    @Transactional
    public Loaded switchTo(String username,Long id,List<CartItemDto> current,PosCheckoutRequest request,Long currentId,boolean saveCurrent,String mode,com.example.demodatn.dto.donhang.PosOnlineOrderForm online) {
        users.lockByUsername(username).orElseThrow();
        locked(username,id,mode);
        if(saveCurrent && currentId != null)save(username,current,request,currentId,mode,online);
        return open(username,id,mode);
    }

    @Transactional
    public void delete(String username,Long id){delete(username,id,"POS");}
    @Transactional
    public void delete(String username,Long id,String mode){holds.delete(locked(username,id,mode));}

    @Transactional(rollbackFor=Exception.class)
    public Order checkout(String username,Long id,List<CartItemDto> cart,PosCheckoutRequest request) {
        var hold=locked(username,id,"POS");
        var employee=employees.findByUser_Username(username).orElseThrow(()->new IllegalArgumentException("Tài khoản chưa gắn với nhân viên."));
        var customer=request.getCustomerId()==null ? null : customers.findById(request.getCustomerId()).orElseThrow(()->new IllegalArgumentException("Khách hàng không tồn tại."));
        if(cart.isEmpty())throw new IllegalArgumentException("Giỏ hàng trống.");
        for(var item:cart) {
            var price=pricing.priceForVariant(item.getVariantId()).getFinalPrice();
            if(price.compareTo(item.getPrice())!=0)throw new IllegalArgumentException("Giá đã thay đổi. Kiểm tra và xác nhận lại giá trước khi thanh toán.");
        }
        Order order;
        if (request.isDelivery()) {
            order = orders.createPosDeliveryOrder(cart, customer, employee, request.getVoucherCode(), request.getPaymentMethod(),
                    request.getReceiverName(), request.getReceiverPhone(), request.getReceiverEmail(),
                    request.getProvinceId(), request.getWardId(), request.getStreetAddress(),
                    request.getLatitude(), request.getLongitude(), request.getCoordinateSource(),
                    request.isTransferReceived(), request.isConfirmNow(), request.getNote());
        } else {
            order = orders.createPosOrder(cart, customer, employee, request.getVoucherCode(), request.getPaymentMethod(), "Tại quầy", request.getCashReceived(), request.getTransferAmount(), request.getPaymentReference(), request.getNote());
        }
        holds.delete(hold);holds.flush();
        return order;
    }

    @Transactional(rollbackFor=Exception.class)
    public Order checkoutOnline(String username,Long id,List<CartItemDto> cart,com.example.demodatn.dto.donhang.PosOnlineOrderForm form){
        var hold=locked(username,id,"ONLINE");
        for(var item:cart)if(pricing.priceForVariant(item.getVariantId()).getFinalPrice().compareTo(item.getPrice())!=0)throw new IllegalArgumentException("Giá đã thay đổi. Kiểm tra và xác nhận lại giá trước khi tạo đơn.");
        var order=onlineOrders.create(username,form,cart);holds.delete(hold);holds.flush();return order;
    }

    @Transactional
    public int cleanup() {
        var expired=holds.findByExpiresAtLessThanEqual(now());holds.deleteAll(expired);holds.flush();return expired.size();
    }
}
