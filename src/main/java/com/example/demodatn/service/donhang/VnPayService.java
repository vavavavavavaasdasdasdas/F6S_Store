package com.example.demodatn.service.donhang;

import com.example.demodatn.config.PaymentGatewayProperties;
import com.example.demodatn.entity.donhang.Order;
import com.example.demodatn.repository.donhang.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VnPayService {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String LOCAL_DEMO_VNPAY_TMN_CODE = "LOCALDEMO";

    private final PaymentGatewayProperties properties;
    private final OrderRepository orderRepository;
    private final String qrDemoFallbackSecret = UUID.randomUUID().toString();

    @Transactional
    public String createPaymentUrl(Order order, String clientIp) {
        PaymentGatewayProperties.VnPay config = properties.getVnpay();
        boolean usingLocalDemo = !properties.isVnPayConfigured() || config.isLocalCheckoutEnabled();
        String transactionRef = "VP" + order.getId() + "T" + System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);

        long amount = order.getFinalAmount() != null ? order.getFinalAmount().longValue() : 0L;
        long vnpAmount = amount * 100L;

        TreeMap<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", usingLocalDemo ? LOCAL_DEMO_VNPAY_TMN_CODE : config.getTmnCode());
        params.put("vnp_Amount", String.valueOf(vnpAmount));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", transactionRef);
        params.put("vnp_OrderInfo", "Thanh toan don hang " + order.getOrderCode());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", properties.baseUrl() + "/payment/vnpay/return");
        params.put("vnp_IpAddr", (clientIp == null || clientIp.isBlank()) ? "127.0.0.1" : clientIp);
        params.put("vnp_CreateDate", now.format(VNPAY_TIME));
        params.put("vnp_ExpireDate", now.plusMinutes(15).format(VNPAY_TIME));

        String query = queryString(params);
        String secretKey = usingLocalDemo ? qrDemoSecret() : config.getHashSecret();
        String signature = hmacSha512(secretKey, query);

        order.setPaymentReference(transactionRef);
        orderRepository.save(order);

        String paymentEndpoint = usingLocalDemo
                ? properties.baseUrl() + "/api/shop/payments/vnpay/checkout"
                : config.getPaymentUrl();

        return paymentEndpoint + "?" + query + "&vnp_SecureHash=" + signature;
    }

    public boolean verifySignature(Map<String, String> source) {
        if (source == null) return false;

        boolean usingLocalDemo = !properties.isVnPayConfigured();
        String expectedTmnCode = usingLocalDemo
                ? LOCAL_DEMO_VNPAY_TMN_CODE
                : properties.getVnpay().getTmnCode();
        String signingSecret = usingLocalDemo
                ? qrDemoSecret()
                : properties.getVnpay().getHashSecret();

        if (expectedTmnCode != null && !expectedTmnCode.isBlank()) {
            String incomingTmn = source.get("vnp_TmnCode");
            if (incomingTmn != null && !expectedTmnCode.equalsIgnoreCase(incomingTmn)) {
                return false;
            }
        }

        String received = source.get("vnp_SecureHash");
        if (received == null || received.isBlank()) return false;

        TreeMap<String, String> signed = new TreeMap<>();
        source.forEach((key, value) -> {
            if (key != null && key.startsWith("vnp_")
                    && !"vnp_SecureHash".equals(key) && !"vnp_SecureHashType".equals(key)
                    && value != null && !value.isBlank()) {
                signed.put(key, value);
            }
        });

        String expected = hmacSha512(signingSecret, queryString(signed));
        return MessageDigest.isEqual(
                expected.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8),
                received.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)
        );
    }

    @Transactional
    public PaymentResult processReturn(Map<String, String> params) {
        boolean valid = verifySignature(params);
        String ref = params.get("vnp_TxnRef");
        Order order = findOrderByRef(ref);

        if (!valid || order == null) {
            log.warn("VNPay return invalid or order not found for ref: {}", ref);
            return new PaymentResult(false, false, order, "Chữ ký không hợp lệ hoặc không tìm thấy đơn hàng");
        }

        long receivedAmount = parseLong(params.get("vnp_Amount")) / 100L;
        long orderAmount = order.getFinalAmount() != null ? order.getFinalAmount().longValue() : 0L;
        if (receivedAmount != orderAmount) {
            log.warn("VNPay return amount mismatch: order {} expects {} but got {}", order.getOrderCode(), orderAmount, receivedAmount);
            return new PaymentResult(false, false, order, "Số tiền thanh toán không khớp");
        }

        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");
        boolean success = "00".equals(responseCode) && ("00".equals(transactionStatus) || transactionStatus == null);

        if (success) {
            order.setPaymentStatus("PAID");
            String transNo = params.get("vnp_TransactionNo");
            if (transNo != null && !transNo.isBlank()) {
                order.setPaymentReference(transNo);
            }
            String bankCode = params.get("vnp_BankCode");
            String payDate = params.get("vnp_PayDate");
            order.setPaymentNote("VNPay thanh toán thành công qua " + (bankCode != null ? bankCode : "VNPAY") + " (GD: " + transNo + ")");
            orderRepository.save(order);
            log.info("Order {} marked PAID via VNPay transaction {}", order.getOrderCode(), transNo);
            return new PaymentResult(true, true, order, "Thanh toán thành công");
        } else {
            log.info("Order {} VNPay payment cancelled/failed with responseCode {}", order.getOrderCode(), responseCode);
            return new PaymentResult(true, false, order, "Giao dịch không thành công hoặc đã bị hủy (Mã lỗi: " + responseCode + ")");
        }
    }

    @Transactional
    public Map<String, String> handleIpn(Map<String, String> params) {
        if (!verifySignature(params)) {
            return Map.of("RspCode", "97", "Message", "Invalid signature");
        }
        String ref = params.get("vnp_TxnRef");
        Order order = findOrderByRef(ref);
        if (order == null) {
            return Map.of("RspCode", "01", "Message", "Order not found");
        }

        long receivedAmount = parseLong(params.get("vnp_Amount")) / 100L;
        long orderAmount = order.getFinalAmount() != null ? order.getFinalAmount().longValue() : 0L;
        if (receivedAmount != orderAmount) {
            return Map.of("RspCode", "04", "Message", "Invalid amount");
        }

        if ("PAID".equals(order.getPaymentStatus())) {
            return Map.of("RspCode", "02", "Message", "Order already confirmed");
        }

        boolean success = "00".equals(params.get("vnp_ResponseCode"))
                && "00".equals(params.get("vnp_TransactionStatus"));
        if (success) {
            order.setPaymentStatus("PAID");
            order.setPaymentReference(params.get("vnp_TransactionNo"));
            orderRepository.save(order);
        }
        return Map.of("RspCode", "00", "Message", "Confirm success");
    }

    @Transactional
    public PaymentResult completeLocalCheckout(Map<String, String> params, boolean approved, String prefix) {
        boolean valid = verifySignature(params);
        String ref = params.get("vnp_TxnRef");
        Order order = findOrderByRef(ref);

        if (!valid || order == null) {
            return new PaymentResult(false, false, order, "Chữ ký không hợp lệ");
        }

        if ("PAID".equals(order.getPaymentStatus())) {
            return new PaymentResult(true, true, order, "Đơn hàng đã được thanh toán");
        }

        if (approved) {
            order.setPaymentStatus("PAID");
            String transNo = prefix + System.currentTimeMillis();
            order.setPaymentReference(transNo);
            order.setPaymentNote("VNPay Demo xác nhận thành công (" + transNo + ")");
            orderRepository.save(order);
            return new PaymentResult(true, true, order, "Thanh toán thành công");
        } else {
            return new PaymentResult(true, false, order, "Giao dịch đã hủy");
        }
    }

    public LocalCheckoutStatus localCheckoutStatus(Map<String, String> params) {
        if (!verifySignature(params)) {
            return new LocalCheckoutStatus(false, false, "");
        }
        Order order = findOrderByRef(params.get("vnp_TxnRef"));
        if (order == null) {
            return new LocalCheckoutStatus(false, false, "");
        }
        boolean isPaid = "PAID".equalsIgnoreCase(order.getPaymentStatus());
        return new LocalCheckoutStatus(true, isPaid, order.getOrderCode());
    }

    private Order findOrderByRef(String ref) {
        if (ref == null || ref.isBlank()) return null;
        var orderOpt = orderRepository.findByPaymentReference(ref);
        if (orderOpt.isPresent()) return orderOpt.get();

        if (ref.startsWith("VP") && ref.contains("T")) {
            try {
                String idPart = ref.substring(2, ref.indexOf("T"));
                Integer orderId = Integer.parseInt(idPart);
                return orderRepository.findById(orderId).orElse(null);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private String qrDemoSecret() {
        String configured = properties.getQrDemo().getSecret();
        return configured == null || configured.isBlank() ? qrDemoFallbackSecret : configured;
    }

    private String queryString(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .map(entry -> urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String hmacSha512(String key, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tạo chữ ký HMAC-SHA512", e);
        }
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value == null || value.isBlank() ? "0" : value.trim());
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    public record PaymentResult(boolean valid, boolean success, Order order, String message) {}
    public record LocalCheckoutStatus(boolean valid, boolean paid, String orderCode) {}
}
