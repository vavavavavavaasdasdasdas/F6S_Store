package com.example.demodatn.dto.donhang;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CheckoutRequest extends com.example.demodatn.dto.donhang.DeliveryAddressForm {
    private Integer shippingAddressId;
    @NotBlank(message = "Vui lòng nhập họ tên người nhận")
    private String receiverName;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    private String phone;

    @NotBlank(message = "Vui lòng nhập thư điện tử")
    private String email;

    private String shippingAddress;

    @NotBlank(message = "Vui lòng chọn phương thức thanh toán")
    private String paymentMethod;

    private String voucherCode;
}
