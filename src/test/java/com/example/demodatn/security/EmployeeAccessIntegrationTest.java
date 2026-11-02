package com.example.demodatn.security;

import com.example.demodatn.dto.nhanvien.EmployeeDto;
import com.example.demodatn.service.PeopleService;
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

@SpringBootTest @Transactional @org.springframework.security.test.context.support.WithMockUser(username="admin",roles="ADMIN")
class EmployeeAccessIntegrationTest {
 @Autowired WebApplicationContext context;@Autowired JdbcTemplate jdbc;@Autowired PeopleService people;
 @Autowired UserDetailsServiceImpl details;@Autowired jakarta.persistence.EntityManager entityManager;
 @Autowired com.example.demodatn.service.donhang.OrderService orderService;
 @Autowired com.example.demodatn.repository.donhang.InvoiceRepository invoices;
 @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
 MockMvc mvc;String username;Integer employeeUserId,protectedCustomerId;
 @BeforeEach void setup(){
  assertTrue(jdbc.queryForObject("SELECT DB_NAME()",String.class).startsWith("shopbangiay_kt_"));mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  String token=UUID.randomUUID().toString().substring(0,8);var f=new EmployeeDto();f.setEmployeeCode("G12-"+token);f.setFullName("Nhân viên kiểm thử phân quyền");f.setEmail(token+"@example.test");f.setPhone("09"+String.format("%08d",Math.floorMod(token.hashCode(),100000000)));f.setPosition("Nhân viên");f.setAddress("Một địa chỉ nhân viên");username="g12_"+token;f.setUsername(username);f.setPassword("NhanVien123");employeeUserId=people.saveEmployee(f).getUser().getId();
 }
 @AfterEach void cleanupCommittedFixture(){
  if(!org.springframework.test.context.transaction.TestTransaction.isActive()){
   if(protectedCustomerId!=null)jdbc.update("DELETE FROM customers WHERE id=?",protectedCustomerId);
   jdbc.update("DELETE FROM employees WHERE user_id=?",employeeUserId);jdbc.update("DELETE FROM users WHERE id=?",employeeUserId);
  }
 }
 @Test void matrixAndMenuUseSamePolicyAndInventoryIsReadOnly()throws Exception{
  for(String path:List.of("/admin/pos","/admin/invoices","/admin/customers","/admin/customers/create","/admin/inventory","/admin/account"))mvc.perform(get(path).with(user(username).roles("EMPLOYEE"))).andExpect(status().isOk());
  for(String path:List.of("/admin","/admin/dashboard","/admin/products-crud","/admin/variants","/admin/categories","/admin/sizes","/admin/soles","/admin/materials","/admin/colors","/admin/employees","/admin/vouchers","/admin/promotions","/admin/shipping-config")){
   mvc.perform(get(path).with(user(username).roles("EMPLOYEE"))).andExpect(status().isForbidden());mvc.perform(get(path).with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
  }
  mvc.perform(get("/admin/statistics").with(user(username).roles("EMPLOYEE"))).andExpect(status().isForbidden());mvc.perform(get("/admin/statistics").with(user("admin").roles("ADMIN"))).andExpect(status().isFound()).andExpect(redirectedUrl("/admin/dashboard"));
  var html=mvc.perform(get("/admin/inventory").with(user(username).roles("EMPLOYEE"))).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
  assertFalse(html.contains("/admin/inventory/adjust/"));String nav=html.substring(html.indexOf("<nav class=\"admin-nav\""));nav=nav.substring(0,nav.indexOf("</nav>"));
  for(String denied:List.of("/admin/products-crud","/admin/employees","/admin/statistics","/admin/dashboard","HỆ THỐNG"))assertFalse(nav.contains(denied),denied);
  assertTrue(nav.contains("/admin/inventory")&&nav.contains("/admin/account")&&nav.contains("/admin/pos"));
  mvc.perform(post("/admin/accounts/username-suggestion").with(user(username).roles("EMPLOYEE")).with(csrf()).param("email","g12-suggestion@example.test")).andExpect(status().isOk()).andExpect(jsonPath("$.username").value("g12-suggestion"));
  System.out.println("GD12 MATRIX employee allowed=6 routes HTTP200; denied=14 HTTP403; Admin same routes HTTP200; inventory adjustment hidden");
 }
 @Test void eightSecurityCasesProtectNewCommands()throws Exception{
  Integer customer=jdbc.queryForObject("SELECT TOP 1 id FROM customers ORDER BY id",Integer.class);
  mvc.perform(get("/admin/invoices").with(anonymous())).andExpect(status().isFound());
  mvc.perform(get("/admin/invoices").with(user("ordinary").roles("USER"))).andExpect(status().isForbidden());
  mvc.perform(get("/admin/invoices").with(user(username).roles("EMPLOYEE"))).andExpect(status().isOk());
  mvc.perform(post("/admin/customers/"+customer+"/addresses/save").with(user(username).roles("EMPLOYEE"))).andExpect(status().isForbidden());
  mvc.perform(post("/admin/customers/"+customer+"/addresses/save").with(user(username).roles("EMPLOYEE")).with(csrf().useInvalidToken())).andExpect(status().isForbidden());
  mvc.perform(post("/admin/employees/save").with(user(username).roles("EMPLOYEE")).with(csrf())).andExpect(status().isForbidden());
  mvc.perform(post("/admin/customers/delete/"+customer).with(user(username).roles("EMPLOYEE")).with(csrf())).andExpect(status().isForbidden());
  mvc.perform(post("/admin/inventory/save").with(user(username).roles("EMPLOYEE")).with(csrf())).andExpect(status().isForbidden());
  System.out.println("GD12 SECURITY 8 statuses=302,403,200,403,403,403,403,403");
 }
 @Test void roleRevocationAppliesToAlreadyAuthenticatedSession()throws Exception{
  jdbc.update("UPDATE users SET role_id=(SELECT id FROM roles WHERE name='ROLE_ADMIN') WHERE id=?",employeeUserId);entityManager.clear();var old=details.loadUserByUsername(username);
  assertTrue(old.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN")));
  jdbc.update("UPDATE users SET role_id=(SELECT id FROM roles WHERE name='ROLE_EMPLOYEE') WHERE id=?",employeeUserId);entityManager.clear();
  mvc.perform(get("/admin/products-crud").with(user(old))).andExpect(status().isForbidden());mvc.perform(get("/admin/pos").with(user(old))).andExpect(status().isOk());
  System.out.println("GD12 role refresh: stale Admin principal after demotion products=403,pos=200");
 }
 @Test @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
 void positionCannotBeCreatedAndPrivilegedCustomerAccountCannotBeResetByEmployee()throws Exception{
  var html=mvc.perform(get("/admin/employees/create").with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
  String select=html.substring(html.indexOf("id=\"position\""));select=select.substring(0,select.indexOf("</select>"));assertFalse(select.contains("data-creatable"));assertTrue(select.contains("Admin")&&select.contains("Nhân viên"));
  protectedCustomerId=new org.springframework.transaction.support.TransactionTemplate(transactions).execute(tx->{
   var c=new com.example.demodatn.entity.khachhang.Customer();c.setCustomerCode("G12-C"+employeeUserId);c.setFullName("Hồ sơ liên kết quyền nhân sự");c.setPhone("09"+String.format("%08d",Math.floorMod(username.hashCode(),100000000)));c.setEmail(username+"-protected@example.test");
   c.setUser(entityManager.find(com.example.demodatn.entity.nhanvien.User.class,employeeUserId));entityManager.persist(c);entityManager.flush();return c.getId();
  });
  mvc.perform(post("/admin/customers/"+protectedCustomerId+"/send-account").with(user(username).roles("EMPLOYEE")).with(csrf()).param("confirmed","true")).andExpect(status().isForbidden());
 }
 @Test void employeeProcessesInvoiceThroughExistingOrderStatusService()throws Exception{
  var row=jdbc.queryForMap("SELECT TOP 1 v.id,v.product_id,v.quantity,p.name,s.size_value FROM product_variants v JOIN products p ON p.id=v.product_id JOIN sizes s ON s.id=v.size_id WHERE v.quantity>1 AND v.status=1 AND p.status=1 ORDER BY v.id");
  int variant=(Integer)row.get("id"),stock=(Integer)row.get("quantity");var ward=jdbc.queryForMap("SELECT TOP 1 id,tinh_thanh_id FROM phuong_xa ORDER BY id");
  var request=new com.example.demodatn.dto.donhang.CheckoutRequest();request.setProvinceId((Integer)ward.get("tinh_thanh_id"));request.setWardId((Integer)ward.get("id"));request.setStreetAddress("Số 12, đường Nguyễn Huệ");request.setReceiverName("Khách hóa đơn nhân viên");request.setPhone("0901234567");request.setEmail("invoice-g12@example.test");request.setPaymentMethod("COD");
  var item=new com.example.demodatn.dto.donhang.CartItemDto((Integer)row.get("product_id"),variant,row.get("name").toString(),row.get("size_value").toString(),java.math.BigDecimal.ONE,1);
  var order=orderService.placeOrder(request,List.of(item),null,java.math.BigDecimal.ZERO);int invoice=invoices.findByOrder_Id(order.getId()).orElseThrow().getId();
  for(String state:List.of("CONFIRMED","CONFIRMED","PREPARING","SHIPPING","DELIVERED")){
   var action=post("/admin/invoices/"+invoice+"/status").with(user(username).roles("EMPLOYEE")).with(csrf()).param("status",state);
   if(state.equals("SHIPPING"))action.param("carrier","Đơn vị vận chuyển thử").param("trackingCode","G12-TRACK");
   var result=mvc.perform(action).andExpect(status().isFound()).andReturn();assertTrue(result.getFlashMap().containsKey("message"),state+" "+result.getFlashMap());
  }
  entityManager.flush();
  assertEquals(stock-1,jdbc.queryForObject("SELECT quantity FROM product_variants WHERE id=?",Integer.class,variant));
  assertEquals("DELIVERED",jdbc.queryForObject("SELECT status FROM orders WHERE id=?",String.class,order.getId()));
  assertEquals(5,jdbc.queryForObject("SELECT COUNT(*) FROM order_status_history WHERE order_id=?",Integer.class,order.getId()));
  mvc.perform(get("/admin/invoices/"+invoice).with(user(username).roles("EMPLOYEE"))).andExpect(status().isOk());
  System.out.println("GD12 invoice employee flow PENDING->CONFIRMED(repeated)->PREPARING->SHIPPING->DELIVERED; stock="+stock+"->"+(stock-1)+" history=5");
 }
 @Test void employeeSeesMaskedIdentityAndBlankEditKeepsStoredDocument()throws Exception{
  String key=UUID.randomUUID().toString().substring(0,8),document=String.format("%012d",Math.floorMod(key.hashCode(),1000000000));
  var c=new com.example.demodatn.entity.khachhang.Customer();c.setCustomerCode("G12-PII-"+key);c.setFullName("Khách kiểm tra riêng tư");c.setPhone("09"+String.format("%08d",Math.floorMod(key.hashCode(),100000000)));c.setEmail(key+"-pii@example.test");c.setSoCccd(document);entityManager.persist(c);entityManager.flush();
  for(String path:List.of("/admin/customers/detail/"+c.getId(),"/admin/customers/edit/"+c.getId())){
   String html=mvc.perform(get(path).with(user(username).roles("EMPLOYEE"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);assertFalse(html.contains(document),"Raw identity document leaked");
  }
  var f=new com.example.demodatn.dto.khachhang.CustomerDto();f.setId(c.getId());f.setCustomerCode(c.getCustomerCode());f.setFullName(c.getFullName());f.setPhone(c.getPhone());f.setEmail(c.getEmail());
  var ward=jdbc.queryForMap("SELECT TOP 1 id,tinh_thanh_id FROM phuong_xa ORDER BY id");f.setProvinceId((Integer)ward.get("tinh_thanh_id"));f.setWardId((Integer)ward.get("id"));f.setStreetAddress("Địa chỉ giữ căn cước");
  var previous=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
  try{org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(username,null,List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_EMPLOYEE"))));people.saveCustomer(f);}
  finally{org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(previous);}
  entityManager.flush();assertTrue(Objects.equals(document,jdbc.queryForObject("SELECT so_cccd FROM customers WHERE id=?",String.class,c.getId())),"Stored document changed");assertNull(f.getSoCccd());
  System.out.println("GD12 privacy: employee detail/edit raw document absent; blank keeps stored value; no identity printed");
 }
}
