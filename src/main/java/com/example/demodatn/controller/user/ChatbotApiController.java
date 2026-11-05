package com.example.demodatn.controller.user;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

/** Legacy client-controlled session endpoints are retired; stored history is retained. */
@RestController
@RequestMapping("/api/chatbot")
public class ChatbotApiController {
    @PostMapping({"/message","/clear","/ticket"})
    public ResponseEntity<Void> legacy(){
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/api/support/session")).build();
    }
}
