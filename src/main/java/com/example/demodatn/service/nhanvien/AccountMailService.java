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
        return sendAccountMail(email, name, username, password, false, null);
    }

    public boolean sendAccountMail(String email, String name, String username, String password, boolean isEmployee, String position) {
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

            // Plain text (tương thích backward và client không hỗ trợ HTML, đáp ứng integration test)
            String text = "Xin chào " + name + ",\n\n" + introText + "\n" +
                    (isEmployee && position != null && !position.isBlank() ? ("Chức vụ: " + position + "\n") : "") +
                    "Tên đăng nhập: " + username + "\n" +
                    "Mật khẩu tạm: " + password + "\n" +
                    "Đăng nhập: " + loginUrl + "\n\n" +
                    noteText + "\n" +
                    "Nếu đây là thư gửi lại, mật khẩu tạm trước đó đã hết hiệu lực.\n\n" +
                    "Trân trọng,\nF6S Store";

            // HTML email chuyên nghiệp, tương thích tốt với email client
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

            sender.getObject().send(message);
            log.info("Đã gửi email thông tin tài khoản thành công tới {}", email);
            return true;
        } catch (Exception ex) {
            log.error("Không thể gửi email thông tin tài khoản tới {}: {}", email, ex.getMessage());
            return false;
        }
    }

    public boolean sendSupportNotice(String email,String name,String excerpt) {
        if(TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Không gọi SMTP trong transaction.");
        if(!isConfigured()||getResolvedFrom().isBlank())return false;
        try {
            var uri=java.net.URI.create(publicBaseUrl);if(!java.util.Set.of("http","https").contains(uri.getScheme())||uri.getHost()==null)return false;
            var message=sender.getObject().createMimeMessage();var helper=new MimeMessageHelper(message,true,java.nio.charset.StandardCharsets.UTF_8.name());
            helper.setFrom(getResolvedFrom());helper.setTo(email);helper.setSubject("Cửa hàng đã trả lời tin nhắn của bạn - F6S Store");
            String link=publicBaseUrl.replaceAll("/+$","")+"/?support=open";
            String text="Xin chào "+name+",\n\nNhân viên cửa hàng vừa trả lời bạn:\n"+excerpt+"\n\nXem cuộc trò chuyện: "+link+"\n\nTrân trọng,\nF6S Store";
            helper.setText(text,"<!doctype html><html lang=\"vi\"><body><p>"+org.springframework.web.util.HtmlUtils.htmlEscape(text).replace("\n","<br>")+"</p></body></html>");
            sender.getObject().send(message);return true;
        }catch(Exception ignored){return false;}
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
