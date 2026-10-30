package com.example.demodatn.service.donhang;

import com.example.demodatn.dto.donhang.*;
import com.example.demodatn.repository.sanpham.ProductVariantRepository;
import com.example.demodatn.repository.donhang.RevenueCondition;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mock.web.MockHttpSession;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @Transactional @WithMockUser(username="admin",roles="ADMIN")
class ShippingOnlineIntegrationTest {
    @Autowired JdbcTemplate jdbc;@Autowired ProductVariantRepository variants;@Autowired OrderService orders;
    @Autowired PosOnlineOrderService online;@Autowired OrderStatusService statuses;@Autowired OrderConfirmationService confirmation;
    @Autowired ShippingFeeService shipping;@Autowired WebApplicationContext context;
    MockMvc mvc;Integer province,ward,variant;int stock;
    @BeforeEach void setup(){
        assertTrue(jdbc.queryForObject("SELECT DB_NAME()",String.class).startsWith("shopbangiay_kt_"),"Chỉ kiểm thử trên bản sao RESTORE riêng.");
        mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        var row=jdbc.queryForMap("SELECT TOP 1 id,tinh_thanh_id FROM phuong_xa ORDER BY id");ward=((Number)row.get("id")).intValue();province=((Number)row.get("tinh_thanh_id")).intValue();
        variant=jdbc.queryForObject("SELECT TOP 1 v.id FROM product_variants v JOIN products p ON p.id=v.product_id WHERE v.quantity>5 AND v.status=1 AND p.status=1 ORDER BY v.id",Integer.class);stock=stock();
    }
    int stock(){return jdbc.queryForObject("SELECT quantity FROM product_variants WHERE id=?",Integer.class,variant);}
    List<CartItemDto> cart(int quantity){var v=variants.findById(variant).orElseThrow();return new ArrayList<>(List.of(new CartItemDto(v.getProduct().getId(),v.getId(),v.getProduct().getName(),v.getSize().getSizeValue(),v.getPrice(),quantity)));}
    PosOnlineOrderForm form(String method){var f=new PosOnlineOrderForm();f.setProvinceId(province);f.setWardId(ward);f.setStreetAddress("Số 12, đường Nguyễn Huệ <script>alert(1)</script>");f.setLatitude(new BigDecimal("21.03801"));f.setLongitude(new BigDecimal("105.7469"));f.setCoordinateSource("GHIM_TAY");f.setReceiverName("Nguyễn Văn Ánh");f.setPhone("09"+String.format("%08d",Math.floorMod(UUID.randomUUID().hashCode(),100000000)));f.setOrderSource("FACEBOOK");f.setPaymentMethod(method);f.setNote("Gọi trước khi giao");return f;}
    @Test void guestPhoneReuseNoAccountCodAndCancelKeepExistingStockLaw() throws Exception {
        var f=form("COD");int users=jdbc.queryForObject("SELECT COUNT(*) FROM users",Integer.class);var before=stock();
        var o=online.create("admin",f,cart(1));assertEquals("ONLINE",o.getOrderType());assertEquals("Online · Facebook",o.getChannelLabel());assertNull(o.getCustomer().getUser());assertEquals(before,stock());
        var customer=o.getCustomer().getId();f.setPhone("+84"+f.getPhone().substring(1));var second=online.create("admin",f,cart(1));assertEquals(customer,second.getCustomer().getId());assertEquals(users,jdbc.queryForObject("SELECT COUNT(*) FROM users",Integer.class));
        statuses.updateStatus(o.getId(),"CONFIRMED");statuses.updateStatus(o.getId(),"CONFIRMED");assertEquals(before-1,stock());statuses.updateStatus(o.getId(),"CANCELLED");statuses.updateStatus(o.getId(),"CANCELLED");assertEquals(before,stock());
        String text=confirmation.text(second);assertTrue(text.contains(f.getReceiverName())&&text.contains("Phí")&&text.contains("Tổng")&&text.contains("Nguyễn Huệ"));
        mvc.perform(get("/admin/orders/"+second.getId())).andExpect(status().isOk()).andExpect(content().string(containsString("&lt;script&gt;")));
        System.out.println("GD16 COD before="+before+" confirmed="+(before-1)+" cancelled="+stock()+" customer reused=true user count unchanged="+users);
    }
    @Test void bankTransferAndSnapshotAndNewRevenueExcludeOnlyNewShipping(){
        var f=form("BANK_TRANSFER");var o=online.create("admin",f,cart(1));assertEquals(stock-1,stock());assertEquals(0,new BigDecimal("25000").compareTo(o.getShippingFee()));assertEquals(o.getTotalAmount().add(o.getShippingFee()),o.getFinalAmount());
        String snapshot=o.getShippingAddress();jdbc.update("UPDATE phuong_xa SET ten=N'Đổi tên sau khi đặt' WHERE id=?",ward);assertEquals(snapshot,o.getShippingAddress());
        assertEquals(0,o.getTotalAmount().compareTo(jdbc.queryForObject("SELECT "+RevenueCondition.AMOUNT+" FROM orders o WHERE id=?",BigDecimal.class,o.getId())));
        jdbc.update("UPDATE orders SET shipping_policy_version=NULL WHERE id=?",o.getId());assertEquals(0,o.getFinalAmount().compareTo(jdbc.queryForObject("SELECT "+RevenueCondition.AMOUNT+" FROM orders o WHERE id=?",BigDecimal.class,o.getId())));
        statuses.updateStatus(o.getId(),"CANCELLED");assertEquals(stock,stock());
        System.out.println("GD16 revenue policy new=goods only; NULL historical=final_amount; shipping="+o.getShippingFee()+" total="+o.getFinalAmount());
    }
    @Test void quoteIgnoresForgedFeeAndDistanceAndRejectsMismatchedWard() throws Exception {
        var f=form("COD");var session=new MockHttpSession();session.setAttribute("posCart",cart(1));
        mvc.perform(post("/shipping/quote").session(session).with(csrf()).param("scope","POS_ONLINE").param("provinceId",province.toString()).param("wardId",ward.toString()).param("streetAddress",f.getStreetAddress()).param("latitude",f.getLatitude().toString()).param("longitude",f.getLongitude().toString()).param("coordinateSource","GHIM_TAY").param("shipping_fee","0").param("distance_km","99999"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.shippingFee").value(25000)).andExpect(jsonPath("$.shipping.distanceKm").value(0));
        Integer other=jdbc.queryForObject("SELECT TOP 1 id FROM tinh_thanh WHERE id<>?",Integer.class,province);f.setProvinceId(other);assertThrows(IllegalArgumentException.class,()->shipping.validateAddress(f));
        f.setProvinceId(province);f.setCoordinateSource("TAM_PHUONG_XA");f.setLatitude(BigDecimal.ZERO);f.setLongitude(BigDecimal.ZERO);jdbc.update("UPDATE phuong_xa SET latitude=NULL,longitude=NULL WHERE id=?",ward);
        assertEquals(0,new BigDecimal("55000").compareTo(shipping.quote(f,1,false,"test").shippingFee()));
    }
    @Test void eightSecurityGroupsAndAuthenticatedReadRoutes() throws Exception {
        mvc.perform(get("/shipping/config").with(anonymous())).andExpect(status().is3xxRedirection());
        mvc.perform(post("/admin/pos/online-orders").with(user("ordinary").roles("USER")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/admin/pos/online-orders")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/pos/online-orders").with(csrf().useInvalidToken())).andExpect(status().isForbidden());
        mvc.perform(post("/shipping/reverse").param("latitude","21").param("longitude","105")).andExpect(status().isForbidden());
        mvc.perform(post("/shipping/quote").with(csrf().useInvalidToken())).andExpect(status().isForbidden());
        mvc.perform(post("/shipping/quote").with(user("ordinary").roles("USER")).with(csrf()).param("scope","POS_ONLINE")).andExpect(status().isForbidden());
        mvc.perform(post("/shipping/reverse").with(csrf()).param("latitude","91").param("longitude","105")).andExpect(status().isBadRequest());
        mvc.perform(get("/shipping/config").with(user("ordinary").roles("USER"))).andExpect(status().isOk());
        mvc.perform(get("/shipping/wards").param("provinceId",province.toString())).andExpect(status().isOk());
        System.out.println("GD16 SECURITY groups=8 statuses=302,403,403,403,403,403,403,400; authenticated config/wards=200");
    }
}
