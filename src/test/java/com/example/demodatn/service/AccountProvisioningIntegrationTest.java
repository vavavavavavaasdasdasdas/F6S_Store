package com.example.demodatn.service;

import com.example.demodatn.dto.khachhang.CustomerDto;
import com.example.demodatn.dto.nhanvien.EmployeeDto;
import com.example.demodatn.exception.PeopleValidationException;
import com.example.demodatn.repository.khachhang.CustomerRepository;
import com.example.demodatn.repository.nhanvien.*;
import com.example.demodatn.security.UserDetailsServiceImpl;
import com.example.demodatn.service.nhanvien.*;
import com.example.demodatn.util.PasswordPolicy;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class AccountProvisioningIntegrationTest {
    static GreenMail SMTP=new GreenMail(new ServerSetup(0,"127.0.0.1",ServerSetup.PROTOCOL_SMTP));
    static {SMTP.setUser("noreply@example.test","fixture","fixture");SMTP.setUser("sink@example.test","sink","sink");SMTP.start();SMTP.setQuotaSupported(false);}
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.mail.host",()->"127.0.0.1");r.add("spring.mail.port",()->SMTP.getSmtp().getPort());
        r.add("spring.mail.properties.mail.smtp.auth",()->"false");r.add("spring.mail.properties.mail.smtp.starttls.enable",()->"false");
        r.add("shop.mail.from",()->"noreply@example.test");r.add("shop.public-base-url",()->"http://localhost:18140");
    }
    @Autowired AccountProvisioningService accounts; @Autowired CustomerRepository customers;
    @Autowired UserRepository users; @Autowired EmployeeRepository employees;
    @Autowired PasswordEncoder encoder; @Autowired JdbcTemplate jdbc;
    @Autowired UserDetailsServiceImpl details;
    @Autowired WebApplicationContext context;
    MockMvc mvc;
    @BeforeEach void setup() throws Exception {
        assertTrue(jdbc.queryForObject("SELECT DB_NAME()",String.class).startsWith("shopbangiay_kt_"),"Disposable restored test copy required");
        SMTP.purgeEmailFromAllMailboxes();SMTP.setUser("sink@example.test","sink","sink");
        mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    @AfterAll static void close(){SMTP.stop();}
    CustomerDto form() {
        String token=UUID.randomUUID().toString().substring(0,8);var f=new CustomerDto();
        f.setCustomerCode("G4-"+token);f.setFullName("Nguyễn Văn Ánh");f.setEmail(token+"@example.test");
        f.setPhone("09"+String.format("%08d",Math.floorMod(token.hashCode(),100000000)));f.setAddress("Số 12, đường Lê Lợi, Đà Nẵng");
        SMTP.setUser(f.getEmail(),token,token);return f;
    }
    MimeMessage received(String email) {
        assertTrue(SMTP.waitForIncomingEmail(3000,1),"Fake SMTP did not receive a message");
        return java.util.Arrays.stream(SMTP.getReceivedMessages()).filter(m->{try{return m.getAllRecipients()[0].toString().equals(email);}catch(Exception ex){return false;}}).reduce((a,b)->b).orElseThrow();
    }
    String password(MimeMessage message) throws Exception {
        String text=(String)message.getContent();return text.substring(text.indexOf("Mật khẩu tạm: ")+13).split("\\r?\\n",2)[0].trim();
    }
    @Test void vietnameseMailCommittedAccountAndForcedPasswordGate() throws Exception {
        var f=form();var result=accounts.saveCustomer(f);assertTrue(result.sent());
        var c=customers.findById(result.personId()).orElseThrow();var u=users.findById(c.getUser().getId()).orElseThrow();
        var message=received(f.getEmail());String body=(String)message.getContent(),temporary=password(message);
        assertTrue(body.contains("Xin chào Nguyễn Văn Ánh")&&body.contains("http://localhost:18140/login")&&body.contains(u.getUsername()));
        assertTrue(temporary.length()==12&&PasswordPolicy.isValid(temporary));assertTrue(encoder.matches(temporary,u.getPassword()));
        assertTrue(u.getMustChangePassword());assertFalse(u.getCredentialMailFailed());assertEquals("ROLE_USER",u.getRole().getName());
        var principal=details.loadUserByUsername(u.getUsername());
        mvc.perform(get("/profile").with(user(principal))).andExpect(status().isFound()).andExpect(redirectedUrl("/account/change-required"));
        mvc.perform(post("/cart/add").with(user(principal)).with(csrf())).andExpect(status().isSeeOther());
        mvc.perform(post("/account/change-required").with(user(principal))).andExpect(status().isForbidden());
        mvc.perform(get("/account/change-required").with(user(principal))).andExpect(status().isOk());
        mvc.perform(post("/account/change-required").session(new org.springframework.mock.web.MockHttpSession()).with(user(principal)).with(csrf())
            .param("currentPassword",temporary).param("newPassword","ChangedG4!Pass").param("confirmPassword","ChangedG4!Pass"))
            .andExpect(status().isFound()).andExpect(redirectedUrl("/"));
        var updated=users.findById(u.getId()).orElseThrow();assertFalse(updated.getMustChangePassword());assertFalse(encoder.matches(temporary,updated.getPassword()));
        mvc.perform(get("/profile").with(user(details.loadUserByUsername(u.getUsername())))).andExpect(status().isOk());
        String currentHash=updated.getPassword();assertThrows(PeopleValidationException.class,()->accounts.resend("customers",c.getId(),false));
        assertEquals(currentHash,users.findById(u.getId()).orElseThrow().getPassword());
    }
    @Test void smtpFailureKeepsAccountAndResendRotatesPasswordAndEnforcesLimit() throws Exception {
        var f=form();int port=SMTP.getSmtp().getPort();SMTP.stop();
        var result=accounts.saveCustomer(f);assertTrue(result.issued());assertFalse(result.sent());
        var c=customers.findById(result.personId()).orElseThrow();String first=users.findById(c.getUser().getId()).orElseThrow().getPassword();
        SMTP=new GreenMail(new ServerSetup(port,"127.0.0.1",ServerSetup.PROTOCOL_SMTP));SMTP.start();SMTP.setUser(f.getEmail(),"retry","retry");
        assertTrue(accounts.resend("customers",c.getId(),false).sent());
        String temporary=password(received(f.getEmail()));var u=users.findById(c.getUser().getId()).orElseThrow();
        assertTrue(encoder.matches(temporary,u.getPassword()));assertNotEquals(first,u.getPassword());
        accounts.resend("customers",c.getId(),false);accounts.resend("customers",c.getId(),false);
        String last=users.findById(u.getId()).orElseThrow().getPassword();
        assertThrows(PeopleValidationException.class,()->accounts.resend("customers",c.getId(),false));
        assertEquals(last,users.findById(u.getId()).orElseThrow().getPassword());
    }
    @Test void duplicateEmailAndIdentityRollbackWithoutAnotherMail() {
        var f=form();f.setSoCccd("001234567890");var first=accounts.saveCustomer(f);assertTrue(first.sent());
        int count=SMTP.getReceivedMessages().length;long before=users.count();
        var sameEmail=form();sameEmail.setEmail(f.getEmail());assertThrows(PeopleValidationException.class,()->accounts.saveCustomer(sameEmail));
        var sameDocument=form();sameDocument.setSoCccd(f.getSoCccd());assertThrows(PeopleValidationException.class,()->accounts.saveCustomer(sameDocument));
        assertEquals(before,users.count());assertEquals(count,SMTP.getReceivedMessages().length);
    }
    @Test void employeeGeneratedCredentialsAndEditablePosition() throws Exception {
        var seed=form();var f=new EmployeeDto();f.setEmployeeCode(seed.getCustomerCode());f.setFullName(seed.getFullName());f.setEmail(seed.getEmail());f.setPhone(seed.getPhone());
        f.setNewPosition("Chuyên viên kiểm thử");f.setPassword("ForgedG4!Password");
        var result=accounts.saveEmployee(f);assertTrue(result.sent());var e=employees.findById(result.personId()).orElseThrow();
        var u=users.findById(e.getUser().getId()).orElseThrow();String temporary=password(received(f.getEmail()));
        assertTrue(encoder.matches(temporary,u.getPassword()));assertFalse(encoder.matches(f.getPassword(),u.getPassword()));
        assertTrue(u.getMustChangePassword());assertEquals("ROLE_ADMIN",u.getRole().getName());assertEquals("Chuyên viên kiểm thử",e.getPosition());
    }
    @Test void existingCustomerWithoutAccountCanReceiveCredentialsAndDisabledNeedsConfirmation() {
        var f=form();var c=new com.example.demodatn.entity.khachhang.Customer();c.setCustomerCode(f.getCustomerCode());c.setFullName(f.getFullName());c.setPhone(f.getPhone());c.setEmail(f.getEmail());c.setStatus(false);customers.saveAndFlush(c);
        assertThrows(PeopleValidationException.class,()->accounts.resend("customers",c.getId(),false));
        assertTrue(accounts.resend("customers",c.getId(),true).sent());
        var linked=customers.findById(c.getId()).orElseThrow();assertNotNull(linked.getUser());assertFalse(users.findById(linked.getUser().getId()).orElseThrow().getStatus());
    }
    @Test void administratorRoutesAndCsrfCannotBeBypassed() throws Exception {
        for(String route:new String[]{"/admin/customers/create","/admin/employees/create"}) {
            mvc.perform(get(route)).andExpect(status().isFound());
            mvc.perform(get(route).with(user("ordinary").roles("USER"))).andExpect(status().isForbidden());
        }
        for(String route:new String[]{"/admin/customers/save","/admin/employees/save","/admin/customers/1/send-account","/admin/employees/1/send-account","/admin/accounts/username-suggestion"}) {
            mvc.perform(post(route).with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
            mvc.perform(post(route).with(user("admin").roles("ADMIN")).with(csrf().useInvalidToken())).andExpect(status().isForbidden());
            mvc.perform(post(route).with(user("ordinary").roles("USER")).with(csrf())).andExpect(status().isForbidden());
        }
    }
}
