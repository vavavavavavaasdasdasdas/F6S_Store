package com.example.demodatn.service.donhang;

import com.example.demodatn.entity.donhang.Order;
import com.example.demodatn.entity.donhang.OrderDetail;
import com.example.demodatn.entity.donhang.OrderMailLog;
import com.example.demodatn.repository.donhang.OrderMailLogRepository;
import com.example.demodatn.repository.donhang.OrderRepository;
import com.example.demodatn.service.nhanvien.AccountMailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEmailService {

    private final ObjectProvider<JavaMailSender> senderProvider;
    private final AccountMailService accountMailService;
    private final OrderRepository orderRepository;
    private final OrderMailLogRepository mailLogRepository;
    private final Clock clock;

    @Value("${shop.public-base-url:http://localhost:8080}")
    private String publicBaseUrl;

    public static final Map<String, String> STATUS_TITLES = Map.of(
            "CONFIRMED", "Đã xác nhận",
            "PREPARING", "Đang chuẩn bị hàng",
            "SHIPPING", "Đang giao hàng",
            "DELIVERED", "Đã giao hàng thành công",
            "CANCELLED", "Đã hủy đơn hàng",
            "DELIVERY_FAILED", "Giao hàng thất bại",
            "PAYMENT_CONFIRMED", "Đã nhận thanh toán chuyển khoản"
    );

    private static final Set<String> SUPPORTED_STATUSES = STATUS_TITLES.keySet();

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Nhận sự kiện đổi trạng thái đơn hàng: orderId={}, status={}", event.orderId(), event.status());
        sendNotification(event.orderId(), event.status(), event.note(), event.carrier(), event.trackingCode(), false);
    }

    public void resend(Integer orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại."));
        sendNotification(orderId, status, null, null, null, true);
    }

    public void sendNotification(Integer orderId, String status, String note, String carrier, String trackingCode, boolean isResend) {
        if (!SUPPORTED_STATUSES.contains(status)) {
            log.debug("Trạng thái {} không thuộc danh sách gửi email thông báo.", status);
            return;
        }

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("Không tìm thấy đơn hàng id={}", orderId);
            return;
        }

        // Chỉ gửi cho đơn hàng trực tuyến, không gửi cho đơn tại quầy
        if (!"ONLINE".equals(order.getOrderType())) {
            log.info("Bỏ qua gửi email trạng thái cho đơn tại quầy: orderId={}", orderId);
            return;
        }

        // Kiểm tra gửi trùng lặp nếu không phải resend
        if (!isResend) {
            var existing = mailLogRepository.findByOrder_IdAndStatus(orderId, status);
            if (existing.isPresent() && existing.get().isThanhCong()) {
                log.info("Email cho trạng thái {} của đơn {} đã gửi thành công trước đó, bỏ qua.", status, orderId);
                return;
            }
        }

        // Xác định địa chỉ email nhận
        String recipientEmail = resolveRecipientEmail(order);
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneId.systemDefault());

        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.info("Đơn hàng {} không có email nhận thông báo.", order.getOrderCode());
            saveLog(order, status, null, now, false, "Không có email");
            return;
        }

        if (!accountMailService.isConfigured()) {
            log.warn("Chưa cấu hình máy chủ email, không thể gửi thông báo cho đơn {}", order.getOrderCode());
            saveLog(order, status, recipientEmail, now, false, "Chưa cấu hình máy chủ email");
            return;
        }

        try {
            JavaMailSender mailSender = senderProvider.getIfAvailable();
            if (mailSender == null) {
                saveLog(order, status, recipientEmail, now, false, "Không thể khởi tạo JavaMailSender");
                return;
            }

            String fromAddress = accountMailService.getResolvedFrom();
            String statusTitle = STATUS_TITLES.getOrDefault(status, status);
            String subject = "[F6S Store] Thông báo đơn hàng " + order.getOrderCode() + " - " + statusTitle;

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(recipientEmail);
            helper.setSubject(subject);

            String html = buildEmailHtml(order, status, statusTitle, note, carrier, trackingCode, now);
            helper.setText(html, true);

            ClassPathResource logoResource = new ClassPathResource("static/images/logo.jpg");
            if (logoResource.exists()) {
                helper.addInline("f6s-logo", logoResource, "image/jpeg");
            }

            mailSender.send(message);
            log.info("Đã gửi email thông báo trạng thái {} cho đơn {} tới {}", status, order.getOrderCode(), recipientEmail);
            saveLog(order, status, recipientEmail, now, true, null);

        } catch (Exception ex) {
            String errorMsg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            log.error("Lỗi gửi email thông báo trạng thái {} cho đơn {}: {}", status, order.getOrderCode(), errorMsg);
            saveLog(order, status, recipientEmail, now, false, "Lỗi gửi: " + errorMsg);
        }
    }

    private String resolveRecipientEmail(Order order) {
        if (order.getEmail() != null && !order.getEmail().isBlank()) {
            return order.getEmail().trim();
        }
        if (order.getCustomer() != null) {
            if (order.getCustomer().getUser() != null && order.getCustomer().getUser().getEmail() != null && !order.getCustomer().getUser().getEmail().isBlank()) {
                return order.getCustomer().getUser().getEmail().trim();
            }
            if (order.getCustomer().getEmail() != null && !order.getCustomer().getEmail().isBlank()) {
                return order.getCustomer().getEmail().trim();
            }
        }
        return null;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLog(Order order, String status, String email, LocalDateTime sentAt, boolean success, String error) {
        try {
            OrderMailLog entry = mailLogRepository.findByOrder_IdAndStatus(order.getId(), status)
                    .orElseGet(OrderMailLog::new);
            entry.setOrder(order);
            entry.setStatus(status);
            entry.setEmail(email);
            entry.setSentAt(sentAt);
            entry.setThanhCong(success);
            entry.setLoi(error != null && error.length() > 1000 ? error.substring(0, 1000) : error);
            mailLogRepository.saveAndFlush(entry);
        } catch (Exception ex) {
            log.error("Không thể ghi order_mail_log: {}", ex.getMessage());
        }
    }

    private String buildEmailHtml(Order order, String status, String statusTitle, String note, String carrier, String trackingCode, LocalDateTime sentAt) {
        DecimalFormat currencyFmt = new DecimalFormat("#,###");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang=\"vi\"><head><meta charset=\"UTF-8\"></head><body style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;\">");

        // Header logo
        sb.append("<div style=\"text-align: center; margin-bottom: 24px; border-bottom: 2px solid #eaeaea; padding-bottom: 16px;\">");
        sb.append("<img src=\"cid:f6s-logo\" alt=\"F6S Store\" width=\"120\" height=\"90\" style=\"object-fit: contain;\">");
        sb.append("<h2 style=\"margin: 8px 0 0; color: #1a73e8;\">F6S STORE - THÔNG BÁO ĐƠN HÀNG</h2>");
        sb.append("</div>");

        // Greeting & Status Summary
        sb.append("<p>Xin chào <strong>").append(HtmlUtils.htmlEscape(order.getReceiverName() != null ? order.getReceiverName() : "Quý khách")).append("</strong>,</p>");
        sb.append("<p>Đơn hàng <strong>#").append(HtmlUtils.htmlEscape(order.getOrderCode())).append("</strong> của bạn hiện có cập nhật trạng thái mới:</p>");

        sb.append("<div style=\"background: #f8f9fa; border-left: 4px solid #1a73e8; padding: 12px 16px; margin: 16px 0;\">");
        sb.append("<p style=\"margin: 0; font-size: 16px;\">Trạng thái: <strong style=\"color: #1a73e8;\">").append(HtmlUtils.htmlEscape(statusTitle)).append("</strong></p>");
        sb.append("<p style=\"margin: 4px 0 0; font-size: 13px; color: #666;\">Thời gian cập nhật: ").append(sentAt.format(timeFmt)).append("</p>");

        if (carrier != null && !carrier.isBlank()) {
            sb.append("<p style=\"margin: 4px 0 0; font-size: 14px;\">Đơn vị vận chuyển: <strong>").append(HtmlUtils.htmlEscape(carrier)).append("</strong></p>");
        }
        if (trackingCode != null && !trackingCode.isBlank()) {
            sb.append("<p style=\"margin: 4px 0 0; font-size: 14px;\">Mã vận đơn: <strong>").append(HtmlUtils.htmlEscape(trackingCode)).append("</strong></p>");
        }
        if (note != null && !note.isBlank()) {
            sb.append("<p style=\"margin: 4px 0 0; font-size: 14px; color: #d93025;\">Ghi chú / Lý do: <em>").append(HtmlUtils.htmlEscape(note)).append("</em></p>");
        }
        sb.append("</div>");

        // Receiver Information
        sb.append("<h4 style=\"margin-top: 24px; border-bottom: 1px solid #ddd; padding-bottom: 6px;\">Thông tin nhận hàng</h4>");
        sb.append("<p style=\"margin: 4px 0;\">Người nhận: <strong>").append(HtmlUtils.htmlEscape(order.getReceiverName() != null ? order.getReceiverName() : "—")).append("</strong></p>");
        sb.append("<p style=\"margin: 4px 0;\">Số điện thoại: ").append(HtmlUtils.htmlEscape(order.getPhoneNumber() != null ? order.getPhoneNumber() : "—")).append("</p>");
        sb.append("<p style=\"margin: 4px 0;\">Địa chỉ giao hàng: ").append(HtmlUtils.htmlEscape(order.getShippingAddress() != null ? order.getShippingAddress() : "—")).append("</p>");

        // Items Table
        sb.append("<h4 style=\"margin-top: 24px; border-bottom: 1px solid #ddd; padding-bottom: 6px;\">Chi tiết sản phẩm</h4>");
        sb.append("<table style=\"width: 100%; border-collapse: collapse; margin-top: 8px;\">");
        sb.append("<thead><tr style=\"background: #f1f3f4; text-align: left; font-size: 13px;\">");
        sb.append("<th style=\"padding: 8px; border: 1px solid #ddd;\">Sản phẩm</th>");
        sb.append("<th style=\"padding: 8px; border: 1px solid #ddd;\">Phân loại</th>");
        sb.append("<th style=\"padding: 8px; border: 1px solid #ddd; text-align: center;\">SL</th>");
        sb.append("<th style=\"padding: 8px; border: 1px solid #ddd; text-align: right;\">Đơn giá</th>");
        sb.append("<th style=\"padding: 8px; border: 1px solid #ddd; text-align: right;\">Thành tiền</th>");
        sb.append("</tr></thead><tbody>");

        if (order.getOrderDetails() != null) {
            for (OrderDetail detail : order.getOrderDetails()) {
                String variantDesc = (detail.getSizeName() != null ? detail.getSizeName() : "")
                        + (detail.getColorName() != null ? " / " + detail.getColorName() : "");
                sb.append("<tr>");
                sb.append("<td style=\"padding: 8px; border: 1px solid #ddd; font-size: 13px;\">").append(HtmlUtils.htmlEscape(detail.getProductName())).append("</td>");
                sb.append("<td style=\"padding: 8px; border: 1px solid #ddd; font-size: 13px; color: #555;\">").append(HtmlUtils.htmlEscape(variantDesc)).append("</td>");
                sb.append("<td style=\"padding: 8px; border: 1px solid #ddd; text-align: center; font-size: 13px;\">").append(detail.getQuantity()).append("</td>");
                sb.append("<td style=\"padding: 8px; border: 1px solid #ddd; text-align: right; font-size: 13px;\">").append(currencyFmt.format(detail.getPrice())).append(" đ</td>");
                sb.append("<td style=\"padding: 8px; border: 1px solid #ddd; text-align: right; font-size: 13px; font-weight: bold;\">").append(currencyFmt.format(detail.getTotalPrice())).append(" đ</td>");
                sb.append("</tr>");
            }
        }
        sb.append("</tbody></table>");

        // Order Summary
        BigDecimal total = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal discount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal finalAmount = order.getFinalAmount() != null ? order.getFinalAmount() : BigDecimal.ZERO;

        sb.append("<div style=\"margin-top: 16px; float: right; width: 260px;\">");
        sb.append("<p style=\"margin: 4px 0; display: flex; justify-content: space-between;\"><span>Tổng tiền hàng:</span> <strong>").append(currencyFmt.format(total)).append(" đ</strong></p>");
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("<p style=\"margin: 4px 0; display: flex; justify-content: space-between; color: #d93025;\"><span>Giảm giá:</span> <strong>-").append(currencyFmt.format(discount)).append(" đ</strong></p>");
        }
        sb.append("<p style=\"margin: 4px 0; display: flex; justify-content: space-between;\"><span>Phí vận chuyển:</span> <em>0 đ (ĐVVC thu riêng)</em></p>");
        sb.append("<p style=\"margin: 8px 0 0; display: flex; justify-content: space-between; border-top: 2px solid #333; padding-top: 4px; font-size: 16px;\"><span>Tổng thanh toán:</span> <strong style=\"color: #1a73e8;\">").append(currencyFmt.format(finalAmount)).append(" đ</strong></p>");
        sb.append("</div>");
        sb.append("<div style=\"clear: both;\"></div>");

        // Link for customers with account
        if (order.getCustomer() != null && order.getCustomer().getUser() != null) {
            String orderUrl = publicBaseUrl.replaceAll("/+$", "") + "/orders/" + order.getId();
            sb.append("<div style=\"text-align: center; margin-top: 28px;\">");
            sb.append("<a href=\"").append(HtmlUtils.htmlEscape(orderUrl)).append("\" style=\"background: #1a73e8; color: #ffffff; text-decoration: none; padding: 10px 20px; border-radius: 4px; font-weight: bold; display: inline-block;\">Xem chi tiết đơn hàng</a>");
            sb.append("</div>");
        }

        sb.append("<div style=\"margin-top: 36px; padding-top: 16px; border-top: 1px solid #eaeaea; font-size: 12px; color: #777; text-align: center;\">");
        sb.append("<p style=\"margin: 0;\">Cảm ơn bạn đã tin tưởng mua sắm tại F6S Store.</p>");
        sb.append("<p style=\"margin: 4px 0 0;\">Đây là email tự động, vui lòng không trả lời trực tiếp email này.</p>");
        sb.append("</div>");

        sb.append("</body></html>");
        return sb.toString();
    }
}
