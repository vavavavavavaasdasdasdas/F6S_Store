package com.example.demodatn.service.khuyenmai;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @Transactional
class DiscountWorkflowIntegrationTest {
 @Autowired WebApplicationContext context;@Autowired JdbcTemplate jdbc;MockMvc mvc;
 @BeforeEach void setup(){assertTrue(jdbc.queryForObject("SELECT DB_NAME()",String.class).startsWith("shopbangiay_kt_"));mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();}
 @Test void voucherIsAlwaysPublicRetainsCounterCannotDeleteAndSwitchHasCsrf()throws Exception{
  String code="G11-"+UUID.randomUUID().toString().substring(0,8);
  mvc.perform(post("/admin/vouchers/save").with(user("admin").roles("ADMIN")).with(csrf()).param("code",code).param("name","Phiếu công khai Nguyễn Huệ").param("discountType","FIXED").param("discountValue","50000").param("startDate",LocalDateTime.now().minusDays(1).toString()).param("endDate",LocalDateTime.now().plusDays(1).toString()).param("isPublic","false").param("usageLimit","-9").param("customerId","-1").param("status","true")).andExpect(status().isFound());
  var row=jdbc.queryForMap("SELECT id,is_public,customer_id,used_count FROM vouchers WHERE code=?",code);Integer id=(Integer)row.get("id");assertEquals(true,row.get("is_public"));assertNull(row.get("customer_id"));assertEquals(0,row.get("used_count"));
  String path="/admin/vouchers/status/"+id;
  mvc.perform(get("/admin/vouchers").with(anonymous())).andExpect(status().isFound());
  mvc.perform(get("/admin/vouchers").with(user("ordinary").roles("USER"))).andExpect(status().isForbidden());
  mvc.perform(get("/admin/vouchers").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
  mvc.perform(post(path).with(anonymous()).with(csrf()).param("active","false")).andExpect(status().isFound());
  mvc.perform(post(path).with(user("ordinary").roles("USER")).with(csrf()).param("active","false")).andExpect(status().isForbidden());
  mvc.perform(post(path).with(user("admin").roles("ADMIN")).param("active","false")).andExpect(status().isForbidden());
  mvc.perform(post(path).with(user("admin").roles("ADMIN")).with(csrf().useInvalidToken()).param("active","false")).andExpect(status().isForbidden());
  mvc.perform(post(path).with(user("admin").roles("ADMIN")).with(csrf()).param("active","false")).andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
  mvc.perform(post("/admin/vouchers/delete/"+id).with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isMethodNotAllowed());
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM vouchers WHERE id=?",Integer.class,id));
  System.out.println("GD11 SECURITY 8 statuses=302,403,200,302,403,403,403,200; voucher DELETE POST=405; public=true; counter=0");
 }
 @Test void selectionPageExcludesStoppedProductsAndRejectsForgedSelection()throws Exception{
  int promotion=jdbc.queryForObject("SELECT TOP 1 id FROM promotions ORDER BY id",Integer.class);
  int product=jdbc.queryForObject("SELECT TOP 1 id FROM products WHERE status=1 ORDER BY id",Integer.class);
  jdbc.update("UPDATE products SET status=0 WHERE id=?",product);
  String html=mvc.perform(get("/admin/promotions/"+promotion+"/products").with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
  assertFalse(html.contains("id=\"product-"+product+"\""));assertFalse(html.contains("<th>Trạng thái</th>"));assertTrue(html.contains("role=\"switch\""));
  var original=jdbc.queryForList("SELECT id,product_id FROM promotion_details WHERE promotion_id=? ORDER BY id",promotion);
  mvc.perform(post("/admin/promotions/"+promotion+"/products").with(user("admin").roles("ADMIN")).with(csrf()).param("visibleIds",""+product).param("productIds",""+product)).andExpect(status().isFound()).andExpect(flash().attributeExists("error"));
  assertEquals(original,jdbc.queryForList("SELECT id,product_id FROM promotion_details WHERE promotion_id=? ORDER BY id",promotion));
 }
}
