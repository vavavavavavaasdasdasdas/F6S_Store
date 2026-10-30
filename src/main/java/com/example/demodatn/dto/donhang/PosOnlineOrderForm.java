package com.example.demodatn.dto.donhang;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class PosOnlineOrderForm extends DeliveryAddressForm {
    @NotBlank(message="Nhập họ tên người nhận.") @Size(max=100,message="Họ tên tối đa 100 ký tự.") private String receiverName;
    @NotBlank(message="Nhập số điện thoại.") @Pattern(regexp="(?:0[0-9]{9}|[+]84[0-9]{9})",message="SĐT gồm 0 và 9 số hoặc +84 và 9 số.") private String phone;
    @NotBlank(message="Chọn nguồn đơn.") @Pattern(regexp="FACEBOOK|ZALO|TIKTOK|INSTAGRAM|PHONE|OTHER",message="Nguồn đơn không hợp lệ.") private String orderSource;
    @Size(max=1000,message="Ghi chú tối đa 1000 ký tự.") private String note;
    @NotBlank(message="Chọn phương thức thanh toán.") @Pattern(regexp="COD|BANK_TRANSFER",message="Chọn COD hoặc Chuyển khoản.") private String paymentMethod="COD";
    @Size(max=50,message="Mã phiếu giảm giá tối đa 50 ký tự.") private String voucherCode;
    private String checkoutToken;
}
