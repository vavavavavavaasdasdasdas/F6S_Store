package com.example.demodatn.service;

import com.example.demodatn.dto.khachhang.CustomerDto;
import com.example.demodatn.repository.khachhang.CustomerRepository;
import com.example.demodatn.repository.nhanvien.UserRepository;
import com.example.demodatn.repository.sanpham.AdminCatalogQuery;
import com.example.demodatn.service.nhanvien.AccountMailService;
import com.example.demodatn.service.nhanvien.AccountProvisioningService;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class GiaiDoan20IntegrationTest {

    static GreenMail SMTP = new GreenMail(new ServerSetup(0, "127.0.0.1", ServerSetup.PROTOCOL_SMTP));

    static {
        SMTP.setUser("noreply@example.test", "test", "test");
        SMTP.start();
        SMTP.setQuotaSupported(false);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) {
        r.add("spring.mail.host", () -> "127.0.0.1");
        r.add("spring.mail.port", () -> SMTP.getSmtp().getPort());
        r.add("spring.mail.properties.mail.smtp.auth", () -> "false");
        r.add("spring.mail.properties.mail.smtp.starttls.enable", () -> "false");
        r.add("shop.mail.from", () -> "noreply@example.test");
        r.add("shop.public-base-url", () -> "http://localhost:18140");
    }

    @Autowired AccountMailService mailService;
    @Autowired AccountProvisioningService accounts;
    @Autowired CustomerRepository customers;
    @Autowired UserRepository users;
    @Autowired AdminCatalogQuery catalogQuery;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;

    MockMvc mvc;

    @BeforeEach
    void setup() throws Exception {
        assertTrue(jdbc.queryForObject("SELECT DB_NAME()", String.class).startsWith("shopbangiay_kt_"),
                "Disposable restored test copy required");
        SMTP.purgeEmailFromAllMailboxes();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterAll
    static void stop() {
        SMTP.stop();
    }

    // --- Section A Tests: Email Provisioning ---

    @Test
    void testMailConfiguredStateAndTestEmailEndpoint() throws Exception {
        assertTrue(mailService.isConfigured(), "Mail service should be configured with GreenMail");
        assertEquals("127.0.0.1", mailService.getResolvedHost());

        // Test sending test email via service
        assertDoesNotThrow(() -> mailService.sendTestMail("admin@example.test"));
        assertTrue(SMTP.waitForIncomingEmail(3000, 1), "Test email should have been received by fake SMTP");

        // Test POST /admin/account/test-email endpoint as ADMIN
        mvc.perform(post("/admin/account/test-email")
                .with(csrf())
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/account"));
    }

    @Test
    void testPosQuickCreateCustomerWithEmailIssuesAccountAndSendsMail() throws Exception {
        String token = UUID.randomUUID().toString().substring(0, 8);
        String testEmail = "quick-" + token + "@example.test";
        SMTP.setUser(testEmail, token, token);

        mvc.perform(post("/admin/pos/customers/quick-create")
                .with(csrf())
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
                .param("fullName", "Khách Hàng Nhanh " + token)
                .param("phone", "09" + String.format("%08d", Math.floorMod(token.hashCode(), 100000000)))
                .param("email", testEmail))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountIssued").value(true))
                .andExpect(jsonPath("$.accountSent").value(true));

        assertTrue(SMTP.waitForIncomingEmail(3000, 1), "Quick-created customer should receive credentials email");
        MimeMessage msg = SMTP.getReceivedMessages()[0];
        assertNotNull(msg);
        assertEquals(testEmail, msg.getAllRecipients()[0].toString());
    }

    // --- Section C Tests: Product Table Stock & Stock Filter ---

    @Test
    void testProductCatalogQueryStockCalculationAndFilter() {
        // Query products with different stock status filters
        var all = catalogQuery.productsWithStock("", null, null, "newest", 0, "ALL", 10);
        assertNotNull(all);
        assertTrue(all.getTotalElements() > 0);

        var inStock = catalogQuery.productsWithStock("", null, null, "newest", 0, "IN_STOCK", 10);
        assertNotNull(inStock);
        for (var row : inStock.getContent()) {
            Number stock = (Number) row.get("stock");
            assertNotNull(stock);
            assertTrue(stock.longValue() > 0, "IN_STOCK items must have stock > 0");
        }

        var outOfStock = catalogQuery.productsWithStock("", null, null, "newest", 0, "OUT_OF_STOCK", 10);
        assertNotNull(outOfStock);
        for (var row : outOfStock.getContent()) {
            Number stock = (Number) row.get("stock");
            assertNotNull(stock);
            assertEquals(0L, stock.longValue(), "OUT_OF_STOCK items must have stock == 0");
        }
    }

    // --- Section D & E Tests: Menu Name & Dashboard Filter Bar Counter ---

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testDashboardDefaultFilterBarCounterIsZero() throws Exception {
        // Default GET /admin/dashboard (7 days, ALL channels, no category)
        mvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("activeFilterCount", 0))
                .andExpect(model().attribute("isDefaultRange", true))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("QUẢN LÝ SẢN PHẨM")));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testDashboardFilterBarCounterIncrementsWhenFilterDiffersFromDefault() throws Exception {
        // 1. Changing channel to ONLINE increments counter by 1
        mvc.perform(get("/admin/dashboard").param("channel", "ONLINE"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("activeFilterCount", 1));

        // 2. Changing period to today increments counter by 1
        mvc.perform(get("/admin/dashboard").param("period", "today"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("activeFilterCount", 1));

        // 3. Changing period to today AND channel to ONLINE increments counter by 2
        mvc.perform(get("/admin/dashboard").param("period", "today").param("channel", "ONLINE"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("activeFilterCount", 2));
    }
}
