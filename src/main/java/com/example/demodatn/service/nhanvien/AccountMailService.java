package com.example.demodatn.service.nhanvien;

import com.example.demodatn.config.DynamicJavaMailSender;
import com.example.demodatn.dto.nhanvien.EmailConfigDto;
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
    private final ObjectProvider<DynamicJavaMailSender> dynamicSenderProvider;

    @Value("${spring.mail.host:}") private String host;
    @Value("${spring.mail.port:587}") private int port;
    @Value("${spring.mail.username:}") private String username;
    @Value("${shop.mail.from:}") private String from;
    @Value("${shop.public-base-url:http://localhost:8080}") private String publicBaseUrl;

    private volatile String localLastError;

    public record TestMailResult(boolean success, String message) {}

    private DynamicJavaMailSender getDynamicSender() {
        return dynamicSenderProvider.getIfAvailable();
    }

    private JavaMailSender getMailSender() {
        DynamicJavaMailSender dynamic = getDynamicSender();
        if (dynamic != null) {
            return dynamic;
        }
        return sender.getIfAvailable();
    }

    public boolean isConfigured() {
        DynamicJavaMailSender dynamic = getDynamicSender();
        if (dynamic != null) {
            return dynamic.isConfigured();
        }
        return host != null && !host.isBlank() && sender.getIfAvailable() != null;
    }

    public String getResolvedHost() {
        DynamicJavaMailSender dynamic = getDynamicSender();
        if (dynamic != null && dynamic.isConfigured()) {
            return dynamic.getHost();
        }
        return host != null ? host.trim() : "";
    }

    public int getResolvedPort() {
        DynamicJavaMailSender dynamic = getDynamicSender();
        if (dynamic != null && dynamic.isConfigured()) {
            return dynamic.getPort();
        }
        return port > 0 ? port : 587;
    }

    public String getResolvedUsername() {
        DynamicJavaMailSender dynamic = getDynamicSender();
        if (dynamic != null && dynamic.isConfigured()) {
            return dynamic.getUsername();
        }
        return username != null ? username.trim() : "";
    }

    public String getResolvedFrom() {
        DynamicJavaMailSender dynamic = getDynamicSender();
        if (dynamic != null && dynamic.isConfigured()) {
            return dynamic.getFrom();
        }
        if (from != null && !from.isBlank()) return from.trim();
        if (username != null && !username.isBlank()) return username.trim();
        return "";
    }

    public boolean isResolvedAuth() {
        DynamicJavaMailSender dynamic = getDynamicSender();
        if (dynamic != null && dynamic.isConfigured()) {
            return dynamic.isAuth();
        }
        return true;
    }

    public boolean isResolvedStarttls() {
        DynamicJavaMailSender dynamic = getDynamicSender();
        if (dynamic != null && dynamic.isConfigured()) {
            return dynamic.isStarttls();
        }
        return true;
    }

    public boolean isPasswordConfigured() {
        DynamicJavaMailSender dynamic = getDynamicSender();
        return dynamic != null && dynamic.isPasswordConfigured();
    }

    public String getLastError() {
        DynamicJavaMailSender dynamic = getDynamicSender();
        if (dynamic != null && dynamic.getLastError() != null) {
            return dynamic.getLastError();
        }
        return localLastError;
    }

    private void recordError(String error) {
        this.localLastError = error;
        DynamicJavaMailSender dynamic = getDynamicSender();
        if (dynamic != null) {
            dynamic.setLastError(error);
        }
    }

    public synchronized TestMailResult updateConfig(EmailConfigDto dto) {
        if (dto == null) {
            return new TestMailResult(false, "Dữ liệu cấu hình không hợp lệ.");
        }
        DynamicJavaMailSender dynamic = getDynamicSender();
        if (dynamic == null) {
            return new TestMailResult(false, "Không tìm thấy bean DynamicJavaMailSender.");
        }

        String h = dto.getHost() != null ? dto.getHost().trim() : "";
        int p = dto.getPort() != null && dto.getPort() > 0 ? dto.getPort() : 587;
        String u = dto.getUsername() != null ? dto.getUsername().trim() : "";
        String pwd = dto.getPassword();
        // Giữ nguyên mật khẩu cũ nếu ô nhập để trống và đã có mật khẩu trước đó
        if ((pwd == null || pwd.isBlank()) && dynamic.isPasswordConfigured()) {
            pwd = dynamic.getPassword();
        }
        String f = (dto.getFrom() != null && !dto.getFrom().isBlank()) ? dto.getFrom().trim() : u;
        boolean a = dto.getAuth() == null || dto.getAuth();
        boolean s = dto.getStarttls() == null || dto.getStarttls();

        dynamic.configure(h, p, u, pwd, f, a, s);
        dynamic.persistConfiguration();

        return new TestMailResult(true, "Đã lưu cấu hình máy chủ SMTP thành công (" + h + ":" + p + ").");
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
        return sendAccountMail(email, name, username, password, false, null);
    }

    public boolean sendAccountMail(String email, String name, String username, String password, boolean isEmployee, String position) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Không được gọi SMTP trong giao dịch database.");
        }
        String resolvedFrom = getResolvedFrom();
        JavaMailSender mailSender = getMailSender();
        if (!isConfigured() || resolvedFrom.isBlank() || mailSender == null) {
            recordError("Chưa cấu hình máy chủ email SMTP hoặc chưa thiết lập email gửi (from).");
            return false;
        }
        try {
            var link = java.net.URI.create(publicBaseUrl);
            if (!java.util.Set.of("http", "https").contains(link.getScheme()) || link.getHost() == null) {
                recordError("publicBaseUrl không hợp lệ (" + publicBaseUrl + ").");
                return false;
            }
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, java.nio.charset.StandardCharsets.UTF_8.name());
            helper.setFrom(resolvedFrom);
            helper.setTo(email);

            String loginUrl = publicBaseUrl.replaceAll("/+$", "") + "/login";
            String subject;
            String headerTitle;
            String introText;
            String noteText;

            if (isEmployee) {
                String posText = (position != null && !position.isBlank()) ? position.trim() : "Nhân viên";
                subject = "[F6S Store] Thông tin tài khoản nhân viên - " + posText;
                headerTitle = "Tài khoản nhân sự F6S Store";
                introText = "Tài khoản nội bộ của bạn tại hệ thống F6S Store đã được quản trị viên khởi tạo thành công.";
                noteText = "Đây là tài khoản nội bộ có quyền truy cập hệ thống quản trị và bán hàng. Bạn bắt buộc phải đổi mật khẩu trong lần đăng nhập đầu tiên và tuyệt đối không chia sẻ mật khẩu này với người khác.";
            } else {
                subject = "[F6S Store] Chào mừng quý khách - Thông tin tài khoản mua sắm";
                headerTitle = "Chào mừng quý khách đến với F6S Store";
                introText = "Tài khoản thành viên của quý khách tại F6S Store đã sẵn sàng để theo dõi đơn hàng và nhận các chương trình ưu đãi.";
                noteText = "Quý khách vui lòng đổi mật khẩu sau lần đăng nhập đầu tiên để đảm bảo tính an toàn cho tài khoản.";
            }

            // Plain text
            String text = "Xin chào " + name + ",\n\n" + introText + "\n" +
                    (isEmployee && position != null && !position.isBlank() ? ("Chức vụ: " + position + "\n") : "") +
                    "Tên đăng nhập: " + username + "\n" +
                    "Mật khẩu tạm: " + password + "\n" +
                    "Đăng nhập: " + loginUrl + "\n\n" +
                    noteText + "\n" +
                    "Nếu đây là thư gửi lại, mật khẩu tạm trước đó đã hết hiệu lực.\n\n" +
                    "Trân trọng,\nF6S Store";

            // HTML email responsive
            String html = "<!doctype html><html lang=\"vi\"><head><meta charset=\"UTF-8\"></head><body style=\"font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px; color: #1e293b;\">" +
                    "<div style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; border: 1px solid #e2e8f0; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.05);\">" +
                    "  <div style=\"background-color: #0f172a; padding: 24px; text-align: center; color: #ffffff;\">" +
                    "    <img src=\"cid:f6s-logo\" alt=\"F6S Store\" width=\"120\" height=\"90\" style=\"object-fit: contain; margin-bottom: 8px;\">" +
                    "    <h2 style=\"margin: 0; font-size: 20px; font-weight: 700; color: #ffffff;\">" + org.springframework.web.util.HtmlUtils.htmlEscape(headerTitle) + "</h2>" +
                    "  </div>" +
                    "  <div style=\"padding: 32px 24px;\">" +
                    "    <p style=\"font-size: 16px; margin-top: 0; margin-bottom: 16px;\">Xin chào <strong>" + org.springframework.web.util.HtmlUtils.htmlEscape(name) + "</strong>,</p>" +
                    "    <p style=\"font-size: 15px; line-height: 1.6; margin-bottom: 24px; color: #475569;\">" + org.springframework.web.util.HtmlUtils.htmlEscape(introText) + "</p>" +
                    "    <div style=\"background-color: #f1f5f9; border-radius: 6px; padding: 20px; margin-bottom: 24px; border: 1px solid #e2e8f0;\">" +
                    "      <table style=\"width: 100%; border-collapse: collapse; font-size: 14px;\">" +
                    (isEmployee && position != null && !position.isBlank() ?
                    "        <tr><td style=\"padding: 6px 0; color: #64748b; width: 140px;\">Chức vụ:</td><td style=\"padding: 6px 0; font-weight: 600;\">" + org.springframework.web.util.HtmlUtils.htmlEscape(position) + "</td></tr>" : "") +
                    "        <tr><td style=\"padding: 6px 0; color: #64748b; width: 140px;\">Tên đăng nhập:</td><td style=\"padding: 6px 0; font-weight: 700; color: #0f172a; font-family: monospace; font-size: 15px;\">" + org.springframework.web.util.HtmlUtils.htmlEscape(username) + "</td></tr>" +
                    "        <tr><td style=\"padding: 6px 0; color: #64748b; width: 140px;\">Mật khẩu tạm:</td><td style=\"padding: 6px 0; font-weight: 700; color: #0284c7; font-family: monospace; font-size: 16px; letter-spacing: 1px;\">" + org.springframework.web.util.HtmlUtils.htmlEscape(password) + "</td></tr>" +
                    "        <tr><td style=\"padding: 6px 0; color: #64748b; width: 140px;\">Đăng nhập:</td><td style=\"padding: 6px 0;\"><a href=\"" + loginUrl + "\" style=\"color: #2563eb; text-decoration: underline;\">" + loginUrl + "</a></td></tr>" +
                    "      </table>" +
                    "    </div>" +
                    "    <div style=\"text-align: center; margin-bottom: 24px;\">" +
                    "      <a href=\"" + loginUrl + "\" style=\"display: inline-block; background-color: #0f172a; color: #ffffff; text-decoration: none; padding: 12px 28px; font-weight: 600; font-size: 15px; border-radius: 6px;\">Đăng nhập ngay</a>" +
                    "    </div>" +
                    "    <p style=\"font-size: 13px; line-height: 1.5; color: #64748b; margin-bottom: 8px;\">⚠️ <em>" + org.springframework.web.util.HtmlUtils.htmlEscape(noteText) + "</em></p>" +
                    "    <p style=\"font-size: 13px; line-height: 1.5; color: #94a3b8; margin-top: 0;\">Nếu đây là thư gửi lại, mật khẩu tạm trước đó đã hết hiệu lực.</p>" +
                    "  </div>" +
                    "  <div style=\"background-color: #f8fafc; border-top: 1px solid #e2e8f0; padding: 16px 24px; text-align: center; font-size: 12px; color: #94a3b8;\">" +
                    "    © F6S Store — Hệ thống phân phối giày thời trang & thể thao chính hãng" +
                    "  </div>" +
                    "</div>" +
                    "</body></html>";

            helper.setSubject(subject);
            helper.setText(text, html);

            try {
                var logoResource = new org.springframework.core.io.ClassPathResource("static/images/logo.jpg");
                if (logoResource.exists()) {
                    helper.addInline("f6s-logo", logoResource, "image/jpeg");
                }
            } catch (Exception ex) {
                log.warn("Không thể đính kèm logo email: {}", ex.getMessage());
            }

            mailSender.send(message);
            log.info("Đã gửi email thông tin tài khoản thành công tới {}", email);
            recordError(null);
            return true;
        } catch (jakarta.mail.AuthenticationFailedException | org.springframework.mail.MailAuthenticationException ex) {
            String err = "Xác thực SMTP thất bại: Sai tài khoản hoặc mật khẩu ứng dụng Gmail (cần App Password 16 chữ số).";
            recordError(err);
            log.error("Lỗi xác thực SMTP khi gửi tài khoản tới {}: {}", email, ex.getMessage());
            return false;
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            String err;
            if (msg.contains("Connection refused") || msg.contains("timeout") || msg.contains("timed out") || msg.contains("UnknownHostException")) {
                err = "Không thể kết nối máy chủ SMTP (" + getResolvedHost() + ":" + getResolvedPort() + "). Vui lòng kiểm tra host, port hoặc mạng.";
            } else {
                err = "Lỗi máy chủ gửi thư: " + msg;
            }
            recordError(err);
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
            var uri = java.net.URI.create(publicBaseUrl);
            if (!java.util.Set.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null) return false;
            JavaMailSender mailSender = getMailSender();
            if (mailSender == null) return false;
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, java.nio.charset.StandardCharsets.UTF_8.name());
            helper.setFrom(getResolvedFrom());
            helper.setTo(email);
            helper.setSubject("Cửa hàng đã trả lời tin nhắn của bạn - F6S Store");
            String link = publicBaseUrl.replaceAll("/+$", "") + "/?support=open";
            String text = "Xin chào " + name + ",\n\nNhân viên cửa hàng vừa trả lời bạn:\n" + excerpt + "\n\nXem cuộc trò chuyện: " + link + "\n\nTrân trọng,\nF6S Store";
            helper.setText(text, "<!doctype html><html lang=\"vi\"><body><p>" + org.springframework.web.util.HtmlUtils.htmlEscape(text).replace("\n", "<br>") + "</p></body></html>");
            mailSender.send(message);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public TestMailResult sendTestMail(String recipientEmail) {
        if (!isConfigured()) {
            return new TestMailResult(false, "Chưa cấu hình máy chủ email. Hãy điền thông tin SMTP bên dưới và bấm 'Lưu cấu hình'.");
        }
        if (recipientEmail == null || recipientEmail.isBlank()) {
            return new TestMailResult(false, "Vui lòng nhập địa chỉ email nhận thư thử nghiệm.");
        }
        String resolvedFrom = getResolvedFrom();
        if (resolvedFrom.isBlank()) {
            return new TestMailResult(false, "Chưa cấu hình địa chỉ email gửi (From hoặc Username).");
        }
        try {
            JavaMailSender mailSender = getMailSender();
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
            recordError(null);
            return new TestMailResult(true, "Gửi email thử nghiệm thành công tới " + recipientEmail + ".");
        } catch (jakarta.mail.AuthenticationFailedException | org.springframework.mail.MailAuthenticationException ex) {
            String err = "Xác thực SMTP thất bại: Sai tên đăng nhập hoặc mật khẩu ứng dụng Gmail (cần App Password 16 ký tự).";
            recordError(err);
            return new TestMailResult(false, err);
        } catch (jakarta.mail.MessagingException ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "";
            String err;
            if (msg.contains("Connection refused") || msg.contains("timeout") || msg.contains("timed out") || msg.contains("UnknownHostException")) {
                err = "Không thể kết nối tới máy chủ SMTP (" + getResolvedHost() + ":" + getResolvedPort() + "). Vui lòng kiểm tra host, port hoặc mạng.";
            } else {
                err = "Lỗi máy chủ thư: " + msg;
            }
            recordError(err);
            return new TestMailResult(false, err);
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "Lỗi không xác định";
            String err;
            if (msg.contains("timeout") || msg.contains("Connection refused")) {
                err = "Không thể kết nối tới máy chủ SMTP (" + getResolvedHost() + ":" + getResolvedPort() + "). Vui lòng kiểm tra host, port hoặc mạng.";
            } else {
                err = "Gửi email thử nghiệm thất bại: " + msg;
            }
            recordError(err);
            return new TestMailResult(false, err);
        }
    }
}
