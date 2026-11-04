package com.example.demodatn.controller.user;

import com.example.demodatn.dto.donhang.CartItemDto;
import com.example.demodatn.repository.sanpham.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
@WithMockUser(username="gd27-checkout",roles="USER")
class CheckoutPresentationIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired ProductVariantRepository variants;
    MockMvc mvc; MockHttpSession session; int variantId; int customerId;
    @BeforeEach void setup() {
        mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        int role=jdbc.queryForObject("SELECT id FROM roles WHERE name='ROLE_USER'",Integer.class);
        int userId=jdbc.queryForObject("INSERT users(username,password,role_id,status) OUTPUT INSERTED.id VALUES('gd27-checkout','mock-only',?,1)",Integer.class,role);
        customerId=jdbc.queryForObject("INSERT customers(customer_code,full_name,email,phone,user_id,status) OUTPUT INSERTED.id VALUES('GD27-CHECKOUT',N'Nguyễn Văn Ánh','gd27@example.test','0912345678',?,1)",Integer.class,userId);
        variantId=jdbc.queryForObject("SELECT TOP 1 v.id FROM product_variants v JOIN products p ON p.id=v.product_id WHERE v.status=1 AND p.status=1 AND v.quantity>=2 AND v.price>0 ORDER BY v.id",Integer.class);
        var v=variants.findById(variantId).orElseThrow();
        var item=new CartItemDto(v.getProduct().getId(),variantId,v.getProduct().getName(),v.getSize().getSizeValue(),v.getMaterial().getName(),v.getColor().getName(),v.getSole().getName(),v.getPrice(),1);
        session=new MockHttpSession();session.setAttribute("cart",new ArrayList<>(List.of(item)));
    }
    @Test void checkoutRendersCardsAndKeepsDeliveryAndEmailFields() throws Exception {
        mvc.perform(get("/checkout").session(session)).andExpect(status().isOk())
            .andExpect(content().string(containsString("checkout-layout")))
            .andExpect(content().string(containsString("checkout-mobile-bar")))
            .andExpect(content().string(containsString("data-delivery-province")))
            .andExpect(content().string(containsString("name=\"email\"")))
            .andExpect(content().string(not(containsString("data-table-key=\"user-thanh-toan"))));
    }
    @Test void previewIgnoresForgedTotalsAndRejectsInvalidVoucher() throws Exception {
        var r=mvc.perform(post("/checkout/preview").session(session).with(csrf()).param("discount","999999999").param("total","1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.pricing.discountAmount").value(0)).andReturn();
        var json=new tools.jackson.databind.ObjectMapper().readTree(r.getResponse().getContentAsString());
        assertTrue(json.at("/pricing/total").decimalValue().signum()>0);
        assertEquals(json.at("/pricing/subtotal"),json.at("/pricing/total"));
        mvc.perform(post("/checkout/preview").session(session).with(csrf()).param("voucherCode","GD27-NOT-EXIST"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.pricing.valid").value(false));
    }
    @Test void everyCheckoutPostRequiresCsrfAndGuestsCannotReadVouchers() throws Exception {
        for(String url:List.of("/checkout/preview","/checkout/validate-voucher-ajax","/checkout/place")) {
            mvc.perform(post(url).session(session)).andExpect(status().isForbidden());
            mvc.perform(post(url).session(session).with(csrf().useInvalidToken())).andExpect(status().isForbidden());
        }
        mvc.perform(get("/checkout/vouchers").with(anonymous())).andExpect(status().isForbidden());
        mvc.perform(post("/admin/products-crud/save").with(csrf())).andExpect(status().isForbidden());
    }
    @Test void codOrderUsesServerPriceAndDoesNotDeductStock() throws Exception {
        int before=jdbc.queryForObject("SELECT quantity FROM product_variants WHERE id=?",Integer.class,variantId);
        int province=jdbc.queryForObject("SELECT TOP 1 tinh_thanh_id FROM phuong_xa ORDER BY id",Integer.class);
        int ward=jdbc.queryForObject("SELECT TOP 1 id FROM phuong_xa WHERE tinh_thanh_id=? ORDER BY id",Integer.class,province);
        mvc.perform(post("/checkout/place").session(session).with(csrf()).param("receiverName","Nguyễn Văn Ánh").param("phone","0912345678").param("email","gd27@example.test")
            .param("provinceId",""+province).param("wardId",""+ward).param("streetAddress","Số 12, đường Lê Lợi").param("paymentMethod","COD")
            .param("coordinateSource","").param("discount","999999999").param("total","1"))
            .andExpect(redirectedUrlPattern("/orders/success/*"));
        assertEquals(before,jdbc.queryForObject("SELECT quantity FROM product_variants WHERE id=?",Integer.class,variantId));
        assertTrue(jdbc.queryForObject("SELECT final_amount FROM orders WHERE customer_id=?",java.math.BigDecimal.class,customerId).signum()>0);
        assertNull(session.getAttribute("cart"));
    }
}
