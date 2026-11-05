package com.example.demodatn.controller.admin.hotro;
import com.example.demodatn.controller.user.SupportChatApiController;
import com.example.demodatn.service.hotro.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
@Controller @RequiredArgsConstructor
@RequestMapping("/admin/chat")
public class AdminChatController {
    private final SupportChatInboxService inbox;
    private final SupportChatService chat;
    @ExceptionHandler(IllegalArgumentException.class) @ResponseBody public org.springframework.http.ResponseEntity<?> invalid(IllegalArgumentException e){return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("message",e.getMessage()));}
    @GetMapping public String page(HttpServletRequest r){inbox.staff(SupportChatApiController.actor(r));return "admin/chat/inbox";}
    @GetMapping("/api/conversations") @ResponseBody public Object list(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="all") String tab,@RequestParam(defaultValue="0") int page,HttpServletRequest r){return inbox.list(SupportChatApiController.actor(r),q,tab,page);}
    @GetMapping("/api/counts") @ResponseBody public Object counts(HttpServletRequest r){return inbox.counts(SupportChatApiController.actor(r));}
    @GetMapping("/api/{id}") @ResponseBody public Object detail(@PathVariable long id,HttpServletRequest r){return inbox.detail(id,SupportChatApiController.actor(r));}
    @GetMapping("/api/quick-replies") @ResponseBody public Object replies(HttpServletRequest r){return inbox.replies(SupportChatApiController.actor(r));}
    @GetMapping("/api/products") @ResponseBody public Object products(@RequestParam(defaultValue="") String q,HttpServletRequest r){return inbox.products(SupportChatApiController.actor(r),q);}
    @GetMapping("/api/{id}/orders") @ResponseBody public Object orders(@PathVariable long id,@RequestParam(defaultValue="0") int page,HttpServletRequest r){return chat.conversationOrders(id,SupportChatApiController.actor(r),page);}
    @GetMapping("/order/{id}") public String invoice(@PathVariable int id,HttpServletRequest r){inbox.staff(SupportChatApiController.actor(r));Integer invoice=inbox.invoice(id);if(invoice==null)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);return "redirect:/admin/invoices/"+invoice;}
    @PostMapping("/api/{id}/{action:claim|queue|close|reopen}") @ResponseBody public Object act(@PathVariable long id,@PathVariable String action,HttpServletRequest r){chat.act(id,SupportChatApiController.actor(r),action);return java.util.Map.of("updated",true);}
}
