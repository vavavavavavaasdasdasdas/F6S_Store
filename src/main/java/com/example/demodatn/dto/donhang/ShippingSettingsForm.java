package com.example.demodatn.dto.donhang;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
@Getter @Setter
public class ShippingSettingsForm {
    @NotBlank(message="Nhập địa chỉ kho.") @Size(max=255,message="Địa chỉ kho tối đa 255 ký tự.") private String originAddress;
    @NotNull(message="Nhập vĩ độ kho.") @DecimalMin(value="-90",message="Giá trị nhỏ hơn giới hạn cho phép.") @DecimalMax(value="90",message="Giá trị vượt giới hạn cho phép.") private BigDecimal originLatitude;
    @NotNull(message="Nhập kinh độ kho.") @DecimalMin(value="-180",message="Giá trị nhỏ hơn giới hạn cho phép.") @DecimalMax(value="180",message="Giá trị vượt giới hạn cho phép.") private BigDecimal originLongitude;
    @NotNull(message="Nhập mốc khoảng cách đầu.") @DecimalMin(value="0",message="Giá trị nhỏ hơn giới hạn cho phép.") private BigDecimal firstLimitKm;
    @NotNull(message="Nhập mốc khoảng cách thứ hai.") @DecimalMin(value="0",message="Giá trị nhỏ hơn giới hạn cho phép.") private BigDecimal secondLimitKm;
    @NotNull(message="Nhập phí nấc đầu.") @DecimalMin(value="0",message="Giá trị nhỏ hơn giới hạn cho phép.") private BigDecimal firstFee;
    @NotNull(message="Nhập phí nấc thứ hai.") @DecimalMin(value="0",message="Giá trị nhỏ hơn giới hạn cho phép.") private BigDecimal secondFee;
    @NotNull(message="Nhập phí xa/tạm tính.") @DecimalMin(value="0",message="Giá trị nhỏ hơn giới hạn cho phép.") private BigDecimal fallbackFee;
    @Min(value=1,message="Ngưỡng miễn phí từ 1 đến 1000 sản phẩm.") @Max(value=1000,message="Ngưỡng miễn phí từ 1 đến 1000 sản phẩm.") private int freeQuantity;
}
