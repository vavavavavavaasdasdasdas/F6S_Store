package com.example.demodatn.service.hotro;
import com.example.demodatn.repository.hotro.SupportChatInboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class SupportChatInboxService {
    private final SupportChatService chat;
    private final SupportChatInboxRepository repository;
    public int staff(SupportChatService.Actor actor){var who=chat.identity(actor);if(!who.staff())throw new ResponseStatusException(HttpStatus.FORBIDDEN);return who.userId();}
    public Object list(SupportChatService.Actor actor,String q,String tab,int page){int user=staff(actor);var rows=new ArrayList<>(repository.list(q,tab,page,user));boolean more=rows.size()>20;if(more)rows.remove(20);return Map.of("content",rows,"hasMore",more,"counts",repository.counts(q,user),"userId",user);}
    public Map<String,Object> counts(SupportChatService.Actor actor){return repository.counts("",staff(actor));}
    public Map<String,Object> detail(long id,SupportChatService.Actor actor){int user=staff(actor);chat.access(id,actor);var data=repository.detail(id);data.put("viewerUserId",user);if(data.get("customerId") instanceof Number customer){data.putAll(repository.metrics(customer.intValue()));data.put("orders",repository.orders(customer.intValue()));}else{data.put("orders",List.of());}data.put("audit",repository.audit(id));return data;}
    public Integer invoice(int order){return repository.invoice(order);}
    public Object products(SupportChatService.Actor actor,String q){staff(actor);return repository.products(q);}
    public Object replies(SupportChatService.Actor actor){staff(actor);var rows=new ArrayList<Map<String,Object>>();String[][] defaults={{"Chào khách","Xin chào bạn! Cửa hàng có thể hỗ trợ gì cho bạn?"},{"Hỏi mã đơn","Bạn cho cửa hàng xin mã đơn hàng để kiểm tra nhé."},{"Xin lỗi vì chờ lâu","Cửa hàng xin lỗi vì đã để bạn chờ. Nhân viên đang kiểm tra thông tin cho bạn."},{"Đổi trả","Cửa hàng hỗ trợ đổi trả theo chính sách tại trang Chính sách. Bạn gửi mã đơn và tình trạng sản phẩm để cửa hàng kiểm tra nhé."},{"Cảm ơn","Cảm ơn bạn đã tin tưởng cửa hàng. Chúc bạn một ngày tốt lành!"}};for(var item:defaults)rows.add(Map.of("title",item[0],"answer",item[1]));rows.addAll(repository.faq());return rows;}
}
