package com.example.demodatn.service.hotro;

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

    // Chạy định kỳ lúc 3:00 sáng mỗi ngày, xóa các cuộc hội thoại cũ hơn 30 ngày
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldChatConversations() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        int deleted = conversationRepository.deleteOldConversations(cutoff);
        if (deleted > 0) {
            log.info("ChatbotRetentionJob: Đã xóa {} cuộc hội thoại cũ hơn 30 ngày.", deleted);
        }
    }
}
