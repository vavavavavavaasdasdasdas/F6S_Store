package com.example.demodatn.email;

import com.example.demodatn.TestDatabaseGuard;
import com.example.demodatn.dto.khachhang.CustomerDto;
import com.example.demodatn.dto.nhanvien.EmployeeDto;
import com.example.demodatn.entity.khachhang.Customer;
import com.example.demodatn.entity.nhanvien.Employee;
import com.example.demodatn.entity.nhanvien.User;
import com.example.demodatn.repository.donhang.ProvinceRepository;
import com.example.demodatn.repository.donhang.WardRepository;
import com.example.demodatn.repository.khachhang.CustomerRepository;
import com.example.demodatn.repository.nhanvien.EmployeeRepository;
import com.example.demodatn.repository.nhanvien.UserRepository;
import com.example.demodatn.service.nhanvien.AccountProvisioningService;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@WithMockUser(username = "admin", roles = "ADMIN")
public class AccountEmailIntegrationTest {

    static GreenMail smtp = new GreenMail(new ServerSetup(0, "127.0.0.1", ServerSetup.PROTOCOL_SMTP));
    static {
        smtp.start();
    }

    @DynamicPropertySource
    static void configureMail(DynamicPropertyRegistry r) {
        r.add("spring.mail.host", () -> "127.0.0.1");
        r.add("spring.mail.port", () -> smtp.getSmtp().getPort());
        r.add("spring.mail.username", () -> "");
        r.add("spring.mail.password", () -> "");
        r.add("spring.mail.properties.mail.smtp.auth", () -> false);
        r.add("spring.mail.properties.mail.smtp.starttls.enable", () -> false);
        r.add("spring.mail.properties.mail.smtp.starttls.required", () -> false);
        r.add("spring.mail.properties.mail.smtp.connectiontimeout", () -> 2000);
        r.add("spring.mail.properties.mail.smtp.timeout", () -> 2000);
        r.add("shop.mail.from", () -> "f6shop-test@example.com");
    }

    @Autowired private AccountProvisioningService provisioningService;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProvinceRepository provinceRepository;
    @Autowired private WardRepository wardRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbc;

    private final List<Integer> createdCustomerIds = new ArrayList<>();
    private final List<Integer> createdEmployeeIds = new ArrayList<>();
    private final List<Integer> createdUserIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        TestDatabaseGuard.check(jdbc);
    }

    @AfterEach
    void tearDown() {
        // DỌN SẠCH DỮ LIỆU KIỂM THỬ ĐÃ TẠO
        for (Integer cid : createdCustomerIds) {
            try {
                jdbc.update("DELETE FROM customers WHERE id = ?", cid);
            } catch (Exception ignored) {}
        }
        for (Integer eid : createdEmployeeIds) {
            try {
                jdbc.update("DELETE FROM employees WHERE id = ?", eid);
            } catch (Exception ignored) {}
        }
        for (Integer uid : createdUserIds) {
            try {
                jdbc.update("DELETE FROM users WHERE id = ?", uid);
            } catch (Exception ignored) {}
        }
        createdCustomerIds.clear();
        createdEmployeeIds.clear();
        createdUserIds.clear();
    }

    @AfterAll
    static void stopSmtp() {
        smtp.stop();
    }

    @Test
    void testSaveCustomerSendsAccountMailWithValidPasswordAndForcedChange() throws Exception {
        int before = smtp.getReceivedMessages().length;

        String testSuffix = UUID.randomUUID().toString().substring(0, 8);
        String testEmail = "test_cust_" + testSuffix + "@example.com";
        String testUsername = "cust_" + testSuffix;

        CustomerDto dto = new CustomerDto();
        dto.setFullName("Nguyễn Văn <script>alert('xss')</script>");
        dto.setEmail(testEmail);
        dto.setPhone("098" + (int)(Math.random() * 9000000 + 1000000));
        dto.setUsername(testUsername);

        var prov = provinceRepository.findAll().get(0);
        var w = wardRepository.findAll().stream()
                .filter(item -> item.getProvince().getId().equals(prov.getId()))
                .findFirst().orElseGet(() -> wardRepository.findAll().get(0));
        dto.setProvinceId(prov.getId());
        dto.setWardId(w.getId());
        dto.setStreetAddress("Số 123 Đường Test Customer");

        var outcome = provisioningService.saveCustomer(dto);
        assertTrue(outcome.issued(), "Tài khoản phải được cấp phát");
        assertTrue(outcome.sent(), "Email phải gửi thành công");
        createdCustomerIds.add(outcome.personId());

        Customer customer = customerRepository.findById(outcome.personId()).orElseThrow();
        User user = customer.getUser();
        assertNotNull(user);
        createdUserIds.add(user.getId());

        // Chờ GreenMail nhận thư
        assertTrue(smtp.waitForIncomingEmail(6000, before + 1), "GreenMail phải nhận được email tạo tài khoản");
        MimeMessage message = smtp.getReceivedMessages()[before];

        // 1. Kiểm tra tiêu đề có dấu đúng chuẩn
        assertEquals("[F6 Shop] Tài khoản của bạn đã sẵn sàng", message.getSubject());

        // 2. Kiểm tra người nhận
        assertEquals(testEmail, message.getAllRecipients()[0].toString());

        // 3. Nội dung email
        String htmlContent = getPart(message, "text/html");
        String textContent = getPart(message, "text/plain");

        // Escape HTML trong HTML content: Họ tên chứa <script> phải được escape thành &lt;script&gt;
        assertTrue(htmlContent.contains("&lt;script&gt;"), "Thẻ <script> trong HTML phải được escape");
        assertFalse(htmlContent.contains("<script>alert"), "Nội dung HTML không được chứa thẻ script thô");

        // Tên tài khoản đúng giá trị
        assertTrue(htmlContent.contains(testUsername), "Thư phải chứa đúng username: " + testUsername);
        assertTrue(textContent.contains(testUsername), "Plain text phải chứa đúng username: " + testUsername);

        // Mật khẩu 8 ký tự, có chữ và số, bỏ 0 O 1 l I
        Pattern passPattern = Pattern.compile("Mật khẩu:\\s*([A-Za-z0-9]{8})");
        Matcher matcher = passPattern.matcher(textContent);
        String extractedPassword = null;
        if (matcher.find()) {
            extractedPassword = matcher.group(1);
        } else {
            Pattern htmlPassPattern = Pattern.compile(">\\s*([A-Za-z0-9]{8})\\s*</span>");
            Matcher htmlMatcher = htmlPassPattern.matcher(htmlContent);
            if (htmlMatcher.find()) {
                extractedPassword = htmlMatcher.group(1);
            }
        }
        assertNotNull(extractedPassword, "Phải tìm thấy mật khẩu 8 ký tự trong thư");
        assertEquals(8, extractedPassword.length());
        assertTrue(extractedPassword.matches(".*[A-Za-z].*") && extractedPassword.matches(".*[0-9].*"), "Mật khẩu phải có cả chữ và số");
        assertFalse(extractedPassword.matches(".*[0O1lI].*"), "Mật khẩu phải loại bỏ các ký tự dễ nhầm 0, O, 1, l, I");

        // 4. Kiểm tra đăng nhập được bằng mật khẩu trong thư
        User reloadedUser = userRepository.findById(user.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches(extractedPassword, reloadedUser.getPassword()), "Mật khẩu trong thư phải khớp hash trong DB");
        assertTrue(reloadedUser.getMustChangePassword(), "Người dùng phải bị ép đổi mật khẩu ở lần đăng nhập đầu");
        assertFalse(reloadedUser.getCredentialMailFailed(), "Cờ credentialMailFailed phải là false khi gửi thành công");

        // 5. Test Gửi lại tài khoản (Resend)
        String oldHash = reloadedUser.getPassword();
        var resendOutcome = provisioningService.resend("customers", customer.getId(), true);
        assertTrue(resendOutcome.sent());

        assertTrue(smtp.waitForIncomingEmail(6000, before + 2), "GreenMail phải nhận được email gửi lại tài khoản");
        MimeMessage resendMessage = smtp.getReceivedMessages()[before + 1];
        String resendHtml = getPart(resendMessage, "text/html");
        String resendText = getPart(resendMessage, "text/plain");

        assertTrue(resendHtml.contains("mật khẩu mới này thay thế cho mật khẩu cũ")
                || resendText.contains("mật khẩu mới này thay thế cho mật khẩu cũ")
                || resendHtml.contains("Mật khẩu mới này thay thế cho mật khẩu cũ")
                || resendText.contains("Mật khẩu mới này thay thế cho mật khẩu cũ"));

        User userAfterResend = userRepository.findById(user.getId()).orElseThrow();
        assertNotEquals(oldHash, userAfterResend.getPassword(), "Mật khẩu cũ phải mất hiệu lực");
        assertFalse(userAfterResend.getCredentialMailFailed(), "Cờ credentialMailFailed phải là false sau khi gửi lại thành công");
    }

    @Test
    void testSaveEmployeeSendsAccountMailWithPosition() throws Exception {
        int before = smtp.getReceivedMessages().length;

        String testSuffix = UUID.randomUUID().toString().substring(0, 8);
        String testEmail = "test_emp_" + testSuffix + "@example.com";
        String testUsername = "emp_" + testSuffix;

        EmployeeDto dto = new EmployeeDto();
        dto.setFullName("Trần Nhân Viên Test");
        dto.setEmail(testEmail);
        dto.setPhone("097" + (int)(Math.random() * 9000000 + 1000000));
        dto.setUsername(testUsername);
        dto.setPosition("Nhân viên");

        var outcome = provisioningService.saveEmployee(dto);
        assertTrue(outcome.issued());
        assertTrue(outcome.sent());
        createdEmployeeIds.add(outcome.personId());

        Employee employee = employeeRepository.findById(outcome.personId()).orElseThrow();
        createdUserIds.add(employee.getUser().getId());

        assertTrue(smtp.waitForIncomingEmail(6000, before + 1));
        MimeMessage message = smtp.getReceivedMessages()[before];
        String htmlContent = getPart(message, "text/html");
        String textContent = getPart(message, "text/plain");

        assertTrue(htmlContent.contains("Nhân viên") || textContent.contains("Nhân viên") || htmlContent.contains("hệ thống quản trị"));
        assertTrue(htmlContent.contains(testUsername) || textContent.contains(testUsername));
    }

    private String getPart(Part part, String mimeType) throws Exception {
        if (part.isMimeType(mimeType) && part.getContent() instanceof String s) {
            return s;
        }
        if (part.getContent() instanceof Multipart mp) {
            for (int i = 0; i < mp.getCount(); i++) {
                BodyPart bp = mp.getBodyPart(i);
                String result = getPart(bp, mimeType);
                if (result != null && !result.isBlank()) {
                    return result;
                }
            }
        }
        return "";
    }
}
