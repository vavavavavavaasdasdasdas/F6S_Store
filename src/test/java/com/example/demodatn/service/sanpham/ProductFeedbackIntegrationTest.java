package com.example.demodatn.service.sanpham;
import com.example.demodatn.entity.sanpham.*;
import com.example.demodatn.entity.khachhang.Customer;
import com.example.demodatn.entity.nhanvien.User;
import com.example.demodatn.dto.sanpham.ReviewForm;
import com.example.demodatn.dto.donhang.*;
import com.example.demodatn.repository.sanpham.*;
import com.example.demodatn.repository.khachhang.CustomerRepository;
import com.example.demodatn.repository.nhanvien.UserRepository;
import com.example.demodatn.service.donhang.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @Transactional @WithMockUser(username="admin",roles="ADMIN")
class ProductFeedbackIntegrationTest {
 @Autowired ProductFeedbackService feedback;@Autowired ProductReviewRepository reviews;@Autowired FavoriteRepository favorites;@Autowired ProductRepository products;@Autowired ProductVariantRepository variants;
 @Autowired CustomerRepository customers;@Autowired UserRepository users;@Autowired OrderService orders;@Autowired OrderStatusService statuses;@Autowired JdbcTemplate jdbc;@Autowired jakarta.persistence.EntityManager em;@Autowired WebApplicationContext context;
 MockMvc mvc;Customer owner;ProductVariant variant;
 @BeforeEach void setup(){assertTrue(jdbc.queryForObject("SELECT DB_NAME()",String.class).startsWith("shopbangiay_kt_"));mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();owner=customer();variant=variants.findByStatusTrueAndProduct_StatusTrue().stream().filter(v->v.getQuantity()>10).findFirst().orElseThrow();}
 Customer customer(){String id=UUID.randomUUID().toString().substring(0,8);var u=new User();u.setUsername("g14_"+id);u.setPassword(users.findByUsername("admin").orElseThrow().getPassword());u.setRole(em.getReference(com.example.demodatn.entity.nhanvien.Role.class,jdbc.queryForObject("SELECT id FROM roles WHERE name='ROLE_USER'",Integer.class)));users.saveAndFlush(u);var c=new Customer();c.setUser(u);c.setFullName("Nguyễn Văn Ánh "+id);c.setCustomerCode("G14-"+id);c.setPhone("09"+String.format("%08d",Math.floorMod(id.hashCode(),100000000)));c.setEmail(id+"@example.test");return customers.saveAndFlush(c);}
 Integer productId(){return variant.getProduct().getId();}
 com.example.demodatn.entity.donhang.Order purchase(Customer c,boolean delivered){var f=new CheckoutRequest();var w=jdbc.queryForMap("SELECT TOP 1 id,tinh_thanh_id FROM phuong_xa ORDER BY id");f.setProvinceId(((Number)w.get("tinh_thanh_id")).intValue());f.setWardId(((Number)w.get("id")).intValue());f.setStreetAddress("Số 12, đường Nguyễn Huệ");f.setReceiverName(c.getFullName());f.setPhone(c.getPhone());f.setPaymentMethod("COD");var o=orders.placeOrder(f,List.of(new CartItemDto(productId(),variant.getId(),variant.getProduct().getName(),variant.getSize().getSizeValue(),variant.getPrice(),1)),c,BigDecimal.ZERO);if(delivered)for(String state:List.of("CONFIRMED","PREPARING","SHIPPING","DELIVERED"))statuses.transition(o.getId(),state,null,null,null);em.flush();return o;}
 ReviewForm form(Integer order,int stars){var f=new ReviewForm();f.setOrderId(order);f.setStars(stars);f.setContent("Đi rất êm — tiếng Việt nguyên vẹn");return f;}
 String name(Customer c){return c.getUser().getUsername();}
 @Test void receivedPurchaseIsRequiredAndDuplicateAndCrossOwnerAreRejected()throws Exception{
  var pending=purchase(owner,false);mvc.perform(post("/reviews/products/"+productId()).with(user(name(owner)).roles("USER")).with(csrf()).param("orderId",pending.getId().toString()).param("stars","5").param("content","Chưa nhận hàng")).andExpect(status().isForbidden());
  assertEquals(0,reviews.count());for(String s:List.of("CONFIRMED","PREPARING","SHIPPING","DELIVERED"))statuses.transition(pending.getId(),s,null,null,null);em.flush();
  var f=form(pending.getId(),5);var review=feedback.save(name(owner),productId(),f);assertTrue(review.getCode().matches("DG[0-9]{4,}"));assertThrows(com.example.demodatn.exception.PeopleValidationException.class,()->feedback.save(name(owner),productId(),f));
  var other=customer();purchase(other,true);var forged=form(pending.getId(),1);forged.setId(review.getId());assertThrows(org.springframework.web.server.ResponseStatusException.class,()->feedback.save(name(other),productId(),forged));assertThrows(org.springframework.web.server.ResponseStatusException.class,()->feedback.delete(name(other),review.getId()));assertEquals(1,reviews.count());
  System.out.println("GD14 pending=403 delivered=allowed duplicate=blocked foreign edit/delete=404; count=1");
 }
 @Test void averageDistributionAndHiddenReviewsMatchIndependentSql(){
  var a=feedback.save(name(owner),productId(),form(purchase(owner,true).getId(),5));var b=customer();feedback.save(name(b),productId(),form(purchase(b,true).getId(),4));var c=customer();feedback.save(name(c),productId(),form(purchase(c,true).getId(),3));
  assertEquals(4.0,feedback.summaries().get(productId()).average());feedback.visibility(a.getId(),false);em.flush();var summary=feedback.summaries().get(productId());var independent=jdbc.queryForObject("SELECT AVG(CAST(so_sao AS DECIMAL(10,2))) FROM danh_gia WHERE product_id=? AND hien_thi=1",BigDecimal.class,productId());assertEquals(independent.doubleValue(),summary.average());assertEquals(3.5,summary.average());assertEquals(2,summary.count());assertEquals(1L,feedback.distribution(productId()).get(3));assertEquals(0L,feedback.distribution(productId()).get(5));
  var edit=form(a.getOrder().getId(),2);edit.setId(a.getId());feedback.save(name(owner),productId(),edit);assertFalse(reviews.findById(a.getId()).orElseThrow().isVisible());
  System.out.println("GD14 visible average=3.5 count=2 independent SQL="+independent+"; hidden edit stays hidden");
 }
 @Test void ownEditDeleteAndRecreateKeepUnicodeAndNeverReuseCode(){var o=purchase(owner,true);var f=form(o.getId(),5);var r=feedback.save(name(owner),productId(),f);String code=r.getCode();f.setId(r.getId());f.setStars(4);f.setContent("Số 12, đường Nguyễn Huệ, Đà Nẵng");feedback.save(name(owner),productId(),f);assertEquals(f.getContent(),jdbc.queryForObject("SELECT noi_dung FROM danh_gia WHERE id=?",String.class,r.getId()));feedback.delete(name(owner),r.getId());f.setId(null);var next=feedback.save(name(owner),productId(),f);assertNotEquals(code,next.getCode());assertEquals(1,reviews.count());}
 @Test void rateLimitIsPersistedAcrossDeleteAndResetAndLengthValidation(){
  var o=purchase(owner,true);var f=form(o.getId(),5);for(int i=0;i<5;i++){var r=feedback.save(name(owner),productId(),f);f.setId(r.getId());f.setContent("Lần "+i);}var ex=assertThrows(org.springframework.web.server.ResponseStatusException.class,()->feedback.save(name(owner),productId(),f));assertEquals(429,ex.getStatusCode().value());assertEquals(5,jdbc.queryForObject("SELECT danh_gia_so_lan FROM customers WHERE id=?",Integer.class,owner.getId()));feedback.delete(name(owner),f.getId());f.setId(null);assertThrows(org.springframework.web.server.ResponseStatusException.class,()->feedback.save(name(owner),productId(),f));
  jdbc.update("UPDATE customers SET danh_gia_cua_so=DATEADD(MINUTE,-2,SYSDATETIME()) WHERE id=?",owner.getId());em.refresh(owner);feedback.save(name(owner),productId(),f);assertEquals(1,jdbc.queryForObject("SELECT danh_gia_so_lan FROM customers WHERE id=?",Integer.class,owner.getId()));f.setContent("x".repeat(1001));assertThrows(com.example.demodatn.exception.PeopleValidationException.class,()->feedback.save(name(owner),productId(),f));
  System.out.println("GD14 rate five/minute; sixth=429; delete does not reset; elapsed window resets to 1");
 }
 @Test void favoriteToggleAndInactiveGridAndXssEscape()throws Exception{
  var first=feedback.toggleFavorite(name(owner),productId());assertTrue(first.favorite());assertEquals(1,favorites.countByProductId(productId()));assertFalse(feedback.toggleFavorite(name(owner),productId()).favorite());assertEquals(0,favorites.count());feedback.toggleFavorite(name(owner),productId());
  var f=form(purchase(owner,true).getId(),5);f.setContent("<script>window.__g14xss=1</script> Nguyễn Huệ");feedback.save(name(owner),productId(),f);
  mvc.perform(get("/products/"+productId())).andExpect(status().isOk()).andExpect(model().attribute("reviewSummaries",hasKey(productId()))).andExpect(content().string(containsString("product-review-score"))).andExpect(content().string(containsString("&lt;script&gt;"))).andExpect(content().string(not(containsString("<script>window.__g14xss"))));
  variant.getProduct().setStatus(false);products.flush();var response=mvc.perform(get("/favorites").with(user(name(owner)).roles("USER"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();assertTrue(response.contains("Ngừng bán"));assertFalse(response.contains("href=\"/products/"+productId()+"\""));
 }
 @Test void eightSecurityGroupsAndAdminCannotDelete()throws Exception{
  var review=feedback.save(name(owner),productId(),form(purchase(owner,true).getId(),5));
  mvc.perform(get("/favorites").with(anonymous())).andExpect(status().isFound());
  mvc.perform(post("/favorites/"+productId()+"/toggle")).andExpect(status().isForbidden());mvc.perform(post("/favorites/"+productId()+"/toggle").with(csrf().useInvalidToken())).andExpect(status().isForbidden());
  mvc.perform(post("/reviews/products/"+productId()).with(user(name(owner)).roles("USER"))).andExpect(status().isForbidden());mvc.perform(post("/reviews/"+review.getId()+"/delete").with(csrf().useInvalidToken())).andExpect(status().isForbidden());
  mvc.perform(get("/admin/reviews").with(user(name(owner)).roles("USER"))).andExpect(status().isForbidden());mvc.perform(get("/admin/reviews").with(user("staff").roles("EMPLOYEE"))).andExpect(status().isForbidden());mvc.perform(get("/admin/reviews")).andExpect(status().isOk());
  mvc.perform(post("/admin/reviews/"+review.getId()).with(csrf())).andExpect(status().isMethodNotAllowed());
  mvc.perform(get("/favorites/return").param("returnTo","//example.test/evil")).andExpect(redirectedUrl("/favorites"));
  System.out.println("GD14 security eight: 302 403 403 403 403 403 403 200; admin delete=405; external redirect rejected");
 }
 @Test void adminAndPublicPagesHandleEmptyAndStrangePages()throws Exception{for(String page:List.of("-1","abc","99999")){mvc.perform(get("/admin/reviews").param("page",page)).andExpect(status().isOk());mvc.perform(get("/favorites").param("page",page).with(user(name(owner)).roles("USER"))).andExpect(status().isOk());mvc.perform(get("/products/"+productId()).param("reviewPage",page)).andExpect(status().isOk());}mvc.perform(get("/admin/reviews").param("from","2026-12-31").param("to","2026-01-01")).andExpect(status().isOk()).andExpect(model().attributeExists("error"));}
 @Test void reviewAndFavoritePaginationUseTwelveAndContinueAdminRowNumbers()throws Exception{
  for(int n=0;n<13;n++){var c=n==0?owner:customer();feedback.save(name(c),productId(),form(purchase(c,true).getId(),n%5+1));}
  var publicPage=mvc.perform(get("/products/"+productId()).param("reviewPage","1")).andExpect(status().isOk()).andReturn();var page=(org.springframework.data.domain.Page<?>)publicPage.getModelAndView().getModel().get("reviews");assertEquals(12,page.getSize());assertEquals(1,page.getNumberOfElements());
  mvc.perform(get("/admin/reviews").param("page","1")).andExpect(status().isOk()).andExpect(content().string(containsString("<td>13</td>")));
  for(int n=0;n<13;n++){var p=new Product();p.setCode("G14-"+UUID.randomUUID().toString().substring(0,8));p.setName("Giày yêu thích phân trang "+n);p.setStatus(true);products.saveAndFlush(p);var v=new ProductVariant();v.setProduct(p);v.setSize(variant.getSize());v.setMaterial(variant.getMaterial());v.setColor(variant.getColor());v.setSole(variant.getSole());v.setPrice(BigDecimal.valueOf(100000));v.setQuantity(5);variants.saveAndFlush(v);feedback.toggleFavorite(name(owner),p.getId());}
  var favoritesPage=mvc.perform(get("/favorites").param("page","1").with(user(name(owner)).roles("USER"))).andExpect(status().isOk()).andReturn();var liked=(org.springframework.data.domain.Page<?>)favoritesPage.getModelAndView().getModel().get("favoritePage");assertEquals(12,liked.getSize());assertEquals(1,liked.getNumberOfElements());
  System.out.println("GD14 pagination reviews/admin/favorites size=12; page2 one row/card; admin STT=13");
 }
 @Test void wishlistDoesNotBlockExistingDeletionOfUnusedCustomerOrProduct(){
  var c=customer();feedback.toggleFavorite(name(c),productId());customers.delete(c);customers.flush();assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM yeu_thich WHERE customer_id=?",Integer.class,c.getId()));
  var p=new Product();p.setCode("G14-"+UUID.randomUUID().toString().substring(0,8));p.setName("Giày chưa giao dịch");p.setStatus(true);products.saveAndFlush(p);feedback.toggleFavorite(name(owner),p.getId());Integer unusedId=p.getId();em.flush();em.clear();/* Deletion is a separate HTTP request with a fresh persistence context. */products.deleteById(unusedId);products.flush();assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM yeu_thich WHERE product_id=?",Integer.class,unusedId));
 }
}
