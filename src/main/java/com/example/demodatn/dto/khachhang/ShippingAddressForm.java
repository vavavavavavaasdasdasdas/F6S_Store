package com.example.demodatn.dto.khachhang;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ShippingAddressForm {
    private Integer id;
    @jakarta.validation.constraints.NotNull(message="Vui lòng chọn Tỉnh/Thành phố.") private Integer provinceId;
    @NotBlank(message="Nhập tên người nhận.") @Size(max=100,message="Tên người nhận tối đa 100 ký tự.")
    private String receiverName;
    @NotBlank(message="Nhập số điện thoại.")
    @Pattern(regexp="(?:0[0-9]{9}|\\+84[0-9]{9})",message="Số điện thoại gồm 0 và 9 số hoặc +84 và 9 số.")
    private String phone;
    @NotBlank(message="Nhập địa chỉ nhận hàng.") @Size(max=255,message="Địa chỉ tối đa 255 ký tự.")
    private String address;
    private boolean makeDefault;
}
