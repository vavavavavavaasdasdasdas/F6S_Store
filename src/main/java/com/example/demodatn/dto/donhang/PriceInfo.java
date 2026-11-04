package com.example.demodatn.dto.donhang;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceInfo {
    private BigDecimal originalPrice;
    private BigDecimal finalPrice;
    private int discountPercentage;
    private boolean hasPromotion;
    private String promotionName;
    private String discountType;
    private BigDecimal discountAmount;
    private String discountLabel;

    public PriceInfo(BigDecimal originalPrice, BigDecimal finalPrice, int discountPercentage, boolean hasPromotion) {
        this(originalPrice, finalPrice, discountPercentage, hasPromotion, null, null, BigDecimal.ZERO, null);
    }
}
