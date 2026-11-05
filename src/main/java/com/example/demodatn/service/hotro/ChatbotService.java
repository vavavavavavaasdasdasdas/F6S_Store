package com.example.demodatn.service.hotro;

import com.example.demodatn.dto.hotro.ChatbotMessageRequest;
import com.example.demodatn.dto.hotro.ChatbotMessageResponse;
import com.example.demodatn.dto.hotro.SupportTicketForm;
import com.example.demodatn.entity.hotro.ChatConversation;
import com.example.demodatn.entity.hotro.ChatMessage;
import com.example.demodatn.entity.hotro.SupportTicket;
import com.example.demodatn.entity.khachhang.Customer;
import com.example.demodatn.repository.hotro.ChatConversationRepository;
import com.example.demodatn.repository.hotro.ChatMessageRepository;
import com.example.demodatn.repository.hotro.SupportTicketRepository;
import com.example.demodatn.repository.khachhang.CustomerRepository;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ChatbotService {

    private final ChatbotEngine rulesEngine;
    private final ChatbotEngine llmEngine;
    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final SupportTicketRepository ticketRepository;
    private final CustomerRepository customerRepository;
    private final ObjectMapper objectMapper;

    @Value("${shop.chatbot.provider:${CHATBOT_PROVIDER:RULES}}")
    private String configuredProvider;

    // Rate Limiter: Session -> (WindowMinute, Count)
    private final Map<String, int[]> sessionRateMap = new ConcurrentHashMap<>();
    // Rate Limiter: IP -> (DayOfYear, Count)
    private final Map<String, int[]> ipRateMap = new ConcurrentHashMap<>();

    // Masking Patterns
    private static final Pattern CARD_PATTERN = Pattern.compile("\\b(?:\\d[ -]*?){13,19}\\b");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("(?i)(?:mật khẩu|mat khau|password|pwd)\\s*(?:(?:của tôi|cua toi|của mình|cua minh)\\s*)?(?:là|la|:|=)?\\s*(\\S+)");

    public ChatbotService(@Qualifier("rulesChatbotEngine") ChatbotEngine rulesEngine,
                          @Qualifier("llmChatbotEngine") ChatbotEngine llmEngine,
                          ChatConversationRepository conversationRepository,
                          ChatMessageRepository messageRepository,
                          SupportTicketRepository ticketRepository,
                          CustomerRepository customerRepository,
                          ObjectMapper objectMapper) {
        this.rulesEngine = rulesEngine;
        this.llmEngine = llmEngine;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.ticketRepository = ticketRepository;
        this.customerRepository = customerRepository;
        this.objectMapper = objectMapper;
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
                    .text("Tin nhắn của bạn quá dài (tối đa 500 ký tự). Vui lòng tóm tắt ngắn gọn hơn nhé!")
                    .build();
        }

        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        // 1. Kiểm tra giới hạn tần suất (Rate Limiting)
        if (!checkRateLimit(sessionId, ipAddress)) {
            return ChatbotMessageResponse.builder()
                    .text("Bạn đang gửi tin nhắn quá nhanh. Vui lòng đợi một chút trước khi gửi tiếp nhé!")
                    .build();
        }

        // 2. Tìm hoặc tạo hội thoại
        Customer customer = null;
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            customer = customerRepository.findByUser_Username(authentication.getName()).orElse(null);
        }

        final Customer finalCustomer = customer;
        final String finalSessionId = sessionId;
        ChatConversation conversation = conversationRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    ChatConversation c = new ChatConversation();
                    c.setSessionId(finalSessionId);
                    c.setCustomer(finalCustomer);
                    c.setIpAddress(ipAddress);
                    c.setCreatedAt(LocalDateTime.now());
                    c.setUpdatedAt(LocalDateTime.now());
                    return conversationRepository.save(c);
                });

        if (customer != null && conversation.getCustomer() == null) {
            conversation.setCustomer(customer);
        }
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        // 3. Lọc số thẻ / mật khẩu trước khi lưu tin nhắn của khách
        String sanitizedUserMsg = maskSensitiveInfo(msg);
        ChatMessage userMsgEntity = new ChatMessage();
        userMsgEntity.setConversation(conversation);
        userMsgEntity.setSender("USER");
        userMsgEntity.setNoiDung(sanitizedUserMsg);
        userMsgEntity.setCreatedAt(LocalDateTime.now());
        messageRepository.save(userMsgEntity);

        // 4. Định tuyến bộ máy xử lý (RULES hoặc LLM)
        ChatbotEngine selectedEngine = "LLM".equalsIgnoreCase(configuredProvider) ? llmEngine : rulesEngine;
        ChatbotMessageResponse response = selectedEngine.process(msg, sessionId, authentication);

        // 5. Lưu tin nhắn trả lời của BOT
        ChatMessage botMsgEntity = new ChatMessage();
        botMsgEntity.setConversation(conversation);
        botMsgEntity.setSender("BOT");
        botMsgEntity.setNoiDung(response.getText() != null ? response.getText() : "");
        if (response.getProducts() != null && !response.getProducts().isEmpty()) {
            try {
                botMsgEntity.setPayloadJson(objectMapper.writeValueAsString(response.getProducts()));
            } catch (Exception ignored) {}
        }
        botMsgEntity.setCreatedAt(LocalDateTime.now());
        messageRepository.save(botMsgEntity);

        return response;
    }

    @Transactional
    public void clearSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return;
        conversationRepository.findBySessionId(sessionId).ifPresent(conversationRepository::delete);
    }

    @Transactional
    public SupportTicket createSupportTicket(SupportTicketForm form, Authentication authentication) {
        Customer customer = null;
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            customer = customerRepository.findByUser_Username(authentication.getName()).orElse(null);
        }

        // Lấy trích đoạn 10 tin nhắn gần nhất
        StringBuilder excerpt = new StringBuilder();
        if (form.getSessionId() != null && !form.getSessionId().isBlank()) {
            List<ChatMessage> recent = messageRepository.findRecentBySessionId(form.getSessionId(), PageRequest.of(0, 10));
            Collections.reverse(recent);
            for (ChatMessage m : recent) {
                excerpt.append("[").append("USER".equals(m.getSender()) ? "Khách" : "Bot").append("]: ")
                       .append(m.getNoiDung()).append("\n");
            }
        }

        // Sinh mã phiếu hỗ trợ HT0001, HT0002...
        Integer maxNum = ticketRepository.findMaxCodeNumber();
        int nextNum = (maxNum != null ? maxNum : 0) + 1;
        String code = String.format("HT%04d", nextNum);

        SupportTicket ticket = new SupportTicket();
        ticket.setMa(code);
        ticket.setCustomer(customer);
        ticket.setHoTen(form.getHoTen().trim());
        ticket.setLienHe(form.getLienHe().trim());
        ticket.setNoiDung(form.getNoiDung().trim());
        ticket.setTrichDoanChat(excerpt.toString());
        ticket.setTrangThai("Mới");
        ticket.setCreatedAt(LocalDateTime.now());

        return ticketRepository.save(ticket);
    }

    private boolean checkRateLimit(String sessionId, String ipAddress) {
        int currentMinute = (int) (System.currentTimeMillis() / 60000);
        int today = LocalDate.now().getDayOfYear();

        // Session: max 20 msgs / min
        int[] sessionData = sessionRateMap.compute(sessionId, (k, v) -> {
            if (v == null || v[0] != currentMinute) return new int[]{currentMinute, 1};
            v[1]++;
            return v;
        });
        if (sessionData[1] > 20) return false;

        // IP: max 300 msgs / day
        String ipKey = ipAddress != null ? ipAddress : "unknown";
        int[] ipData = ipRateMap.compute(ipKey, (k, v) -> {
            if (v == null || v[0] != today) return new int[]{today, 1};
            v[1]++;
            return v;
        });
        return ipData[1] <= 300;
    }

    public static String maskSensitiveInfo(String text) {
        if (text == null) return "";
        // Che số thẻ
        Matcher cardMatcher = CARD_PATTERN.matcher(text);
        String masked = cardMatcher.replaceAll("****-****-****");

        // Che mật khẩu
        Matcher passMatcher = PASSWORD_PATTERN.matcher(masked);
        StringBuffer sb = new StringBuffer();
        while (passMatcher.find()) {
            passMatcher.appendReplacement(sb, "mật khẩu [ĐÃ BẢO MẬT]");
        }
        passMatcher.appendTail(sb);
        return sb.toString();
    }
}
