package com.example.demodatn.dto.donhang;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PosCheckoutRequest {
    private Integer customerId;
    private boolean transferReceived;
    private BigDecimal transferAmount;
    private String paymentReference;
    @NotBlank(message = "Chọn phương thức thanh toán.")
    @Pattern(regexp = "CASH|TRANSFER", message = "Phương thức thanh toán không hợp lệ.")
    private String paymentMethod;
    private String voucherCode;
    @DecimalMin(value = "0", message = "Tiền khách đưa không được âm.")
    @Digits(integer = 15, fraction = 2, message = "Số tiền không hợp lệ.")
    private BigDecimal cashReceived;
    private String checkoutToken;
    @Size(max=500,message="Ghi chú tối đa 500 ký tự.")
    private String note;
}
