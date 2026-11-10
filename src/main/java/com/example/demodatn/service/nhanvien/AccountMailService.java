package com.example.demodatn.service.nhanvien;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountMailService {
    private final ObjectProvider<JavaMailSender> sender;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.host:}") private String host;
    @Value("${spring.mail.port:587}") private int port;
    @Value("${spring.mail.username:}") private String username;
    @Value("${shop.mail.from:}") private String from;
    @Value("${shop.name:F6 Shop}") private String shopName;
    @Value("${shop.address:}") private String shopAddress;
    @Value("${shop.hotline:}") private String shopHotline;
    @Value("${shop.support-email:}") private String shopSupportEmail;
    @Value("${shop.public-url:${shop.public-base-url:http://localhost:8080}}") private String publicUrl;

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

    public String getMaskedUsername() {
        if (username == null || username.isBlank()) return "";
        String u = username.trim();
        int at = u.indexOf('@');
        if (at > 0) {
            String name = u.substring(0, at);
            String domain = u.substring(at);
            if (name.length() <= 2) {
                return name.charAt(0) + "***" + domain;
            } else {
                return name.substring(0, 3) + "***" + domain;
            }
        }
        if (u.length() <= 3) return "***";
        return u.substring(0, 3) + "***";
    }

    public String getStatusText() {
        if (isConfigured()) {
            String masked = getMaskedUsername();
            String accountPart = masked.isBlank() ? "" : ", " + masked;
            return "Email: Đã cấu hình (" + getResolvedHost() + ":" + getResolvedPort() + accountPart + ")";
        }
        return "Email: Chưa cấu hình";
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logEmailStartupStatus() {
        if (isConfigured()) {
            log.info("{}", getStatusText());
        } else {
            log.warn("Email: CHƯA cấu hình, tài khoản mới sẽ không được gửi mail");
        }
    }

    public boolean send(String email, String name, String username, String password) {
        return sendAccountMail(email, name, username, password, false, null, false);
    }

    public boolean sendAccountMail(String email, String name, String username, String password, boolean isEmployee, String position) {
        return sendAccountMail(email, name, username, password, isEmployee, position, false);
    }

    public boolean sendAccountMail(String email, String name, String username, String password, boolean isEmployee, String position, boolean isResend) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Không được gọi SMTP trong giao dịch database.");
        }
        String resolvedFrom = getResolvedFrom();
        if (host.isBlank() || resolvedFrom.isBlank() || sender.getIfAvailable() == null) {
            return false;
        }
        try {
            var link = java.net.URI.create(publicUrl);
            if (!java.util.Set.of("http", "https").contains(link.getScheme()) || link.getHost() == null) {
                return false;
            }

            String currentShopName = (shopName != null && !shopName.isBlank()) ? shopName.trim() : "F6 Shop";
            String subject = "[" + currentShopName + "] Tài khoản của bạn đã sẵn sàng";
            String loginUrl = publicUrl.replaceAll("/+$", "") + "/login";

            Context ctx = new Context(Locale.forLanguageTag("vi"));
            ctx.setVariable("emailSubject", subject);
            ctx.setVariable("preheader", "Tài khoản của bạn tại " + currentShopName + " đã sẵn sàng.");
            ctx.setVariable("fullName", name != null ? name : "Quý khách");
            ctx.setVariable("isEmployee", isEmployee);
            ctx.setVariable("position", position);
            ctx.setVariable("username", username);
            ctx.setVariable("password", password);
            ctx.setVariable("isResend", isResend);
            ctx.setVariable("loginUrl", loginUrl);
            ctx.setVariable("shopName", currentShopName);
            ctx.setVariable("shopAddress", shopAddress);
            ctx.setVariable("shopHotline", shopHotline);
            ctx.setVariable("shopSupportEmail", shopSupportEmail);
            ctx.setVariable("publicUrl", publicUrl);

            String html = templateEngine.process("email/tai-khoan", ctx);

            // Plain text tương ứng
            String introText = isEmployee
                    ? "Bạn được thêm vào hệ thống quản trị của " + currentShopName + " với chức vụ " + (position != null && !position.isBlank() ? position : "Nhân viên") + "."
                    : "Cửa hàng đã tạo tài khoản mua hàng cho bạn tại hệ thống " + currentShopName + ".";
            String resendNotice = isResend ? "\nLưu ý: Mật khẩu mới này thay thế cho mật khẩu cũ trước đó. Mật khẩu tạm trước đây đã hết hiệu lực.\n" : "";

            String text = "Xin chào " + (name != null ? name : "Quý khách") + ",\n\n"
                    + introText + "\n"
                    + resendNotice + "\n"
                    + "Thông tin đăng nhập:\n"
                    + "- Tên tài khoản: " + username + "\n"
                    + "- Mật khẩu: " + password + "\n"
                    + "- Đăng nhập: " + loginUrl + "\n\n"
                    + "Lưu ý bảo mật:\n"
                    + "- Vui lòng đổi mật khẩu ngay sau lần đăng nhập đầu tiên.\n"
                    + "- Tuyệt đối không chia sẻ mật khẩu này với bất kỳ ai khác.\n"
                    + "- Nếu bạn không yêu cầu tạo tài khoản tại " + currentShopName + ", xin vui lòng bỏ qua thư này.\n\n"
                    + "Trân trọng,\n" + currentShopName;

            MimeMessage message = sender.getObject().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(resolvedFrom);
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(text, html);

            try {
                ClassPathResource logoResource = new ClassPathResource("static/images/logo.jpg");
                if (logoResource.exists()) {
                    helper.addInline("f6s-logo", logoResource, "image/jpeg");
                }
            } catch (Exception ex) {
                log.warn("Không thể đính kèm logo email: {}", ex.getMessage());
            }

            sender.getObject().send(message);
            log.info("Đã gửi email thông tin tài khoản thành công tới {}", email);
            return true;
        } catch (Exception ex) {
            log.error("Không thể gửi email thông tin tài khoản tới {}: {}", email, ex.getMessage());
            return false;
        }
    }

    public boolean sendSupportNotice(String email, String name, String excerpt) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Không gọi SMTP trong transaction.");
        }
        if (!isConfigured() || getResolvedFrom().isBlank()) return false;
        try {
            var uri = java.net.URI.create(publicUrl);
            if (!java.util.Set.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null) return false;

            String currentShopName = (shopName != null && !shopName.isBlank()) ? shopName.trim() : "F6 Shop";
            var message = sender.getObject().createMimeMessage();
            var helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(getResolvedFrom());
            helper.setTo(email);
            helper.setSubject("[" + currentShopName + "] Cửa hàng đã trả lời tin nhắn của bạn");

            String link = publicUrl.replaceAll("/+$", "") + "/?support=open";
            String text = "Xin chào " + name + ",\n\nNhân viên cửa hàng vừa trả lời bạn:\n" + excerpt + "\n\nXem cuộc trò chuyện: " + link + "\n\nTrân trọng,\n" + currentShopName;
            helper.setText(text, "<!doctype html><html lang=\"vi\"><body><p>" + org.springframework.web.util.HtmlUtils.htmlEscape(text).replace("\n", "<br>") + "</p></body></html>");
            sender.getObject().send(message);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public TestMailResult sendTestMail(String recipientEmail) {
        if (!isConfigured()) {
            return new TestMailResult(false, "Chưa cấu hình thông tin máy chủ email. Vui lòng cấu hình trong application-local.properties hoặc biến môi trường.");
        }
        if (recipientEmail == null || recipientEmail.isBlank()) {
            return new TestMailResult(false, "Vui lòng nhập địa chỉ email nhận thư thử nghiệm.");
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
            String currentShopName = (shopName != null && !shopName.isBlank()) ? shopName.trim() : "F6 Shop";
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(resolvedFrom);
            helper.setTo(recipientEmail.trim());
            helper.setSubject("[" + currentShopName + "] Thử nghiệm cấu hình email");

            String text = "Xin chào,\n\nĐây là thư thử nghiệm gửi từ hệ thống " + currentShopName + ".\n"
                    + "Cấu hình máy chủ gửi thư (SMTP " + getResolvedHost() + ":" + getResolvedPort() + ") đã hoạt động chính xác!\n\n"
                    + "Thời gian gửi: " + java.time.LocalDateTime.now() + "\nTrân trọng,\n" + currentShopName;

            String html = "<!doctype html><html lang=\"vi\"><head><meta charset=\"UTF-8\"></head><body style=\"font-family: Arial, sans-serif; padding: 20px; color: #272d2d;\">"
                    + "<h2>" + org.springframework.web.util.HtmlUtils.htmlEscape(currentShopName) + " - Kiểm tra cấu hình Email</h2>"
                    + "<p>Xin chào,</p>"
                    + "<p>Thư này xác nhận cấu hình gửi thư SMTP (<strong>" + getResolvedHost() + ":" + getResolvedPort() + "</strong>) của bạn đã hoạt động tốt!</p>"
                    + "<p style=\"color: #6c757d; font-size: 13px;\">Thời gian gửi: " + java.time.LocalDateTime.now() + "</p>"
                    + "<hr style=\"border: none; border-top: 1px solid #dee2e6;\">"
                    + "<p style=\"color: #6c757d; font-size: 12px;\">Đây là thư thử nghiệm tự động. Không cần phản hồi thư này.</p>"
                    + "</body></html>";

            helper.setText(text, html);
            mailSender.send(message);
            return new TestMailResult(true, "Gửi email thử nghiệm thành công tới " + recipientEmail.trim() + ".");
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
