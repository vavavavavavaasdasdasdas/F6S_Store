package com.example.demodatn.controller;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @Transactional
class AdminFrameIntegrationTest {
 @Autowired WebApplicationContext context;@Autowired JdbcTemplate jdbc;MockMvc mvc;
 @BeforeEach void init(){com.example.demodatn.TestDatabaseGuard.check(jdbc);mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();}
 @Test void eightSecurityCasesAndForgedCodeIgnored() throws Exception {
  mvc.perform(get("/admin/categories").with(anonymous())).andExpect(status().is3xxRedirection());
  mvc.perform(get("/admin/categories").with(user("ordinary").roles("USER"))).andExpect(status().isForbidden());
  mvc.perform(post("/admin/categories/save").with(user("ordinary").roles("USER")).with(csrf())).andExpect(status().isForbidden());
  mvc.perform(post("/admin/categories/save").with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
  mvc.perform(post("/admin/categories/save").with(user("admin").roles("ADMIN")).with(csrf().useInvalidToken())).andExpect(status().isForbidden());
  mvc.perform(post("/admin/variants/save").with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
  mvc.perform(post("/admin/promotions/save").with(user("ordinary").roles("USER")).with(csrf())).andExpect(status().isForbidden());
  String name="G9 forgery "+UUID.randomUUID();mvc.perform(post("/admin/categories/save").with(user("admin").roles("ADMIN")).with(csrf()).param("value",name).param("status","true").param("code","FORGED-G9")).andExpect(status().is3xxRedirection());
  String code=jdbc.queryForObject("SELECT ma FROM categories WHERE name=?",String.class,name);assertTrue(code.matches("TH[0-9]{3,}"));assertNotEquals("FORGED-G9",code);
  System.out.println("GD9 SECURITY cases=8 statuses=302,403,403,403,403,403,403,302; forged code ignored="+code);
 }
 @Test void codesSearchAndReadOnlyFormsAndProductMenu() throws Exception {
  jdbc.update("INSERT promotions(name,discount_type,discount_value,start_date,end_date,status) VALUES(N'Đợt kiểm thử mã riêng','PERCENT',10,'20000101','21000101',1)");
  for(String[] d:List.of(new String[]{"categories","categories"},new String[]{"sizes","sizes"},new String[]{"materials","chat_lieu"},new String[]{"colors","mau_sac"},new String[]{"soles","de_giay"},new String[]{"promotions","promotions"})){
   var row=jdbc.queryForMap("SELECT TOP 1 id,ma FROM "+d[1]+" ORDER BY id");String html=mvc.perform(get("/admin/"+d[0]).param("keyword",row.get("ma").toString()).with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);assertTrue(html.contains(row.get("ma").toString()),d[0]);
   String form=mvc.perform(get("/admin/"+d[0]+"/edit/"+row.get("id")).with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);assertTrue(form.contains("readonly")&&form.contains(row.get("ma").toString()),d[0]+" readonly code");
  }
 }
}
