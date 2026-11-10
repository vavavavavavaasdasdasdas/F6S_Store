package com.example.demodatn.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

@Slf4j
@Configuration
public class MailConfig {

    @Bean
    @ConditionalOnMissingBean(DynamicJavaMailSender.class)
    public DynamicJavaMailSender javaMailSender(
            @Value("${spring.mail.host:}") String envHost,
            @Value("${spring.mail.port:587}") int envPort,
            @Value("${spring.mail.username:}") String envUsername,
            @Value("${spring.mail.password:}") String envPassword,
            @Value("${shop.mail.from:}") String envFrom,
            @Value("${spring.mail.properties.mail.smtp.auth:true}") String envAuth,
            @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}") String envStarttls) {

        DynamicJavaMailSender dynamicSender = new DynamicJavaMailSender();

        boolean isExplicitHost = envHost != null && !envHost.isBlank();
        boolean resolvedAuth = !"false".equalsIgnoreCase(envAuth);
        boolean resolvedStarttls = !"false".equalsIgnoreCase(envStarttls);

        if (isExplicitHost) {
            // Khi môi trường (testcontainers, greenmail, CI) đã chỉ định rõ host
            dynamicSender.configure(envHost, envPort, envUsername, envPassword, envFrom, resolvedAuth, resolvedStarttls);
            log.info("Khởi tạo DynamicJavaMailSender từ môi trường: host={}:{}, auth={}", envHost, envPort, resolvedAuth);
            return dynamicSender;
        }

        String resolvedHost = "";
        int resolvedPort = envPort;
        String resolvedUsername = envUsername;
        String resolvedPassword = envPassword;
        String resolvedFrom = envFrom;

        // Nếu host chưa có, nạp từ application-local.properties
        List<Path> candidatePaths = List.of(
                Paths.get("application-local.properties"),
                Paths.get("../application-local.properties"),
                Paths.get("../../application-local.properties")
        );
        for (Path candidate : candidatePaths) {
            if (Files.exists(candidate)) {
                try (InputStream is = Files.newInputStream(candidate)) {
                    Properties localProps = new Properties();
                    localProps.load(is);
                    String h = localProps.getProperty("spring.mail.host");
                    if (h != null && !h.isBlank()) {
                        resolvedHost = h.trim();
                        String p = localProps.getProperty("spring.mail.port");
                        if (p != null && !p.isBlank()) {
                            try { resolvedPort = Integer.parseInt(p.trim()); } catch (NumberFormatException ignored) {}
                        }
                        resolvedUsername = localProps.getProperty("spring.mail.username", "");
                        resolvedPassword = localProps.getProperty("spring.mail.password", "");
                        resolvedFrom = localProps.getProperty("shop.mail.from", "");
                        resolvedAuth = !"false".equalsIgnoreCase(localProps.getProperty("spring.mail.properties.mail.smtp.auth", "true"));
                        resolvedStarttls = !"false".equalsIgnoreCase(localProps.getProperty("spring.mail.properties.mail.smtp.starttls.enable", "true"));
                        log.info("Đã nạp cấu hình email SMTP từ {}: host={}:{}", candidate.toAbsolutePath(), resolvedHost, resolvedPort);
                        break;
                    }
                } catch (Exception ex) {
                    log.warn("Không thể đọc cấu hình email từ {}: {}", candidate.toAbsolutePath(), ex.getMessage());
                }
            }
        }

        if (!resolvedHost.isBlank()) {
            dynamicSender.configure(resolvedHost, resolvedPort, resolvedUsername, resolvedPassword, resolvedFrom, resolvedAuth, resolvedStarttls);
        } else {
            log.info("Khởi tạo DynamicJavaMailSender: Chưa có cấu hình SMTP (sẵn sàng nhận cấu hình qua giao diện quản trị)");
        }

        return dynamicSender;
    }
}
