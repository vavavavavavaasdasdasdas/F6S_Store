package com.example.demodatn.dto.donhang;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDto {
    @NotNull(message = "Product ID is required")
    private Integer productId;

    @NotNull(message = "Variant ID is required")
    private Integer variantId;

    private String productName;
    private String size;
    private String materialName;
    private String colorName;
    private String soleName;

    public CartItemDto(Integer productId,Integer variantId,String productName,String size,String materialName,BigDecimal price,Integer quantity) {
        this.productId=productId;this.variantId=variantId;this.productName=productName;this.size=size;this.materialName=materialName;this.price=price;this.quantity=quantity;
    }

    public CartItemDto(Integer productId, Integer variantId, String productName, String size, BigDecimal price, Integer quantity) {
        this(productId,variantId,productName,size,null,price,quantity);
    }

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price must be non-negative")
    private BigDecimal price;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    public BigDecimal getTotalPrice() {
        if (price != null && quantity != null) {
            return price.multiply(new BigDecimal(quantity));
        }
        return BigDecimal.ZERO;
    }
}
