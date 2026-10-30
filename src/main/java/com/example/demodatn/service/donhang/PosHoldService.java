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
    private final Clock clock;
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
    public List<HoldSummary> list(String username) {
        return holds.findByStaffUser_UsernameAndExpiresAtAfterOrderByCreatedAtAsc(username,now()).stream()
                .filter(h->h.getExpiresAt().isAfter(now())).map(h->new HoldSummary(h.getId(),h.getCode(),
                    h.getItems().stream().mapToInt(PosHeldItem::getQuantity).sum(),
                    h.getItems().stream().map(i->pricing.calculatePrice(i.getVariant()).getFinalPrice().multiply(BigDecimal.valueOf(i.getQuantity()))).reduce(BigDecimal.ZERO,BigDecimal::add),
                    h.getExpiresAt().atZone(clock.getZone()).toInstant().toEpochMilli(),
                    h.getItems().stream().map(i -> i.getVariant().getMaterial().getName()).distinct().sorted().collect(java.util.stream.Collectors.joining(", ")),
                    h.getItems().stream().map(i -> i.getVariant().getColor().getName()).distinct().sorted().collect(java.util.stream.Collectors.joining(", ")),
                    h.getItems().stream().map(i -> i.getVariant().getSole().getName()).distinct().sorted().collect(java.util.stream.Collectors.joining(", ")))).toList();
    }
    public record HoldSummary(Long id,String code,int itemCount,BigDecimal subtotal,long expiresEpoch,String materialNames,String colorNames,String soleNames) {}
    public record Loaded(Long id,String code,List<CartItemDto> cart,PosCheckoutRequest request) {}

    private PosHeldOrder locked(String username,Long id) {
        return holds.lockActive(id,username,now()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Hóa đơn chờ không tồn tại hoặc đã hết hạn."));
    }

    @Transactional
    public Long save(String username,List<CartItemDto> cart,PosCheckoutRequest request,Long currentId) {
        var user=users.lockByUsername(username).orElseThrow();
        if(cart.isEmpty())throw new IllegalArgumentException("Giỏ hàng trống, không thể lưu chờ.");
        if(!Set.of("CASH","TRANSFER").contains(Objects.toString(request.getPaymentMethod(),"")))
            throw new IllegalArgumentException("Chọn Tiền mặt hoặc Chuyển khoản.");
        if(request.getNote()!=null && request.getNote().length()>500)throw new IllegalArgumentException("Ghi chú tối đa 500 ký tự.");
        if(request.getVoucherCode()!=null && request.getVoucherCode().length()>50)throw new IllegalArgumentException("Mã phiếu giảm giá tối đa 50 ký tự.");
        PosHeldOrder hold;
        if(currentId==null) {
            if(holds.countByStaffUser_IdAndExpiresAtAfter(user.getId(),now())>=5)throw new IllegalArgumentException(LIMIT_MESSAGE);
            hold=new PosHeldOrder();hold.setStaffUser(user);hold.setCode("HC-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT));
            hold.setCreatedAt(now());hold.setExpiresAt(expiry(clock,expiryMode));
        } else hold=locked(username,currentId);
        var customer=request.getCustomerId()==null ? null : customers.findById(request.getCustomerId()).orElseThrow(()->new IllegalArgumentException("Khách hàng không tồn tại."));
        if(customer!=null && !Boolean.TRUE.equals(customer.getStatus()))throw new IllegalArgumentException("Khách hàng đã bị vô hiệu hóa.");
        hold.setCustomer(customer);hold.setCustomerLabel(customer==null ? "Khách lẻ" : customer.getFullName());
        hold.setVoucherCode(request.getVoucherCode());hold.setPaymentMethod(request.getPaymentMethod());hold.setNote(request.getNote());
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
    public Loaded open(String username,Long id) {
        var h=locked(username,id);var request=new PosCheckoutRequest();
        request.setCustomerId(h.getCustomer()==null ? null : h.getCustomer().getId());
        request.setVoucherCode(h.getVoucherCode());request.setPaymentMethod(h.getPaymentMethod());request.setNote(h.getNote());
        var cart=h.getItems().stream().map(i->{var v=i.getVariant();return new CartItemDto(v.getProduct().getId(),v.getId(),v.getProduct().getName(),v.getSize().getSizeValue(),v.getMaterial().getName(),v.getColor().getName(),v.getSole().getName(),pricing.calculatePrice(v).getFinalPrice(),i.getQuantity());}).toList();
        return new Loaded(h.getId(),h.getCode(),new ArrayList<>(cart),request);
    }

    @Transactional
    public Loaded switchTo(String username,Long id,List<CartItemDto> current,PosCheckoutRequest request,Long currentId,boolean saveCurrent) {
        users.lockByUsername(username).orElseThrow();
        locked(username,id);
        if(saveCurrent && !current.isEmpty())save(username,current,request,currentId);
        return open(username,id);
    }

    @Transactional
    public void delete(String username,Long id) {holds.delete(locked(username,id));}

    @Transactional(rollbackFor=Exception.class)
    public Order checkout(String username,Long id,List<CartItemDto> cart,PosCheckoutRequest request) {
        var hold=locked(username,id);
        var employee=employees.findByUser_Username(username).orElseThrow(()->new IllegalArgumentException("Tài khoản chưa gắn với nhân viên."));
        var customer=request.getCustomerId()==null ? null : customers.findById(request.getCustomerId()).orElseThrow(()->new IllegalArgumentException("Khách hàng không tồn tại."));
        if(cart.isEmpty())throw new IllegalArgumentException("Giỏ hàng trống.");
        for(var item:cart) {
            var price=pricing.priceForVariant(item.getVariantId()).getFinalPrice();
            if(price.compareTo(item.getPrice())!=0)throw new IllegalArgumentException("Giá đã thay đổi. Kiểm tra và xác nhận lại giá trước khi thanh toán.");
        }
        var order=orders.createPosOrder(cart,customer,employee,request.getVoucherCode(),request.getPaymentMethod(),"Tại quầy",request.getCashReceived());
        holds.delete(hold);holds.flush();
        return order;
    }

    @Transactional
    public int cleanup() {
        var expired=holds.findByExpiresAtLessThanEqual(now());holds.deleteAll(expired);holds.flush();return expired.size();
    }
}
