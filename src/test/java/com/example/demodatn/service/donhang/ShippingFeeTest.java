package com.example.demodatn.service.donhang;

import com.example.demodatn.dto.donhang.*;
import com.example.demodatn.entity.donhang.*;
import com.example.demodatn.repository.donhang.ShippingSettingsRepository;
import jakarta.validation.Validation;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShippingFeeTest {
    BigDecimal n(String s){return new BigDecimal(s);}
    ShippingSettings settings(){var s=new ShippingSettings();s.setOriginLatitude(n("21.03801"));s.setOriginLongitude(n("105.7469"));s.setFirstLimitKm(n("30"));s.setSecondLimitKm(n("50"));s.setFirstFee(n("25000"));s.setSecondFee(n("35000"));s.setFallbackFee(n("55000"));s.setFreeQuantity(2);return s;}
    @Test void configuredBoundariesAndMissingPoint(){
        var s=settings();String[][] cases={{"0","25000"},{"14.9","25000"},{"15","25000"},{"29.99","25000"},{"30","25000"},{"30.01","35000"},{"49.99","35000"},{"50","35000"},{"50.01","55000"},{"500","55000"}};
        for(var c:cases)assertEquals(0,n(c[1]).compareTo(ShippingFeeService.tier(s,n(c[0]))),Arrays.toString(c));
        assertEquals(n("55000"),ShippingFeeService.tier(s,null));assertThrows(IllegalArgumentException.class,()->ShippingFeeService.tier(s,n("-1")));
        System.out.println("GD16 FEE boundaries="+Arrays.deepToString(cases)+" missing coordinates=55000");
    }
    @Test void configurationRejectsUnrepresentableDistancesAndFractionalDong(){
        var repo=mock(ShippingSettingsRepository.class);
        try(var validation=Validation.buildDefaultValidatorFactory()){
            var service=new ShippingFeeService(repo,mock(AddressDirectory.class),mock(GeocodingService.class),validation.getValidator());
            var f=new ShippingSettingsForm();org.springframework.beans.BeanUtils.copyProperties(settings(),f);f.setOriginAddress("Kho thử");
            f.setFirstLimitKm(n("0.001"));f.setSecondLimitKm(n("0.002"));assertThrows(com.example.demodatn.exception.PeopleValidationException.class,()->service.update(f));
            f.setFirstLimitKm(n("30"));f.setSecondLimitKm(n("50"));f.setFirstFee(n("25000.5"));assertThrows(com.example.demodatn.exception.PeopleValidationException.class,()->service.update(f));
            verifyNoInteractions(repo);
        }
    }
    @Test void haversineKnownDistanceAndInvalidCoordinates(){
        assertEquals(n("0.00"),ShippingFeeService.haversine(n("21"),n("105"),n("21"),n("105")));
        assertEquals(n("111.20"),ShippingFeeService.haversine(n("0"),n("0"),n("1"),n("0")));
        assertThrows(IllegalArgumentException.class,()->ShippingFeeService.haversine(n("91"),n("0"),n("1"),n("0")));
    }
    @Test void freeQuantityCountsUnitsAndManualPinOverridesWardCenter(){
        var repo=mock(ShippingSettingsRepository.class);when(repo.findById(1)).thenReturn(Optional.of(settings()));
        var directory=mock(AddressDirectory.class);var geocoder=mock(GeocodingService.class);
        var p=new Province();p.setId(1);p.setName("Hà Nội");p.setAdministrativeCode("01");var w=new Ward();w.setId(2);w.setProvince(p);w.setName("Phường Tây Tựu");w.setCode("00622");w.setLatitude(n("10"));w.setLongitude(n("105"));
        when(directory.requireWard(1,2)).thenReturn(w);
        try(var validation=Validation.buildDefaultValidatorFactory()){
            var service=new ShippingFeeService(repo,directory,geocoder,validation.getValidator());
            var f=new DeliveryAddressForm();f.setProvinceId(1);f.setWardId(2);f.setStreetAddress("Số 12, đường Nguyễn Huệ");f.setLatitude(n("21.03801"));f.setLongitude(n("105.7469"));f.setCoordinateSource("GPS");
            var q=service.quote(f,1,false,"test");assertEquals(n("25000"),q.shippingFee());assertEquals(n("0.00"),q.distanceKm());assertEquals("GPS",q.coordinateSource());assertTrue(q.hint().contains("1 sản phẩm"));
            q=service.quote(f,2,false,"test");assertTrue(q.freeShip());assertEquals(0,q.shippingFee().signum());
            var item=new CartItemDto();item.setQuantity(2);assertEquals(2,ShippingFeeService.quantity(List.of(item)));item.setQuantity(1);assertEquals(2,ShippingFeeService.quantity(List.of(item,item)));
            f.setLatitude(null);f.setLongitude(null);f.setCoordinateSource(null);q=service.quote(f,1,false,"test");assertEquals("TAM_PHUONG_XA",q.coordinateSource());assertEquals(n("55000"),q.shippingFee());
            w.setLatitude(null);w.setLongitude(null);q=service.quote(f,1,false,"test");assertNull(q.distanceKm());assertTrue(q.note().contains("tạm tính"));assertEquals(n("55000"),q.shippingFee());verifyNoInteractions(geocoder);
        }
    }
}
