package com.example.demodatn.controller.admin.sanpham;
import com.example.demodatn.entity.sanpham.ProductReview;
import com.example.demodatn.repository.sanpham.*;
import com.example.demodatn.service.sanpham.ProductFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
@Controller @RequestMapping("/admin/reviews") @RequiredArgsConstructor
public class AdminReviewController {
 private final ProductReviewRepository reviews;private final ProductRepository products;private final ProductFeedbackService feedback;private final com.example.demodatn.util.Pagination pagination;
 @GetMapping public String list(@RequestParam(defaultValue="") String q,@RequestParam(required=false) Integer productId,@RequestParam(defaultValue="") String stars,@RequestParam(required=false) Boolean visible,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,@RequestParam(defaultValue="0") String page,Model model){
  Specification<ProductReview> spec=(r,x,c)->c.conjunction();
  if(!q.isBlank()){String pattern="%"+q.trim().toLowerCase(java.util.Locale.ROOT)+"%";spec=spec.and((r,x,c)->c.or(c.like(c.lower(r.get("code")),pattern),c.like(c.lower(r.get("product").get("name")),pattern)));}
  if(productId!=null)spec=spec.and((r,x,c)->c.equal(r.get("product").get("id"),productId));
  if(!stars.isBlank()){if(stars.matches("[1-5]"))spec=spec.and((r,x,c)->c.equal(r.get("stars"),Integer.valueOf(stars)));else{spec=spec.and((r,x,c)->c.disjunction());model.addAttribute("error","Số sao phải từ 1 đến 5.");}}
  if(visible!=null)spec=spec.and((r,x,c)->c.equal(r.get("visible"),visible));
  if(from!=null)spec=spec.and((r,x,c)->c.greaterThanOrEqualTo(r.get("createdAt"),from.atStartOfDay()));if(to!=null)spec=spec.and((r,x,c)->c.lessThan(r.get("createdAt"),to.plusDays(1).atStartOfDay()));
  if(from!=null&&to!=null&&to.isBefore(from))model.addAttribute("error","Ngày kết thúc phải từ ngày bắt đầu.");var selection=spec;
  model.addAttribute("reviewPage",pagination.load(page,Sort.by(Sort.Order.desc("createdAt"),Sort.Order.desc("id")),request->reviews.findAll(selection,request)));
  model.addAttribute("q",q);model.addAttribute("productId",productId);model.addAttribute("stars",stars);model.addAttribute("visible",visible);model.addAttribute("from",from);model.addAttribute("to",to);model.addAttribute("reviewProducts",products.findAll());return "admin/danh-gia/danh-sach";
 }
 @GetMapping("/{id}") public String detail(@PathVariable Integer id,Model model){model.addAttribute("review",reviews.findById(id).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND)));return "admin/danh-gia/chi-tiet";}
 @PostMapping("/status/{id}") @ResponseBody public Object visibility(@PathVariable Integer id,@RequestParam boolean active){feedback.visibility(id,active);return java.util.Map.of("active",active,"stateLabel",active?"Đang hiển thị":"Đã ẩn");}
}
