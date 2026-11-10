package com.example.demodatn.service;

import com.example.demodatn.config.DynamicJavaMailSender;
import com.example.demodatn.dto.nhanvien.EmailConfigDto;
import com.example.demodatn.service.nhanvien.AccountMailService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
public class EmailConfigurationIntegrationTest {

    @BeforeAll
    static void disablePersist() {
        System.setProperty("email.disable.persist", "true");
    }

    @AfterAll
    static void cleanup() {
        System.clearProperty("email.disable.persist");
    }

    @Autowired private WebApplicationContext context;
    @Autowired private DynamicJavaMailSender dynamicMailSender;
    @Autowired private AccountMailService mailService;
    @Autowired private JdbcTemplate jdbc;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        com.example.demodatn.TestDatabaseGuard.check(jdbc);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void testDynamicSenderConfigurationLifecycle() {
        dynamicMailSender.configure("smtp.test-provider.com", 587, "admin@test.com", "secret-pass", "F6S <admin@test.com>", true, true);

        assertTrue(dynamicMailSender.isConfigured());
        assertEquals("smtp.test-provider.com", dynamicMailSender.getHost());
        assertEquals(587, dynamicMailSender.getPort());
        assertEquals("admin@test.com", dynamicMailSender.getUsername());
        assertEquals("F6S <admin@test.com>", dynamicMailSender.getFrom());
        assertTrue(dynamicMailSender.isPasswordConfigured());

        assertTrue(mailService.isConfigured());
        assertEquals("smtp.test-provider.com", mailService.getResolvedHost());
        assertEquals("F6S <admin@test.com>", mailService.getResolvedFrom());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testAdminCanAccessAccountPageAndEmailFormIsRendered() throws Exception {
        mvc.perform(get("/admin/account"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("emailConfigForm"))
                .andExpect(model().attributeExists("mailConfigured"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Cấu hình &amp; Thử nghiệm Email (SMTP)")));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testAdminCanSaveEmailConfigurationViaPost() throws Exception {
        mvc.perform(post("/admin/account/email-config")
                .with(csrf())
                .param("host", "smtp.custom-domain.com")
                .param("port", "465")
                .param("username", "support@custom-domain.com")
                .param("password", "custom-app-pass")
                .param("from", "Custom Shop <support@custom-domain.com>")
                .param("auth", "true")
                .param("starttls", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/account"))
                .andExpect(flash().attributeExists("success"));

        assertEquals("smtp.custom-domain.com", dynamicMailSender.getHost());
        assertEquals(465, dynamicMailSender.getPort());
        assertEquals("support@custom-domain.com", dynamicMailSender.getUsername());
    }

    @Test
    @WithMockUser(username = "nhanvien1", roles = {"EMPLOYEE"})
    void testEmployeeCannotSaveEmailConfiguration() throws Exception {
        mvc.perform(post("/admin/account/email-config")
                .with(csrf())
                .param("host", "smtp.hacker.com")
                .param("port", "587")
                .param("username", "hack@domain.com")
                .param("password", "hack"))
                .andExpect(status().isForbidden());
    }
}
