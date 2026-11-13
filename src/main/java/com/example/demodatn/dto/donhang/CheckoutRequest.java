package com.example.demodatn.dto.donhang;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CheckoutRequest extends com.example.demodatn.dto.donhang.DeliveryAddressForm {
    private Integer shippingAddressId;
    private boolean saveAddress;
    @NotBlank(message = "Vui lòng nhập họ tên người nhận")
    private String receiverName;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @jakarta.validation.constraints.Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Số điện thoại không đúng định dạng")
    private String phone;

    @NotBlank(message = "Vui lòng nhập thư điện tử")
    @jakarta.validation.constraints.Email(message = "Địa chỉ email không đúng định dạng")
    private String email;

    private String shippingAddress;

    @NotBlank(message = "Vui lòng chọn phương thức thanh toán")
    private String paymentMethod;

    private String voucherCode;
}
