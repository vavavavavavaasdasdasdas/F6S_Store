package com.example.demodatn.service.hotro;

import com.example.demodatn.dto.hotro.*;
import com.example.demodatn.entity.hotro.*;
import com.example.demodatn.repository.hotro.*;
import com.example.demodatn.repository.khachhang.CustomerRepository;
import com.example.demodatn.repository.sanpham.*;
import com.example.demodatn.repository.donhang.OrderRepository;
import com.example.demodatn.service.donhang.*;
import com.example.demodatn.service.sanpham.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service @RequiredArgsConstructor
public class SupportChatService {
    private final ChatConversationRepository conversations;
    private final ChatMessageRepository messages;
    private final CustomerRepository customers;
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final OrderRepository orders;
    private final PricingService prices;
    private final ProductImageUrlService productImages;
    private final SupportChatImages images;
    private final JdbcTemplate jdbc;
    private final EntityManager entityManager;
    private final ObjectMapper json;
    private final Clock clock;
    private final ApplicationEventPublisher events;
    private static final SecureRandom RANDOM=new SecureRandom();
    private final Map<String,Instant> presence=new ConcurrentHashMap<>();
    private final Map<String,Actor> customerSockets=new ConcurrentHashMap<>();
    private final Map<String,String> socketPresence=new ConcurrentHashMap<>();
    private final Map<String,Instant> typing=new ConcurrentHashMap<>();
    public record Actor(String username,String guestHash) {}
    public record Identity(Integer customerId,Integer employeeId,Integer userId,boolean staff) {}
    public record Opened(Long id,String code,String status,int unread,boolean online,String clientScope) {}
    public record MessageView(Long id,String clientId,String senderType,String type,String content,Object payload,
                              String attachmentUrl,LocalDateTime createdAt,LocalDateTime seenAt) {}
    public record History(Opened conversation,List<MessageView> messages,boolean hasMore,boolean staffTyping,boolean customerTyping) {}
    public record AutoRequest(Long conversationId,Long messageId,String content,org.springframework.security.core.Authentication authentication) {}
    private LocalDateTime now(){return LocalDateTime.now(clock.withZone(ZoneId.of("Asia/Ho_Chi_Minh")));}
    public static String newToken(){byte[] bytes=new byte[32];RANDOM.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
    public static String hashToken(String token){
        if(token==null)return null;
        if(!token.matches("[A-Za-z0-9_-]{43}"))throw denied();
        try{return "support:"+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII)));}
        catch(NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}
    }
    private static ResponseStatusException denied(){return new ResponseStatusException(HttpStatus.FORBIDDEN,"Bạn không có quyền truy cập cuộc trò chuyện này.");}
    public Identity identity(Actor actor){
        if(actor==null)throw denied();
        if(actor.username()==null)return new Identity(null,null,null,false);
        var rows=jdbc.queryForList("SELECT u.id user_id,r.name role_name,c.id customer_id,e.id employee_id FROM users u JOIN roles r ON r.id=u.role_id LEFT JOIN customers c ON c.user_id=u.id AND c.status=1 LEFT JOIN employees e ON e.user_id=u.id AND e.status=1 WHERE u.username=? AND u.status=1 AND u.must_change_password=0",actor.username());
        if(rows.isEmpty())throw denied();var row=rows.get(0);
        boolean staff=Set.of("ROLE_ADMIN","ROLE_EMPLOYEE").contains(row.get("role_name"));
        Integer customer=(Integer)row.get("customer_id");if(!staff&&customer==null)throw denied();
        return new Identity(customer,(Integer)row.get("employee_id"),(Integer)row.get("user_id"),staff);
    }
    private void check(ChatConversation c,Actor actor){
        var who=identity(actor);if(who.staff())return;
        if(who.customerId()!=null && who.customerId().equals(ownerId(c)))return;
        if(actor.username()==null && c.getCustomer()==null && actor.guestHash()!=null && actor.guestHash().equals(c.getTokenHash()))return;
        throw denied();
    }
    @Transactional(readOnly=true) public ChatConversation access(Long id,Actor actor){
        var c=conversations.findById(id).orElseThrow(SupportChatService::denied);check(c,actor);return c;
    }
    private ChatConversation locked(Long id,Actor actor){
        var c=conversations.lock(id).orElseThrow(SupportChatService::denied);entityManager.refresh(c);check(c,actor);return c;
    }
    @Transactional(readOnly=true) public Optional<Opened> current(Actor actor){
        var who=identity(actor);
        Optional<ChatConversation> found=who.customerId()!=null?conversations.findByCustomer_Id(who.customerId()):actor.guestHash()==null?Optional.empty():conversations.findByTokenHash(actor.guestHash());
        if(found.isPresent()){check(found.get(),actor);return found.map(c->opened(c,who));}
        if(actor.guestHash()!=null && actor.username()==null)throw denied();
        return Optional.empty();
    }
    @Transactional(readOnly=true) public String guestScope(Actor actor){
        if(actor.guestHash()==null)return null;
        var c=conversations.findByTokenHash(actor.guestHash()).orElseThrow(SupportChatService::denied);
        var who=identity(actor);
        if(c.getCustomer()!=null && (who.customerId()==null||!who.customerId().equals(c.getCustomer().getId())))throw denied();
        return c.getSessionId();
    }
    @Transactional public Opened open(Actor actor,String name,String contact,String ip){
        var who=identity(actor);ChatConversation c;
        if(who.customerId()!=null){
            var customer=customers.findForUpdate(who.customerId()).orElseThrow(SupportChatService::denied);
            c=conversations.findByCustomer_Id(customer.getId()).orElse(null);
            var anonymous=actor.guestHash()==null?null:conversations.findByTokenHash(actor.guestHash()).orElseThrow(SupportChatService::denied);
            if(anonymous!=null){
                anonymous=conversations.lock(anonymous.getId()).orElseThrow();entityManager.refresh(anonymous);
                if(anonymous.getCustomer()!=null && !anonymous.getCustomer().getId().equals(customer.getId()))throw denied();
                if(c==null)c=anonymous;
                else if(!c.getId().equals(anonymous.getId())){
                    c=conversations.lock(c.getId()).orElseThrow();entityManager.refresh(c);
                    jdbc.update("UPDATE a SET client_id=NULL FROM chat_tin_nhan a WHERE a.hoi_thoai_id=? AND a.client_id IS NOT NULL AND EXISTS(SELECT 1 FROM chat_tin_nhan b WHERE b.hoi_thoai_id=? AND b.nguoi_gui_loai=a.nguoi_gui_loai AND b.client_id=a.client_id)",anonymous.getId(),c.getId());
                    jdbc.update("UPDATE chat_tin_nhan SET hoi_thoai_id=? WHERE hoi_thoai_id=?",c.getId(),anonymous.getId());
                    c.setCustomerUnread(c.getCustomerUnread()+anonymous.getCustomerUnread());c.setStaffUnread(c.getStaffUnread()+anonymous.getStaffUnread());
                    c.setHuman(c.isHuman()||anonymous.isHuman());c.setWaiting(c.isWaiting()||anonymous.isWaiting());if(c.getEmployeeId()==null)c.setEmployeeId(anonymous.getEmployeeId());
                    anonymous.setTokenHash(null);anonymous.setStatus("DA_DONG");anonymous.setCustomerUnread(0);anonymous.setStaffUnread(0);
                }
            }
            if(c==null){c=new ChatConversation();c.setSessionId(UUID.randomUUID().toString());}
            c.setCustomer(customer);c.setName(customer.getFullName());c.setContact(customer.getEmail()!=null?customer.getEmail():customer.getPhone());
        }else{
            if(who.staff())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Nhân viên dùng API hỗ trợ trong trang quản trị.");
            if(actor.guestHash()==null)throw denied();
            c=conversations.findByTokenHash(actor.guestHash()).orElse(null);
            if(c==null){
                if(name==null||name.isBlank()||name.length()>100||contact==null||contact.length()>100||!contact.matches("(?:0[0-9]{9}|\\+84[0-9]{9}|[^\\s@]+@[^\\s@]+\\.[^\\s@]+)"))throw new IllegalArgumentException("Vui lòng nhập họ tên và số điện thoại hoặc email hợp lệ.");
                c=new ChatConversation();c.setSessionId(UUID.randomUUID().toString());c.setTokenHash(actor.guestHash());c.setName(ChatbotService.maskSensitiveInfo(name.trim()));c.setContact(contact.trim());
            }else check(c,actor);
        }
        if(c.getEmployeeId()!=null&&"MOI".equals(c.getStatus()))c.setStatus("DANG_XU_LY");
        c.setIpAddress(ip);c.setUpdatedAt(now());if(c.getLastMessageAt()==null)c.setLastMessageAt(now());
        conversations.saveAndFlush(c);return opened(c,who);
    }
    private Opened opened(ChatConversation c,Identity who){Integer owner=ownerId(c);int unread=who.staff()?c.getStaffUnread():owner==null?c.getCustomerUnread():jdbc.queryForObject("SELECT COALESCE(SUM(chua_doc_khach),0) FROM chat_hoi_thoai WHERE customer_id=? OR khach_id=?",Integer.class,owner,owner);return new Opened(c.getId(),c.getCode(),c.getStatus(),unread,online(),c.getSessionId());}
    public boolean online(){presence.entrySet().removeIf(e->e.getValue().isBefore(clock.instant().minusSeconds(65)));return !presence.isEmpty()||!socketPresence.isEmpty();}
    public void staffPresence(Actor actor){if(!identity(actor).staff())throw denied();presence.put(actor.username(),clock.instant());events.publishEvent(new SupportChatEvents.Presence(true));}
    public void socketConnected(String sessionId,Actor actor){if(!identity(actor).staff()){customerSockets.put(sessionId,actor);customerPresent(actor);}if(identity(actor).staff()){socketPresence.put(sessionId,actor.username());events.publishEvent(new SupportChatEvents.Presence(true));}}
    public void socketDisconnected(String sessionId){var customer=customerSockets.remove(sessionId);if(customer!=null)customerPresent(customer);String username=socketPresence.remove(sessionId);if(username!=null){if(!socketPresence.containsValue(username))presence.remove(username);events.publishEvent(new SupportChatEvents.Presence(online()));}}
    @Transactional(readOnly=true) public History history(Long id,Long before,Actor actor){
        var c=access(id,actor);var who=identity(actor);Integer owner=ownerId(c);var rows=new ArrayList<>(owner==null||who.staff()?messages.history(id,before,PageRequest.of(0,31)):messages.historyForCustomer(id,owner,before,PageRequest.of(0,31)));boolean more=rows.size()>30;if(more)rows.remove(30);Collections.reverse(rows);
        return new History(opened(c,identity(actor)),rows.stream().map(this::view).toList(),more,isTyping(id,"NHAN_VIEN"),isTyping(id,"KHACH"));
    }
    private boolean isTyping(Long id,String side){var at=typing.get(id+":"+side);return at!=null && at.isAfter(clock.instant().minusSeconds(6));}
    @Transactional(readOnly=true) public void typing(Long id,Actor actor){
        access(id,actor);boolean staff=identity(actor).staff();String side=staff?"NHAN_VIEN":"KHACH";
        var prior=typing.put(id+":"+side,clock.instant());
        if(prior==null||prior.isBefore(clock.instant().minusSeconds(2)))events.publishEvent(new SupportChatEvents.Typing(id,side));
    }
    @Transactional public void read(Long id,long through,Actor actor){
        var c=locked(id,actor);boolean staff=identity(actor).staff();Integer owner=ownerId(c);
        if(!staff&&owner!=null){
            jdbc.update("UPDATE m SET da_xem_luc=? FROM chat_tin_nhan m JOIN chat_hoi_thoai h ON h.id=m.hoi_thoai_id WHERE (h.customer_id=? OR h.khach_id=?) AND m.id<=? AND m.da_xem_luc IS NULL AND m.nguoi_gui_loai<>N'KHACH'",now(),owner,owner,through);
            jdbc.update("UPDATE h SET chua_doc_khach=(SELECT COUNT(*) FROM chat_tin_nhan m WHERE m.hoi_thoai_id=h.id AND m.da_xem_luc IS NULL AND m.nguoi_gui_loai<>N'KHACH') FROM chat_hoi_thoai h WHERE h.customer_id=? OR h.khach_id=?",owner,owner);
            entityManager.refresh(c);events.publishEvent(new SupportChatEvents.Changed(id,"READ"));return;
        }
        String condition=staff?"nguoi_gui_loai=N'KHACH'":"nguoi_gui_loai<>N'KHACH'";
        jdbc.update("UPDATE chat_tin_nhan SET da_xem_luc=? WHERE hoi_thoai_id=? AND id<=? AND da_xem_luc IS NULL AND "+condition,now(),id,through);
        int remaining=jdbc.queryForObject("SELECT COUNT(*) FROM chat_tin_nhan WHERE hoi_thoai_id=? AND da_xem_luc IS NULL AND "+condition,Integer.class,id);
        if(staff)c.setStaffUnread(remaining);else c.setCustomerUnread(remaining);
        events.publishEvent(new SupportChatEvents.Changed(id,"READ"));
    }
    @Transactional public void claim(Long id,Actor actor,boolean close){act(id,actor,close?"close":"claim");}
    @Transactional public void act(Long id,Actor actor,String action){
        var who=identity(actor);if(!who.staff())throw denied();var c=locked(id,actor);
        switch(action){
            case "claim" -> {c.setAssignedUserId(who.userId());c.setEmployeeId(who.employeeId());c.setStatus("DANG_XU_LY");}
            case "queue" -> {c.setAssignedUserId(null);c.setEmployeeId(null);c.setStatus("MOI");}
            case "close" -> c.setStatus("DA_DONG");
            case "reopen" -> c.setStatus(c.getAssignedUserId()==null?"MOI":"DANG_XU_LY");
            default -> throw new IllegalArgumentException("Thao tác hội thoại không hợp lệ.");
        }
        c.setHuman(true);c.setWaiting("queue".equals(action));c.setUpdatedAt(now());
        jdbc.update("INSERT chat_action_audit(hoi_thoai_id,user_id,actor_name,hanh_dong,created_at) VALUES(?,?,?,?,?)",id,who.userId(),actor.username(),action,now());
        events.publishEvent(new SupportChatEvents.Changed(id,"STATE"));staffPresence(actor);
    }
    public Integer ownerId(ChatConversation c){return c.getCustomer()!=null?c.getCustomer().getId():c.getLegacyCustomerId();}
    public void customerPresent(Actor actor){
        if(identity(actor).staff())return;
        if(actor.username()!=null)jdbc.update("UPDATE c SET last_customer_seen_at=? FROM chat_hoi_thoai c JOIN customers k ON k.id=c.customer_id OR k.id=c.khach_id JOIN users u ON u.id=k.user_id WHERE u.username=?",now(),actor.username());
        else if(actor.guestHash()!=null)jdbc.update("UPDATE chat_hoi_thoai SET last_customer_seen_at=? WHERE session_token_hash=?",now(),actor.guestHash());
    }
    public boolean customerOnline(ChatConversation c){
        for(var actor:customerSockets.values()){
            try{var who=identity(actor);if(who.customerId()!=null&&who.customerId().equals(ownerId(c))||actor.guestHash()!=null&&actor.guestHash().equals(c.getTokenHash()))return true;}catch(RuntimeException ignored){}
        }
        var last=c.getLastCustomerSeenAt()!=null?c.getLastCustomerSeenAt():c.getCreatedAt();return last.isAfter(now().minusMinutes(2));
    }
    private void rate(Long id,String ip){
        jdbc.execute("DECLARE @r INT; EXEC @r=sys.sp_getapplock @Resource=N'chat.ip."+Integer.toUnsignedString(ip.hashCode())+"',@LockMode='Exclusive',@LockOwner='Transaction',@LockTimeout=5000; IF @r<0 THROW 51000,N'Chưa gửi được tin, vui lòng thử lại.',1;");
        int minute=jdbc.queryForObject("SELECT COUNT(*) FROM chat_tin_nhan WHERE hoi_thoai_id=? AND nguoi_gui_loai IN(N'KHACH',N'NHAN_VIEN') AND created_at>=?",Integer.class,id,now().minusMinutes(1));
        int day=jdbc.queryForObject("SELECT COUNT(*) FROM chat_tin_nhan WHERE ip_address=? AND created_at>=?",Integer.class,ip,now().toLocalDate().atStartOfDay());
        if(minute>=30||day>=500)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Bạn đang gửi quá nhanh. Vui lòng đợi trước khi gửi tiếp.");
    }
    @Transactional public MessageView send(Long id,SupportChatRequest request,MultipartFile file,Actor actor,String ip,org.springframework.security.core.Authentication auth){
        var c=locked(id,actor);var who=identity(actor);String side=who.staff()?"NHAN_VIEN":"KHACH";
        var previous=messages.findByConversation_IdAndSenderTypeAndClientId(id,side,request.clientId());if(previous.isPresent())return view(previous.get());
        rate(id,ip);String content=ChatbotService.maskSensitiveInfo(Objects.toString(request.content(),"").trim());
        if(content.length()>2000)throw new IllegalArgumentException("Tin nhắn tối đa 2000 ký tự.");
        if("VAN_BAN".equals(request.type())&&content.isBlank())throw new IllegalArgumentException("Vui lòng nhập tin nhắn.");
        if(content.length()>0 && !jdbc.queryForList("SELECT TOP 1 id FROM chat_tin_nhan WHERE hoi_thoai_id=? AND nguoi_gui_loai=? AND noi_dung=? AND created_at>=?",id,side,content,now().minusSeconds(10)).isEmpty())throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Tin nhắn này vừa được gửi. Vui lòng đợi trước khi gửi lại.");
        var m=new ChatMessage();m.setConversation(c);m.setSender(who.staff()?"STAFF":"USER");m.setSenderType(side);m.setSenderId(who.staff()?(who.employeeId()!=null?who.employeeId():who.userId()):who.customerId());m.setType(request.type());m.setNoiDung(content);m.setClientId(request.clientId());m.setIpAddress(ip);m.setCreatedAt(now());
        switch(request.type()){
            case "VAN_BAN" -> { }
            case "ANH" -> {String name=images.store(file);m.setAttachment(name);TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)images.remove(name);}});}
            case "SAN_PHAM" -> m.setPayloadJson(json.writeValueAsString(productCard(request.productId())));
            case "DON_HANG" -> m.setPayloadJson(json.writeValueAsString(orderCard(request.orderId(),c,who)));
            default -> throw new IllegalArgumentException("Loại tin nhắn không hợp lệ.");
        }
        if(who.staff()){c.setHuman(true);if(c.getAssignedUserId()==null){c.setAssignedUserId(who.userId());c.setEmployeeId(who.employeeId());jdbc.update("INSERT chat_action_audit(hoi_thoai_id,user_id,actor_name,hanh_dong,created_at) VALUES(?,?,?,N'claim',?)",id,who.userId(),actor.username(),now());}c.setWaiting(false);c.setStatus("DANG_XU_LY");staffPresence(actor);}
        else if("DA_DONG".equals(c.getStatus()))c.setStatus(c.getAssignedUserId()!=null?"DANG_XU_LY":"MOI");
        append(c,m);if(!who.staff()&&!"VAN_BAN".equals(m.getType())&&!c.isHuman()&&c.getEmployeeId()==null&&!c.isWaiting())queue(c);messages.flush();
        if(!who.staff()&&"VAN_BAN".equals(m.getType())&&!c.isHuman()&&c.getEmployeeId()==null)events.publishEvent(new AutoRequest(id,m.getId(),content,auth));
        if(who.staff())events.publishEvent(new SupportChatEvents.StaffReply(id,m.getId()));
        return view(m);
    }
    private void append(ChatConversation c,ChatMessage m){
        messages.save(m);if("KHACH".equals(m.getSenderType()))c.setStaffUnread(c.getStaffUnread()+1);else c.setCustomerUnread(c.getCustomerUnread()+1);
        c.setUpdatedAt(now());c.setLastMessageAt(now());events.publishEvent(new SupportChatEvents.Changed(c.getId(),"MESSAGE"));
    }
    @Transactional public void human(Long id,Actor actor){
        var c=locked(id,actor);if(!c.isHuman()&&!c.isWaiting())queue(c);else events.publishEvent(new SupportChatEvents.Changed(id,"STATE"));
    }
    private void queue(ChatConversation c){
        c.setWaiting(true);var m=new ChatMessage();m.setConversation(c);m.setSender("SYSTEM");m.setSenderType("HE_THONG");m.setNoiDung("Mình đã chuyển tin nhắn cho nhân viên, bạn sẽ được phản hồi sớm");m.setCreatedAt(now());append(c,m);
    }
    @Transactional(readOnly=true) public boolean automaticAllowed(Long id){return conversations.findById(id).map(c->!c.isHuman()&&c.getEmployeeId()==null).orElse(false);}
    @Transactional public void automatic(AutoRequest request,ChatbotMessageResponse response){
        var c=conversations.lock(request.conversationId()).orElse(null);if(c==null)return;entityManager.refresh(c);
        if(c.isHuman()||c.getEmployeeId()!=null)return;
        if(response==null){if(!c.isWaiting())queue(c);return;}
        String client="auto-"+request.messageId();if(messages.findByConversation_IdAndSenderTypeAndClientId(c.getId(),"TRO_LY",client).isPresent())return;
        var m=new ChatMessage();m.setConversation(c);m.setSender("BOT");m.setSenderType("TRO_LY");m.setClientId(client);String answer=ChatbotService.maskSensitiveInfo(Objects.toString(response.getText(),""));int length=Math.min(2000,answer.length());if(length>0&&Character.isHighSurrogate(answer.charAt(length-1)))length--;m.setNoiDung(answer.substring(0,length));m.setCreatedAt(now());
        if(response.getProducts()!=null&&!response.getProducts().isEmpty())m.setPayloadJson(json.writeValueAsString(response.getProducts()));append(c,m);
    }
    private Object productCard(Integer id){
        if(id==null)throw new IllegalArgumentException("Vui lòng chọn sản phẩm.");
        var p=products.findById(id).orElseThrow(()->new IllegalArgumentException("Không tìm thấy sản phẩm."));
        var items=variants.findByProduct_Id(id).stream().filter(ProductSalePolicy::isSelling).toList();
        if(!Boolean.TRUE.equals(p.getStatus())||items.isEmpty())throw new IllegalArgumentException("Sản phẩm đã ngừng bán.");
        var price=prices.calculatePrices(items).values().stream().map(v->v.getFinalPrice()).min(java.math.BigDecimal::compareTo).orElse(java.math.BigDecimal.ZERO);
        return Map.of("kind","product","id",id,"name",p.getName(),"price",price,"image",productImages.source(p.getDisplayImageUrl()),"url","/products/"+id);
    }
    private Object orderCard(Integer id,ChatConversation c,Identity who){
        if(ownerId(c)==null||(!who.staff()&&who.customerId()==null))throw denied();
        var order=orders.findById(id==null?-1:id).orElseThrow(SupportChatService::denied);
        if(order.getCustomer()==null||!order.getCustomer().getId().equals(ownerId(c)))throw denied();
        return Map.of("kind","order","id",order.getId(),"name",order.getOrderCode(),"price",order.getFinalAmount(),"status",OrderStatusService.LABELS.getOrDefault(order.getStatus(),"Đang xử lý"),"url","/orders/"+id);
    }
    @Transactional(readOnly=true) public Object ownOrders(Actor actor,int page){
        var who=identity(actor);if(who.customerId()==null)throw denied();
        return orders.findByCustomer_IdOrderByCreatedAtDescIdDesc(who.customerId(),PageRequest.of(Math.max(0,Math.min(page,10000)),12)).map(o->Map.of("id",o.getId(),"code",o.getOrderCode(),"total",o.getFinalAmount(),"status",OrderStatusService.LABELS.getOrDefault(o.getStatus(),"Đang xử lý")));
    }
    @Transactional(readOnly=true) public Object conversationOrders(Long id,Actor actor,int page){
        if(!identity(actor).staff())throw denied();var owner=ownerId(access(id,actor));if(owner==null)return org.springframework.data.domain.Page.empty();
        return orders.findByCustomer_IdOrderByCreatedAtDescIdDesc(owner,PageRequest.of(Math.max(0,Math.min(page,10000)),12)).map(o->Map.of("id",o.getId(),"code",o.getOrderCode(),"total",o.getFinalAmount()));
    }
    private Object publicPayload(Object value){
        if(value instanceof List<?> list)return list.stream().map(this::publicPayload).filter(Objects::nonNull).toList();
        if(!(value instanceof Map<?,?> source))return null;
        var result=new LinkedHashMap<String,Object>();
        for(String key:List.of("kind","id","name","code","price","finalPrice","status","url","productUrl")){
            Object v=source.get(key);if(v instanceof String||v instanceof Number||v instanceof Boolean)result.put(key,v);
        }
        for(String key:List.of("image","imageUrl"))if(source.get(key) instanceof String image)result.put(key,productImages.source(image));
        return result;
    }
    public MessageView view(ChatMessage m){
        Object payload=null;try{if(m.getPayloadJson()!=null)payload=publicPayload(json.readValue(m.getPayloadJson(),Object.class));}catch(Exception ignored){}
        return new MessageView(m.getId(),m.getClientId(),m.getSenderType(),m.getType(),m.getNoiDung(),payload,m.getAttachment()==null?null:"/api/support/"+m.getConversation().getId()+"/images/"+m.getId(),m.getCreatedAt(),m.getSeenAt());
    }
    @Transactional(readOnly=true) public java.nio.file.Path image(Long id,Long messageId,Actor actor){
        access(id,actor);var m=messages.findById(messageId).orElseThrow(SupportChatService::denied);
        if(!m.getConversation().getId().equals(id)||m.getAttachment()==null)throw denied();return images.path(m.getAttachment());
    }
}
