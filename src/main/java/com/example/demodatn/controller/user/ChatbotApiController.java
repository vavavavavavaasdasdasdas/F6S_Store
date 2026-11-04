package com.example.demodatn.controller.user;

import com.example.demodatn.dto.hotro.ChatbotMessageRequest;
import com.example.demodatn.dto.hotro.ChatbotMessageResponse;
import com.example.demodatn.dto.hotro.SupportTicketForm;
import com.example.demodatn.entity.hotro.SupportTicket;
import com.example.demodatn.service.hotro.ChatbotService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotApiController {

    private final ChatbotService chatbotService;

    @PostMapping("/message")
    public ResponseEntity<ChatbotMessageResponse> sendMessage(@Valid @RequestBody ChatbotMessageRequest request,
                                                              HttpServletRequest httpRequest,
                                                              Authentication authentication) {
        String clientIp = getClientIp(httpRequest);
        ChatbotMessageResponse response = chatbotService.handleMessage(request, clientIp, authentication);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/clear")
    public ResponseEntity<?> clearChat(@RequestBody Map<String, String> body) {
        String sessionId = body.get("sessionId");
        if (sessionId != null && !sessionId.isBlank()) {
            chatbotService.clearSession(sessionId);
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/ticket")
    public ResponseEntity<?> createTicket(@Valid @RequestBody SupportTicketForm form,
                                          Authentication authentication) {
        SupportTicket ticket = chatbotService.createSupportTicket(form, authentication);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "ticketCode", ticket.getMa(),
                "message", "Phiếu hỗ trợ #" + ticket.getMa() + " đã được tạo thành công! Nhân viên F6S Store sẽ liên hệ lại với bạn sớm nhất có thể."
        ));
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
