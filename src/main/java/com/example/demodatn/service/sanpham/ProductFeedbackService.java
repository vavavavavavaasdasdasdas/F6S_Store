package com.example.demodatn.service.sanpham;
import com.example.demodatn.dto.sanpham.ReviewForm;
import com.example.demodatn.entity.khachhang.Customer;
import com.example.demodatn.entity.sanpham.*;
import com.example.demodatn.repository.sanpham.*;
import com.example.demodatn.repository.khachhang.CustomerRepository;
import com.example.demodatn.repository.donhang.OrderRepository;
import com.example.demodatn.exception.PeopleValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class ProductFeedbackService {
 private final CustomerRepository customers;private final ProductRepository products;private final ProductReviewRepository reviews;private final FavoriteRepository favorites;private final OrderRepository orders;private final JdbcTemplate jdbc;private final Clock clock;private final jakarta.persistence.EntityManager entityManager;
 private LocalDateTime now(){return LocalDateTime.ofInstant(clock.instant(),com.example.demodatn.service.donhang.DeliveryService.SHOP_ZONE);}
 private Customer customer(String username,boolean lock){
  var c=(lock?customers.findForUpdateByUsername(username):customers.findByUser_Username(username)).orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN,"Cần tài khoản khách hàng đang hoạt động."));
  // Request advice may have loaded this entity before another request acquired the row lock.
  if(lock)entityManager.refresh(c);
  if(!Boolean.TRUE.equals(c.getStatus()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Cần tài khoản khách hàng đang hoạt động.");return c;
 }

 public record Summary(double average,long count){}
 @Transactional(readOnly=true) public Map<Integer,Summary> summaries(){var result=new HashMap<Integer,Summary>();for(var r:reviews.summaries())result.put(((Number)r[0]).intValue(),new Summary(((Number)r[1]).doubleValue(),((Number)r[2]).longValue()));return result;}
 @Transactional(readOnly=true) public Map<Integer,Long> distribution(Integer id){var result=new LinkedHashMap<Integer,Long>();for(int star=5;star>=1;star--)result.put(star,0L);for(var row:reviews.distribution(id))result.put(((Number)row[0]).intValue(),((Number)row[1]).longValue());return result;}
 @Transactional(readOnly=true) public Set<Integer> favoriteIds(String username){return customers.findByUser_Username(username).map(c->favorites.productIds(c.getId())).orElse(Set.of());}
 @Transactional(readOnly=true) public ProductReview ownReview(String username,Integer productId){return customers.findByUser_Username(username).flatMap(c->reviews.findByCustomer_IdAndProduct_Id(c.getId(),productId)).orElse(null);}
 @Transactional(readOnly=true) public boolean canReview(String username,Integer productId){var c=customers.findByUser_Username(username).orElse(null);if(c==null)return false;try{deliveredOrder(c,productId,null);return true;}catch(ResponseStatusException ex){return false;}}
 private Integer deliveredOrder(Customer customer,Integer productId,Integer orderId){
  var args=new ArrayList<Object>(List.of(customer.getId(),productId));String condition="";if(orderId!=null){condition=" AND o.id=?";args.add(orderId);}
  var ids=jdbc.queryForList("SELECT TOP 1 o.id FROM orders o WHERE o.customer_id=? AND o.status='DELIVERED' AND EXISTS(SELECT 1 FROM order_details d JOIN product_variants v ON v.id=d.product_variant_id WHERE d.order_id=o.id AND v.product_id=?)"+condition+" ORDER BY o.id DESC",Integer.class,args.toArray());
  if(ids.isEmpty())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Chỉ khách đã nhận hàng mới được đánh giá sản phẩm này.");return ids.get(0);
 }
 @Transactional(readOnly=true) public Product eligibleProduct(String username,Integer productId,Integer orderId){var p=products.findById(productId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));deliveredOrder(customer(username,false),productId,orderId);return p;}
 private void rate(Customer customer){var current=now();if(customer.getReviewWindow()==null||!customer.getReviewWindow().plusMinutes(1).isAfter(current)){customer.setReviewWindow(current);customer.setReviewCount(0);}if(customer.getReviewCount()>=5)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Bạn gửi đánh giá quá nhanh. Vui lòng thử lại sau một phút.");customer.setReviewCount(customer.getReviewCount()+1);jdbc.update("UPDATE customers SET danh_gia_cua_so=?,danh_gia_so_lan=? WHERE id=?",customer.getReviewWindow(),customer.getReviewCount(),customer.getId());}
 @Transactional public ProductReview save(String username,Integer productId,ReviewForm form){
  var c=customer(username,true);var p=products.findById(productId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  if(form.getStars()==null||form.getStars()<1||form.getStars()>5)throw new PeopleValidationException("stars","Số sao từ 1 đến 5.");if(form.getContent()!=null&&form.getContent().length()>1000)throw new PeopleValidationException("content","Nội dung tối đa 1000 ký tự.");
  var existing=reviews.findByCustomer_IdAndProduct_Id(c.getId(),productId).orElse(null);ProductReview review;
  if(form.getId()!=null){review=reviews.findById(form.getId()).filter(r->r.getCustomer().getId().equals(c.getId())&&r.getProduct().getId().equals(productId)).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));}
  else {if(existing!=null)throw new PeopleValidationException("content","Bạn đã đánh giá sản phẩm này. Chọn Sửa đánh giá.");review=new ProductReview();review.setCustomer(c);review.setProduct(p);review.setCreatedAt(now());}
  Integer orderId=deliveredOrder(c,productId,review.getId()==null?form.getOrderId():review.getOrder().getId());rate(c);
  review.setOrder(orders.getReferenceById(orderId));review.setStars(form.getStars());review.setContent(form.getContent()==null?"":form.getContent().trim());review.setUpdatedAt(now());return reviews.saveAndFlush(review);
 }
 @Transactional public void delete(String username,Integer id){var c=customer(username,true);var r=reviews.findById(id).filter(x->x.getCustomer().getId().equals(c.getId())).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));reviews.delete(r);reviews.flush();}
 @Transactional public void visibility(Integer id,boolean visible){var r=reviews.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));r.setVisible(visible);}
 public record FavoriteState(boolean favorite){}
 @Transactional public FavoriteState toggleFavorite(String username,Integer productId){
  var c=customer(username,true);if(!products.existsById(productId))throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  var key=new Favorite.Key(c.getId(),productId);var old=favorites.findById(key);boolean favorite=old.isEmpty();
  if(old.isPresent())favorites.delete(old.get());else{var f=new Favorite();f.setCustomerId(c.getId());f.setProductId(productId);f.setProduct(products.getReferenceById(productId));f.setCreatedAt(now());favorites.save(f);}favorites.flush();return new FavoriteState(favorite);
 }
}
