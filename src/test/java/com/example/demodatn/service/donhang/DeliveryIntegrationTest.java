package com.example.demodatn.service.donhang;
import com.example.demodatn.dto.donhang.*;
import com.example.demodatn.entity.donhang.*;
import com.example.demodatn.repository.donhang.*;
import com.example.demodatn.repository.khachhang.CustomerRepository;
import com.example.demodatn.repository.sanpham.ProductVariantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
@SpringBootTest @Transactional @WithMockUser(username="admin",roles="ADMIN")
class DeliveryIntegrationTest {
 @Autowired OrderService sales;@Autowired OrderStatusService statuses;@Autowired DeliveryService delivery;@Autowired OrderStatusHistoryRepository history;
 @Autowired ProvinceRepository provinces;@Autowired ProductVariantRepository variants;@Autowired CustomerRepository customers;@Autowired OrderRepository orders;@Autowired JdbcTemplate jdbc;@Autowired WebApplicationContext context;
 MockMvc mvc;
 @BeforeEach void setup(){mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();com.example.demodatn.TestDatabaseGuard.check(jdbc);}
 Order order(String payment){var v=variants.findByStatusTrueAndProduct_StatusTrue().stream().filter(x->x.getQuantity()>10).findFirst().orElseThrow();var c=customers.findAll().stream().filter(x->x.getUser()!=null && !Boolean.TRUE.equals(x.getUser().getMustChangePassword())).findFirst().orElseThrow();var r=new CheckoutRequest();r.setProvinceId(provinces.findAllByOrderByNameAsc().stream().filter(p->p.getName().equals("Hà Nội")).findFirst().orElseThrow().getId());r.setWardId(jdbc.queryForObject("SELECT TOP 1 id FROM phuong_xa WHERE tinh_thanh_id=? ORDER BY id",Integer.class,r.getProvinceId()));r.setStreetAddress("Số 12, đường Nguyễn Huệ");r.setReceiverName("Nguyễn Văn Giao");r.setPhone("0901234567");r.setEmail("giao@example.test");r.setShippingAddress("Số 12, đường Nguyễn Huệ");r.setStreetAddress("Số 12, đường Nguyễn Huệ");r.setPaymentMethod(payment);return sales.placeOrder(r,List.of(new CartItemDto(v.getProduct().getId(),v.getId(),v.getProduct().getName(),v.getSize().getSizeValue(),v.getPrice(),1)),c,BigDecimal.ZERO);}
 int quantity(Order o){return jdbc.queryForObject("SELECT quantity FROM product_variants WHERE id=?",Integer.class,o.getOrderDetails().get(0).getProductVariant().getId());}
 @Test void fullOnlineFlowRecordsActorsAndCarrierWithoutEstimatedDelivery(){
  var o=order("COD");int before=quantity(o);assertEquals(1,history.findByOrder_IdOrderByChangedAtAscIdAsc(o.getId()).size());
  statuses.transition(o.getId(),"CONFIRMED",null,null,null);
  assertEquals(before-1,quantity(o));
  assertNull(o.getEstimatedFrom());assertNull(o.getEstimatedTo());
  statuses.transition(o.getId(),"CONFIRMED",null,null,null);
  statuses.transition(o.getId(),"PREPARING",null,null,null);
  statuses.transition(o.getId(),"SHIPPING",null,"Giao hàng Việt","VN123");
  statuses.transition(o.getId(),"DELIVERED",null,null,null);
  orders.flush();
  assertEquals(before-1,quantity(o));assertEquals("PAID",o.getPaymentStatus());assertNotNull(o.getDeliveredOn());
  var rows=history.findByOrder_IdOrderByChangedAtAscIdAsc(o.getId());
  assertEquals(List.of("PENDING","CONFIRMED","PREPARING","SHIPPING","DELIVERED"),rows.stream().map(OrderStatusHistory::getStatus).toList());
  assertEquals("Giao hàng Việt",rows.get(3).getCarrier());assertEquals("VN123",rows.get(3).getTrackingCode());
  assertTrue(rows.stream().allMatch(h->h.getActor().equals("admin")));
 }
 @Test void cancellationAndDeliveryFailureReturnStockExactlyOnce(){for(boolean failed:new boolean[]{false,true}){var o=order("COD");int before=quantity(o);statuses.transition(o.getId(),"CONFIRMED",null,null,null);if(failed){statuses.transition(o.getId(),"PREPARING",null,null,null);statuses.transition(o.getId(),"SHIPPING",null,null,null);}String next=failed?"DELIVERY_FAILED":"CANCELLED";assertThrows(IllegalArgumentException.class,()->statuses.transition(o.getId(),next," ",null,null));statuses.transition(o.getId(),next,"Khách không thể nhận hàng",null,null);statuses.transition(o.getId(),next,"Khách không thể nhận hàng",null,null);assertEquals(before,quantity(o));}}
 @Test void provinceChangesAreAuditedWithoutEstimatedDelivery(){var o=order("COD");o.setCreatedAt(LocalDateTime.now().minusDays(20));orders.flush();statuses.transition(o.getId(),"CONFIRMED",null,null,null);assertNull(o.getEstimatedFrom());var central=provinces.findAllByOrderByNameAsc().stream().filter(p->p.getName().equals("Đà Nẵng")).findFirst().orElseThrow();statuses.updateProvince(o.getId(),central.getId());assertEquals("Đà Nẵng",o.getProvinceName());assertNull(o.getEstimatedFrom());statuses.transition(o.getId(),"PREPARING",null,null,null);statuses.transition(o.getId(),"SHIPPING",null,null,null);assertThrows(IllegalArgumentException.class,()->statuses.updateProvince(o.getId(),central.getId()));var legacy=order("COD");legacy.setProvince(null);legacy.setProvinceName(null);orders.flush();statuses.transition(legacy.getId(),"CONFIRMED",null,null,null);assertNull(legacy.getEstimatedFrom());}
 @Test void provinceValidationInService(){assertEquals(34,provinces.count());assertThrows(IllegalArgumentException.class,()->delivery.province(-1));assertThrows(IllegalArgumentException.class,()->delivery.province(null));var o=order("COD");statuses.transition(o.getId(),"CONFIRMED",null,null,null);assertNull(o.getEstimatedFrom());assertNull(o.getEstimatedTo());}
 @Test void malformedProvinceValuesHaveVietnameseMessages()throws Exception{
  var o=order("COD");String owner=o.getCustomer().getUser().getUsername();
  mvc.perform(post("/profile/addresses/save").with(user(owner).roles("USER")).with(csrf()).param("provinceId","abc").param("receiverName","Nguyễn Văn Mẫu").param("phone","0968123456").param("address","Số 12, đường Nguyễn Huệ")).andExpect(status().isOk()).andExpect(content().string(containsString("Vui lòng chọn Tỉnh/Thành phố hợp lệ trong danh sách."))).andExpect(content().string(not(containsString("Failed to convert"))));
  mvc.perform(post("/admin/customers/save").with(csrf()).param("provinceId","abc")).andExpect(status().isOk()).andExpect(content().string(containsString("Vui lòng chọn Tỉnh/Thành phố hợp lệ trong danh sách.")));
  var session=new org.springframework.mock.web.MockHttpSession();var detail=o.getOrderDetails().get(0);session.setAttribute("cart",new ArrayList<>(List.of(new CartItemDto(detail.getProductVariant().getProduct().getId(),detail.getProductVariant().getId(),detail.getProductName(),detail.getSizeName(),detail.getPrice(),1))));
  var invalid=mvc.perform(post("/checkout/place").with(user(owner).roles("USER")).with(csrf()).session(session).param("provinceId","abc").param("receiverName","Nguyễn Văn Mẫu").param("phone","0968123456").param("email","giao@example.test").param("streetAddress","Số 12").param("paymentMethod","COD")).andExpect(redirectedUrl("/checkout")).andReturn();
  mvc.perform(get("/checkout").with(user(owner).roles("USER")).session(session).flashAttrs(invalid.getFlashMap())).andExpect(status().isOk()).andExpect(content().string(containsString("Vui lòng chọn Tỉnh/Thành phố hợp lệ trong danh sách.")));
  mvc.perform(get("/admin/shipping-config/settings")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/dashboard"));
  mvc.perform(post("/admin/invoices/"+jdbc.queryForObject("SELECT id FROM invoices WHERE order_id=?",Integer.class,o.getId())+"/province").with(csrf()).param("provinceId","abc")).andExpect(status().is3xxRedirection()).andExpect(flash().attribute("error","Vui lòng chọn Tỉnh/Thành phố hợp lệ trong danh sách."));
 }
 @Test void customerInvoiceOwnershipAndEightSecurityScenarios()throws Exception{
  var o=order("COD");String owner=o.getCustomer().getUser().getUsername();
  mvc.perform(get("/orders/"+o.getId()+"/hoa-don").with(user(owner).roles("USER"))).andExpect(status().isOk()).andExpect(content().string(containsString("Tỉnh/Thành phố"))).andExpect(content().string(containsString("Nguyễn Văn Giao")));
  mvc.perform(get("/orders/"+o.getId()+"/hoa-don").with(user("another").roles("USER"))).andExpect(status().isForbidden());
  mvc.perform(get("/orders/"+o.getId()+"/hoa-don").with(anonymous())).andExpect(status().is3xxRedirection());
  mvc.perform(get("/admin/shipping-config").with(user("another").roles("USER"))).andExpect(status().isForbidden());
  mvc.perform(get("/admin/shipping-config")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/dashboard"));
  mvc.perform(get("/admin/shipping-times")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/dashboard"));
  for(String path:new String[]{"/admin/invoices/"+jdbc.queryForObject("SELECT id FROM invoices WHERE order_id=?",Integer.class,o.getId())+"/status","/admin/invoices/"+jdbc.queryForObject("SELECT id FROM invoices WHERE order_id=?",Integer.class,o.getId())+"/province"}){
   mvc.perform(post(path)).andExpect(status().isForbidden());
   mvc.perform(post(path).with(csrf().useInvalidToken())).andExpect(status().isForbidden());
   mvc.perform(post(path).with(user("another").roles("USER")).with(csrf())).andExpect(status().isForbidden());
  }
  mvc.perform(post("/admin/invoices/"+jdbc.queryForObject("SELECT id FROM invoices WHERE order_id=?",Integer.class,o.getId())+"/status").with(csrf()).param("status","SHIPPING")).andExpect(flash().attribute("error",containsString("Không thể chuyển")));
  assertEquals("PENDING",o.getStatus());
 }
}
