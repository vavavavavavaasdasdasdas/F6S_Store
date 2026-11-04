package com.example.demodatn.service.hotro;

import com.example.demodatn.dto.hotro.AdminChatbotResult;
import com.example.demodatn.dto.hotro.ChatbotMessageRequest;
import com.example.demodatn.dto.hotro.ChatbotMessageResponse;
import com.example.demodatn.entity.hotro.AdminChatAudit;
import com.example.demodatn.entity.hotro.AdminChatConversation;
import com.example.demodatn.entity.hotro.AdminChatMessage;
import com.example.demodatn.repository.hotro.AdminChatAuditRepository;
import com.example.demodatn.repository.hotro.AdminChatConversationRepository;
import com.example.demodatn.repository.hotro.AdminChatMessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AdminChatbotService {

    private final AdminChatbotEngine rulesEngine;
    private final AdminChatbotEngine llmEngine;
    private final AdminChatConversationRepository conversationRepository;
    private final AdminChatMessageRepository messageRepository;
    private final AdminChatAuditRepository auditRepository;

    @Value("${shop.chatbot.provider:${CHATBOT_PROVIDER:RULES}}")
    private String configuredProvider;

    // Rate limiter: Session -> [WindowMinute, Count]
    private final Map<String, int[]> sessionRateMap = new ConcurrentHashMap<>();

    private static final Pattern CARD_PATTERN = Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("(?i)(?:mật khẩu|mat khau|password)\\s*(?:là|la|:)?\\s*(\\S+)");

    public AdminChatbotService(@Qualifier("adminRulesChatbotEngine") AdminChatbotEngine rulesEngine,
                               @Qualifier("adminLlmChatbotEngine") AdminChatbotEngine llmEngine,
                               AdminChatConversationRepository conversationRepository,
                               AdminChatMessageRepository messageRepository,
                               AdminChatAuditRepository auditRepository) {
        this.rulesEngine = rulesEngine;
        this.llmEngine = llmEngine;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.auditRepository = auditRepository;
    }

    public void setConfiguredProvider(String provider) {
        this.configuredProvider = provider;
    }

    public String getConfiguredProvider() {
        return configuredProvider;
    }

    @Transactional
    public ChatbotMessageResponse handleMessage(ChatbotMessageRequest request, String ipAddress, Authentication authentication) {
        String msg = request.getMessage() != null ? request.getMessage().trim() : "";
        if (msg.length() > 500) {
            return ChatbotMessageResponse.builder()
                    .text("Câu hỏi quá dài (tối đa 500 ký tự). Vui lòng tóm tắt ngắn gọn hơn.")
                    .build();
        }

        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = "admin_" + UUID.randomUUID().toString();
        }

        // Rate limit: tối đa 60 câu hỏi / phút cho mỗi phiên làm việc
        if (!checkRateLimit(sessionId)) {
            return ChatbotMessageResponse.builder()
                    .text("Bạn đang gửi yêu cầu quá nhanh. Vui lòng đợi trong giây lát.")
                    .build();
        }

        String username = authentication != null ? authentication.getName() : "anonymous";
        String roleStr = authentication != null
                ? authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(","))
                : "NONE";

        // 1. Tìm hoặc tạo hội thoại
        final String finalSessionId = sessionId;
        AdminChatConversation conversation = conversationRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    AdminChatConversation c = new AdminChatConversation();
                    c.setSessionId(finalSessionId);
                    c.setUsername(username);
                    c.setCreatedAt(LocalDateTime.now());
                    c.setUpdatedAt(LocalDateTime.now());
                    return conversationRepository.save(c);
                });

        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        // 2. Lưu tin nhắn người dùng
        AdminChatMessage userMessage = new AdminChatMessage();
        userMessage.setConversation(conversation);
        userMessage.setSender("USER");
        userMessage.setNoiDung(maskSensitiveInfo(msg));
        userMessage.setCreatedAt(LocalDateTime.now());
        messageRepository.save(userMessage);

        // 3. Chọn Engine xử lý (LLM hoặc RULES)
        AdminChatbotEngine engine = "LLM".equalsIgnoreCase(configuredProvider) ? llmEngine : rulesEngine;
        AdminChatbotResult result = engine.process(msg, sessionId, authentication);

        // 4. Ghi nhật ký Audit (người, thời gian, công cụ đã gọi, tóm tắt câu hỏi, không lưu số liệu nhạy cảm)
        try {
            AdminChatAudit audit = new AdminChatAudit();
            audit.setUsername(username);
            audit.setRole(roleStr);
            audit.setThoiGian(LocalDateTime.now());
            String summary = maskSensitiveInfo(msg);
            if (summary.length() > 500) summary = summary.substring(0, 500);
            audit.setCauHoiTomTat(summary);
            audit.setCongCuDaGoi(result.getToolName() != null ? result.getToolName() : "UNKNOWN");
            audit.setSessionId(sessionId);
            auditRepository.save(audit);
        } catch (Exception ex) {
            log.error("Lỗi khi ghi nhật ký AdminChatAudit: {}", ex.getMessage());
        }

        // 5. Lưu tin nhắn bot
        AdminChatMessage botMessage = new AdminChatMessage();
        botMessage.setConversation(conversation);
        botMessage.setSender("BOT");
        botMessage.setNoiDung(result.getResponse().getText());
        botMessage.setCreatedAt(LocalDateTime.now());
        messageRepository.save(botMessage);

        return result.getResponse();
    }

    @Transactional
    public void clearConversation(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return;
        conversationRepository.findBySessionId(sessionId).ifPresent(c -> {
            messageRepository.deleteAll(messageRepository.findAllBySessionId(sessionId));
            conversationRepository.delete(c);
        });
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getConversationHistory(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return Collections.emptyList();
        List<AdminChatMessage> messages = messageRepository.findAllBySessionId(sessionId);
        List<Map<String, Object>> history = new ArrayList<>();
        for (AdminChatMessage m : messages) {
            history.add(Map.of(
                    "sender", m.getSender(),
                    "text", m.getNoiDung(),
                    "time", m.getCreatedAt().toString()
            ));
        }
        return history;
    }

    private boolean checkRateLimit(String sessionId) {
        int currentMinute = (int) (System.currentTimeMillis() / 60000L);
        int[] rate = sessionRateMap.computeIfAbsent(sessionId, k -> new int[]{currentMinute, 0});
        synchronized (rate) {
            if (rate[0] != currentMinute) {
                rate[0] = currentMinute;
                rate[1] = 0;
            }
            if (rate[1] >= 60) {
                return false;
            }
            rate[1]++;
            return true;
        }
    }

    private String maskSensitiveInfo(String input) {
        if (input == null) return "";
        String masked = CARD_PATTERN.matcher(input).replaceAll("****-****-****-****");
        masked = PASSWORD_PATTERN.matcher(masked).replaceAll("mat khau: [ĐÃ ẨN]");
        return masked;
    }
}
