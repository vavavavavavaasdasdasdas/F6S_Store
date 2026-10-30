package com.example.demodatn.service.donhang;
import com.example.demodatn.dto.donhang.*;
import com.example.demodatn.entity.donhang.*;
import com.example.demodatn.repository.donhang.ShippingSettingsRepository;
import com.example.demodatn.exception.PeopleValidationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class ShippingFeeService {
    private final ShippingSettingsRepository settings;private final AddressDirectory directory;private final GeocodingService geocoding;private final Validator validator;
    private static final double EARTH_RADIUS_KM=6371.0088;
    public record Quote(BigDecimal shippingFee,BigDecimal distanceKm,BigDecimal latitude,BigDecimal longitude,String coordinateSource,boolean freeShip,String note,String hint,String fullAddress,String provinceCode,String wardCode,String wardName) {}
    public ShippingSettings settings(){return settings.findById(1).orElseThrow(()->new IllegalStateException("Thiếu cấu hình phí vận chuyển. Chạy migration016."));}
    public static int quantity(List<CartItemDto> items){int n=0;for(var item:items){if(item.getQuantity()==null || item.getQuantity()<1)throw new IllegalArgumentException("Số lượng sản phẩm không hợp lệ.");n=Math.addExact(n,item.getQuantity());}return n;}
    public static BigDecimal haversine(BigDecimal lat1,BigDecimal lng1,BigDecimal lat2,BigDecimal lng2){
        var a=GeocodingService.point(lat1,lng1);var b=GeocodingService.point(lat2,lng2);double x=Math.toRadians(a.latitude().doubleValue()),y=Math.toRadians(b.latitude().doubleValue()),dl=Math.toRadians(b.latitude().subtract(a.latitude()).doubleValue()),dn=Math.toRadians(b.longitude().subtract(a.longitude()).doubleValue());
        double h=Math.sin(dl/2)*Math.sin(dl/2)+Math.cos(x)*Math.cos(y)*Math.sin(dn/2)*Math.sin(dn/2);
        return BigDecimal.valueOf(2*EARTH_RADIUS_KM*Math.asin(Math.sqrt(Math.max(0,Math.min(1,h))))).setScale(2,RoundingMode.HALF_UP);
    }
    public static BigDecimal tier(ShippingSettings s,BigDecimal distance){
        if(distance==null)return s.getFallbackFee();if(distance.signum()<0)throw new IllegalArgumentException("Khoảng cách không hợp lệ.");
        return distance.compareTo(s.getFirstLimitKm())<=0?s.getFirstFee():distance.compareTo(s.getSecondLimitKm())<=0?s.getSecondFee():s.getFallbackFee();
    }
    public Ward validateAddress(DeliveryAddressForm form){
        var ward=directory.requireWard(form.getProvinceId(),form.getWardId());
        if(form.getStreetAddress()==null || form.getStreetAddress().isBlank())throw new IllegalArgumentException("Vui lòng nhập số nhà, tên đường.");
        if(form.getStreetAddress().trim().length()>255)throw new IllegalArgumentException("Số nhà, tên đường tối đa 255 ký tự.");
        validatePoint(form);return ward;
    }
    private void validatePoint(DeliveryAddressForm form){
        String source=form.getCoordinateSource();
        if(source!=null && !source.isBlank() && !Set.of("GPS","GHIM_TAY","TAM_PHUONG_XA").contains(source))throw new IllegalArgumentException("Nguồn tọa độ không hợp lệ.");
        if((form.getLatitude()==null)!=(form.getLongitude()==null))throw new IllegalArgumentException("Vui lòng chọn đủ tọa độ hoặc bỏ ghim.");
        if(form.getLatitude()!=null)GeocodingService.point(form.getLatitude(),form.getLongitude());
    }
    public void prepare(DeliveryAddressForm form,String client){var w=validateAddress(form);if(form.getLatitude()==null || "TAM_PHUONG_XA".equals(form.getCoordinateSource()))geocoding.wardCenter(w,client);}
    public Quote quote(DeliveryAddressForm form,int quantity,boolean allowGeocoding,String client){
        var s=settings();validatePoint(form);Ward ward=form.getWardId()==null?null:directory.requireWard(form.getProvinceId(),form.getWardId());
        GeocodingService.Point point=null;String source=null;
        if(form.getLatitude()!=null && Set.of("GPS","GHIM_TAY").contains(Objects.toString(form.getCoordinateSource(),""))){point=GeocodingService.point(form.getLatitude(),form.getLongitude());source=form.getCoordinateSource();}
        else if(ward!=null){var found=ward.getLatitude()!=null && ward.getLongitude()!=null?Optional.of(GeocodingService.point(ward.getLatitude(),ward.getLongitude())):allowGeocoding?geocoding.wardCenter(ward,client):Optional.<GeocodingService.Point>empty();if(found.isPresent()){point=found.get();source="TAM_PHUONG_XA";}}
        BigDecimal distance=point==null?null:haversine(s.getOriginLatitude(),s.getOriginLongitude(),point.latitude(),point.longitude());boolean free=quantity>=s.getFreeQuantity();
        String address=ward==null || form.getStreetAddress()==null || form.getStreetAddress().isBlank()?null:form.getStreetAddress().trim()+", "+ward.getName()+", "+ward.getProvince().getName();
        return new Quote(free?BigDecimal.ZERO:tier(s,distance),distance,point==null?null:point.latitude(),point==null?null:point.longitude(),source,free,free?"Miễn phí vận chuyển":distance==null?"Phí tạm tính, shop sẽ xác nhận lại":"Phí tính theo khoảng cách đường chim bay",free?"":quantity>0?"Mua thêm "+(s.getFreeQuantity()-quantity)+" sản phẩm để được freeship":"Thêm sản phẩm để tính phí vận chuyển",address,ward==null?null:ward.getProvince().getAdministrativeCode(),ward==null?null:ward.getCode(),ward==null?null:ward.getName());
    }
    public void apply(Order order,DeliveryAddressForm form,int quantity){
        var ward=validateAddress(form);var q=quote(form,quantity,false,"order");
        order.setProvince(ward.getProvince());order.setProvinceName(ward.getProvince().getName());order.setWard(ward);order.setStreetAddress(form.getStreetAddress().trim());
        order.setShippingAddress(form.getStreetAddress().trim());order.setDeliveryAddressSnapshot(q.fullAddress());order.setDeliveryProvinceCode(q.provinceCode());order.setDeliveryWardCode(q.wardCode());order.setDeliveryWardName(q.wardName());
        order.setLatitude(q.latitude());order.setLongitude(q.longitude());order.setCoordinateSource(q.coordinateSource());order.setDistanceKm(q.distanceKm());order.setShippingFee(q.shippingFee());order.setFreeShip(q.freeShip());order.setShippingNote(q.note());order.setShippingPolicyVersion(1);
    }
    @Transactional public void update(ShippingSettingsForm form){
        var errors=validator.validate(form);if(!errors.isEmpty()){var e=errors.iterator().next();throw new PeopleValidationException(e.getPropertyPath().toString(),"Kiểm tra giá trị cấu hình vận chuyển.");}
        GeocodingService.point(form.getOriginLatitude(),form.getOriginLongitude());
        if(form.getFirstLimitKm().signum()<0 || form.getSecondLimitKm().compareTo(form.getFirstLimitKm())<=0)throw new PeopleValidationException("secondLimitKm","Mốc thứ hai phải lớn hơn mốc đầu.");
        for(var fee:List.of(form.getFirstFee(),form.getSecondFee(),form.getFallbackFee()))if(fee.signum()<0 || fee.stripTrailingZeros().scale()>0 || fee.precision()-fee.scale()>16)throw new PeopleValidationException("firstFee","Phí phải là số nguyên đồng không âm, tối đa 16 chữ số.");
        if(form.getFirstLimitKm().precision()-form.getFirstLimitKm().scale()>8 || form.getSecondLimitKm().precision()-form.getSecondLimitKm().scale()>8)throw new PeopleValidationException("secondLimitKm","Mốc khoảng cách tối đa 8 chữ số phần nguyên.");
        if(form.getFirstLimitKm().stripTrailingZeros().scale()>2 || form.getSecondLimitKm().stripTrailingZeros().scale()>2)throw new PeopleValidationException("secondLimitKm","Mốc khoảng cách tối đa 2 chữ số thập phân.");
        var s=settings();org.springframework.beans.BeanUtils.copyProperties(form,s);s.setOriginLatitude(form.getOriginLatitude().setScale(7,RoundingMode.HALF_UP));s.setOriginLongitude(form.getOriginLongitude().setScale(7,RoundingMode.HALF_UP));settings.save(s);
    }
}
