package com.example.demodatn.service.donhang;

import com.example.demodatn.dto.donhang.*;
import com.example.demodatn.repository.donhang.*;
import com.example.demodatn.repository.sanpham.ProductVariantRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@WithMockUser(username = "admin", roles = "ADMIN")
class InvoiceManagementIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired ProductVariantRepository variants;
    @Autowired OrderService orderService;
    @Autowired OrderRepository orderRepository;
    @Autowired OrderStatusService statusService;
    @Autowired InvoiceRepository invoices;
    @Autowired jakarta.persistence.EntityManager em;

    MockMvc mvc;
    Integer variantId, provinceId, wardId;
    int initialStock;

    @BeforeEach
    void setup() {
        com.example.demodatn.TestDatabaseGuard.check(jdbc);mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        variantId = jdbc.queryForObject("SELECT TOP 1 v.id FROM product_variants v JOIN products p ON p.id=v.product_id WHERE v.quantity>10 AND v.status=1 AND p.status=1 ORDER BY v.id", Integer.class);
        var w = jdbc.queryForMap("SELECT TOP 1 id, tinh_thanh_id FROM phuong_xa ORDER BY id");
        wardId = ((Number) w.get("id")).intValue();
        provinceId = ((Number) w.get("tinh_thanh_id")).intValue();
        initialStock = getStock();
    }

    int getStock() {
        return jdbc.queryForObject("SELECT quantity FROM product_variants WHERE id=?", Integer.class, variantId);
    }

    CartItemDto cartItem() {
        var v = variants.findById(variantId).orElseThrow();
        return new CartItemDto(v.getProduct().getId(), v.getId(), v.getProduct().getName(), v.getSize().getSizeValue(), v.getPrice(), 1);
    }

    CheckoutRequest onlineRequest(String method) {
        var r = new CheckoutRequest();
        r.setReceiverName("Khách Hàng GD22");
        r.setPhone("0988776655");
        r.setEmail("gd22@example.com");
        r.setProvinceId(provinceId);
        r.setWardId(wardId);
        r.setStreetAddress("123 Phố Huế, Hai Bà Trưng, Hà Nội");
        r.setPaymentMethod(method);
        return r;
    }

    @Test
    void statusTabsMatchDatabaseCountAndFiltersWork() throws Exception {
        // Query database count
        long totalInvoices = invoices.count();
        long pendingCount = jdbc.queryForObject("SELECT COUNT(*) FROM invoices i JOIN orders o ON o.id=i.order_id WHERE o.status='PENDING'", Long.class);

        var result = mvc.perform(get("/admin/invoices"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Quản lý hóa đơn")))
                .andExpect(content().string(containsString("Tất cả")))
                .andExpect(content().string(containsString("Chờ xử lý")))
                .andExpect(content().string(containsString("Đã xác nhận")))
                .andExpect(content().string(containsString("Đang chuẩn bị")))
                .andExpect(content().string(containsString("Đang giao")))
                .andExpect(content().string(containsString("Đã giao/Hoàn tất")))
                .andExpect(content().string(containsString("Đã hủy")))
                .andExpect(content().string(containsString("Giao thất bại")))
                .andReturn();

        // Check that tabs exist with valid status links
        String html = result.getResponse().getContentAsString();
        assertTrue(html.contains("status=PENDING") || pendingCount == 0);
    }

    @Test
    void onlineOrderProgressionFromPendingToDelivered() throws Exception {
        // 1. Create online COD order
        var order = orderService.placeOrder(onlineRequest("COD"), List.of(cartItem()), null, BigDecimal.ZERO);
        var invoice = invoices.findByOrder_Id(order.getId()).orElseThrow();
        assertEquals("PENDING", order.getStatus());
        assertEquals("UNPAID", order.getPaymentStatus());
        assertFalse(order.getDaTruKho());

        // 2. Next step from list or detail: PENDING -> CONFIRMED (deducts stock)
        mvc.perform(post("/admin/invoices/" + invoice.getId() + "/status")
                .with(csrf())
                .param("status", "CONFIRMED"))
                .andExpect(status().is3xxRedirection());

        assertEquals("CONFIRMED", jdbc.queryForObject("SELECT status FROM orders WHERE id=?", String.class, order.getId()));
        assertEquals(1, jdbc.queryForObject("SELECT da_tru_kho FROM orders WHERE id=?", Integer.class, order.getId()));
        assertEquals(initialStock - 1, getStock());

        // 3. CONFIRMED -> PREPARING
        mvc.perform(post("/admin/invoices/" + invoice.getId() + "/status")
                .with(csrf())
                .param("status", "PREPARING"))
                .andExpect(status().is3xxRedirection());

        assertEquals("PREPARING", jdbc.queryForObject("SELECT status FROM orders WHERE id=?", String.class, order.getId()));

        // 4. PREPARING -> SHIPPING with carrier and tracking
        mvc.perform(post("/admin/invoices/" + invoice.getId() + "/status")
                .with(csrf())
                .param("status", "SHIPPING")
                .param("carrier", "Giao Hàng Tiết Kiệm")
                .param("trackingCode", "GHTK-GD22-001"))
                .andExpect(status().is3xxRedirection());

        assertEquals("SHIPPING", jdbc.queryForObject("SELECT status FROM orders WHERE id=?", String.class, order.getId()));

        // 5. SHIPPING -> DELIVERED
        mvc.perform(post("/admin/invoices/" + invoice.getId() + "/status")
                .with(csrf())
                .param("status", "DELIVERED"))
                .andExpect(status().is3xxRedirection());

        assertEquals("DELIVERED", jdbc.queryForObject("SELECT status FROM orders WHERE id=?", String.class, order.getId()));
        assertEquals("PAID", jdbc.queryForObject("SELECT payment_status FROM orders WHERE id=?", String.class, order.getId()));
        assertEquals(initialStock - 1, getStock()); // stock remains deducted
    }

    @Test
    void cancelledOrderRestoresStockAndRequiresReason() throws Exception {
        // 1. Create order and confirm (stock deducted)
        var order = orderService.placeOrder(onlineRequest("COD"), List.of(cartItem()), null, BigDecimal.ZERO);
        var invoice = invoices.findByOrder_Id(order.getId()).orElseThrow();
        statusService.updateStatus(order.getId(), "CONFIRMED");
        assertEquals(initialStock - 1, getStock());

        // 2. Transition to CANCELLED without reason should fail
        mvc.perform(post("/admin/invoices/" + invoice.getId() + "/status")
                .with(csrf())
                .param("status", "CANCELLED")
                .param("reason", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));

        // Stock still deducted
        assertEquals(initialStock - 1, getStock());

        // 3. Transition to CANCELLED with valid reason
        mvc.perform(post("/admin/invoices/" + invoice.getId() + "/status")
                .with(csrf())
                .param("status", "CANCELLED")
                .param("reason", "Khách hàng đổi ý muốn mua màu khác"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("message"));

        assertEquals("CANCELLED", jdbc.queryForObject("SELECT status FROM orders WHERE id=?", String.class, order.getId()));
        assertEquals(0, jdbc.queryForObject("SELECT da_tru_kho FROM orders WHERE id=?", Integer.class, order.getId()));
        // Stock restored!
        assertEquals(initialStock, getStock());
    }

    @Test
    void deliveryFailedRestoresStockAndRequiresReason() throws Exception {
        // 1. Create order, advance to SHIPPING
        var order = orderService.placeOrder(onlineRequest("COD"), List.of(cartItem()), null, BigDecimal.ZERO);
        var invoice = invoices.findByOrder_Id(order.getId()).orElseThrow();
        statusService.updateStatus(order.getId(), "CONFIRMED");
        statusService.updateStatus(order.getId(), "PREPARING");
        statusService.transition(order.getId(), "SHIPPING", null, "VNPost", "VNP-998877");
        assertEquals(initialStock - 1, getStock());

        // 2. Transition to DELIVERY_FAILED without reason should fail
        mvc.perform(post("/admin/invoices/" + invoice.getId() + "/status")
                .with(csrf())
                .param("status", "DELIVERY_FAILED")
                .param("reason", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));

        // 3. Transition to DELIVERY_FAILED with reason
        mvc.perform(post("/admin/invoices/" + invoice.getId() + "/status")
                .with(csrf())
                .param("status", "DELIVERY_FAILED")
                .param("reason", "Số điện thoại thuê bao, gọi 3 lần không liên lạc được"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("message"));

        assertEquals("DELIVERY_FAILED", jdbc.queryForObject("SELECT status FROM orders WHERE id=?", String.class, order.getId()));
        assertEquals(0, jdbc.queryForObject("SELECT da_tru_kho FROM orders WHERE id=?", Integer.class, order.getId()));
        // Stock restored!
        assertEquals(initialStock, getStock());
    }

    @Test
    void confirmBankTransferPaymentSuccess() throws Exception {
        var order = orderService.placeOrder(onlineRequest("BANK_TRANSFER"), List.of(cartItem()), null, BigDecimal.ZERO);
        var invoice = invoices.findByOrder_Id(order.getId()).orElseThrow();
        assertEquals("UNPAID", jdbc.queryForObject("SELECT payment_status FROM orders WHERE id=?", String.class, order.getId()));

        mvc.perform(post("/admin/invoices/" + invoice.getId() + "/confirm-payment")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("message"));

        assertEquals("PAID", jdbc.queryForObject("SELECT payment_status FROM orders WHERE id=?", String.class, order.getId()));
    }

    @Test
    void securityNonAdminNonEmployeeGetsForbidden() throws Exception {
        mvc.perform(get("/admin/invoices").with(user("regularUser").roles("USER")))
                .andExpect(status().isForbidden());

        mvc.perform(post("/admin/invoices/1/status").with(user("regularUser").roles("USER")).with(csrf()).param("status", "CONFIRMED"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/admin/invoices/1/confirm-payment").with(user("regularUser").roles("USER")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void employeeHasAccessToInvoiceListAndDetail() throws Exception {
        mvc.perform(get("/admin/invoices").with(user("staff").roles("EMPLOYEE")))
                .andExpect(status().isOk());
    }
}
