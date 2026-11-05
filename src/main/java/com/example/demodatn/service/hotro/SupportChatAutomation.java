package com.example.demodatn.service.hotro;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;
@Component
public class SupportChatAutomation {
    private final SupportChatService chat;
    private final RulesChatbotEngine rules;
    private final ChatbotEngine llm;
    @Value("${shop.chatbot.provider:${CHATBOT_PROVIDER:RULES}}") private String provider;
    public SupportChatAutomation(SupportChatService chat,RulesChatbotEngine rules,@Qualifier("llmChatbotEngine") ChatbotEngine llm){this.chat=chat;this.rules=rules;this.llm=llm;}
    @Async("supportChatExecutor")
    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void reply(SupportChatService.AutoRequest event){
        if(!chat.automaticAllowed(event.conversationId()))return;
        com.example.demodatn.dto.hotro.ChatbotMessageResponse response=null;
        if(rules.supportsSupportMessage(event.content())){
            try{response=("LLM".equalsIgnoreCase(provider)?llm:rules).process(event.content(),"support-"+event.conversationId(),event.authentication());}
            catch(Exception ignored){try{response=rules.process(event.content(),"support-"+event.conversationId(),event.authentication());}catch(Exception fallbackFailure){response=null;}}
            String answer=response==null?"":ChatbotToolService.removeAccents(response.getText()).toLowerCase(java.util.Locale.ROOT);
            if(answer.contains("gia von")||answer.contains("doanh thu")||answer.contains("loi nhuan"))response.setText("Trợ lý chỉ hỗ trợ sản phẩm, kích cỡ, chính sách và đơn hàng của chính bạn.");
        }
        // Recheck under the conversation lock after a potentially slow provider call.
        chat.automatic(event,response);
    }
}
