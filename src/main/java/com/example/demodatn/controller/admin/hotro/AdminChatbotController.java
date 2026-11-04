package com.example.demodatn.controller.admin.hotro;

import com.example.demodatn.dto.hotro.ChatbotMessageRequest;
import com.example.demodatn.dto.hotro.ChatbotMessageResponse;
import com.example.demodatn.service.hotro.AdminChatbotService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/chatbot")
@RequiredArgsConstructor
public class AdminChatbotController {

    private final AdminChatbotService adminChatbotService;

    @PostMapping("/message")
    public ResponseEntity<ChatbotMessageResponse> sendMessage(@RequestBody ChatbotMessageRequest request,
                                                              HttpServletRequest httpRequest,
                                                              Authentication authentication) {
        String clientIp = httpRequest.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = httpRequest.getRemoteAddr();
        } else {
            clientIp = clientIp.split(",")[0].trim();
        }

        ChatbotMessageResponse response = adminChatbotService.handleMessage(request, clientIp, authentication);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/clear")
    public ResponseEntity<Map<String, String>> clearConversation(@RequestBody(required = false) Map<String, String> payload) {
        String sessionId = payload != null ? payload.get("sessionId") : null;
        if (sessionId != null) {
            adminChatbotService.clearConversation(sessionId);
        }
        return ResponseEntity.ok(Map.of("status", "cleared"));
    }

    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getHistory(@RequestParam(name = "sessionId", required = false) String sessionId) {
        return ResponseEntity.ok(adminChatbotService.getConversationHistory(sessionId));
    }
}
