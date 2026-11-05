package com.example.demodatn.controller.admin.hotro;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
/** Only compatibility redirects remain for the retired ticket/FAQ pages. */
@Controller
public class AdminSupportTicketController {
    public static final String TICKETS="/admin/support-tickets",FAQ="/admin/chatbot-faq";
    public static boolean legacy(String path){return path.equals(TICKETS)||path.startsWith(TICKETS+"/")||path.equals(FAQ)||path.startsWith(FAQ+"/");}
    @RequestMapping(value={TICKETS,TICKETS+"/{*path}",FAQ,FAQ+"/{*path}"},method={RequestMethod.GET,RequestMethod.POST})
    public String redirect(){return "redirect:/admin/chat";}
}
