package com.example.demodatn.dto.hotro;
import jakarta.validation.constraints.*;
public record SupportChatRequest(
    @NotBlank @Pattern(regexp="[A-Za-z0-9_-]{8,64}",message="Mã tin nhắn không hợp lệ.") String clientId,
    @NotBlank @Pattern(regexp="VAN_BAN|ANH|SAN_PHAM|DON_HANG",message="Loại tin nhắn không hợp lệ.") String type,
    @Size(max=2000,message="Tin nhắn tối đa 2000 ký tự.") String content,
    Integer productId,Integer orderId) {}
