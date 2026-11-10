package com.example.demodatn.config;

import jakarta.mail.internet.MimeMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessagePreparator;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@Getter
public class DynamicJavaMailSender implements JavaMailSender {

    private volatile JavaMailSenderImpl delegate = new JavaMailSenderImpl();
    private volatile String host = "";
    private volatile int port = 587;
    private volatile String username = "";
    private volatile String password = "";
    private volatile String from = "";
    private volatile boolean auth = true;
    private volatile boolean starttls = true;
    private volatile boolean configured = false;
    private volatile String lastError;

    public DynamicJavaMailSender() {
        this.delegate.setDefaultEncoding("UTF-8");
    }

    public synchronized void configure(String host, int port, String username, String password, String from, boolean auth, boolean starttls) {
        this.host = host != null ? host.trim() : "";
        this.port = port > 0 ? port : 587;
        this.username = username != null ? username.trim() : "";
        if (password != null && !password.isBlank()) {
            this.password = password.trim();
        }
        this.from = (from != null && !from.isBlank()) ? from.trim() : this.username;
        this.auth = auth;
        this.starttls = starttls;

        JavaMailSenderImpl impl = new JavaMailSenderImpl();
        impl.setDefaultEncoding("UTF-8");
        impl.setHost(this.host);
        impl.setPort(this.port);

        // Chỉ xác thực username/password khi auth = true và username không rỗng
        if (this.auth && !this.username.isBlank()) {
            impl.setUsername(this.username);
            if (!this.password.isBlank()) {
                impl.setPassword(this.password);
            }
        }

        Properties props = impl.getJavaMailProperties();
        props.put("mail.smtp.auth", String.valueOf(this.auth));
        props.put("mail.smtp.starttls.enable", String.valueOf(this.starttls));
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        props.put("mail.debug", "false");

        this.delegate = impl;
        this.configured = !this.host.isBlank();
        this.lastError = null;

        log.info("Cập nhật cấu hình email SMTP runtime: {}:{}, user={}", this.host, this.port, this.username);
    }

    public void setLastError(String error) {
        this.lastError = error;
    }

    public boolean isPasswordConfigured() {
        return password != null && !password.isBlank();
    }

    public synchronized void persistConfiguration() {
        if ("true".equalsIgnoreCase(System.getProperty("email.disable.persist"))) {
            return;
        }

        List<Path> targetPaths = List.of(
                Paths.get("application-local.properties"),
                Paths.get("../application-local.properties"),
                Paths.get("../../application-local.properties")
        );

        for (Path target : targetPaths) {
            try {
                if (Files.exists(target) || target.equals(Paths.get("application-local.properties"))) {
                    updatePropertiesFile(target);
                }
            } catch (Exception ex) {
                log.warn("Không thể lưu cấu hình email vào {}: {}", target.toAbsolutePath(), ex.getMessage());
            }
        }
    }

    private void updatePropertiesFile(Path filePath) {
        try {
            List<String> lines;
            if (Files.exists(filePath)) {
                lines = new ArrayList<>(Files.readAllLines(filePath, StandardCharsets.UTF_8));
            } else {
                lines = new ArrayList<>();
            }

            Map<String, String> newProps = new LinkedHashMap<>();
            newProps.put("spring.mail.host", this.host);
            newProps.put("spring.mail.port", String.valueOf(this.port));
            newProps.put("spring.mail.username", this.username);
            newProps.put("spring.mail.password", this.password);
            newProps.put("spring.mail.properties.mail.smtp.auth", String.valueOf(this.auth));
            newProps.put("spring.mail.properties.mail.smtp.starttls.enable", String.valueOf(this.starttls));
            newProps.put("shop.mail.from", this.from);

            Set<String> keysHandled = new HashSet<>();
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                for (Map.Entry<String, String> entry : newProps.entrySet()) {
                    if (line.startsWith(entry.getKey() + "=") || line.startsWith(entry.getKey() + " =")) {
                        lines.set(i, entry.getKey() + "=" + entry.getValue());
                        keysHandled.add(entry.getKey());
                        break;
                    }
                }
            }

            for (Map.Entry<String, String> entry : newProps.entrySet()) {
                if (!keysHandled.contains(entry.getKey())) {
                    lines.add(entry.getKey() + "=" + entry.getValue());
                }
            }

            Files.write(filePath, lines, StandardCharsets.UTF_8);
            log.info("Đã lưu cấu hình email vào {}", filePath.toAbsolutePath());
        } catch (Exception ex) {
            log.error("Lỗi khi ghi file properties {}: {}", filePath, ex.getMessage());
        }
    }

    // --- Triển khai các phương thức của JavaMailSender ---

    @Override
    public MimeMessage createMimeMessage() {
        return delegate.createMimeMessage();
    }

    @Override
    public MimeMessage createMimeMessage(InputStream contentStream) throws MailException {
        return delegate.createMimeMessage(contentStream);
    }

    @Override
    public void send(MimeMessage mimeMessage) throws MailException {
        if (!configured || host.isBlank()) {
            throw new org.springframework.mail.MailSendException("Máy chủ email chưa được cấu hình. Vui lòng thiết lập SMTP tại trang Hồ sơ tài khoản.");
        }
        delegate.send(mimeMessage);
    }

    @Override
    public void send(MimeMessage... mimeMessages) throws MailException {
        if (!configured || host.isBlank()) {
            throw new org.springframework.mail.MailSendException("Máy chủ email chưa được cấu hình. Vui lòng thiết lập SMTP tại trang Hồ sơ tài khoản.");
        }
        delegate.send(mimeMessages);
    }

    @Override
    public void send(MimeMessagePreparator mimeMessagePreparator) throws MailException {
        if (!configured || host.isBlank()) {
            throw new org.springframework.mail.MailSendException("Máy chủ email chưa được cấu hình. Vui lòng thiết lập SMTP tại trang Hồ sơ tài khoản.");
        }
        delegate.send(mimeMessagePreparator);
    }

    @Override
    public void send(MimeMessagePreparator... mimeMessagePreparators) throws MailException {
        if (!configured || host.isBlank()) {
            throw new org.springframework.mail.MailSendException("Máy chủ email chưa được cấu hình. Vui lòng thiết lập SMTP tại trang Hồ sơ tài khoản.");
        }
        delegate.send(mimeMessagePreparators);
    }

    @Override
    public void send(SimpleMailMessage simpleMessage) throws MailException {
        if (!configured || host.isBlank()) {
            throw new org.springframework.mail.MailSendException("Máy chủ email chưa được cấu hình. Vui lòng thiết lập SMTP tại trang Hồ sơ tài khoản.");
        }
        delegate.send(simpleMessage);
    }

    @Override
    public void send(SimpleMailMessage... simpleMessages) throws MailException {
        if (!configured || host.isBlank()) {
            throw new org.springframework.mail.MailSendException("Máy chủ email chưa được cấu hình. Vui lòng thiết lập SMTP tại trang Hồ sơ tài khoản.");
        }
        delegate.send(simpleMessages);
    }
}
