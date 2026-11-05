package com.example.demodatn.service.hotro;
import com.example.demodatn.dto.hotro.ChatbotMessageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class SupportChatAutomationTest {
    @Test void unknownIntentNeverCallsProviderAndStaffTakeoverStopsEverything(){
        var chat=mock(SupportChatService.class);var rules=mock(RulesChatbotEngine.class);var llm=mock(ChatbotEngine.class);var automation=new SupportChatAutomation(chat,rules,llm);ReflectionTestUtils.setField(automation,"provider","LLM");
        var event=new SupportChatService.AutoRequest(1L,2L,"Tin không khớp",null);
        when(chat.automaticAllowed(1L)).thenReturn(true);when(rules.supportsSupportMessage(event.content())).thenReturn(false);automation.reply(event);verifyNoInteractions(llm);verify(chat).automatic(event,null);
        clearInvocations(chat,rules,llm);when(chat.automaticAllowed(1L)).thenReturn(false);automation.reply(event);verifyNoInteractions(rules,llm);verify(chat,never()).automatic(any(),any());
    }
    @Test void providerIsStubbedAndCannotExposeInternalFinanceInAnswer(){
        var chat=mock(SupportChatService.class);var rules=mock(RulesChatbotEngine.class);var llm=mock(ChatbotEngine.class);var automation=new SupportChatAutomation(chat,rules,llm);ReflectionTestUtils.setField(automation,"provider","LLM");
        var event=new SupportChatService.AutoRequest(1L,2L,"Tìm giày",null);var reply=ChatbotMessageResponse.builder().text("Giá vốn bí mật").build();when(chat.automaticAllowed(1L)).thenReturn(true);when(rules.supportsSupportMessage(event.content())).thenReturn(true);when(llm.process(event.content(),"support-1",null)).thenReturn(reply);automation.reply(event);
        assertFalse(reply.getText().contains("Giá vốn"));verify(chat).automatic(event,reply);
    }
}
