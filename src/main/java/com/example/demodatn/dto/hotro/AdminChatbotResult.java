package com.example.demodatn.dto.hotro;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminChatbotResult {
    private ChatbotMessageResponse response;
    private String toolName;
}
