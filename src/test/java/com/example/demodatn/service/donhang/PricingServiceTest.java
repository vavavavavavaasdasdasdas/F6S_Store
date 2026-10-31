package com.example.demodatn.service.donhang;
import com.example.demodatn.dto.donhang.PriceInfo;
import com.example.demodatn.entity.khuyenmai.Promotion;
import com.example.demodatn.entity.khuyenmai.PromotionDetail;
import com.example.demodatn.entity.sanpham.Product;
import com.example.demodatn.entity.sanpham.ProductVariant;
import com.example.demodatn.repository.khuyenmai.PromotionDetailRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PricingServiceTest {

    @Mock
    private PromotionDetailRepository promotionDetailRepository;

    @org.mockito.Spy
    private com.example.demodatn.service.khuyenmai.DiscountEffectivity effectivity=new com.example.demodatn.service.khuyenmai.DiscountEffectivity(java.time.Clock.system(java.time.ZoneId.of("Asia/Ho_Chi_Minh")));

    @InjectMocks
    private PricingService pricingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private ProductVariant createVariant(BigDecimal price) {
        Product p = new Product();
        p.setId(1);
        ProductVariant v = new ProductVariant();
        v.setProduct(p);
        v.setPrice(price);
        return v;
    }

    private PromotionDetail createDetail(Promotion promo) {
        PromotionDetail detail = new PromotionDetail();
        detail.setPromotion(promo);
        return detail;
    }

    @Test
    void testNoPromotion() {
        ProductVariant variant = createVariant(new BigDecimal("100000"));
        when(promotionDetailRepository.findByProductId(1)).thenReturn(new ArrayList<>());

        PriceInfo info = pricingService.calculatePrice(variant);

        assertEquals(0, new BigDecimal("100000").compareTo(info.getOriginalPrice()));
        assertEquals(0, new BigDecimal("100000").compareTo(info.getFinalPrice()));
        assertFalse(info.isHasPromotion());
    }

    @Test
    void testOnePromotion() {
        ProductVariant variant = createVariant(new BigDecimal("100000"));
        Promotion p = new Promotion(1, "10% off", "PERCENT", new BigDecimal("10"), LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1), true);
        when(promotionDetailRepository.findByProductId(1)).thenReturn(List.of(createDetail(p)));

        PriceInfo info = pricingService.calculatePrice(variant);

        assertEquals(0, new BigDecimal("90000").compareTo(info.getFinalPrice()));
        assertEquals(10, info.getDiscountPercentage());
        assertTrue(info.isHasPromotion());
    }

    @Test
    void testMultiplePromotionsTakesHighest() {
        ProductVariant variant = createVariant(new BigDecimal("100000"));
        Promotion p1 = new Promotion(1, "10% off", "PERCENT", new BigDecimal("10"), LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1), true);
        Promotion p2 = new Promotion(2, "20% off", "PERCENT", new BigDecimal("20"), LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1), true);
        when(promotionDetailRepository.findByProductId(1)).thenReturn(List.of(createDetail(p1), createDetail(p2)));

        PriceInfo info = pricingService.calculatePrice(variant);

        assertEquals(0, new BigDecimal("80000").compareTo(info.getFinalPrice()));
        assertEquals(20, info.getDiscountPercentage());
        assertTrue(info.isHasPromotion());
    }

    @Test
    void testExpiredPromotion() {
        ProductVariant variant = createVariant(new BigDecimal("100000"));
        Promotion p = new Promotion(1, "10% off", "PERCENT", new BigDecimal("10"), LocalDateTime.now().minusDays(5), LocalDateTime.now().minusDays(1), true);
        when(promotionDetailRepository.findByProductId(1)).thenReturn(List.of(createDetail(p)));

        PriceInfo info = pricingService.calculatePrice(variant);

        assertEquals(0, new BigDecimal("100000").compareTo(info.getFinalPrice()));
        assertFalse(info.isHasPromotion());
    }

    @Test
    void testInactivePromotion() {
        ProductVariant variant = createVariant(new BigDecimal("100000"));
        Promotion p = new Promotion(1, "10% off", "PERCENT", new BigDecimal("10"), LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1), false);
        when(promotionDetailRepository.findByProductId(1)).thenReturn(List.of(createDetail(p)));

        PriceInfo info = pricingService.calculatePrice(variant);

        assertEquals(0, new BigDecimal("100000").compareTo(info.getFinalPrice()));
        assertFalse(info.isHasPromotion());
    }
}
