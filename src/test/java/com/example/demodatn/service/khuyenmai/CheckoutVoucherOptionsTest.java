package com.example.demodatn.service.khuyenmai;

import com.example.demodatn.entity.khuyenmai.Voucher;
import com.example.demodatn.repository.khuyenmai.VoucherRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CheckoutVoucherOptionsTest {
    private Voucher voucher(String code,String type,String value,String min,String cap) {
        var v=new Voucher();v.setCode(code);v.setName("Giảm tiếng Việt "+code);v.setDiscountType(type);v.setDiscountValue(new BigDecimal(value));
        v.setMinOrderValue(new BigDecimal(min));v.setMaxDiscount(cap==null?null:new BigDecimal(cap));
        v.setStartDate(LocalDateTime.of(2026,11,1,0,0));v.setEndDate(LocalDateTime.of(2026,11,10,0,0));return v;
    }
    @Test void optionsUseExistingCappedCalculationAndHideNonRunningVouchers() {
        var repository=mock(VoucherRepository.class);
        var service=new VoucherService(repository,new DiscountEffectivity(Clock.fixed(Instant.parse("2026-11-04T00:00:00Z"),ZoneId.of("Asia/Ho_Chi_Minh"))));
        var capped=voucher("CAP","PERCENT","25","0","1200000");
        var fixed=voucher("FIX","FIXED","500000","0",null);
        var unmet=voucher("MIN","FIXED","2000000","20000000",null);
        var future=voucher("FUTURE","FIXED","1","0",null);future.setStartDate(LocalDateTime.of(2026,11,5,0,0));
        var expired=voucher("EXPIRED","FIXED","1","0",null);expired.setEndDate(LocalDateTime.of(2026,11,3,0,0));
        var off=voucher("OFF","FIXED","1","0",null);off.setStatus(false);
        when(repository.findAll()).thenReturn(List.of(capped,fixed,unmet,future,expired,off));
        var result=service.checkoutOptions(null,new BigDecimal("10000000"));
        assertEquals(3,result.size());assertEquals("CAP",result.get(0).code());assertTrue(result.get(0).best());
        assertEquals(0,new BigDecimal("1200000").compareTo(result.get(0).discountAmount()));
        assertFalse(result.get(1).best());assertFalse(result.get(2).eligible());
        assertEquals(0,new BigDecimal("10000000").compareTo(result.get(2).missingAmount()));
    }
}
