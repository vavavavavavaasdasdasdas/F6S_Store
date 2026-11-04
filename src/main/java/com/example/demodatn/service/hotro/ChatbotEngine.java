package com.example.demodatn.service.hotro;

import com.example.demodatn.dto.hotro.ChatbotMessageResponse;
import org.springframework.security.core.Authentication;

public interface ChatbotEngine {
    ChatbotMessageResponse process(String message, String sessionId, Authentication authentication);
}
