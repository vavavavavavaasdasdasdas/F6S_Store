package com.example.demodatn.dto.donhang;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
@Getter @Setter
public class DeliveryAddressForm {
    @NotNull(message="Vui lòng chọn Tỉnh/Thành phố.") private Integer provinceId;
    @NotNull(message="Vui lòng chọn Phường/Xã.") private Integer wardId;
    @NotBlank(message="Vui lòng nhập số nhà, tên đường.") @Size(max=255,message="Số nhà, tên đường tối đa 255 ký tự.") private String streetAddress;
    @DecimalMin(value="-90",message="Vĩ độ không hợp lệ.") @DecimalMax(value="90",message="Vĩ độ không hợp lệ.") private BigDecimal latitude;
    @DecimalMin(value="-180",message="Kinh độ không hợp lệ.") @DecimalMax(value="180",message="Kinh độ không hợp lệ.") private BigDecimal longitude;
    @Pattern(regexp="(?:GPS|GHIM_TAY|TAM_PHUONG_XA)?",message="Nguồn tọa độ không hợp lệ.") private String coordinateSource;
}
