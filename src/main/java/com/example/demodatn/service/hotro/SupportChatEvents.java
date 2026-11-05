package com.example.demodatn.service.hotro;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;
import java.util.Map;
@Component @RequiredArgsConstructor
public class SupportChatEvents {
    private final SimpMessagingTemplate messaging;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    public record Changed(Long conversationId,String type) {}
    public record StaffReply(Long conversationId,Long messageId) {}
    public record Presence(boolean online) {}
    @EventListener public void presence(Presence event){messaging.convertAndSend("/topic/support-presence",(Object)Map.of("type","PRESENCE","online",event.online()));}
    public record Typing(Long conversationId,String senderType) {}
    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void committed(Changed event) {
        var targets=jdbc.queryForList("SELECT DISTINCT h.id FROM chat_hoi_thoai source JOIN chat_hoi_thoai h ON h.id=source.id OR COALESCE(source.customer_id,source.khach_id)=COALESCE(h.customer_id,h.khach_id) WHERE source.id=?",Long.class,event.conversationId());
        for(var id:targets)messaging.convertAndSend("/topic/support/"+id,(Object)Map.of("type",event.type()));
        messaging.convertAndSend("/topic/support-staff",(Object)Map.of("id",event.conversationId(),"type",event.type()));
    }
    @EventListener public void typing(Typing event) {
        messaging.convertAndSend("/topic/support/"+event.conversationId(),(Object)Map.of("type","TYPING","senderType",event.senderType()));
    }
}
