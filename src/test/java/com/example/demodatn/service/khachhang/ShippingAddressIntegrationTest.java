package com.example.demodatn.service.khachhang;

import com.example.demodatn.dto.khachhang.ShippingAddressForm;
import com.example.demodatn.dto.donhang.CartItemDto;
import com.example.demodatn.exception.PeopleValidationException;
import com.example.demodatn.repository.sanpham.ProductVariantRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class ShippingAddressIntegrationTest {
    @Autowired ShippingAddressService service;
    @Autowired ProductVariantRepository variants;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;
    MockMvc mvc;
    static final String UNICODE="Số 12, đường Nguyễn Huệ, Đà Nẵng";
    static final AtomicInteger sequence=new AtomicInteger(1000);
    record Owner(int id,int userId,String username) {}
    @BeforeEach void guard() {
        assertTrue((Set.of("shopbangiay_kt","shop_verify","shopbangiay_thu").contains(jdbc.queryForObject("SELECT DB_NAME()",String.class)) || jdbc.queryForObject("SELECT DB_NAME()",String.class).startsWith("shopbangiay_kt_")));
        mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    Owner owner() {
        String name="addr-"+UUID.randomUUID();int role=jdbc.queryForObject("SELECT id FROM roles WHERE name='ROLE_USER'",Integer.class);
        int user=jdbc.queryForObject("INSERT users(username,password,role_id,status) OUTPUT INSERTED.id VALUES(?,'mock-only',?,1)",Integer.class,name,role);
        int id=jdbc.queryForObject("INSERT customers(customer_code,full_name,phone,email,user_id,status) OUTPUT INSERTED.id VALUES(?,N'Khách Nguyễn Huệ',?,?,?,1)",Integer.class,"ADDR-"+UUID.randomUUID().toString().substring(0,12),"08"+String.format("%08d",sequence.incrementAndGet()),name+"@example.test",user);
        return new Owner(id,user,name);
    }
    ShippingAddressForm form(String text) {var f=new ShippingAddressForm();f.setProvinceId(jdbc.queryForObject("SELECT TOP 1 id FROM tinh_thanh ORDER BY id",Integer.class));f.setWardId(jdbc.queryForObject("SELECT TOP 1 id FROM phuong_xa WHERE tinh_thanh_id=? ORDER BY id",Integer.class,f.getProvinceId()));f.setStreetAddress("Số 12, đường Nguyễn Huệ");f.setReceiverName("Nguyễn Đà Nẵng");f.setPhone("+84901234567");f.setAddress(text);f.setStreetAddress(text);return f;}
    MockHttpSession cart() {
        var v=variants.findByStatusTrueAndProduct_StatusTrue().stream().filter(x->x.getQuantity()>0).findFirst().orElseThrow();
        var s=new MockHttpSession();s.setAttribute("cart",new ArrayList<>(List.of(new CartItemDto(v.getProduct().getId(),v.getId(),v.getProduct().getName(),v.getSize().getSizeValue(),v.getPrice(),1))));return s;
    }
    @Test @Transactional void threeAddressesAndUnicodeAndServiceLimit() {
        var o=owner();for(int n=0;n<3;n++)service.save(o.username(),form(n==0?UNICODE:"Địa chỉ "+n));
        assertEquals(3,service.list(o.username()).size());assertEquals(1,service.list(o.username()).stream().filter(a->a.isDefaultAddress()).count());
        assertEquals(UNICODE,jdbc.queryForObject("SELECT dia_chi FROM dia_chi_nhan_hang WHERE customer_id=? AND thu_tu=1",String.class,o.id()));
        assertEquals(UNICODE,jdbc.queryForObject("SELECT address FROM customers WHERE id=?",String.class,o.id()));
        assertEquals(ShippingAddressService.LIMIT_MESSAGE,assertThrows(PeopleValidationException.class,()->service.save(o.username(),form("Thứ tư"))).getMessage());
        System.out.println("PHASE_C before=0 after=3 fourth service rejected; Unicode equality=true");
    }
    @Test @Transactional void databaseEnforcesSlotLimitUniquenessAndOneDefault() {
        var o=owner();for(int n=0;n<3;n++)service.save(o.username(),form("Địa chỉ "+n));
        String sql="INSERT dia_chi_nhan_hang(customer_id,ten_nguoi_nhan,so_dien_thoai,dia_chi,la_mac_dinh,thu_tu) VALUES(?,N'Thử chèn','0901234567',N'Địa chỉ SQL',0,?)";
        assertThrows(DataIntegrityViolationException.class,()->jdbc.update(sql,o.id(),4));
        assertThrows(DataIntegrityViolationException.class,()->jdbc.update(sql,o.id(),3));
        assertThrows(DataIntegrityViolationException.class,()->jdbc.update("UPDATE dia_chi_nhan_hang SET la_mac_dinh=1 WHERE customer_id=? AND thu_tu=2",o.id()));
        assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM dia_chi_nhan_hang WHERE customer_id=?",Integer.class,o.id()));
        System.out.println("PHASE_C direct SQL: slot4 CHECK rejected; duplicate slot UNIQUE rejected; second default UNIQUE rejected");
    }
    @Test @Transactional void defaultSwitchEditDeleteAndFreeSlotReuseSyncLegacyAddress() {
        var o=owner();var a=service.save(o.username(),form(UNICODE));var b=service.save(o.username(),form("Thứ hai"));
        service.makeDefault(o.username(),b.getId());assertEquals("Thứ hai",jdbc.queryForObject("SELECT address FROM customers WHERE id=?",String.class,o.id()));
        var f=form("Địa chỉ mặc định đã sửa");f.setId(b.getId());service.save(o.username(),f);
        assertEquals(f.getAddress(),jdbc.queryForObject("SELECT address FROM customers WHERE id=?",String.class,o.id()));
        service.delete(o.username(),b.getId());assertTrue(service.owned(o.username(),a.getId()).isDefaultAddress());
        assertEquals(UNICODE,jdbc.queryForObject("SELECT address FROM customers WHERE id=?",String.class,o.id()));
        var c=service.save(o.username(),form("Dùng vị trí trống"));assertEquals(2,c.getSlot());
        service.delete(o.username(),a.getId());service.delete(o.username(),c.getId());assertNull(jdbc.queryForObject("SELECT address FROM customers WHERE id=?",String.class,o.id()));
    }
    @Test void concurrentAddsFromTwoAddressesHaveOneWinner() throws Exception {
        var o=owner();var pool=Executors.newFixedThreadPool(2);
        try {
            service.save(o.username(),form("Trước đồng thời 1"));service.save(o.username(),form("Trước đồng thời 2"));
            var start=new CountDownLatch(1);List<Future<Boolean>> futures=new ArrayList<>();
            for(int n=0;n<2;n++){final int index=n;futures.add(pool.submit(()->{start.await();try{service.save(o.username(),form("Đồng thời "+index));return true;}catch(PeopleValidationException e){assertEquals(ShippingAddressService.LIMIT_MESSAGE,e.getMessage());return false;}}));}
            start.countDown();int wins=0;for(var f:futures)if(f.get(20,TimeUnit.SECONDS))wins++;
            assertEquals(1,wins);assertEquals(3,service.list(o.username()).size());
            System.out.println("PHASE_C concurrency: before=2 requests=2 success=1 rejected=1 after=3");
        } finally {pool.shutdownNow();jdbc.update("DELETE FROM customers WHERE id=?",o.id());jdbc.update("DELETE FROM users WHERE id=?",o.userId());}
    }
    @Test @Transactional void validationAndProfileAdminBookRender() throws Exception {
        var o=owner();var f=form(UNICODE);f.setReceiverName(" ");assertThrows(PeopleValidationException.class,()->service.save(o.username(),f));
        f.setReceiverName("Nguyễn Huệ");f.setPhone("12345");assertThrows(PeopleValidationException.class,()->service.save(o.username(),f));
        f.setPhone("0901234567");f.setAddress("a".repeat(256));f.setStreetAddress("a".repeat(256));assertThrows(PeopleValidationException.class,()->service.save(o.username(),f));
        mvc.perform(post("/profile/addresses/save").with(user(o.username()).roles("USER")).with(csrf()).param("receiverName","").param("phone","wrong").param("address",""))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("shippingAddress","receiverName","phone","streetAddress"));
        for(int n=0;n<3;n++)service.save(o.username(),form(n==0?UNICODE:"Địa chỉ "+n));
        mvc.perform(get("/profile").with(user(o.username()).roles("USER"))).andExpect(status().isOk()).andExpect(content().string(containsString(ShippingAddressService.LIMIT_MESSAGE))).andExpect(content().string(not(containsString(">Thêm địa chỉ</a>"))));
        mvc.perform(get("/admin/customers/detail/"+o.id()).with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(content().string(containsString(UNICODE))).andExpect(content().string(not(containsString("/profile/addresses/"))));
    }
    @Test @Transactional void checkoutSnapshotsAllThreeAddressesAndTemporaryEdit() throws Exception {
        var o=owner();for(int n=0;n<3;n++)service.save(o.username(),form(n==0?UNICODE:"Địa chỉ "+n));
        var book=service.list(o.username());service.makeDefault(o.username(),book.get(1).getId());
        var r=mvc.perform(get("/checkout").with(user(o.username()).roles("USER")).session(cart())).andExpect(status().isOk()).andExpect(content().string(containsString("Giao tới"))).andReturn();
        var req=(com.example.demodatn.dto.donhang.CheckoutRequest)r.getModelAndView().getModel().get("checkoutRequest");assertEquals(book.get(1).getId(),req.getShippingAddressId());
        for(int n=0;n<4;n++) {
            var a=book.get(n%3);String text=n==3?"Địa chỉ tạm cho đơn này":a.getAddress();
            r=mvc.perform(post("/checkout/place").with(user(o.username()).roles("USER")).with(csrf()).session(cart()).param("shippingAddressId",a.getId().toString()).param("receiverName",a.getReceiverName()).param("phone",a.getPhone()).param("email","order@example.test").param("streetAddress",text).param("provinceId",jdbc.queryForObject("SELECT TOP 1 id FROM tinh_thanh ORDER BY id",String.class)).param("wardId",jdbc.queryForObject("SELECT TOP 1 w.id FROM phuong_xa w WHERE w.tinh_thanh_id=(SELECT TOP 1 id FROM tinh_thanh ORDER BY id) ORDER BY w.id",String.class)).param("paymentMethod","COD")).andExpect(status().is3xxRedirection()).andReturn();
            String url=r.getResponse().getRedirectedUrl();assertTrue(url.startsWith("/orders/success/"),url);int id=Integer.parseInt(url.substring(url.lastIndexOf('/')+1));
            var row=jdbc.queryForMap("SELECT receiver_name,phone_number,shipping_address FROM orders WHERE id=?",id);assertEquals(a.getReceiverName(),row.get("receiver_name"));assertEquals(a.getPhone(),row.get("phone_number"));assertEquals(text,row.get("shipping_address"));
            System.out.println("PHASE_C order="+id+" addressId="+a.getId()+" temporary="+(n==3)+" snapshot equality=true");
        }
        assertEquals(UNICODE,service.owned(o.username(),book.get(0).getId()).getAddress());
    }
    @Test @Transactional void checkoutWithNoBookAndPersonalUpdateCannotOverwriteMirror() throws Exception {
        var o=owner();mvc.perform(get("/checkout").with(user(o.username()).roles("USER")).session(cart())).andExpect(status().isOk()).andExpect(content().string(containsString("Bạn chưa có địa chỉ đã lưu")));
        service.save(o.username(),form(UNICODE));
        mvc.perform(post("/profile/update").with(user(o.username()).roles("USER")).with(csrf()).param("fullName","Khách đổi tên").param("phone","0904567890").param("email",o.username()+"@example.test").param("address","Giả mạo địa chỉ cũ"))
            .andExpect(status().is3xxRedirection());
        assertEquals(UNICODE,jdbc.queryForObject("SELECT address FROM customers WHERE id=?",String.class,o.id()));
    }
    @Test @Transactional void eightSecurityScenarios() throws Exception {
        var a=owner();var b=owner();int id=service.save(a.username(),form(UNICODE)).getId();
        mvc.perform(get("/profile")).andExpect(status().is3xxRedirection());System.out.println("C_SECURITY 1 anonymous profile=302");
        mvc.perform(post("/profile/addresses/save").with(user(a.username()).roles("USER"))).andExpect(status().isForbidden());System.out.println("C_SECURITY 2 owner missing CSRF=403");
        mvc.perform(post("/profile/addresses/"+id+"/delete").with(user(a.username()).roles("USER")).with(csrf().useInvalidToken())).andExpect(status().isForbidden());System.out.println("C_SECURITY 3 invalid CSRF=403");
        mvc.perform(post("/profile/addresses/save").with(user(b.username()).roles("USER")).with(csrf()).param("id",""+id).param("receiverName","Tấn công").param("phone","0901234567").param("address","Không được sửa")).andExpect(status().isNotFound());System.out.println("C_SECURITY 4 foreign edit=404");
        mvc.perform(post("/profile/addresses/"+id+"/delete").with(user(b.username()).roles("USER")).with(csrf())).andExpect(status().isNotFound());System.out.println("C_SECURITY 5 foreign delete=404");
        mvc.perform(post("/profile/addresses/"+id+"/default").with(user(b.username()).roles("USER")).with(csrf())).andExpect(status().isNotFound());System.out.println("C_SECURITY 6 foreign default=404");
        mvc.perform(get("/profile").param("addressId",""+id).with(user(b.username()).roles("USER"))).andExpect(status().isNotFound());System.out.println("C_SECURITY 7 foreign view=404");
        mvc.perform(post("/checkout/place").with(user(b.username()).roles("USER")).with(csrf()).session(cart()).param("shippingAddressId",""+id).param("receiverName","Khách B").param("phone","0901234567").param("email","b@example.test").param("streetAddress","Địa chỉ B").param("provinceId",jdbc.queryForObject("SELECT TOP 1 id FROM tinh_thanh ORDER BY id",String.class)).param("wardId",jdbc.queryForObject("SELECT TOP 1 w.id FROM phuong_xa w WHERE w.tinh_thanh_id=(SELECT TOP 1 id FROM tinh_thanh ORDER BY id) ORDER BY w.id",String.class)).param("paymentMethod","COD")).andExpect(status().isNotFound());System.out.println("C_SECURITY 8 checkout foreign ID=404");
        assertEquals(UNICODE,service.owned(a.username(),id).getAddress());
    }
}
