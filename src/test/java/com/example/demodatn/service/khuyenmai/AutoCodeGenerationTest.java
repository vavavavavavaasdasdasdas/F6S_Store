package com.example.demodatn.service.khuyenmai;

import com.example.demodatn.dto.khuyenmai.PromotionDto;
import com.example.demodatn.dto.khuyenmai.VoucherDto;
import com.example.demodatn.entity.khuyenmai.Promotion;
import com.example.demodatn.entity.khuyenmai.Voucher;
import com.example.demodatn.repository.khuyenmai.PromotionRepository;
import com.example.demodatn.repository.khuyenmai.VoucherRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AutoCodeGenerationTest {

    @Autowired
    private DiscountAdminService discountAdminService;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private PromotionRepository promotionRepository;

    @Test
    @Transactional
    void testNextVoucherCodeFormat() {
        String nextCode = discountAdminService.nextVoucherCode();
        assertNotNull(nextCode);
        assertTrue(nextCode.matches("^PGG\\d{5}$"), "Voucher code must follow PGG%05d format: " + nextCode);
    }

    @Test
    @Transactional
    void testNextPromotionCodeFormat() {
        String nextCode = discountAdminService.nextPromotionCode();
        assertNotNull(nextCode);
        assertTrue(nextCode.matches("^KM\\d{3,}$"), "Promotion code must follow KM%03d format: " + nextCode);
    }

    @Test
    @Transactional
    void testAutoGenerateVoucherCodeWhenNullOrEmpty() {
        VoucherDto dto = new VoucherDto();
        dto.setCode(""); // để trống
        dto.setName("Voucher Test Tự Sinh Mã");
        dto.setDiscountType("PERCENT");
        dto.setDiscountValue(BigDecimal.valueOf(10));
        dto.setStartDate(LocalDateTime.now().minusDays(1));
        dto.setEndDate(LocalDateTime.now().plusDays(10));
        dto.setStatus(true);

        Voucher saved = discountAdminService.saveVoucher(dto);
        assertNotNull(saved.getId());
        assertNotNull(saved.getCode());
        assertTrue(saved.getCode().matches("^PGG\\d{5}$"), "Generated voucher code: " + saved.getCode());
    }

    @Test
    @Transactional
    void testAutoGeneratePromotionCode() {
        PromotionDto dto = new PromotionDto();
        dto.setName("Khuyến Mãi Test Tự Sinh Mã");
        dto.setDiscountType("PERCENT");
        dto.setDiscountValue(BigDecimal.valueOf(15));
        dto.setStartDate(LocalDateTime.now().minusDays(1));
        dto.setEndDate(LocalDateTime.now().plusDays(10));
        dto.setStatus(true);

        Promotion saved = discountAdminService.savePromotion(dto);
        assertNotNull(saved.getId());
        assertNotNull(saved.getCode());
        assertTrue(saved.getCode().matches("^KM\\d{3,}$"), "Generated promotion code: " + saved.getCode());
    }
}
