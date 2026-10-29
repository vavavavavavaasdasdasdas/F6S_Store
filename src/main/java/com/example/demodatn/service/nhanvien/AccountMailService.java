package com.example.demodatn.service.nhanvien;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service @RequiredArgsConstructor
public class AccountMailService {
    private final ObjectProvider<JavaMailSender> sender;
    @Value("${spring.mail.host:}") private String host;
    @Value("${shop.mail.from:}") private String from;
    @Value("${shop.public-base-url:http://localhost:8080}") private String publicBaseUrl;

    public boolean send(String email,String name,String username,String password) {
        if(TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("Không được gọi SMTP trong giao dịch database.");
        if(host.isBlank() || from.isBlank() || sender.getIfAvailable()==null)return false;
        try {
            var link=java.net.URI.create(publicBaseUrl);
            if(!java.util.Set.of("http","https").contains(link.getScheme()) || link.getHost()==null)return false;
            var message=sender.getObject().createMimeMessage();
            var helper=new MimeMessageHelper(message,false,java.nio.charset.StandardCharsets.UTF_8.name());
            helper.setFrom(from);helper.setTo(email);helper.setSubject("Tài khoản đăng nhập F6S Store");
            helper.setText("Xin chào "+name+",\n\nTài khoản của bạn đã sẵn sàng.\nTên đăng nhập: "+username+
                    "\nMật khẩu tạm: "+password+"\nĐăng nhập: "+publicBaseUrl.replaceAll("/+$","")+"/login"+
                    "\n\nBạn phải đổi mật khẩu ngay sau lần đăng nhập đầu tiên. Không chia sẻ mật khẩu này.\n"+
                    "Nếu đây là thư gửi lại, mật khẩu tạm trước đó đã hết hiệu lực.\n\nTrân trọng,\nF6S Store",false);
            sender.getObject().send(message);
            return true;
        } catch(Exception ignored) {
            // SMTP diagnostics can contain message data; never log the exception or mail body.
            return false;
        }
    }
}
