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
    private final com.example.demodatn.security.AdminAccess access;
    private final AddressDirectory directory;private final ShippingFeeService shipping;private final ShippingPreviewService preview;private final GeocodingService geocoding;private final GeocodingProperties config;private final CustomerRepository customers;private final PosOnlineOrderService online;
    private final com.example.demodatn.service.khachhang.ShippingAddressService shippingAddressesService;
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
        if(pos){var auth=SecurityContextHolder.getContext().getAuthentication();if(!access.allowed(auth,"GET","/admin/pos"))throw new org.springframework.security.access.AccessDeniedException("Không có quyền dùng POS.");}
        else if(!"USER".equals(scope))return ResponseEntity.badRequest().body(Map.of("message","Ngữ cảnh giao hàng không hợp lệ."));
        synchronized(session){try{
            @SuppressWarnings("unchecked") var cart=(List<CartItemDto>)session.getAttribute(pos?"online-posCart":"cart");if(cart==null)cart=List.of();
            var customer=pos?online.byPhone(phone).stream().findFirst().orElse(null):customers.findByUser_Username(principal.getName()).orElse(null);
            var result=preview.preview(cart,address,customer,voucherCode,true,request.getRemoteAddr());
            if(!pos){var saved=new DeliveryAddressForm();org.springframework.beans.BeanUtils.copyProperties(address,saved);session.setAttribute("shippingDestination",saved);}
            return ResponseEntity.ok(result);
        }catch(IllegalArgumentException ex){return ResponseEntity.badRequest().body(Map.of("message",ex.getMessage()));}}
    }

    @PostMapping("/addresses/quick-save")
    public ResponseEntity<?> quickSaveAddress(
            @RequestParam String receiverName,
            @RequestParam String phone,
            @RequestParam(required = false) Integer provinceId,
            @RequestParam(required = false) Integer wardId,
            @RequestParam String streetAddress,
            @RequestParam(required = false) java.math.BigDecimal latitude,
            @RequestParam(required = false) java.math.BigDecimal longitude,
            @RequestParam(required = false) String coordinateSource,
            @RequestParam(defaultValue = "false") boolean makeDefault,
            Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Vui lòng đăng nhập để lưu địa chỉ."));
        }
        if (receiverName == null || receiverName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng nhập họ và tên người nhận."));
        }
        if (phone == null || phone.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng nhập số điện thoại người nhận."));
        }
        if (provinceId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng chọn Tỉnh/Thành phố."));
        }
        if (wardId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng chọn Phường/Xã."));
        }
        if (streetAddress == null || streetAddress.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng nhập số nhà, tên đường."));
        }

        try {
            var form = new com.example.demodatn.dto.khachhang.ShippingAddressForm();
            form.setReceiverName(receiverName.trim());
            form.setPhone(phone.trim());
            form.setProvinceId(provinceId);
            form.setWardId(wardId);
            form.setStreetAddress(streetAddress.trim());
            form.setLatitude(latitude);
            form.setLongitude(longitude);
            form.setCoordinateSource(coordinateSource);
            form.setMakeDefault(makeDefault);

            var saved = shippingAddressesService.save(principal.getName(), form);
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("id", saved.getId());
            res.put("receiverName", saved.getReceiverName());
            res.put("phone", saved.getPhone());
            res.put("fullAddress", saved.getFullAddress());
            res.put("displayStreetAddress", saved.getDisplayStreetAddress());
            res.put("provinceId", saved.getProvince() != null ? saved.getProvince().getId() : "");
            res.put("wardId", saved.getWard() != null ? saved.getWard().getId() : "");
            res.put("latitude", saved.getLatitude() != null ? saved.getLatitude() : "");
            res.put("longitude", saved.getLongitude() != null ? saved.getLongitude() : "");
            res.put("source", saved.getCoordinateSource() != null ? saved.getCoordinateSource() : "");
            res.put("defaultAddress", saved.isDefaultAddress());
            return ResponseEntity.ok(res);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }
}
