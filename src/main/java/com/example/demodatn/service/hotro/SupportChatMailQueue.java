package com.example.demodatn.service.hotro;
import com.example.demodatn.repository.hotro.ChatConversationRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
@Service @RequiredArgsConstructor
public class SupportChatMailQueue {
    private final ChatConversationRepository conversations;
    private final SupportChatService chat;
    private final JdbcTemplate jdbc;
    private final EntityManager em;
    private final Clock clock;
    public record Notice(long messageId,String email,String name,String excerpt) {}
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public Notice reserve(SupportChatEvents.StaffReply event){
        var c=conversations.lock(event.conversationId()).orElse(null);if(c==null)return null;em.refresh(c);
        var now=LocalDateTime.now(clock.withZone(ZoneId.of("Asia/Ho_Chi_Minh")));
        if(chat.customerOnline(c)||c.getLastMailAt()!=null&&c.getLastMailAt().isAfter(now.minusMinutes(30)))return null;
        Integer owner=chat.ownerId(c);String email=owner==null?c.getContact():jdbc.queryForObject("SELECT email FROM customers WHERE id=?",String.class,owner);
        if(email==null||!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))return null;
        var rows=jdbc.queryForList("SELECT noi_dung FROM chat_tin_nhan WHERE id=? AND hoi_thoai_id=? AND nguoi_gui_loai=N'NHAN_VIEN'",event.messageId(),c.getId());if(rows.isEmpty())return null;
        String text=java.util.Objects.toString(rows.get(0).get("noi_dung"),"[Tin nhắn đính kèm]");if(text.isBlank())text="[Tin nhắn đính kèm]";if(text.length()>180)text=text.substring(0,180)+"…";
        c.setLastMailAt(now);jdbc.update("INSERT chat_mail_log(hoi_thoai_id,message_id,trang_thai,created_at) VALUES(?,?,N'PENDING',?)",c.getId(),event.messageId(),now);
        return new Notice(event.messageId(),email,c.getName(),text);
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW) public void finish(long messageId,boolean success){jdbc.update("UPDATE chat_mail_log SET trang_thai=? WHERE message_id=?",success?"SENT":"FAILED",messageId);}
}
