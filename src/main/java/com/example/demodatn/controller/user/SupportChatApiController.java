package com.example.demodatn.controller.user;

import com.example.demodatn.dto.hotro.SupportChatRequest;
import com.example.demodatn.service.hotro.SupportChatService;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.Duration;
import java.util.*;

@RestController @RequiredArgsConstructor
public class SupportChatApiController {
    public static final String COOKIE="F6S_SUPPORT";
    private final SupportChatService chat;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    public static String cookie(HttpServletRequest request){return request.getCookies()==null?null:Arrays.stream(request.getCookies()).filter(c->COOKIE.equals(c.getName())).map(Cookie::getValue).findFirst().orElse(null);}
    public static SupportChatService.Actor actor(HttpServletRequest request){
        var principal=request.getUserPrincipal();String username=principal==null||principal instanceof AnonymousAuthenticationToken?null:principal.getName();
        return new SupportChatService.Actor(username,SupportChatService.hashToken(cookie(request)));
    }
    private String ip(HttpServletRequest request){return request.getRemoteAddr();}
    @GetMapping("/api/support/session") public Object session(HttpServletRequest request){
        var a=actor(request);var current=chat.current(a);if(current.isPresent())chat.customerPresent(a);var result=new LinkedHashMap<String,Object>();var csrf=(org.springframework.security.web.csrf.CsrfToken)request.getAttribute(org.springframework.security.web.csrf.CsrfToken.class.getName());result.put("csrfToken",csrf==null?null:csrf.getToken());result.put("authenticated",a.username()!=null);String scope=chat.guestScope(a);result.put("mergePending",a.username()!=null&&scope!=null);result.put("mergeFromScope",a.username()!=null?scope:null);result.put("identified",current.isPresent());result.put("conversation",current.orElse(null));return result;
    }
    public record Contact(String name,String contact) {}
    @PostMapping("/api/support/session") public ResponseEntity<?> open(@RequestBody(required=false) Contact contact,HttpServletRequest request){
        var a=actor(request);String token=cookie(request);
        if(token!=null && a.username()==null)chat.current(a); // A supplied but unknown cookie must never mint an identity.
        if(token==null && a.username()==null){token=SupportChatService.newToken();a=new SupportChatService.Actor(null,SupportChatService.hashToken(token));}
        var opened=chat.open(a,contact==null?null:contact.name(),contact==null?null:contact.contact(),ip(request));
        var response=ResponseEntity.ok();
        if(a.username()==null)response.header(HttpHeaders.SET_COOKIE,ResponseCookie.from(COOKIE,token).httpOnly(true).secure(request.isSecure()).sameSite("Lax").path("/").maxAge(Duration.ofDays(180)).build().toString());
        else if(token!=null)response.header(HttpHeaders.SET_COOKIE,ResponseCookie.from(COOKIE,"").httpOnly(true).secure(request.isSecure()).sameSite("Lax").path("/").maxAge(0).build().toString());
        return response.body(opened);
    }
    @PostMapping("/api/support/reset-session") public ResponseEntity<?> reset(HttpServletRequest request){return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,ResponseCookie.from(COOKIE,"").path("/").httpOnly(true).sameSite("Lax").maxAge(0).build().toString()).body(Map.of("reset",true));}
    @GetMapping({"/api/support/{id}/messages","/admin/support-chat/{id}/messages"})
    public Object history(@PathVariable Long id,@RequestParam(required=false) Long before,HttpServletRequest request){var actor=actor(request);var history=chat.history(id,before,actor);chat.customerPresent(actor);return history;}
    @PostMapping(value={"/api/support/{id}/messages","/admin/support-chat/{id}/messages"},consumes=MediaType.APPLICATION_JSON_VALUE)
    public Object send(@PathVariable Long id,@Valid @RequestBody SupportChatRequest body,HttpServletRequest request,Authentication auth){return chat.send(id,body,null,actor(request),ip(request),auth);}
    @PostMapping(value={"/api/support/{id}/messages","/admin/support-chat/{id}/messages"},consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Object image(@PathVariable Long id,@Valid @ModelAttribute SupportChatRequest body,@RequestParam MultipartFile file,HttpServletRequest request,Authentication auth){
        if(!"ANH".equals(body.type()))throw new IllegalArgumentException("File chỉ được gửi trong tin nhắn ảnh.");return chat.send(id,body,file,actor(request),ip(request),auth);
    }
    @PostMapping({"/api/support/{id}/read","/admin/support-chat/{id}/read"})
    public Object read(@PathVariable Long id,@RequestParam(defaultValue="0") long through,HttpServletRequest request){chat.read(id,through,actor(request));return Map.of("read",true);}
    @PostMapping({"/api/support/{id}/typing","/admin/support-chat/{id}/typing"})
    public Object typing(@PathVariable Long id,HttpServletRequest request){chat.typing(id,actor(request));return Map.of("typing",true);}
    @PostMapping("/api/support/{id}/human") public Object human(@PathVariable Long id,HttpServletRequest request){chat.human(id,actor(request));return Map.of("queued",true);}
    @GetMapping("/api/support/orders") public Object orders(@RequestParam(defaultValue="0") int page,HttpServletRequest request){return chat.ownOrders(actor(request),page);}
    @GetMapping("/api/support/{id}/images/{messageId}") public ResponseEntity<?> file(@PathVariable Long id,@PathVariable Long messageId,HttpServletRequest request){
        var path=chat.image(id,messageId,actor(request));if(!java.nio.file.Files.isRegularFile(path))return ResponseEntity.notFound().build();
        String name=path.getFileName().toString(),mime=name.endsWith(".png")?"image/png":name.endsWith(".webp")?"image/webp":"image/jpeg";
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL,"private, no-store").header("X-Content-Type-Options","nosniff").contentType(MediaType.parseMediaType(mime)).body(new FileSystemResource(path));
    }
    @GetMapping("/admin/support-chat") public Object inbox(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="") String status,@RequestParam(defaultValue="") String q,HttpServletRequest request){
        if(!chat.identity(actor(request)).staff())throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN);
        if(!status.isBlank()&&!Set.of("MOI","DANG_XU_LY","DA_DONG").contains(status))throw new IllegalArgumentException("Trạng thái không hợp lệ.");
        String query=q.trim().replace("!","!!").replace("%","!%").replace("_","!_");
        return jdbc.queryForList("SELECT id,ma,ho_ten,trang_thai,nhan_vien_phu_trach_id,last_message_at,chua_doc_nhan_vien FROM chat_hoi_thoai WHERE (customer_id IS NOT NULL OR khach_id IS NULL) AND (?='' OR trang_thai=?) AND (ma LIKE ? ESCAPE '!' OR ho_ten LIKE ? ESCAPE '!') ORDER BY last_message_at DESC,id DESC OFFSET ? ROWS FETCH NEXT 12 ROWS ONLY",status,status,"%"+query+"%","%"+query+"%",Math.max(0,Math.min(page,10000))*12);
    }
    @PostMapping("/admin/support-chat/{id}/claim") public Object claim(@PathVariable Long id,HttpServletRequest request){chat.claim(id,actor(request),false);return Map.of("claimed",true);}
    @PostMapping("/admin/support-chat/{id}/close") public Object close(@PathVariable Long id,HttpServletRequest request){chat.claim(id,actor(request),true);return Map.of("closed",true);}
    @PostMapping("/admin/support-chat/presence") public Object presence(HttpServletRequest request){chat.staffPresence(actor(request));return Map.of("online",true);}
    @ExceptionHandler({IllegalArgumentException.class,com.example.demodatn.exception.CatalogValidationException.class})
    public ResponseEntity<?> invalid(RuntimeException error){return ResponseEntity.badRequest().body(Map.of("message",Objects.toString(error.getMessage(),"Thông tin chat không hợp lệ.")));}
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<?> validation(org.springframework.web.bind.MethodArgumentNotValidException error){return ResponseEntity.badRequest().body(Map.of("message","Kiểm tra mã tin nhắn, loại tin và nội dung tối đa 2000 ký tự."));}
}
