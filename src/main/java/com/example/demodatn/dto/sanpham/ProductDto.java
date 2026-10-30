package com.example.demodatn.dto.sanpham;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class ProductDto {
    private Integer id;
    
    @NotBlank(message = "Tên không được để trống")
    private String name;
    
    @NotBlank(message = "Mã không được để trống")
    private String code;
    
    private String imageUrl;
    
    @NotNull(message = "Thương hiệu không được để trống")
    private Integer categoryId;
    
    private String categoryName;
    private String description;
    private Boolean status;
    
    private BigDecimal minOriginalPrice;
    private BigDecimal maxOriginalPrice;
    private BigDecimal minFinalPrice;
    private BigDecimal maxFinalPrice;
    private int discountPercentage;
    private boolean hasPromotion;
    private int totalStock;
    private long totalSold;
    
    private List<VariantDto> variants;
    public String getMaterialNames() {
        return variants == null ? "" : variants.stream().map(VariantDto::getMaterialName)
                .filter(java.util.Objects::nonNull).distinct().sorted().collect(java.util.stream.Collectors.joining(", "));
    }
    
    @Data
    public static class VariantDto {
        private Integer id;
        private String sizeValue;
        private Integer sizeId;
        private Integer materialId;
        private String materialName;
        private Integer colorId;
        private String colorName;
        private String colorHexCode;
        private Integer soleId;
        private String soleName;
        private int quantity;
    }
}
