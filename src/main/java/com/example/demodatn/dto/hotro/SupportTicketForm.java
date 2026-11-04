package com.example.demodatn.dto.hotro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicketForm {

    @NotBlank(message = "Vui lòng nhập họ và tên.")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự.")
    private String hoTen;

    @NotBlank(message = "Vui lòng nhập số điện thoại hoặc email liên hệ.")
    @Size(max = 100, message = "Thông tin liên hệ tối đa 100 ký tự.")
    private String lienHe;

    @NotBlank(message = "Vui lòng nhập nội dung cần hỗ trợ.")
    @Size(max = 1000, message = "Nội dung tối đa 1000 ký tự.")
    private String noiDung;

    private String sessionId;
}
