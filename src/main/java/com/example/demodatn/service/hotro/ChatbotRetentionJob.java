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
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final SupportChatImages images;
    private final java.time.Clock clock;
    @org.springframework.beans.factory.annotation.Value("${shop.chat.retention-human-days:180}") private int humanDays;


    // Chạy định kỳ lúc 3:00 sáng mỗi ngày, xóa các cuộc hội thoại cũ hơn 30 ngày
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldChatConversations() {
        LocalDateTime cutoff = LocalDateTime.now(clock.withZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))).minusDays(30);
        var humanCutoff=LocalDateTime.now(clock.withZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))).minusDays(Math.max(30,humanDays));
        var files=jdbc.queryForList("SELECT m.tep_dinh_kem FROM chat_tin_nhan m JOIN chat_hoi_thoai c ON c.id=m.hoi_thoai_id WHERE m.tep_dinh_kem IS NOT NULL AND ((c.da_co_nhan_vien=1 OR c.nhan_vien_phu_trach_id IS NOT NULL OR c.cho_nhan_vien=1) AND COALESCE(c.last_message_at,c.updated_at)<? OR c.da_co_nhan_vien=0 AND c.nhan_vien_phu_trach_id IS NULL AND c.cho_nhan_vien=0 AND COALESCE(c.last_message_at,c.updated_at)<?)",String.class,humanCutoff,cutoff);
        int deletedCustomer=conversationRepository.deleteHumanBefore(humanCutoff)+conversationRepository.deleteAutomaticBefore(cutoff);
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization(){@Override public void afterCommit(){files.forEach(images::remove);}});
        if (deletedCustomer > 0) {
            log.info("ChatbotRetentionJob: Đã xóa {} cuộc hội thoại khách hàng hết thời hạn lưu.", deletedCustomer);
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
