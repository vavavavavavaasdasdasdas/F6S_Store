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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEmailService {

    private final ObjectProvider<JavaMailSender> senderProvider;
    private final AccountMailService accountMailService;
    private final OrderRepository orderRepository;
    private final OrderMailLogRepository mailLogRepository;
    private final SpringTemplateEngine templateEngine;
    private final VietQrService vietQrService;
    private final QrCodeGenerator qrCodeGenerator;
    private final Clock clock;

    @Value("${shop.name:F6 Shop}") private String shopName;
    @Value("${shop.address:}") private String shopAddress;
    @Value("${shop.hotline:}") private String shopHotline;
    @Value("${shop.support-email:}") private String shopSupportEmail;
    @Value("${shop.public-url:${shop.public-base-url:http://localhost:8080}}") private String publicUrl;
    @Value("${shop.order.bank-transfer-timeout-hours:24}") private long bankTransferTimeoutHours;

    public static final Map<String, String> STATUS_TITLES = Map.of(
            "CONFIRMED", "Đã xác nhận",
            "PREPARING", "Đang chuẩn bị hàng",
            "SHIPPING", "Đang giao hàng",
            "DELIVERED", "Đã giao hàng thành công",
            "CANCELLED", "Đã hủy đơn hàng",
            "DELIVERY_FAILED", "Giao hàng thất bại",
            "PAYMENT_CONFIRMED", "Đã nhận thanh toán chuyển khoản"
    );

    public record EmailItemView(
            String productName,
            String attributesText,
            int quantity,
            String priceFormatted,
            String totalFormatted
    ) {}

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.info("Nhận sự kiện đơn hàng mới: orderId={}", event.orderId());
        sendOrderPlacedConfirmation(event.orderId(), false);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Nhận sự kiện đổi trạng thái đơn hàng: orderId={}, status={}", event.orderId(), event.status());
        sendNotification(event.orderId(), event.status(), event.note(), event.carrier(), event.trackingCode(), false);
    }

    public void resend(Integer orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại."));
        if ("PLACED".equalsIgnoreCase(status)) {
            sendOrderPlacedConfirmation(orderId, true);
        } else {
            sendNotification(orderId, status, null, null, null, true);
        }
    }

    @Transactional(readOnly = true)
    public void sendOrderPlacedConfirmation(Integer orderId, boolean isResend) {
        Order order = orderRepository.findWithDetailsById(orderId).orElse(null);
        if (order == null) {
            log.warn("Không tìm thấy đơn hàng id={} để gửi thư xác nhận đặt hàng", orderId);
            return;
        }

        boolean isOnline = "ONLINE".equalsIgnoreCase(order.getOrderType());
        boolean hasDelivery = order.getShippingAddress() != null
                && !order.getShippingAddress().isBlank()
                && !"Tại quầy".equalsIgnoreCase(order.getShippingAddress().trim());
        boolean isStorePickup = !isOnline && !hasDelivery;

        String recipientEmail = resolveRecipientEmail(order);
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneId.systemDefault());

        // Đơn tại quầy không giao hàng chỉ gửi khi khách đã chọn có email
        if (isStorePickup && (recipientEmail == null || recipientEmail.isBlank())) {
            log.info("Bỏ qua gửi email biên nhận tại quầy do đơn hàng {} không có email khách hàng", order.getOrderCode());
            return;
        }

        // Kiểm tra gửi trùng lặp nếu không phải resend (đảm bảo đúng MỘT thư PLACED)
        if (!isResend) {
            var existing = mailLogRepository.findByOrder_IdAndStatus(orderId, "PLACED");
            if (existing.isPresent() && existing.get().isThanhCong()) {
                log.info("Email xác nhận đặt hàng PLACED cho đơn {} đã gửi thành công trước đó, bỏ qua.", orderId);
                return;
            }
        }

        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.info("Đơn hàng {} không có email nhận thư xác nhận đặt hàng.", order.getOrderCode());
            saveLog(order, "PLACED", null, now, false, "Không có email");
            return;
        }

        if (!accountMailService.isConfigured()) {
            log.warn("Chưa cấu hình máy chủ email, không thể gửi xác nhận đặt hàng cho đơn {}", order.getOrderCode());
            saveLog(order, "PLACED", recipientEmail, now, false, "Chưa cấu hình máy chủ email");
            return;
        }

        try {
            JavaMailSender mailSender = senderProvider.getIfAvailable();
            if (mailSender == null) {
                saveLog(order, "PLACED", recipientEmail, now, false, "Không thể khởi tạo JavaMailSender");
                return;
            }

            String fromAddress = accountMailService.getResolvedFrom();
            String currentShopName = (shopName != null && !shopName.isBlank()) ? shopName.trim() : "F6 Shop";
            String subject = isStorePickup
                    ? "[" + currentShopName + "] Cảm ơn bạn đã mua hàng - Đơn " + order.getOrderCode()
                    : "[" + currentShopName + "] Cảm ơn bạn đã đặt hàng - Đơn " + order.getOrderCode();

            DecimalFormat currencyFmt = new DecimalFormat("#,###");
            DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String orderTime = order.getCreatedAt() != null ? order.getCreatedAt().format(timeFmt) : now.format(timeFmt);

            // Bảng sản phẩm
            List<EmailItemView> items = buildItemViews(order, currencyFmt);

            // Phương thức & trạng thái thanh toán
            String paymentMethodText = resolvePaymentMethodName(order.getPaymentMethod());
            boolean isPaid = "PAID".equalsIgnoreCase(order.getPaymentStatus());

            // Xử lý VietQR nếu là chuyển khoản chưa thanh toán
            boolean isBankTransfer = "TRANSFER".equalsIgnoreCase(order.getPaymentMethod()) || "BANK_TRANSFER".equalsIgnoreCase(order.getPaymentMethod());
            boolean hasQrPayment = isBankTransfer && !isPaid;
            byte[] qrBytes = null;
            String bankName = "";
            String bankAccount = "";
            String bankAccountName = "";
            String qrAmountFormatted = "";
            String qrContent = "";
            String paymentDeadline = null;

            if (hasQrPayment) {
                var qrData = vietQrService.create(order.getFinalAmount(), order.getOrderCode());
                if (qrData != null && qrData.configured() && qrData.payload() != null) {
                    try {
                        qrBytes = qrCodeGenerator.generateQrPng(qrData.payload(), 200, 200);
                        bankName = qrData.bankName();
                        bankAccount = qrData.account();
                        bankAccountName = qrData.accountName();
                        qrAmountFormatted = currencyFmt.format(order.getFinalAmount()) + " đ";
                        qrContent = qrData.content();
                        paymentDeadline = "Trước 24 giờ kể từ khi đặt hàng";
                    } catch (Exception ex) {
                        log.warn("Không thể sinh mã VietQR cho đơn {}: {}", order.getOrderCode(), ex.getMessage());
                        hasQrPayment = false;
                    }
                } else {
                    hasQrPayment = false;
                }
            }

            // Tính tạm tính, giảm giá
            BigDecimal subtotal = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal discount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
            BigDecimal finalAmount = order.getFinalAmount() != null ? order.getFinalAmount() : BigDecimal.ZERO;

            String voucherCode = (order.getVoucher() != null) ? order.getVoucher().getCode() : null;
            String voucherDiscount = (discount.compareTo(BigDecimal.ZERO) > 0) ? currencyFmt.format(discount) + " đ" : null;

            String receiverName = order.getReceiverName() != null && !order.getReceiverName().isBlank()
                    ? order.getReceiverName()
                    : (order.getCustomer() != null ? order.getCustomer().getFullName() : "Quý khách");

            String receiverPhone = order.getPhoneNumber() != null && !order.getPhoneNumber().isBlank()
                    ? order.getPhoneNumber()
                    : (order.getCustomer() != null ? order.getCustomer().getPhone() : "");

            String shippingAddress = order.getShippingAddress() != null ? order.getShippingAddress() : "";

            String orderDetailUrl = (order.getCustomer() != null && order.getCustomer().getUser() != null)
                    ? publicUrl.replaceAll("/+$", "") + "/orders/" + order.getId()
                    : publicUrl.replaceAll("/+$", "");

            // Thymeleaf context
            Context ctx = new Context(Locale.forLanguageTag("vi"));
            ctx.setVariable("emailSubject", subject);
            ctx.setVariable("preheader", "Xác nhận đơn hàng " + order.getOrderCode() + " từ " + currentShopName);
            ctx.setVariable("isStorePickup", isStorePickup);
            ctx.setVariable("receiverName", receiverName);
            ctx.setVariable("receiverPhone", receiverPhone);
            ctx.setVariable("shippingAddress", shippingAddress);
            ctx.setVariable("orderCode", order.getOrderCode());
            ctx.setVariable("orderTime", orderTime);
            ctx.setVariable("paymentMethodText", paymentMethodText);
            ctx.setVariable("isPaid", isPaid);
            ctx.setVariable("hasQrPayment", hasQrPayment);
            ctx.setVariable("bankName", bankName);
            ctx.setVariable("bankAccount", bankAccount);
            ctx.setVariable("bankAccountName", bankAccountName);
            ctx.setVariable("qrAmountFormatted", qrAmountFormatted);
            ctx.setVariable("qrContent", qrContent);
            ctx.setVariable("paymentDeadline", paymentDeadline);
            ctx.setVariable("orderItems", items);
            ctx.setVariable("subtotalFormatted", currencyFmt.format(subtotal) + " đ");
            ctx.setVariable("promotionDiscount", "0 đ");
            ctx.setVariable("voucherDiscount", voucherDiscount);
            ctx.setVariable("voucherCode", voucherCode);
            ctx.setVariable("totalFormatted", currencyFmt.format(finalAmount) + " đ");
            ctx.setVariable("orderDetailUrl", orderDetailUrl);
            ctx.setVariable("shopName", currentShopName);
            ctx.setVariable("shopAddress", shopAddress);
            ctx.setVariable("shopHotline", shopHotline);
            ctx.setVariable("shopSupportEmail", shopSupportEmail);
            ctx.setVariable("publicUrl", publicUrl);

            String html = templateEngine.process("email/don-hang-xac-nhan", ctx);

            // Plain text tương ứng
            StringBuilder textBuilder = new StringBuilder();
            textBuilder.append("Xin chào ").append(receiverName).append(",\n\n");
            if (isStorePickup) {
                textBuilder.append("Cảm ơn bạn đã tin tưởng mua sắm trực tiếp tại ").append(currentShopName).append(".\n\n");
            } else {
                textBuilder.append("Chúng tôi đã nhận được đơn hàng của bạn tại ").append(currentShopName).append(".\n\n");
            }
            textBuilder.append("THÔNG TIN ĐƠN HÀNG:\n")
                    .append("- Mã đơn hàng: ").append(order.getOrderCode()).append("\n")
                    .append("- Thời gian đặt: ").append(orderTime).append("\n")
                    .append("- Hình thức thanh toán: ").append(paymentMethodText).append("\n")
                    .append("- Tình trạng thanh toán: ").append(isPaid ? "Đã thanh toán" : "Chờ thanh toán").append("\n");

            if (!isStorePickup && !shippingAddress.isBlank()) {
                textBuilder.append("- Người nhận: ").append(receiverName).append(" (").append(receiverPhone).append(")\n")
                        .append("- Địa chỉ nhận hàng: ").append(shippingAddress).append("\n");
            }

            textBuilder.append("\nCHI TIẾT SẢN PHẨM:\n");
            for (var it : items) {
                textBuilder.append("• ").append(it.productName()).append(" [").append(it.attributesText()).append("] x ")
                        .append(it.quantity()).append(" = ").append(it.totalFormatted()).append("\n");
            }

            textBuilder.append("\nTỔNG KẾT:\n")
                    .append("- Tạm tính: ").append(currencyFmt.format(subtotal)).append(" đ\n");
            if (voucherDiscount != null) {
                textBuilder.append("- Giảm giá phiếu (").append(voucherCode != null ? voucherCode : "").append("): -").append(voucherDiscount).append("\n");
            }
            textBuilder.append("- Phí vận chuyển do đơn vị vận chuyển tính và thu riêng.\n")
                    .append("- Tổng thanh toán: ").append(currencyFmt.format(finalAmount)).append(" đ\n\n");

            if (hasQrPayment) {
                textBuilder.append("THÔNG TIN CHUYỂN KHOẢN:\n")
                        .append("- Ngân hàng: ").append(bankName).append("\n")
                        .append("- Số tài khoản: ").append(bankAccount).append("\n")
                        .append("- Chủ tài khoản: ").append(bankAccountName).append("\n")
                        .append("- Số tiền: ").append(qrAmountFormatted).append("\n")
                        .append("- Nội dung CK: ").append(qrContent).append("\n")
                        .append("- Hạn thanh toán: ").append(paymentDeadline).append("\n\n");
            }

            textBuilder.append("Xem chi tiết đơn hàng: ").append(orderDetailUrl).append("\n\n")
                    .append("Trân trọng,\n").append(currentShopName);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(textBuilder.toString(), html);

            // Inline logo
            try {
                ClassPathResource logoResource = new ClassPathResource("static/images/logo.jpg");
                if (logoResource.exists()) {
                    helper.addInline("f6s-logo", logoResource, "image/jpeg");
                }
            } catch (Exception ex) {
                log.warn("Không thể đính kèm logo email: {}", ex.getMessage());
            }

            // Inline QR
            if (hasQrPayment && qrBytes != null && qrBytes.length > 0) {
                try {
                    helper.addInline("f6s-qr", new ByteArrayResource(qrBytes), "image/png");
                } catch (Exception ex) {
                    log.warn("Không thể đính kèm ảnh QR email: {}", ex.getMessage());
                }
            }

            mailSender.send(message);
            log.info("Đã gửi email xác nhận đặt hàng thành công cho đơn {} tới {}", order.getOrderCode(), recipientEmail);
            saveLog(order, "PLACED", recipientEmail, now, true, null);

        } catch (Exception ex) {
            String errorMsg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            log.error("Lỗi gửi email xác nhận đặt hàng cho đơn {}: {}", order.getOrderCode(), errorMsg);
            saveLog(order, "PLACED", recipientEmail, now, false, "Lỗi gửi: " + errorMsg);
        }
    }

    @Transactional(readOnly = true)
    public void sendNotification(Integer orderId, String status, String note, String carrier, String trackingCode, boolean isResend) {
        if (!STATUS_TITLES.containsKey(status)) {
            log.debug("Trạng thái {} không thuộc danh sách gửi email thông báo.", status);
            return;
        }

        Order order = orderRepository.findWithDetailsById(orderId).orElse(null);
        if (order == null) {
            log.warn("Không tìm thấy đơn hàng id={}", orderId);
            return;
        }

        // Chỉ gửi cho đơn hàng trực tuyến hoặc đơn có giao hàng
        boolean isOnline = "ONLINE".equalsIgnoreCase(order.getOrderType());
        boolean hasDelivery = order.getShippingAddress() != null
                && !order.getShippingAddress().isBlank()
                && !"Tại quầy".equalsIgnoreCase(order.getShippingAddress().trim());
        if (!isOnline && !hasDelivery) {
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
            String currentShopName = (shopName != null && !shopName.isBlank()) ? shopName.trim() : "F6 Shop";
            String statusTitle = STATUS_TITLES.getOrDefault(status, status);
            String subject = "[" + currentShopName + "] Cập nhật đơn hàng " + order.getOrderCode() + " - " + statusTitle;

            DecimalFormat currencyFmt = new DecimalFormat("#,###");
            DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String updateTime = now.format(timeFmt);

            // Xác định bước tiến trình (1: Đã đặt, 2: Xác nhận, 3: Chuẩn bị, 4: Đang giao, 5: Đã giao)
            boolean showProgress = !Set.of("CANCELLED", "DELIVERY_FAILED").contains(status);
            int step = resolveProgressStep(status);

            String statusHeading = "Cập nhật đơn hàng: " + statusTitle;
            String statusMessage = resolveStatusMessage(status);

            List<EmailItemView> items = buildItemViews(order, currencyFmt);

            String receiverName = order.getReceiverName() != null && !order.getReceiverName().isBlank()
                    ? order.getReceiverName()
                    : (order.getCustomer() != null ? order.getCustomer().getFullName() : "Quý khách");

            String orderDetailUrl = (order.getCustomer() != null && order.getCustomer().getUser() != null)
                    ? publicUrl.replaceAll("/+$", "") + "/orders/" + order.getId()
                    : publicUrl.replaceAll("/+$", "");

            BigDecimal finalAmount = order.getFinalAmount() != null ? order.getFinalAmount() : BigDecimal.ZERO;

            Context ctx = new Context(Locale.forLanguageTag("vi"));
            ctx.setVariable("emailSubject", subject);
            ctx.setVariable("preheader", "Đơn hàng " + order.getOrderCode() + " có cập nhật mới: " + statusTitle);
            ctx.setVariable("statusHeading", statusHeading);
            ctx.setVariable("receiverName", receiverName);
            ctx.setVariable("statusMessage", statusMessage);
            ctx.setVariable("showProgress", showProgress);
            ctx.setVariable("step", step);
            ctx.setVariable("status", status);
            ctx.setVariable("carrier", carrier);
            ctx.setVariable("trackingCode", trackingCode);
            ctx.setVariable("note", note);
            ctx.setVariable("orderCode", order.getOrderCode());
            ctx.setVariable("totalFormatted", currencyFmt.format(finalAmount) + " đ");
            ctx.setVariable("updateTime", updateTime);
            ctx.setVariable("orderItems", items);
            ctx.setVariable("orderDetailUrl", orderDetailUrl);
            ctx.setVariable("shopName", currentShopName);
            ctx.setVariable("shopAddress", shopAddress);
            ctx.setVariable("shopHotline", shopHotline);
            ctx.setVariable("shopSupportEmail", shopSupportEmail);
            ctx.setVariable("publicUrl", publicUrl);

            String html = templateEngine.process("email/don-hang-trang-thai", ctx);

            // Plain text tương ứng
            StringBuilder textBuilder = new StringBuilder();
            textBuilder.append("Xin chào ").append(receiverName).append(",\n\n")
                    .append("Đơn hàng #").append(order.getOrderCode()).append(" của bạn hiện có cập nhật mới:\n")
                    .append("- Trạng thái: ").append(statusTitle).append("\n")
                    .append("- Thời gian: ").append(updateTime).append("\n");

            if (carrier != null && !carrier.isBlank()) {
                textBuilder.append("- Đơn vị vận chuyển: ").append(carrier).append("\n");
            }
            if (trackingCode != null && !trackingCode.isBlank()) {
                textBuilder.append("- Mã vận đơn: ").append(trackingCode).append("\n");
            }
            if (note != null && !note.isBlank()) {
                textBuilder.append("- Ghi chú / Lý do: ").append(note).append("\n");
            }

            textBuilder.append("\nXem chi tiết đơn hàng: ").append(orderDetailUrl).append("\n\n")
                    .append("Trân trọng,\n").append(currentShopName);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(textBuilder.toString(), html);

            try {
                ClassPathResource logoResource = new ClassPathResource("static/images/logo.jpg");
                if (logoResource.exists()) {
                    helper.addInline("f6s-logo", logoResource, "image/jpeg");
                }
            } catch (Exception ex) {
                log.warn("Không thể đính kèm logo email: {}", ex.getMessage());
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

    private int resolveProgressStep(String status) {
        return switch (status) {
            case "PENDING" -> 1;
            case "CONFIRMED", "PAYMENT_CONFIRMED" -> 2;
            case "PREPARING" -> 3;
            case "SHIPPING" -> 4;
            case "DELIVERED" -> 5;
            default -> 1;
        };
    }

    private String resolveStatusMessage(String status) {
        return switch (status) {
            case "CONFIRMED" -> "Cửa hàng đã xác nhận đơn của bạn và đang tiến hành chuẩn bị đóng gói.";
            case "PREPARING" -> "Đơn hàng của bạn đang được nhân viên chuẩn bị và đóng gói cẩn thận.";
            case "SHIPPING" -> "Đơn hàng đã được bàn giao cho đơn vị vận chuyển và đang trên đường giao đến bạn.";
            case "DELIVERED" -> "Đơn hàng đã được giao thành công. Cảm ơn bạn đã tin tưởng mua sắm tại " + ((shopName != null && !shopName.isBlank()) ? shopName.trim() : "F6 Shop") + "!";
            case "CANCELLED" -> "Đơn hàng của bạn đã được hủy.";
            case "DELIVERY_FAILED" -> "Giao hàng không thành công. Đơn hàng sẽ được chuyển hoàn về kho.";
            case "PAYMENT_CONFIRMED" -> "Cửa hàng đã nhận được thanh toán chuyển khoản cho đơn hàng của bạn.";
            default -> "Đơn hàng của bạn đã được cập nhật trạng thái: " + STATUS_TITLES.getOrDefault(status, status);
        };
    }

    private List<EmailItemView> buildItemViews(Order order, DecimalFormat currencyFmt) {
        List<EmailItemView> items = new ArrayList<>();
        if (order.getOrderDetails() != null) {
            for (OrderDetail d : order.getOrderDetails()) {
                String pName = d.getProductName() != null ? d.getProductName() : "Sản phẩm";
                List<String> attrs = new ArrayList<>();
                if (d.getSizeName() != null && !d.getSizeName().isBlank()) attrs.add("Kích cỡ: " + d.getSizeName());
                if (d.getColorName() != null && !d.getColorName().isBlank()) attrs.add("Màu: " + d.getColorName());
                if (d.getMaterialName() != null && !d.getMaterialName().isBlank()) attrs.add("Chất liệu: " + d.getMaterialName());
                if (d.getSoleName() != null && !d.getSoleName().isBlank()) attrs.add("Đế: " + d.getSoleName());
                String attrStr = attrs.isEmpty() ? "—" : String.join(" · ", attrs);

                int qty = d.getQuantity() != null ? d.getQuantity() : 1;
                BigDecimal price = d.getPrice() != null ? d.getPrice() : BigDecimal.ZERO;
                BigDecimal total = d.getTotalPrice() != null ? d.getTotalPrice() : price.multiply(BigDecimal.valueOf(qty));

                items.add(new EmailItemView(pName, attrStr, qty, currencyFmt.format(price) + " đ", currencyFmt.format(total) + " đ"));
            }
        }
        return items;
    }

    private String resolvePaymentMethodName(String method) {
        if (method == null) return "Không xác định";
        return switch (method.toUpperCase(Locale.ROOT)) {
            case "COD" -> "Thanh toán khi nhận hàng (COD)";
            case "TRANSFER", "BANK_TRANSFER" -> "Chuyển khoản ngân hàng (VietQR)";
            case "CASH" -> "Tiền mặt tại quầy";
            case "COMBINED" -> "Tiền mặt kết hợp Chuyển khoản";
            default -> method;
        };
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
}
