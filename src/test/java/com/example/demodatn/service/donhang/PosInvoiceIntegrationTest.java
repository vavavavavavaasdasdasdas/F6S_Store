package com.example.demodatn.service.donhang;
import com.example.demodatn.dto.donhang.CartItemDto;
import com.example.demodatn.dto.donhang.CheckoutRequest;
import com.example.demodatn.entity.donhang.Order;
import com.example.demodatn.entity.sanpham.Product;
import com.example.demodatn.entity.sanpham.ProductVariant;
import com.example.demodatn.repository.donhang.InvoiceRepository;
import com.example.demodatn.repository.donhang.OrderRepository;
import com.example.demodatn.repository.nhanvien.EmployeeRepository;
import com.example.demodatn.repository.sanpham.MaterialRepository;
import com.example.demodatn.repository.sanpham.ProductRepository;
import com.example.demodatn.repository.sanpham.ProductVariantRepository;
import com.example.demodatn.repository.sanpham.SizeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@WithMockUser(username="admin",roles="ADMIN")
class PosInvoiceIntegrationTest {
    @org.springframework.beans.factory.annotation.Autowired com.example.demodatn.repository.sanpham.ColorRepository phase5Colors;
    @org.springframework.beans.factory.annotation.Autowired com.example.demodatn.repository.sanpham.SoleRepository phase5Soles;

    @Autowired OrderService service;
    @Autowired OrderRepository orders;
    @Autowired InvoiceRepository invoices;
    @Autowired ProductRepository products;
    @Autowired ProductVariantRepository variants;
    @Autowired SizeRepository sizes;
    @Autowired MaterialRepository materials;
    @Autowired EmployeeRepository employees;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;

    private ProductVariant variant() {
        var p=new Product();p.setName("Kiểm thử Phase 9");p.setCode("P9-"+UUID.randomUUID());products.save(p);
        var v=new ProductVariant();v.setProduct(p);v.setSize(sizes.findAll().get(0));v.setMaterial(materials.findAll().get(0));v.setColor(phase5Colors.findAll().get(0));v.setSole(phase5Soles.findAll().get(0));v.setPrice(new BigDecimal("150000"));v.setQuantity(5);return variants.saveAndFlush(v);
    }
    private CartItemDto item(ProductVariant v) {
        return new CartItemDto(v.getProduct().getId(),v.getId(),v.getProduct().getName(),v.getSize().getSizeValue(),BigDecimal.ONE,2);
    }
    private Order sale(ProductVariant v,String method,BigDecimal tendered) {
        return service.createPosOrder(List.of(item(v)),null,employees.findByUser_Username("admin").orElseThrow(),null,method,"Tại quầy",tendered);
    }
    @Test @Transactional
    void cashSaleCreatesOneInvoiceWithServerPricesAndCorrectChange() {
        var v=variant();var o=sale(v,"CASH",new BigDecimal("500000"));
        assertEquals(0,new BigDecimal("300000").compareTo(o.getFinalAmount()));assertEquals(3,v.getQuantity());
        var i=invoices.findByOrder_Id(o.getId()).orElseThrow();assertEquals(o.getId(),i.getOrder().getId());
        assertEquals(0,new BigDecimal("200000").compareTo(i.getChangeAmount()));assertEquals("Khách lẻ",i.getCustomerName());assertEquals("admin",o.getEmployee().getUser().getUsername());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM invoices WHERE order_id=?",Integer.class,o.getId()));
    }
    @Test @Transactional
    void onlineSaleAlsoCreatesInvoiceAndStatusCannotSkipSteps() {
        var v=variant();var r=new CheckoutRequest();r.setProvinceId(jdbc.queryForObject("SELECT TOP 1 id FROM tinh_thanh ORDER BY id",Integer.class));r.setWardId(jdbc.queryForObject("SELECT TOP 1 id FROM phuong_xa WHERE tinh_thanh_id=? ORDER BY id",Integer.class,r.getProvinceId()));r.setStreetAddress("Số 12, đường Nguyễn Huệ");r.setReceiverName("Online P9");r.setPaymentMethod("COD");
        var o=service.placeOrder(r,List.of(item(v)),null,BigDecimal.ZERO);
        assertTrue(invoices.findByOrder_Id(o.getId()).isPresent());
        assertThrows(IllegalArgumentException.class,()->service.updateStatus(o.getId(),"DELIVERED"));
        service.updateStatus(o.getId(),"CONFIRMED");service.updateStatus(o.getId(),"PREPARING");service.updateStatus(o.getId(),"SHIPPING");service.updateStatus(o.getId(),"DELIVERED");
        assertEquals("PAID",o.getPaymentStatus());assertThrows(IllegalArgumentException.class,()->service.updateStatus(o.getId(),"CANCELLED"));
    }
    @Test @Transactional
    void transferRequiresConfirmationBeforeCompletionAndCancelRestocksOnce() {
        var v=variant();var o=sale(v,"TRANSFER",null);assertEquals("PAID",o.getPaymentStatus());
        assertThrows(IllegalArgumentException.class,()->service.updateStatus(o.getId(),"DELIVERED"));
        service.confirmPayment(o.getId());assertEquals("PAID",o.getPaymentStatus());
        service.updateStatus(o.getId(),"CANCELLED");service.updateStatus(o.getId(),"CANCELLED");assertEquals(5,v.getQuantity());
        assertThrows(IllegalArgumentException.class,()->service.confirmPayment(o.getId()));assertTrue(invoices.findByOrder_Id(o.getId()).isPresent());
    }
    @Test @Transactional
    void missingEmployeeAndShortCashAreRejectedWithoutCreatingSale() {
        var v=variant();long before=orders.count();
        assertThrows(IllegalArgumentException.class,()->service.createPosOrder(List.of(item(v)),null,null,null,"CASH","Quầy",new BigDecimal("500000")));
        assertThrows(IllegalArgumentException.class,()->sale(v,"CASH",new BigDecimal("299999")));
        assertEquals(before,orders.count());assertEquals(5,v.getQuantity());
    }
    @Test @Transactional
    void sessionCheckoutTokenPreventsRepeatedSale() throws Exception {
        var v=variant();var session=new org.springframework.mock.web.MockHttpSession();session.setAttribute("posCart",new ArrayList<>(List.of(item(v))));session.setAttribute("posCheckoutToken","P9-token");
        var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();long before=orders.count();
        for(int repeat=0;repeat<2;repeat++)mvc.perform(post("/admin/pos/checkout").with(csrf()).session(session).param("checkoutToken","P9-token").param("paymentMethod","CASH").param("cashReceived","500000")).andExpect(status().is3xxRedirection());
        assertEquals(before+1,orders.count());assertEquals(3,v.getQuantity());assertNull(session.getAttribute("posCart"));
    }
    @Test @WithMockUser(username="user",roles="USER")
    void customerCannotReadOrMutatePosInvoicesOrdersAndStatistics() throws Exception {
        var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        for(String url:List.of("/admin/pos","/admin/pos/success/1","/admin/invoices","/admin/invoices/1","/admin/orders","/admin/orders/1","/admin/reports/revenue"))mvc.perform(get(url)).andExpect(status().isForbidden());
        for(String url:List.of("/admin/pos/add","/admin/pos/quantity","/admin/pos/remove","/admin/pos/checkout","/admin/orders/1/status","/admin/orders/1/confirm-payment"))mvc.perform(post(url).with(csrf())).andExpect(status().isForbidden());
    }
    @Test @Transactional
    void detailRendersAllowedStatusesInvoiceLinkAndExactFractionalAmounts() throws Exception {
        var v=variant();v.setPrice(new BigDecimal("150000.25"));variants.flush();var o=sale(v,"CASH",new BigDecimal("400000"));
        var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        mvc.perform(get("/admin/orders/"+o.getId())).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Đang chuẩn bị")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("300.000,5")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("/admin/invoices/")));
    }
    @Test
    void unknownOrderAndInvoiceAre404() throws Exception {
        var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();mvc.perform(get("/admin/orders/-1")).andExpect(status().isNotFound());mvc.perform(get("/admin/invoices/-1")).andExpect(status().isNotFound());
    }
    @Test
    void invoiceAndStatisticsRoutesRenderRealPagesWithoutPlaceholderCollision() throws Exception {
        var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        for(String route:List.of("/admin/invoices","/admin/reports/revenue","/admin/reports/best-sellers")) {
            mvc.perform(get(route)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Sẽ hoàn thiện"))));
        }
    }
    @Test
    void revenueCardsOpenMatchingTodayAndMonthRanges() throws Exception {
        var today=java.time.LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        mvc.perform(get("/admin/reports/revenue").param("period","today")).andExpect(status().isOk())
            .andExpect(model().attribute("from",today)).andExpect(model().attribute("to",today));
        mvc.perform(get("/admin/reports/revenue").param("period","month")).andExpect(status().isOk())
            .andExpect(model().attribute("from",today.withDayOfMonth(1))).andExpect(model().attribute("to",today));
    }
}
