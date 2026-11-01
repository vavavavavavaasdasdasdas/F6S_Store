package com.example.demodatn.controller.user;
import com.example.demodatn.service.sanpham.*;
import com.example.demodatn.repository.sanpham.*;
import com.example.demodatn.repository.khachhang.CustomerRepository;
import com.example.demodatn.dto.sanpham.*;
import com.example.demodatn.entity.sanpham.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.validation.BindingResult;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.*;

@Controller @RequiredArgsConstructor
public class ProductFeedbackController {
 private final ProductFeedbackService feedback;private final FavoriteRepository favorites;private final CustomerRepository customers;private final CategoryRepository brands;private final ProductFilterService catalog;private final com.example.demodatn.util.Pagination pagination;
 @GetMapping("/reviews/products/{productId}") public String form(@PathVariable Integer productId,@RequestParam(required=false) Integer orderId,Principal principal,Model model){
  var product=feedback.eligibleProduct(principal.getName(),productId,orderId);var own=feedback.ownReview(principal.getName(),productId);var form=new ReviewForm();if(own!=null)org.springframework.beans.BeanUtils.copyProperties(own,form);form.setOrderId(own==null?orderId:own.getOrder().getId());model.addAttribute("reviewForm",form);model.addAttribute("reviewProduct",product);return "user/danh-gia-bieu-mau";
 }
 @PostMapping("/reviews/products/{productId}") public String save(@PathVariable Integer productId,@Valid @ModelAttribute("reviewForm") ReviewForm form,BindingResult errors,Principal principal,Model model,RedirectAttributes flash){
  var product=feedback.eligibleProduct(principal.getName(),productId,form.getOrderId());
  if(!errors.hasErrors())try{feedback.save(principal.getName(),productId,form);flash.addFlashAttribute("message","Đã lưu đánh giá sản phẩm.");return "redirect:/reviews/products/"+productId;}
  catch(com.example.demodatn.exception.PeopleValidationException ex){errors.rejectValue(ex.getField(),"invalid",ex.getMessage());}
  catch(org.springframework.dao.DataIntegrityViolationException ex){errors.rejectValue("content","duplicate","Bạn đã đánh giá sản phẩm này. Tải lại để sửa đánh giá đã có.");}
  model.addAttribute("reviewProduct",product);return "user/danh-gia-bieu-mau";
 }
 @PostMapping("/reviews/{id}/delete") public String delete(@PathVariable Integer id,@RequestParam Integer productId,Principal principal,RedirectAttributes flash){feedback.delete(principal.getName(),id);flash.addFlashAttribute("message","Đã xóa đánh giá.");return "redirect:/reviews/products/"+productId;}
 @PostMapping("/favorites/{productId}/toggle") @ResponseBody public Object favorite(@PathVariable Integer productId,Principal principal){return feedback.toggleFavorite(principal.getName(),productId);}
 @GetMapping("/favorites/return") public String back(@RequestParam(defaultValue="/favorites") String returnTo){
  try{var uri=java.net.URI.create(returnTo);if(!uri.isAbsolute()&&uri.getRawAuthority()==null&&uri.getPath().matches("/(?:products(?:/[0-9]+)?|category/[0-9]+|search|favorites)?"))return "redirect:"+uri;}catch(IllegalArgumentException ignored){}
  return "redirect:/favorites";
 }
 @GetMapping("/favorites") public String favorites(@RequestParam(defaultValue="") String keyword,@RequestParam(required=false) Integer categoryId,@RequestParam(defaultValue="0") String page,Principal principal,Model model){
  Integer cid=customers.findByUser_Username(principal.getName()).map(c->c.getId()).orElse(-1);Specification<Favorite> spec=(r,q,c)->c.equal(r.get("customerId"),cid);
  if(!keyword.isBlank()){String pattern="%"+keyword.trim().toLowerCase(Locale.ROOT)+"%";spec=spec.and((r,q,c)->c.or(c.like(c.lower(r.get("product").get("name")),pattern),c.like(c.lower(r.get("product").get("code")),pattern)));}
  if(categoryId!=null)spec=spec.and((r,q,c)->c.equal(r.get("product").get("category").get("id"),categoryId));var selection=spec;
  model.addAttribute("favoritePage",pagination.load(page,Sort.by(Sort.Order.desc("createdAt"),Sort.Order.desc("productId")),request->favorites.findAll(selection,request)));
  var active=catalog.filterProducts(null,null,null,null,null,null,"newest",0,Integer.MAX_VALUE).getContent();model.addAttribute("favoriteCards",active.stream().collect(java.util.stream.Collectors.toMap(ProductDto::getId,p->p)));
  model.addAttribute("keyword",keyword);model.addAttribute("categoryId",categoryId);model.addAttribute("brands",brands.findAll());return "user/yeu-thich";
 }
}
