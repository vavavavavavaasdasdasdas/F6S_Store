package com.example.demodatn.controller.admin.donhang;
import com.example.demodatn.repository.donhang.InvoiceRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
/** Compatibility routes only. Processing belongs to the invoice workspace. */
@Controller @RequestMapping(AdminOrderController.LEGACY_PATH) @RequiredArgsConstructor
public class AdminOrderController {
 public static final String LEGACY_PATH="/admin/orders";
 private final InvoiceRepository invoices;
 public static final java.util.Map<String,String> STATUS_LABELS=com.example.demodatn.service.donhang.OrderStatusService.LABELS;
 @GetMapping public ResponseEntity<Void> list(HttpServletRequest request){return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/admin/invoices"+(request.getQueryString()==null?"":"?"+request.getQueryString()))).build();}
 private String target(Integer id){return "/admin/invoices/"+invoices.findByOrder_Id(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND)).getId();}
 @GetMapping({"/{id}","/detail/{id}"}) public ResponseEntity<Void> detail(@PathVariable Integer id){return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(target(id))).build();}
 @PostMapping({"/update-status/{id}","/{id}/status"}) public ResponseEntity<Void> status(@PathVariable Integer id){return forward(id,"status");}
 @PostMapping("/{id}/confirm-payment") public ResponseEntity<Void> payment(@PathVariable Integer id){return forward(id,"confirm-payment");}
 @PostMapping("/{id}/province") public ResponseEntity<Void> province(@PathVariable Integer id){return forward(id,"province");}
 private ResponseEntity<Void> forward(Integer id,String action){return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT).location(URI.create(target(id)+"/"+action)).build();}
}
