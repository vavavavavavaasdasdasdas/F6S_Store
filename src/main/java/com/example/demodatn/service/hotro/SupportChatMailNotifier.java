package com.example.demodatn.service.hotro;
import com.example.demodatn.service.nhanvien.AccountMailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.*;
@Component @RequiredArgsConstructor @Slf4j
public class SupportChatMailNotifier {
    private final AccountMailService mail;
    private final SupportChatMailQueue queue;
    @Async("supportChatExecutor") @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void reply(SupportChatEvents.StaffReply event){
        if(!mail.isConfigured()){log.info("Chat: chưa cấu hình email, bỏ qua thông báo.");return;}
        try{var notice=queue.reserve(event);if(notice==null)return;boolean sent=mail.sendSupportNotice(notice.email(),notice.name(),notice.excerpt());queue.finish(notice.messageId(),sent);if(!sent)log.warn("Chat: chưa gửi được email thông báo.");}
        catch(Exception ignored){log.warn("Chat: chưa xử lý được email thông báo, tin nhắn vẫn đã lưu.");}
    }
}
