package com.example.demodatn.service.khachhang;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @Transactional
class AdminAddressBookIntegrationTest {
 @Autowired WebApplicationContext context;@Autowired JdbcTemplate jdbc;MockMvc mvc;int customer,province,ward;
 @BeforeEach void setup(){
  assertTrue(jdbc.queryForObject("SELECT DB_NAME()",String.class).startsWith("shopbangiay_kt_"));mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  String key=UUID.randomUUID().toString().substring(0,8);customer=jdbc.queryForObject("INSERT customers(customer_code,full_name,phone,email,status) OUTPUT INSERTED.id VALUES(?,N'Khách kiểm thử địa chỉ','0968123400',?,1)",Integer.class,"G12-"+key,key+"@example.test");
  var row=jdbc.queryForMap("SELECT TOP 1 w.id,w.tinh_thanh_id FROM phuong_xa w ORDER BY w.id");ward=(Integer)row.get("id");province=(Integer)row.get("tinh_thanh_id");
 }
 org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder save(String street){return post("/admin/customers/"+customer+"/addresses/save").with(user("staff").roles("EMPLOYEE")).with(csrf()).param("receiverName","Nguyễn Văn Ánh").param("phone","0901234567").param("provinceId",""+province).param("wardId",""+ward).param("streetAddress",street);}
 @Test void administratorBookUsesSharedLimitsDefaultMirrorAndOwnership()throws Exception{
  String unicode="Số 12, đường Nguyễn Huệ, Đà Nẵng";
  for(String street:List.of(unicode,"Địa chỉ thứ hai","Địa chỉ thứ ba"))mvc.perform(save(street)).andExpect(status().isFound());
  assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM dia_chi_nhan_hang WHERE customer_id=?",Integer.class,customer));
  assertEquals(unicode,jdbc.queryForObject("SELECT address FROM customers WHERE id=?",String.class,customer));
  mvc.perform(save("Thứ tư")).andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("addressForm","address"));
  var ids=jdbc.queryForList("SELECT id FROM dia_chi_nhan_hang WHERE customer_id=? ORDER BY thu_tu",Integer.class,customer);
  mvc.perform(post("/admin/customers/"+customer+"/addresses/"+ids.get(1)+"/default").with(user("staff").roles("EMPLOYEE")).with(csrf())).andExpect(status().isFound());
  assertEquals("Địa chỉ thứ hai",jdbc.queryForObject("SELECT address FROM customers WHERE id=?",String.class,customer));
  mvc.perform(post("/admin/customers/"+customer+"/addresses/"+ids.get(1)+"/delete").with(user("staff").roles("EMPLOYEE")).with(csrf())).andExpect(status().isFound());
  assertEquals(unicode,jdbc.queryForObject("SELECT address FROM customers WHERE id=?",String.class,customer));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dia_chi_nhan_hang WHERE customer_id=? AND la_mac_dinh=1",Integer.class,customer));
  mvc.perform(get("/admin/customers/"+(customer+999999)+"/addresses/"+ids.get(0)+"/edit").with(user("staff").roles("EMPLOYEE"))).andExpect(status().isNotFound());
  mvc.perform(post("/admin/customers/"+customer+"/addresses/"+ids.get(0)+"/delete").with(user("ordinary").roles("USER")).with(csrf())).andExpect(status().isForbidden());
  System.out.println("GD12 Admin address SQL: before=0 added=3 fourth=200 field error; default switch mirrors customer; deleting default selects first; Unicode exact=true; wrong owner=404,user=403");
 }
 @Test void fourthAddressIsAlsoRejectedByDatabase()throws Exception{
  for(int i=0;i<3;i++)mvc.perform(save("Địa chỉ "+i)).andExpect(status().isFound());
  assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("INSERT dia_chi_nhan_hang(customer_id,ten_nguoi_nhan,so_dien_thoai,dia_chi,la_mac_dinh,thu_tu) VALUES(?,N'Thử SQL','0901234567',N'Địa chỉ SQL',0,4)",customer));
  assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM dia_chi_nhan_hang WHERE customer_id=?",Integer.class,customer));
  System.out.println("GD12 direct SQL slot4 rejected by DB CHECK; before=3 after=3");
 }
}
