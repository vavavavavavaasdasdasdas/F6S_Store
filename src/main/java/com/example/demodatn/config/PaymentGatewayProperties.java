package com.example.demodatn.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "payment")
public class PaymentGatewayProperties {

    private String publicBaseUrl = "http://localhost:8080";
    private final VnPay vnpay = new VnPay();
    private final QrDemo qrDemo = new QrDemo();

    public boolean isVnPayConfigured() {
        return vnpay.isEnabled()
                && hasText(vnpay.getTmnCode())
                && hasText(vnpay.getHashSecret())
                && hasText(vnpay.getPaymentUrl());
    }

    public String baseUrl() {
        String value = hasText(publicBaseUrl) ? publicBaseUrl.trim() : "http://localhost:8080";
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Getter
    @Setter
    public static class VnPay {
        private boolean enabled = true;
        private boolean reconcileOnReturn = true;
        private boolean localCheckoutEnabled = false;
        private String tmnCode = "L5HQXLHO";
        private String hashSecret = "ETHX3ND34R4DBRK5V8J95H6I8G5QO040";
        private String paymentUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    }

    @Getter
    @Setter
    public static class QrDemo {
        private boolean enabled = true;
        private String secret = "";
    }
}
