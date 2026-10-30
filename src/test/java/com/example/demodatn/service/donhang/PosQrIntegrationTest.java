package com.example.demodatn.service.donhang;

import com.example.demodatn.dto.donhang.CartItemDto;
import com.example.demodatn.entity.sanpham.*;
import com.example.demodatn.repository.sanpham.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest @Transactional @WithMockUser(username="admin",roles="ADMIN")
@TestPropertySource(properties={"shop.bank.bin=970436","shop.bank.account=123456789","shop.bank.account-name=NGUYEN VAN A","shop.bank.name=Ngân hàng kiểm thử"})
class PosQrIntegrationTest {
    @Autowired ProductRepository products;
    @Autowired ProductVariantRepository variants;
    @Autowired SizeRepository sizes;
    @Autowired ColorRepository colors;
    @Autowired MaterialRepository materials;
    @Autowired SoleRepository soles;
    @Autowired PosCatalogQuery catalog;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;
    MockMvc mvc;
    @BeforeEach void setup(){assertTrue(jdbc.queryForObject("SELECT DB_NAME()",String.class).startsWith("shopbangiay_kt_"));mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();}
    MockHttpSession session(){var p=new Product();p.setCode("QR-"+UUID.randomUUID());p.setName("Giày QR kiểm thử");products.saveAndFlush(p);var v=new ProductVariant();v.setProduct(p);v.setSize(sizes.findAll().get(0));v.setColor(colors.findAll().get(0));v.setMaterial(materials.findAll().get(0));v.setSole(soles.findAll().get(0));v.setPrice(BigDecimal.valueOf(10000000));v.setQuantity(5);variants.saveAndFlush(v);var session=new MockHttpSession();session.setAttribute("posCart",new ArrayList<>(List.of(new CartItemDto(p.getId(),v.getId(),p.getName(),v.getSize().getSizeValue(),v.getPrice(),1))));session.setAttribute("posCheckoutToken","test-token");session.setAttribute("posPaymentReference","F6SABC123");return session;}
    @Test void initialPosHasNoCustomerListAndShortSearchIsEmpty()throws Exception{
        String marker="Khách riêng "+UUID.randomUUID();jdbc.update("INSERT customers(customer_code,full_name,status) VALUES(?,?,1)","QR-C-"+UUID.randomUUID().toString().substring(0,12),marker);
        mvc.perform(get("/admin/pos")).andExpect(status().isOk()).andExpect(content().string(not(containsString(marker)))).andExpect(content().string(containsString("Nhập tên hoặc số điện thoại để tìm")));
        assertTrue(catalog.customers("",0).isEmpty());assertTrue(catalog.customers("N",0).isEmpty());assertFalse(catalog.customers(marker,0).isEmpty());
    }
    @Test void posQrUsesServerCartAndIgnoresSubmittedTotal()throws Exception{
        mvc.perform(post("/admin/pos/payment-qr").session(session()).with(csrf()).param("total","1")).andExpect(status().isOk()).andExpect(jsonPath("$.configured").value(true)).andExpect(jsonPath("$.amount").value("10000000")).andExpect(jsonPath("$.content").value("F6SABC123")).andExpect(jsonPath("$.payload",containsString("540810000000")));
    }
    @Test void receiptCheckboxAndExactAmountAreEnforcedBeforeCreatingOrder()throws Exception{
        var session=session();int before=jdbc.queryForObject("SELECT COUNT(*) FROM orders",Integer.class);
        mvc.perform(post("/admin/pos/checkout").session(session).with(csrf()).param("paymentMethod","TRANSFER").param("checkoutToken","test-token")).andExpect(status().is3xxRedirection()).andExpect(flash().attribute("error",containsString("Cần xác nhận")));
        mvc.perform(post("/admin/pos/checkout").session(session).with(csrf()).param("paymentMethod","TRANSFER").param("checkoutToken","test-token").param("transferReceived","true").param("transferAmount","1")).andExpect(status().is3xxRedirection()).andExpect(flash().attribute("error",containsString("Tổng tiền đã thay đổi")));
        assertEquals(before,jdbc.queryForObject("SELECT COUNT(*) FROM orders",Integer.class));
        mvc.perform(post("/admin/pos/checkout").session(session).with(csrf()).param("paymentMethod","TRANSFER").param("checkoutToken","test-token").param("transferReceived","true").param("transferAmount","10000000").param("paymentReference","FORGED")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("/admin/pos/success/*"));
        var row=jdbc.queryForMap("SELECT payment_reference,final_amount,payment_status FROM orders WHERE payment_reference='F6SABC123'");assertEquals("PAID",row.get("payment_status"));assertEquals(0,new BigDecimal(row.get("final_amount").toString()).compareTo(BigDecimal.valueOf(10000000)));System.out.println("GD7 exact receipt "+row);
    }
    @Test void invoiceChannelHasOwnColumnAndEightSecurityGroups()throws Exception{
        mvc.perform(get("/admin/invoices")).andExpect(status().isOk()).andExpect(content().string(containsString("<th>Đơn hàng</th><th>Kênh bán</th>")));
        mvc.perform(get("/admin/pos/customers").with(anonymous())).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/pos/customers").with(user("ordinary").roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(get("/admin/pos/customers").param("customerQuery","kh")).andExpect(status().isOk());
        mvc.perform(post("/admin/pos/payment-qr").with(anonymous()).with(csrf())).andExpect(status().is3xxRedirection());
        mvc.perform(post("/admin/pos/payment-qr").with(user("ordinary").roles("USER")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/admin/pos/payment-qr")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/pos/payment-qr").with(csrf().useInvalidToken())).andExpect(status().isForbidden());
        mvc.perform(post("/admin/pos/payment-qr").with(csrf()).session(session())).andExpect(status().isOk());
    }
}
