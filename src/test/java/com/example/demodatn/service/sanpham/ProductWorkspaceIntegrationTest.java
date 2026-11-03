package com.example.demodatn.service.sanpham;

import com.example.demodatn.dto.sanpham.*;
import com.example.demodatn.entity.sanpham.Product;
import com.example.demodatn.repository.sanpham.ProductRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class ProductWorkspaceIntegrationTest {
 @Autowired WebApplicationContext context;
 @Autowired JdbcTemplate jdbc;
 @Autowired ProductRepository products;
 MockMvc mvc;Integer productId;String createdColorName;
 @BeforeEach void setup(){
  assertTrue(jdbc.queryForObject("SELECT DB_NAME()",String.class).startsWith("shopbangiay_kt_"));
  mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  var p=new Product();p.setCode("G10-"+UUID.randomUUID());p.setName("Giày kiểm tra không trùng");p.setStatus(false);productId=products.saveAndFlush(p).getId();
 }
 @AfterEach void cleanup(){
  jdbc.update("DELETE FROM inventory_logs WHERE variant_id IN (SELECT id FROM product_variants WHERE product_id=?)",productId);
  jdbc.update("DELETE FROM product_variants WHERE product_id=?",productId);products.deleteById(productId);
  if(createdColorName!=null)jdbc.update("DELETE FROM mau_sac WHERE ten=?",createdColorName);
 }
 int first(String table){return jdbc.queryForObject("SELECT TOP 1 id FROM "+table+" ORDER BY id",Integer.class);}
 org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder batch(String... ids){
  return post("/admin/products-crud/"+productId+"/variants/batch").with(user("admin").roles("ADMIN")).with(csrf())
   .param("sizeIds",ids).param("materialId",""+first("chat_lieu")).param("colorId",""+first("mau_sac")).param("soleId",""+first("de_giay")).param("price","200000").param("quantity","3");
 }
 @Test void dedicatedWorkspaceHasTwelveSeparateColumnsAndViewHasNoTable()throws Exception{
  var view=mvc.perform(get("/admin/products-crud/view/"+productId).with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
  assertTrue(view.contains("data-product-variants"));assertTrue(view.contains("<th>Mã SP</th><th>Tên SP</th><th>Mã biến thể</th>"));
  mvc.perform(get("/admin/products-crud/"+productId+"/variants").with(user("admin").roles("ADMIN"))).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/products-crud/view/"+productId));
  var list=mvc.perform(get("/admin/products-crud").with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
  assertTrue(list.contains("<th>Tồn kho</th>"));assertFalse(list.contains("name=\"materialId\""));
  assertTrue(list.contains("Xem chi tiết"));assertFalse(list.contains("Chi tiết biến thể"));
  mvc.perform(post("/admin/products-crud/delete/"+productId).with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isMethodNotAllowed());
  mvc.perform(delete("/admin/products-crud/delete/"+productId).with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isMethodNotAllowed());
  mvc.perform(get("/admin/products-crud/delete/"+productId).with(user("admin").roles("ADMIN"))).andExpect(status().isMethodNotAllowed());
  int catId=first("categories");
  mvc.perform(post("/admin/categories/delete/"+catId).with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isMethodNotAllowed());
  mvc.perform(delete("/admin/categories/delete/"+catId).with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isMethodNotAllowed());
  mvc.perform(get("/admin/categories/delete/"+catId).with(user("admin").roles("ADMIN"))).andExpect(status().isMethodNotAllowed());
  // Forged product create POSTs: array of multiple values, unknown ID, custom string
  String mat1=""+first("chat_lieu"), sole1=""+first("de_giay");
  mvc.perform(multipart("/admin/products-crud/save").with(user("admin").roles("ADMIN")).with(csrf())
   .param("name","SP Forge Multi").param("categoryId",""+catId).param("status","false")
   .param("initialMaterialId",mat1,"999999").param("initialSoleId",sole1,"999999"))
   .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("product","initialMaterialId","initialSoleId"));
  mvc.perform(multipart("/admin/products-crud/save").with(user("admin").roles("ADMIN")).with(csrf())
   .param("name","SP Forge Unknown").param("categoryId",""+catId).param("status","false")
   .param("initialMaterialId","999999").param("initialSoleId","999999"))
   .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("product","initialMaterialId","initialSoleId"));
  mvc.perform(multipart("/admin/products-crud/save").with(user("admin").roles("ADMIN")).with(csrf())
   .param("name","SP Forge Custom").param("categoryId",""+catId).param("status","false")
   .param("newInitialMaterialName","Chat lieu tu go").param("newInitialSoleName","De giay tu go"))
   .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("product","initialMaterialId","initialSoleId"));
 }
 @Test void repeatedSizeInBatchRejectedWithoutPartialRows()throws Exception{
  String id=""+first("sizes");mvc.perform(batch(id,id)).andExpect(status().isBadRequest());
  assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM product_variants WHERE product_id=?",Integer.class,productId));
 }
 @Test void simultaneousDuplicateBatchHasOneWinnerAndFriendlyRejection()throws Exception{
  var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
  try{
   Callable<Integer> create=()->{start.await();return mvc.perform(batch(""+first("sizes"))).andReturn().getResponse().getStatus();};
   var a=pool.submit(create);var b=pool.submit(create);start.countDown();var statuses=new ArrayList<>(List.of(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS)));Collections.sort(statuses);
   assertEquals(List.of(200,400),statuses);assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM product_variants WHERE product_id=?",Integer.class,productId));
   assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM inventory_logs l JOIN product_variants v ON v.id=l.variant_id WHERE v.product_id=?",Integer.class,productId));
   System.out.println("GD10 concurrent duplicate statuses="+statuses+" variants=1 logs=1");
  }finally{pool.shutdownNow();}
 }
 @Test void simultaneousCreatableColorThroughVariantFormDoesNotReturnFiveHundred()throws Exception{
  createdColorName="G10 màu mới "+UUID.randomUUID();var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
  try {
   Callable<Integer> create=()->{start.await();return mvc.perform(post("/admin/variants/save").with(user("admin").roles("ADMIN")).with(csrf())
    .param("productId",""+productId).param("sizeId",""+first("sizes")).param("newColorName",createdColorName)
    .param("materialId",""+first("chat_lieu")).param("soleId",""+first("de_giay")).param("price","200000").param("quantity","3").param("status","true"))
    .andReturn().getResponse().getStatus();};
   var a=pool.submit(create);var b=pool.submit(create);start.countDown();var statuses=new ArrayList<>(List.of(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS)));Collections.sort(statuses);
   assertEquals(List.of(200,302),statuses);assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM product_variants WHERE product_id=?",Integer.class,productId));
   assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM mau_sac WHERE ten=?",Integer.class,createdColorName));
   System.out.println("GD10 concurrent creatable statuses="+statuses+" variants=1 colors=1");
  }finally{pool.shutdownNow();}
 }
}
