package com.example.demodatn.dto.khachhang;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CustomerDto {
    private Integer id;
    @Pattern(regexp="(?:|[A-Za-z0-9_.-]{3,50})",message="Tên đăng nhập gồm 3–50 chữ, số, dấu chấm, gạch dưới hoặc gạch ngang.")
    private String username;
    @NotBlank(message="Nhập mã.")
    @Pattern(regexp="[A-Za-z0-9_-]{1,20}",message="Mã gồm tối đa 20 chữ, số, dấu - hoặc _.")
    private String customerCode;
    @NotBlank(message="Nhập họ tên.") @Size(max=100,message="Họ tên tối đa 100 ký tự.")
    private String fullName;
    @NotBlank(message="Nhập số điện thoại.")
    @Pattern(regexp="(?:0[0-9]{9}|\\+84[0-9]{9})",message="SĐT gồm 10 số bắt đầu bằng 0 hoặc +84 và 9 số.")
    private String phone;
    @NotBlank(message="Nhập thư điện tử.") @Email(message="Thư điện tử không hợp lệ.") @Size(max=100,message="Thư điện tử tối đa 100 ký tự.")
    private String email;
    @Size(max=255,message="Địa chỉ tối đa 255 ký tự.") private String address;
    @Pattern(regexp="(?:|[0-9]{12})",message="Số căn cước phải gồm đúng 12 chữ số.")
    @lombok.ToString.Exclude private String soCccd;
    @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    @PastOrPresent(message="Ngày sinh không được ở tương lai.") private java.time.LocalDate ngaySinh;
    @Pattern(regexp="(?:|Nam|Nữ|Khác)",message="Chọn giới tính hợp lệ.") private String gioiTinh;
}
