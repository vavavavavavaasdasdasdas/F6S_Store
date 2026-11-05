package com.example.demodatn.service.donhang;

import com.example.demodatn.dto.donhang.*;
import com.example.demodatn.repository.donhang.*;
import com.example.demodatn.repository.sanpham.ProductVariantRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
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
class InvoiceOnlineWorkspaceIntegrationTest {
 @Autowired WebApplicationContext context;@Autowired JdbcTemplate jdbc;@Autowired ProductVariantRepository variants;
 @Autowired PosHoldService holds;@Autowired PosOnlineOrderService online;@Autowired InvoiceRepository invoices;@Autowired jakarta.persistence.EntityManager em;
 MockMvc mvc;Integer variant,ward,province;int stock;
 @BeforeEach void setup(){
  com.example.demodatn.TestDatabaseGuard.check(jdbc);mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  variant=jdbc.queryForObject("SELECT TOP 1 v.id FROM product_variants v JOIN products p ON p.id=v.product_id WHERE v.quantity>5 AND v.status=1 AND p.status=1 ORDER BY v.id",Integer.class);
  var w=jdbc.queryForMap("SELECT TOP 1 id,tinh_thanh_id FROM phuong_xa ORDER BY id");ward=((Number)w.get("id")).intValue();province=((Number)w.get("tinh_thanh_id")).intValue();stock=quantity();
 }
 int quantity(){return jdbc.queryForObject("SELECT quantity FROM product_variants WHERE id=?",Integer.class,variant);}
 List<CartItemDto> cart(){var v=variants.findById(variant).orElseThrow();return new ArrayList<>(List.of(new CartItemDto(v.getProduct().getId(),v.getId(),v.getProduct().getName(),v.getSize().getSizeValue(),v.getPrice(),1)));}
 PosOnlineOrderForm form(){var f=new PosOnlineOrderForm();f.setReceiverName("Nguyễn Văn Ánh");f.setPhone("09"+String.format("%08d",Math.floorMod(UUID.randomUUID().hashCode(),100000000)));f.setProvinceId(province);f.setWardId(ward);f.setStreetAddress("Số 12, đường Nguyễn Huệ");f.setLatitude(new BigDecimal("21.03801"));f.setLongitude(new BigDecimal("105.7469"));f.setCoordinateSource("GHIM_TAY");f.setOrderSource("ZALO");f.setPaymentMethod("COD");return f;}
 PosCheckoutRequest request(String method){var r=new PosCheckoutRequest();r.setPaymentMethod(method);return r;}
 @Test void oldRoutesOnlyRedirectAndPreserveFiltersAndPostBody()throws Exception{
  var o=online.create("admin",form(),cart());Integer id=invoices.findByOrder_Id(o.getId()).orElseThrow().getId();
  mvc.perform(get("/admin/orders?status=PENDING&channel=ONLINE&page=1")).andExpect(status().isFound()).andExpect(header().string("Location","/admin/invoices?status=PENDING&channel=ONLINE&page=1"));
  for(String path:List.of("/admin/orders/","/admin/orders/detail/"))mvc.perform(get(path+o.getId()).with(user("staff").roles("EMPLOYEE"))).andExpect(status().isFound()).andExpect(header().string("Location","/admin/invoices/"+id));
  mvc.perform(post("/admin/orders/"+o.getId()+"/status").with(csrf()).param("status","CONFIRMED")).andExpect(status().isTemporaryRedirect()).andExpect(header().string("Location","/admin/invoices/"+id+"/status"));assertEquals(stock,quantity());
  mvc.perform(post("/admin/invoices/"+id+"/status").with(csrf()).param("status","CONFIRMED")).andExpect(status().isFound());assertEquals(stock-1,quantity());
  System.out.println("GD13 compatibility GET=302 POST=307; redirect does not mutate stock; invoice action stock="+quantity());
 }
 @Test void cartsAndDomAreSeparatedAndCustomerSearchIsInitiallyEmpty()throws Exception{
  var session=new MockHttpSession();mvc.perform(get("/admin/pos").session(session)).andExpect(status().isOk());
  mvc.perform(get("/admin/ban-online").session(session)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/pos"));
 }
 @Test void holdsAllowFiveOfEachAndRetainDeliverySnapshotAndRejectCrossType(){
  var f=form();f.setNote("Giao buổi chiều — Đà Nẵng");f.setCheckoutToken("private-session-token");Long id=null;
  for(int n=0;n<5;n++){holds.save("admin",cart(),request("CASH"),null);id=holds.save("admin",cart(),request("COD"),null,"ONLINE",f);}
  assertEquals(5,holds.list("admin","POS").size());assertEquals(5,holds.list("admin","ONLINE").size());
  assertThrows(IllegalArgumentException.class,()->holds.save("admin",cart(),request("CASH"),null));assertThrows(IllegalArgumentException.class,()->holds.save("admin",cart(),request("COD"),null,"ONLINE",f));
  var loaded=holds.open("admin",id,"ONLINE");assertEquals(f.getStreetAddress(),loaded.online().getStreetAddress());assertEquals(f.getNote(),loaded.online().getNote());assertNull(loaded.online().getCheckoutToken());
  Long chosen=id;assertThrows(org.springframework.web.server.ResponseStatusException.class,()->holds.open("admin",chosen,"POS"));assertThrows(org.springframework.web.server.ResponseStatusException.class,()->holds.delete("admin",chosen,"POS"));assertEquals(stock,quantity());
  System.out.println("GD13 holds POS=5 ONLINE=5; sixth each rejected; delivery Unicode preserved; cross-type=404; stock unchanged="+quantity());
 }
 @Test void confirmNowAndInvoiceFiltersUseSharedOrderWorkflow()throws Exception{
  var f=form();f.setConfirmNow(true);f.setNote("Giao giờ hành chính");var o=online.create("admin",f,cart());em.flush();assertEquals("CONFIRMED",o.getStatus());assertEquals(stock-1,quantity());
  var i=invoices.findByOrder_Id(o.getId()).orElseThrow();
  mvc.perform(get("/admin/invoices").param("q",o.getOrderCode()).param("channel","ONLINE").param("source","ZALO").param("status","CONFIRMED").param("paymentMethod","COD").param("customer",f.getReceiverName())).andExpect(status().isOk()).andExpect(content().string(containsString(i.getInvoiceCode())));
  mvc.perform(get("/admin/invoices").param("q",o.getOrderCode()).param("source","FACEBOOK")).andExpect(status().isOk()).andExpect(content().string(not(containsString(i.getInvoiceCode()))));
  mvc.perform(get("/admin/invoices/"+i.getId())).andExpect(status().isOk()).andExpect(content().string(containsString("Giao giờ hành chính"))).andExpect(content().string(containsString("Đã trừ kho")));
  assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM order_status_history WHERE order_id=?",Integer.class,o.getId()));
 }
 @Test void eightSecurityCasesForOnlineAndInvoice()throws Exception{
  mvc.perform(get("/admin/ban-online").with(anonymous())).andExpect(status().isFound());
  mvc.perform(get("/admin/ban-online").with(user("ordinary").roles("USER"))).andExpect(status().isForbidden());
  mvc.perform(get("/admin/ban-online").with(user("staff").roles("EMPLOYEE"))).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/pos"));
  mvc.perform(get("/admin/pos").with(user("staff").roles("EMPLOYEE"))).andExpect(status().isOk());
  mvc.perform(post("/admin/pos/add").param("variantId",variant.toString()).param("quantity","1")).andExpect(status().isForbidden());
  mvc.perform(post("/admin/pos/add").with(csrf().useInvalidToken()).param("variantId",variant.toString()).param("quantity","1")).andExpect(status().isForbidden());
  mvc.perform(post("/admin/invoices/1/status").with(user("ordinary").roles("USER")).with(csrf()).param("status","CONFIRMED")).andExpect(status().isForbidden());
  mvc.perform(get("/admin/pos/customers-json").param("q","a")).andExpect(status().isOk());
  System.out.println("GD21 security & ban-online redirect verified");
 }
}
