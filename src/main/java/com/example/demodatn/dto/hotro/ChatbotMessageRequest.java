package com.example.demodatn.dto.hotro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotMessageRequest {
    @NotBlank(message = "Tin nhắn không được để trống.")
    @Size(max = 500, message = "Tin nhắn tối đa 500 ký tự.")
    private String message;

    private String sessionId;
}
