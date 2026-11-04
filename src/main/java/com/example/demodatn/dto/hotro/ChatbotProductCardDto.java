package com.example.demodatn.dto.hotro;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotProductCardDto {
    private Integer id;
    private String code;
    private String name;
    private String imageUrl;
    private BigDecimal originalPrice;
    private BigDecimal finalPrice;
    private int discountPercent;
    private int stock;
    private String productUrl;
}
