package com.example.demodatn.controller.user;
import com.example.demodatn.dto.donhang.*;
import com.example.demodatn.service.donhang.*;
import com.example.demodatn.repository.khachhang.CustomerRepository;
import com.example.demodatn.config.GeocodingProperties;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.BindingResult;
import java.util.*;
import java.security.Principal;
// Existing SecurityConfig.anyRequest().authenticated() protects every shipping endpoint.
@RestController @RequestMapping("/shipping") @RequiredArgsConstructor
public class DeliveryAddressController {
    private final AddressDirectory directory;private final ShippingFeeService shipping;private final ShippingPreviewService preview;private final GeocodingService geocoding;private final GeocodingProperties config;private final CustomerRepository customers;private final PosOnlineOrderService online;
    @GetMapping("/config") public Object config(){var s=shipping.settings();return Map.of("originLatitude",s.getOriginLatitude(),"originLongitude",s.getOriginLongitude(),"originAddress",s.getOriginAddress(),"freeQuantity",s.getFreeQuantity(),"tileUrl",config.getTileUrl());}
    @GetMapping("/wards") public ResponseEntity<?> wards(@RequestParam(defaultValue="") String provinceId){try{return ResponseEntity.ok(directory.wards(Integer.valueOf(provinceId)).stream().map(w->Map.of("id",w.getId(),"code",w.getCode(),"name",w.getName())).toList());}catch(NumberFormatException ex){return ResponseEntity.badRequest().body(Map.of("message","Tỉnh/Thành phố không hợp lệ."));}}
    @PostMapping("/reverse") public ResponseEntity<?> reverse(@ModelAttribute DeliveryAddressForm point,BindingResult errors,HttpServletRequest request){
        if(errors.hasErrors())return ResponseEntity.badRequest().body(Map.of("message","Tọa độ không hợp lệ."));
        try{return ResponseEntity.ok(geocoding.reverse(point.getLatitude(),point.getLongitude(),request.getRemoteAddr()));}
        catch(IllegalArgumentException ex){return ResponseEntity.badRequest().body(Map.of("message",ex.getMessage()));}
    }
    @PostMapping("/quote") public ResponseEntity<?> quote(@ModelAttribute DeliveryAddressForm address,BindingResult errors,@RequestParam(defaultValue="USER") String scope,@RequestParam(defaultValue="") String voucherCode,@RequestParam(defaultValue="") String phone,HttpSession session,Principal principal,HttpServletRequest request){
        if(errors.hasErrors())return ResponseEntity.badRequest().body(Map.of("message","Kiểm tra Tỉnh/Thành phố, Phường/Xã và tọa độ."));
        boolean pos="POS_ONLINE".equals(scope);
        if(pos){var auth=SecurityContextHolder.getContext().getAuthentication();if(auth==null || auth.getAuthorities().stream().noneMatch(a->a.getAuthority().equals("ROLE_ADMIN")))throw new org.springframework.security.access.AccessDeniedException("Không có quyền dùng POS.");}
        else if(!"USER".equals(scope))return ResponseEntity.badRequest().body(Map.of("message","Ngữ cảnh giao hàng không hợp lệ."));
        synchronized(session){try{
            @SuppressWarnings("unchecked") var cart=(List<CartItemDto>)session.getAttribute(pos?"posCart":"cart");if(cart==null)cart=List.of();
            var customer=pos?online.byPhone(phone).stream().findFirst().orElse(null):customers.findByUser_Username(principal.getName()).orElse(null);
            var result=preview.preview(cart,address,customer,voucherCode,true,request.getRemoteAddr());
            if(!pos){var saved=new DeliveryAddressForm();org.springframework.beans.BeanUtils.copyProperties(address,saved);session.setAttribute("shippingDestination",saved);}
            return ResponseEntity.ok(result);
        }catch(IllegalArgumentException ex){return ResponseEntity.badRequest().body(Map.of("message",ex.getMessage()));}}
    }
}
