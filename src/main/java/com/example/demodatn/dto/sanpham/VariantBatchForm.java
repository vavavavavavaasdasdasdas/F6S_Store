package com.example.demodatn.dto.sanpham;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class VariantBatchForm {
    private Integer productId;
    @NotEmpty(message="Chọn ít nhất một kích cỡ.")
    @Size(max=100,message="Chọn tối đa 100 kích cỡ.")
    private List<Integer> sizeIds=new ArrayList<>();
    private Integer colorId;
    private String newColorName;
    private Integer materialId;
    private String newMaterialName;
    private Integer soleId;
    private String newSoleName;
    @NotNull(message="Nhập giá.") @DecimalMin(value="1",message="Giá phải lớn hơn 0.")
    @Digits(integer=16,fraction=2,message="Giá tối đa 16 chữ số.")
    private BigDecimal price;
    @NotNull(message="Nhập tồn kho đầu.") @Min(value=0,message="Tồn kho không được âm.")
    private Integer quantity=0;
}
