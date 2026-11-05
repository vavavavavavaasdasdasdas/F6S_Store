package com.example.demodatn.controller;

import com.example.demodatn.dto.nhanvien.RegisterRequest;
import com.example.demodatn.service.nhanvien.AuthService;
import com.example.demodatn.util.PasswordPolicy;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @Transactional
class PasswordPolicyIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired AuthService auth;
    @Autowired jakarta.persistence.EntityManager entityManager;
    MockMvc mvc;
    String owner;

    @BeforeEach void fixture() {
        com.example.demodatn.TestDatabaseGuard.check(jdbc);mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner="qa-pass-"+UUID.randomUUID().toString().substring(0,8);
        int userId=jdbc.queryForObject("INSERT users(username,password,role_id,status,full_name) OUTPUT INSERTED.id VALUES(?,?,(SELECT id FROM roles WHERE name='ROLE_USER'),1,N'Khách mật khẩu')",Integer.class,owner,encoder.encode("Current123!"));
        jdbc.update("INSERT customers(customer_code,full_name,phone,email,user_id,status) VALUES(?,N'Khách mật khẩu',?,?,?,1)",
                owner,"09"+String.format("%08d",java.util.concurrent.ThreadLocalRandom.current().nextInt(100000000)),owner+"@example.test",userId);
    }

    @Test void registrationRejectsWeakPasswordAtFormAndServiceBoundaries() throws Exception {
        mvc.perform(post("/register").with(csrf()).param("username",owner+"new")
                .param("fullName","Khách mới").param("email",owner+"new@example.test").param("phone","0788000987")
                .param("password","123").param("confirmPassword","123"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("registerRequest","password"));
        RegisterRequest form=new RegisterRequest();form.setPassword("123");
        assertEquals(PasswordPolicy.MESSAGE,assertThrows(IllegalArgumentException.class,()->auth.register(form)).getMessage());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE username=?",Integer.class,owner+"new"));
    }

    @Test void userPasswordChangeRejectsWeakValueAndAcceptsApprovedValue() throws Exception {
        mvc.perform(post("/profile/change-password").with(user(owner)).with(csrf())
                .param("oldPassword","Current123!").param("newPassword","abcdefgh").param("confirmPassword","abcdefgh"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("error",PasswordPolicy.MESSAGE));
        assertTrue(encoder.matches("Current123!",storedPassword()));
        mvc.perform(post("/profile/change-password").with(user(owner)).with(csrf())
                .param("oldPassword","Current123!").param("newPassword","Changed123!").param("confirmPassword","Changed123!"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("message"));
        assertTrue(encoder.matches("Changed123!",storedPassword()));
    }

    @Test void adminAndEmployeeFormsUseTheSamePolicy() throws Exception {
        mvc.perform(post("/admin/account/password").with(user(owner).roles("ADMIN")).with(csrf())
                .param("currentPassword","Current123!").param("newPassword","abcdefgh").param("confirmPassword","abcdefgh"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("passwordForm","newPassword"));
        assertTrue(encoder.matches("Current123!",storedPassword()));
        mvc.perform(post("/admin/employees/save").with(user(owner).roles("ADMIN")).with(csrf())
                .param("employeeCode","QA-PASS").param("fullName","Nhân viên kiểm thử").param("phone","0788000986")
                .param("email",owner+"staff@example.test").param("username",owner+"staff").param("password","12345678"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("employee","password"));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE username=?",Integer.class,owner+"staff"));
    }

    private String storedPassword() {
        entityManager.flush();
        return jdbc.queryForObject("SELECT password FROM users WHERE username=?",String.class,owner);
    }
}
