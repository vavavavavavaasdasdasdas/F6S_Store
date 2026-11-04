package com.example.demodatn.service.hotro;

import com.example.demodatn.repository.hotro.AdminChatAuditRepository;
import com.example.demodatn.repository.hotro.AdminChatConversationRepository;
import com.example.demodatn.repository.hotro.ChatConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatbotRetentionJob {

    private final ChatConversationRepository conversationRepository;
    private final AdminChatConversationRepository adminChatConversationRepository;
    private final AdminChatAuditRepository adminChatAuditRepository;

    // Chạy định kỳ lúc 3:00 sáng mỗi ngày, xóa các cuộc hội thoại cũ hơn 30 ngày
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldChatConversations() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        int deletedCustomer = conversationRepository.deleteOldConversations(cutoff);
        if (deletedCustomer > 0) {
            log.info("ChatbotRetentionJob: Đã xóa {} cuộc hội thoại khách hàng cũ hơn 30 ngày.", deletedCustomer);
        }

        int deletedAdmin = adminChatConversationRepository.deleteOldConversations(cutoff);
        if (deletedAdmin > 0) {
            log.info("ChatbotRetentionJob: Đã xóa {} cuộc hội thoại nội bộ cũ hơn 30 ngày.", deletedAdmin);
        }

        int deletedAudits = adminChatAuditRepository.deleteOldAudits(cutoff);
        if (deletedAudits > 0) {
            log.info("ChatbotRetentionJob: Đã xóa {} bản ghi audit nội bộ cũ hơn 30 ngày.", deletedAudits);
        }
    }
}
