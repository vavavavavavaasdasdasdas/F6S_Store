package com.example.demodatn.dto.sanpham;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class VariantForm {
    private Integer id;
    @NotNull(message="Chọn sản phẩm.") private Integer productId;
    @NotNull(message="Chọn kích cỡ.") private Integer sizeId;
    private String newSizeValue;
    @NotNull(message="Chọn chất liệu.") private Integer materialId;
    private String newMaterialName;
    private Integer colorId;
    private String newColorName;
    private Integer soleId;
    private String newSoleName;
    @NotNull(message="Nhập giá.") @DecimalMin(value="1", message="Giá phải lớn hơn 0.")
    @Digits(integer=16,fraction=2,message="Giá tối đa 16 chữ số.") private BigDecimal price;
    @NotNull(message="Nhập số lượng.") @Min(value=0,message="Số lượng không được âm.") private Integer quantity = 0;
    @NotNull(message="Chọn trạng thái.") private Boolean status = true;
    @jakarta.validation.constraints.Size(max=500,message="Lý do tối đa 500 ký tự.") private String reason;
    private Integer expectedQuantity;
}
