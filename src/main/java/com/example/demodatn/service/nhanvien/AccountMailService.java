package com.example.demodatn.service.nhanvien;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountMailService {
    private final ObjectProvider<JavaMailSender> sender;
    @Value("${spring.mail.host:}") private String host;
    @Value("${spring.mail.port:587}") private int port;
    @Value("${spring.mail.username:}") private String username;
    @Value("${shop.mail.from:}") private String from;
    @Value("${shop.public-base-url:http://localhost:8080}") private String publicBaseUrl;

    public record TestMailResult(boolean success, String message) {}

    public boolean isConfigured() {
        return host != null && !host.isBlank() && sender.getIfAvailable() != null;
    }

    public String getResolvedHost() {
        return host != null ? host.trim() : "";
    }

    public int getResolvedPort() {
        return port > 0 ? port : 587;
    }

    public String getResolvedFrom() {
        if (from != null && !from.isBlank()) return from.trim();
        if (username != null && !username.isBlank()) return username.trim();
        return "";
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logEmailStartupStatus() {
        if (isConfigured()) {
            log.info("Email: ĐÃ cấu hình ({}:{})", getResolvedHost(), getResolvedPort());
        } else {
            log.warn("Email: CHƯA cấu hình, tài khoản mới sẽ không được gửi mail");
        }
    }

    public boolean send(String email, String name, String username, String password) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Không được gọi SMTP trong giao dịch database.");
        }
        String resolvedFrom = getResolvedFrom();
        if (host.isBlank() || resolvedFrom.isBlank() || sender.getIfAvailable() == null) {
            return false;
        }
        try {
            var link = java.net.URI.create(publicBaseUrl);
            if (!java.util.Set.of("http", "https").contains(link.getScheme()) || link.getHost() == null) {
                return false;
            }
            var message = sender.getObject().createMimeMessage();
            var helper = new MimeMessageHelper(message, true, java.nio.charset.StandardCharsets.UTF_8.name());
            helper.setFrom(resolvedFrom);
            helper.setTo(email);
            helper.setSubject("Tài khoản đăng nhập F6S Store");
            String text = "Xin chào " + name + ",\n\nTài khoản của bạn đã sẵn sàng.\nTên đăng nhập: " + username +
                    "\nMật khẩu tạm: " + password + "\nĐăng nhập: " + publicBaseUrl.replaceAll("/+$", "") + "/login" +
                    "\n\nBạn phải đổi mật khẩu ngay sau lần đăng nhập đầu tiên. Không chia sẻ mật khẩu này.\n" +
                    "Nếu đây là thư gửi lại, mật khẩu tạm trước đó đã hết hiệu lực.\n\nTrân trọng,\nF6S Store";
            helper.setText(text, "<!doctype html><html lang=\"vi\"><body><img src=\"cid:f6s-logo\" alt=\"Biểu trưng F6S Store\" width=\"120\" height=\"90\" style=\"object-fit:contain\"><p>" +
                    org.springframework.web.util.HtmlUtils.htmlEscape(text).replace("\n", "<br>") + "</p></body></html>");
            helper.addInline("f6s-logo", new org.springframework.core.io.ClassPathResource("static/images/logo.jpg"), "image/jpeg");
            sender.getObject().send(message);
            return true;
        } catch (Exception ignored) {
            // SMTP diagnostics can contain sensitive data; never log the exception or mail body.
            return false;
        }
    }

    public TestMailResult sendTestMail(String recipientEmail) {
        if (!isConfigured()) {
            return new TestMailResult(false, "Chưa cấu hình thông tin máy chủ email. Vui lòng cấu hình trong application-local.properties hoặc biến môi trường.");
        }
        if (recipientEmail == null || recipientEmail.isBlank()) {
            return new TestMailResult(false, "Tài khoản quản trị chưa có địa chỉ email nhận thư.");
        }
        String resolvedFrom = getResolvedFrom();
        if (resolvedFrom.isBlank()) {
            return new TestMailResult(false, "Chưa cấu hình địa chỉ email gửi (shop.mail.from hoặc spring.mail.username).");
        }
        try {
            JavaMailSender mailSender = sender.getIfAvailable();
            if (mailSender == null) {
                return new TestMailResult(false, "Không thể khởi tạo dịch vụ gửi thư.");
            }
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, java.nio.charset.StandardCharsets.UTF_8.name());
            helper.setFrom(resolvedFrom);
            helper.setTo(recipientEmail);
            helper.setSubject("Thử nghiệm cấu hình email - F6S Store");
            String text = "Xin chào,\n\nĐây là thư thử nghiệm gửi từ hệ thống quản trị F6S Store.\n"
                    + "Cấu hình máy chủ gửi thư (SMTP " + getResolvedHost() + ":" + getResolvedPort() + ") đã hoạt động chính xác!\n\n"
                    + "Thời gian gửi: " + java.time.LocalDateTime.now() + "\nTrân trọng,\nF6S Store";
            helper.setText(text, false);
            mailSender.send(message);
            return new TestMailResult(true, "Gửi email thử nghiệm thành công tới " + recipientEmail + ".");
        } catch (jakarta.mail.AuthenticationFailedException | org.springframework.mail.MailAuthenticationException ex) {
            return new TestMailResult(false, "Xác thực SMTP thất bại: Sai tên đăng nhập hoặc mật khẩu ứng dụng. Nếu dùng Gmail, hãy tạo Mật khẩu ứng dụng (App Password).");
        } catch (jakarta.mail.MessagingException ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "";
            if (msg.contains("Connection refused") || msg.contains("timeout") || msg.contains("timed out") || msg.contains("UnknownHostException")) {
                return new TestMailResult(false, "Không thể kết nối tới máy chủ SMTP (" + getResolvedHost() + ":" + getResolvedPort() + "). Vui lòng kiểm tra host, port hoặc mạng.");
            }
            return new TestMailResult(false, "Lỗi máy chủ thư: " + msg);
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "Lỗi không xác định";
            if (msg.contains("timeout") || msg.contains("Connection refused")) {
                return new TestMailResult(false, "Không thể kết nối tới máy chủ SMTP (" + getResolvedHost() + ":" + getResolvedPort() + "). Vui lòng kiểm tra host, port hoặc mạng.");
            }
            return new TestMailResult(false, "Gửi email thử nghiệm thất bại: " + msg);
        }
    }
}
