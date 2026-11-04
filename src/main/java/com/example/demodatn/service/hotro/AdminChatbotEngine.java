package com.example.demodatn.service.hotro;

import com.example.demodatn.dto.hotro.AdminChatbotResult;
import org.springframework.security.core.Authentication;

public interface AdminChatbotEngine {
    AdminChatbotResult process(String message, String sessionId, Authentication authentication);
}
