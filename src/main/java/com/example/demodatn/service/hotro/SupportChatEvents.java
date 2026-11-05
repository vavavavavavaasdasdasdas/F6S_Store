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
    public record Changed(Long conversationId,String type) {}
    public record Presence(boolean online) {}
    @EventListener public void presence(Presence event){messaging.convertAndSend("/topic/support-presence",(Object)Map.of("type","PRESENCE","online",event.online()));}
    public record Typing(Long conversationId,String senderType) {}
    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void committed(Changed event) {
        messaging.convertAndSend("/topic/support/"+event.conversationId(),(Object)Map.of("type",event.type()));
    }
    @EventListener public void typing(Typing event) {
        messaging.convertAndSend("/topic/support/"+event.conversationId(),(Object)Map.of("type","TYPING","senderType",event.senderType()));
    }
}
