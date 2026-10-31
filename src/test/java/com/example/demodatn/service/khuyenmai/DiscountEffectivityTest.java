package com.example.demodatn.service.khuyenmai;

import com.example.demodatn.entity.khuyenmai.*;
import com.example.demodatn.entity.sanpham.*;
import com.example.demodatn.repository.khuyenmai.*;
import com.example.demodatn.service.donhang.PricingService;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DiscountEffectivityTest {
 DiscountEffectivity at(String time){return new DiscountEffectivity(Clock.fixed(LocalDateTime.parse(time).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(),ZoneId.of("UTC")));}
 @Test void scheduledVoucherStartsWithoutMutationAndEndsAfterWholeFinalDay(){
  var v=new Voucher();v.setStatus(true);v.setStartDate(LocalDateTime.parse("2026-11-01T00:00:00"));v.setEndDate(LocalDateTime.parse("2026-11-02T00:00:00"));
  assertEquals("UPCOMING",at("2026-10-31T23:59:59").voucherState(v));
  assertEquals("RUNNING",at("2026-11-01T00:00:00").voucherState(v));
  assertEquals("RUNNING",at("2026-11-02T23:59:59.999999999").voucherState(v));
  assertEquals("EXPIRED",at("2026-11-03T00:00:00").voucherState(v));v.setStatus(false);
  assertEquals("INACTIVE",at("2026-11-01T12:00:00").voucherState(v));
  System.out.println("GD11 Clock: upcoming -> running -> final day 23:59:59.999999999 running -> next midnight expired; timezone Asia/Ho_Chi_Minh");
 }
 @Test void checkoutAndPosUseSameClockAndIgnoreRetiredPrivateLimitFields(){
  var v=new Voucher();v.setCode("CLOCK");v.setStatus(true);v.setStartDate(LocalDateTime.parse("2026-11-01T00:00:00"));v.setEndDate(LocalDateTime.parse("2026-11-01T00:00:00"));v.setDiscountType("FIXED");v.setDiscountValue(new BigDecimal("500000"));v.setUsageLimit(1);v.setUsedCount(99);v.setIsPublic(false);
  var repo=mock(VoucherRepository.class);when(repo.findByCodeIgnoreCase("CLOCK")).thenReturn(java.util.Optional.of(v));
  var before=new VoucherService(repo,at("2026-10-31T23:59:59"));assertFalse(before.validateAndCalculate("CLOCK",null,new BigDecimal("200000")).valid);
  var after=new VoucherService(repo,at("2026-11-01T12:00:00"));var result=after.validateAndCalculate("CLOCK",null,new BigDecimal("200000"));assertTrue(result.valid);assertEquals(0,new BigDecimal("200000").compareTo(result.discountAmount));assertEquals(99,v.getUsedCount());
 }
 Promotion promo(String type,String value){return new Promotion(1,"Giảm thử",type,new BigDecimal(value),LocalDateTime.parse("2026-10-01T00:00:00"),LocalDateTime.parse("2026-12-01T00:00:00"),true);}
 com.example.demodatn.dto.donhang.PriceInfo price(Promotion... promotions){
  var p=new Product();p.setId(1);var v=new ProductVariant();v.setProduct(p);v.setPrice(new BigDecimal("100000"));
  var repo=mock(PromotionDetailRepository.class);when(repo.findByProductId(1)).thenReturn(java.util.Arrays.stream(promotions).map(promo->{var d=new PromotionDetail();d.setPromotion(promo);return d;}).toList());
  return new PricingService(repo,null,at("2026-11-01T12:00:00")).calculatePrice(v);
 }
 @Test void mixedPromotionsChooseLargestActualMoneyDiscount(){
  assertEquals(0,new BigDecimal("85000").compareTo(price(promo("PERCENT","10"),promo("AMOUNT","15000")).getFinalPrice()));
  assertEquals(0,new BigDecimal("80000").compareTo(price(promo("PERCENT","20"),promo("AMOUNT","15000")).getFinalPrice()));
 }
 @Test void fixedDiscountClampsAtPriceAndIgnoresDisabledExpiredNegative(){
  var capped=price(promo("AMOUNT","500000"));assertEquals(0,capped.getFinalPrice().signum());assertEquals(100,capped.getDiscountPercentage());
  var disabled=promo("AMOUNT","90000");disabled.setStatus(false);var expired=promo("AMOUNT","80000");expired.setEndDate(LocalDateTime.parse("2026-10-31T23:59:59"));
  assertEquals(0,new BigDecimal("100000").compareTo(price(disabled,expired,promo("AMOUNT","-1000")).getFinalPrice()));
 }
}
