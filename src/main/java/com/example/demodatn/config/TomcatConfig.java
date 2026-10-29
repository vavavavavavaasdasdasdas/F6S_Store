package com.example.demodatn.config;

import org.apache.catalina.session.StandardManager;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Vô hiệu hóa việc Tomcat lưu / tải session từ file đĩa (SESSIONS.ser) qua các lần restart.
 * Giúp tránh lỗi InvalidClassException / "Cannot deserialize session attribute [SPRING_SECURITY_CONTEXT]".
 */
@Configuration
public class TomcatConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatSessionPersistenceCustomizer() {
        return factory -> factory.addContextCustomizers(context -> {
            StandardManager manager = new StandardManager();
            manager.setPathname(null);
            context.setManager(manager);
        });
    }
}
