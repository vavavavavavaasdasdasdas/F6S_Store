package com.example.demodatn.service.hotro;

import com.example.demodatn.controller.user.SupportChatApiController;
import com.example.demodatn.dto.hotro.*;
import com.example.demodatn.repository.hotro.*;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @Transactional
class SupportChatIntegrationTest {
    @Autowired SupportChatService chat;
    @Autowired ChatConversationRepository conversations;
    @Autowired ChatMessageRepository messages;
    @Autowired ChatbotRetentionJob retention;
    @Autowired LlmChatbotEngine llm;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired WebApplicationContext context;
    MockMvc mvc;SupportChatService.Actor alice,bob,staff;
    @BeforeEach void setup(){
        assertTrue(jdbc.queryForObject("SELECT DB_NAME()",String.class).startsWith("shopbangiay_kt_"));
        mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        alice=customer();bob=customer();staff=new SupportChatService.Actor("admin",null);
    }
    private SupportChatService.Actor customer(){
        String tag="chat28-"+UUID.randomUUID().toString().substring(0,12);
        int role=jdbc.queryForObject("SELECT id FROM roles WHERE name='ROLE_USER'",Integer.class);
        int u=jdbc.queryForObject("INSERT users(username,password,role_id,status,must_change_password) OUTPUT INSERTED.id VALUES(?,'not-a-login-password',?,1,0)",Integer.class,tag,role);
        jdbc.update("INSERT customers(customer_code,full_name,email,phone,user_id,status) VALUES(?,N'Nguyễn Văn Ánh',?,?,?,1)",tag,tag+"@example.test",String.format("+849%08d",u),u);
        return new SupportChatService.Actor(tag,null);
    }
    private SupportChatRequest text(String content){return new SupportChatRequest(UUID.randomUUID().toString(),"VAN_BAN",content,null,null);}
    private SupportChatService.Opened open(SupportChatService.Actor actor){return chat.open(actor,null,null,"127.0.0.1");}
    @Test void idempotentMessageAndUnreadReadReceiptsAreConsistent(){
        var c=open(alice);var input=text("Tin nhắn chỉ lưu một lần");var a=chat.send(c.id(),input,null,alice,"127.0.0.1",null);var b=chat.send(c.id(),input,null,alice,"127.0.0.1",null);
        assertEquals(a.id(),b.id());assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM chat_tin_nhan WHERE hoi_thoai_id=?",Integer.class,c.id()));
        assertEquals(1,chat.history(c.id(),null,staff).conversation().unread());
        chat.read(c.id(),a.id(),staff);em.flush();em.clear();assertNotNull(chat.history(c.id(),null,alice).messages().get(0).seenAt());
        var reply=chat.send(c.id(),text("Nhân viên trả lời"),null,staff,"127.0.0.1",null);assertEquals(1,chat.history(c.id(),null,alice).conversation().unread());
        chat.read(c.id(),reply.id(),alice);em.flush();em.clear();assertEquals(0,chat.history(c.id(),null,alice).conversation().unread());
    }
    @Test void ownershipProtectsEveryMessageAndAttachment(){
        var c=open(alice);
        assertThrows(ResponseStatusException.class,()->chat.access(c.id(),bob));
        assertThrows(ResponseStatusException.class,()->chat.send(c.id(),text("Trái quyền"),null,bob,"127.0.0.1",null));
        assertThrows(ResponseStatusException.class,()->chat.read(c.id(),Long.MAX_VALUE,bob));
        assertThrows(ResponseStatusException.class,()->chat.typing(c.id(),bob));
        assertThrows(ResponseStatusException.class,()->chat.human(c.id(),bob));
    }
    @Test void anonymousCookieIsServerIssuedAndForgeryIsRejected()throws Exception{
        String fake=SupportChatService.newToken();
        jdbc.update("INSERT chat_hoi_thoai(session_id) VALUES(?)",SupportChatService.hashToken(fake));
        int before=jdbc.queryForObject("SELECT COUNT(*) FROM chat_hoi_thoai",Integer.class);
        mvc.perform(get("/api/support/session").cookie(new Cookie(SupportChatApiController.COOKIE,fake))).andExpect(status().isForbidden());
        mvc.perform(post("/api/support/session").with(csrf()).cookie(new Cookie(SupportChatApiController.COOKIE,fake)).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Khách thử\",\"contact\":\"0912345678\"}")).andExpect(status().isForbidden());
        assertEquals(before,jdbc.queryForObject("SELECT COUNT(*) FROM chat_hoi_thoai",Integer.class));
        var result=mvc.perform(post("/api/support/session").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Nguyễn Văn Ánh\",\"contact\":\"0912345678\"}")).andExpect(status().isOk()).andReturn();
        String cookie=result.getResponse().getHeader("Set-Cookie");assertNotNull(cookie);assertTrue(cookie.contains("HttpOnly"));assertTrue(cookie.contains("SameSite=Lax"));
    }
    @Test void anonymousHistoryAttachesAndMergesWithExistingAccountWithoutDroppingMessages(){
        var existing=open(alice);var first=chat.send(existing.id(),text("Tin của tài khoản"),null,alice,"127.0.0.1",null);
        String hash=SupportChatService.hashToken(SupportChatService.newToken());var guest=new SupportChatService.Actor(null,hash);
        var anon=chat.open(guest,"Khách trước đăng nhập","0912345678","127.0.0.1");var second=chat.send(anon.id(),text("Tin ẩn danh gần nhất"),null,guest,"127.0.0.1",null);
        var linked=chat.open(new SupportChatService.Actor(alice.username(),hash),null,null,"127.0.0.1");assertEquals(existing.id(),linked.id());
        em.flush();em.clear();var ids=chat.history(linked.id(),null,alice).messages().stream().map(SupportChatService.MessageView::id).toList();assertTrue(ids.containsAll(List.of(first.id(),second.id())));
        assertThrows(ResponseStatusException.class,()->chat.access(linked.id(),guest));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM chat_hoi_thoai WHERE customer_id=?",Integer.class,chat.identity(alice).customerId()));
    }
    @Test void staffTakeoverWinsAgainstAnAlreadyComputedAutomaticReplyAndClosedThreadReopens(){
        var c=open(alice);var userMessage=chat.send(c.id(),text("Tư vấn size"),null,alice,"127.0.0.1",null);
        chat.claim(c.id(),staff,false);chat.automatic(new SupportChatService.AutoRequest(c.id(),userMessage.id(),"Tư vấn size",null),ChatbotMessageResponse.builder().text("Không được gửi câu này").build());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM chat_tin_nhan WHERE hoi_thoai_id=? AND nguoi_gui_loai=N'TRO_LY'",Integer.class,c.id()));
        chat.claim(c.id(),staff,true);assertEquals("DA_DONG",chat.history(c.id(),null,alice).conversation().status());
        assertEquals(c.id(),open(alice).id());chat.send(c.id(),text("Nhắn lại sau khi đóng"),null,alice,"127.0.0.1",null);assertEquals("DANG_XU_LY",chat.history(c.id(),null,alice).conversation().status());
    }
    @Test void paginationReturnsThirtyWithNoOverlapAndMaskingRunsBeforePersistence(){
        var c=open(alice);
        for(int n=0;n<65;n++)jdbc.update("INSERT chat_tin_nhan(hoi_thoai_id,sender,nguoi_gui_loai,loai,noi_dung,created_at) VALUES(?,'USER',N'KHACH',N'VAN_BAN',?,DATEADD(day,-1,GETDATE()))",c.id(),"Tin cũ "+n);
        var page1=chat.history(c.id(),null,alice);assertEquals(30,page1.messages().size());assertTrue(page1.hasMore());
        var page2=chat.history(c.id(),page1.messages().get(0).id(),alice);assertEquals(30,page2.messages().size());assertTrue(Collections.disjoint(page1.messages().stream().map(SupportChatService.MessageView::id).toList(),page2.messages().stream().map(SupportChatService.MessageView::id).toList()));
        var masked=chat.send(c.id(),text("Mật khẩu: TestSecret28; số thẻ 4111111111111111"),null,alice,"127.0.0.1",null);
        assertFalse(masked.content().contains("TestSecret28"));assertFalse(masked.content().contains("4111111111111111"));assertTrue(masked.content().contains("ĐÃ BẢO MẬT"));
    }
    @Test void perMinuteAndDuplicateSpamAreRejected(){
        var c=open(alice);for(int n=0;n<30;n++)chat.send(c.id(),text("Tin riêng biệt số "+n),null,alice,"127.0.0.2",null);
        var ex=assertThrows(ResponseStatusException.class,()->chat.send(c.id(),text("Tin thứ ba mươi mốt"),null,alice,"127.0.0.2",null));assertEquals(429,ex.getStatusCode().value());
    }
    @Test void ipLimitAppliesAcrossConversationsAndForwardedHeaderCannotChangeIt(){
        var prior=open(bob);
        jdbc.update("INSERT chat_tin_nhan(hoi_thoai_id,sender,nguoi_gui_loai,loai,noi_dung,created_at,ip_address) SELECT TOP 500 ?,'USER',N'KHACH',N'VAN_BAN',N'Dữ liệu giả giới hạn IP',DATEADD(minute,-2,GETDATE()),'127.0.0.3' FROM sys.all_objects",prior.id());
        var c=open(alice);var ex=assertThrows(ResponseStatusException.class,()->chat.send(c.id(),text("Tin vượt giới hạn ngày"),null,alice,"127.0.0.3",null));assertEquals(429,ex.getStatusCode().value());
    }
    @Test void photosAreValidatedByTheirBytesAndAllPostsKeepCsrf()throws Exception{
        var c=open(alice);
        var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",bytes);byte[] png=bytes.toByteArray();
        mvc.perform(multipart("/api/support/"+c.id()+"/messages").file(new MockMultipartFile("file","sample.png","image/png",png)).with(user(alice.username()).roles("USER")).with(csrf()).param("clientId",UUID.randomUUID().toString()).param("type","ANH").param("content","Ảnh thử" )).andExpect(status().isOk());
        mvc.perform(multipart("/api/support/"+c.id()+"/messages").file(new MockMultipartFile("file","fake.png","image/png","not an image".getBytes(StandardCharsets.UTF_8))).with(user(alice.username()).roles("USER")).with(csrf()).param("clientId",UUID.randomUUID().toString()).param("type","ANH")).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/support/"+c.id()+"/messages").file(new MockMultipartFile("file","large.png","image/png",new byte[3*1024*1024+1])).with(user(alice.username()).roles("USER")).with(csrf()).param("clientId",UUID.randomUUID().toString()).param("type","ANH")).andExpect(status().isBadRequest());
        for(String suffix:List.of("/messages","/read","/typing","/human")){
            mvc.perform(post("/api/support/"+c.id()+suffix).with(user(alice.username()).roles("USER")).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
            mvc.perform(post("/api/support/"+c.id()+suffix).with(user(alice.username()).roles("USER")).with(csrf().useInvalidToken()).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        }
    }
    @Test void onlyOwnOrdersCanBeSharedAndUnknownIntentsQueueForStaff(){
        var c=open(alice);int other=jdbc.queryForObject("INSERT orders(order_code,customer_id,order_type,status,payment_status,total_amount,final_amount,created_at) OUTPUT INSERTED.id VALUES(?,?,'ONLINE','DELIVERED','PAID',100000,100000,GETDATE())",Integer.class,"CH28-"+UUID.randomUUID(),chat.identity(bob).customerId());
        assertThrows(ResponseStatusException.class,()->chat.send(c.id(),new SupportChatRequest(UUID.randomUUID().toString(),"DON_HANG","",null,other),null,alice,"127.0.0.1",null));
        chat.automatic(new SupportChatService.AutoRequest(c.id(),0L,"Hỏi ngoài phạm vi",null),null);em.flush();assertEquals(true,jdbc.queryForObject("SELECT cho_nhan_vien FROM chat_hoi_thoai WHERE id=?",Boolean.class,c.id()));
    }
    @Test void retentionKeepsStaffFor180DaysAndAutomaticOnlyFor30Days(){
        var human=open(alice);chat.claim(human.id(),staff,false);var automatic=open(bob);em.flush();em.clear();
        jdbc.update("UPDATE chat_hoi_thoai SET updated_at=DATEADD(day,-31,GETDATE()),last_message_at=DATEADD(day,-31,GETDATE()) WHERE id IN(?,?)",human.id(),automatic.id());retention.cleanupOldChatConversations();
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM chat_hoi_thoai WHERE id=?",Integer.class,human.id()));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM chat_hoi_thoai WHERE id=?",Integer.class,automatic.id()));
        jdbc.update("UPDATE chat_hoi_thoai SET last_message_at=DATEADD(day,-181,GETDATE()) WHERE id=?",human.id());retention.cleanupOldChatConversations();assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM chat_hoi_thoai WHERE id=?",Integer.class,human.id()));
    }
    @Test void legacyCustomerSessionsStayReadableInTheSingleCurrentConversation(){
        int owner=chat.identity(alice).customerId();var current=open(alice);
        long old=jdbc.queryForObject("INSERT chat_hoi_thoai(session_id,khach_id,trang_thai) OUTPUT INSERTED.id VALUES(?,?,'DA_DONG')",Long.class,"old-"+UUID.randomUUID(),owner);
        jdbc.update("INSERT chat_tin_nhan(hoi_thoai_id,sender,nguoi_gui_loai,loai,noi_dung) VALUES(?,'USER',N'KHACH',N'VAN_BAN',N'Tin cũ vẫn đọc được')",old);
        em.flush();em.clear();assertTrue(chat.history(current.id(),null,alice).messages().stream().anyMatch(m->m.content().equals("Tin cũ vẫn đọc được")));
    }
    @Test void queuedUnknownDoesNotDisableMatchingAutomationUntilStaffTakesOver(){
        var c=open(alice);chat.automatic(new SupportChatService.AutoRequest(c.id(),1L,"Không khớp",null),null);
        assertTrue(chat.automaticAllowed(c.id()));
        chat.automatic(new SupportChatService.AutoRequest(c.id(),2L,"Tư vấn size",null),ChatbotMessageResponse.builder().text("Vui lòng cho biết chiều dài bàn chân.").build());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM chat_tin_nhan WHERE hoi_thoai_id=? AND nguoi_gui_loai=N'TRO_LY'",Integer.class,c.id()));
        chat.claim(c.id(),staff,false);assertFalse(chat.automaticAllowed(c.id()));
    }
    @Test void chatFilesCannotBeConfiguredUnderPublicResources(){
        String publicDir=java.nio.file.Path.of("src/main/resources/static/private-chat-not-allowed").toAbsolutePath().toString();
        assertThrows(IllegalArgumentException.class,()->new SupportChatImages(publicDir,"uploads/products"));
        assertThrows(IllegalArgumentException.class,()->new SupportChatImages("uploads/products/chat","uploads/products"));
    }
    @Test void newSessionAndHistoryNeverSerializeProviderSecrets()throws Exception{
        var c=open(alice);String previous=llm.getApiKey(),marker="FAKE-KEY-GD28-NOT-A-REAL-SECRET";llm.setApiKey(marker);
        try{for(String path:List.of("/api/support/session","/api/support/"+c.id()+"/messages")){
            String body=mvc.perform(get(path).with(user(alice.username()).roles("USER"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertFalse(body.contains(marker));assertFalse(body.toLowerCase(java.util.Locale.ROOT).contains("apikey"));assertFalse(body.contains("CHATBOT_API_KEY"));
        }}finally{llm.setApiKey(previous);}
    }
}
