package com.example.demodatn.email;

import com.example.demodatn.service.donhang.OrderEmailService.EmailItemView;
import com.example.demodatn.service.donhang.QrCodeGenerator;
import com.example.demodatn.service.donhang.VietQrService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class EmailPreviewGeneratorTest {

    @Autowired private SpringTemplateEngine templateEngine;
    @Autowired private QrCodeGenerator qrCodeGenerator;

    private static final String PREVIEW_DIR = "docs/email-preview";

    @Test
    void generateAllPreviewHtmlFiles() throws Exception {
        File dir = new File(PREVIEW_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // Lấy base64 logo để preview trong trình duyệt
        String logoBase64 = "";
        try {
            ClassPathResource res = new ClassPathResource("static/images/logo.jpg");
            if (res.exists()) {
                try (InputStream is = res.getInputStream()) {
                    logoBase64 = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(is.readAllBytes());
                }
            }
        } catch (Exception ignored) {}

        // 1. tai-khoan-khach.html
        renderTaiKhoan("tai-khoan-khach.html", logoBase64,
                "Nguyễn Văn An", false, null, "an.nguyen", "K7m9Px2A", false);

        // 2. tai-khoan-nhan-vien.html
        renderTaiKhoan("tai-khoan-nhan-vien.html", logoBase64,
                "Trần Thị Mai", true, "Quản trị viên", "mai.tran", "W3b8Tn5K", false);

        // 3. tai-khoan-gui-lai.html
        renderTaiKhoan("tai-khoan-gui-lai.html", logoBase64,
                "Lê Hoàng Long", false, null, "long.le", "R9k2Mh4C", true);

        // 4. dat-hang-cod.html
        renderDonHangXacNhan("dat-hang-cod.html", logoBase64, false,
                "Phạm Minh Đức", "0912345678", "Số 45 Đường Cầu Giấy, Phường Quan Hoa, Cầu Giấy, Hà Nội",
                "DH001001", "10/11/2026 09:30", "Thanh toán khi nhận hàng (COD)", false, false,
                null, null, null, null, null, null, null,
                List.of(
                        new EmailItemView("Giày Sneaker Nam Sport Classic", "Kích cỡ: 41 · Màu: Trắng · Chất liệu: Da · Đế: Cao su", 1, "950.000 đ", "950.000 đ"),
                        new EmailItemView("Giày Chạy Bộ F6 Pro Air", "Kích cỡ: 42 · Màu: Đen · Chất liệu: Vải dệt · Đế: Phylon", 1, "1.000.000 đ", "1.000.000 đ")
                ),
                "1.950.000 đ", "100.000 đ", "SALE100", "1.850.000 đ");

        // 5. dat-hang-chuyen-khoan.html (Có QR VietQR)
        String fakePayload = VietQrService.payload("970422", "0123456789", "1500000", "DH001002");
        byte[] qrBytes = qrCodeGenerator.generateQrPng(fakePayload, 200, 200);
        String qrBase64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(qrBytes);

        renderDonHangXacNhan("dat-hang-chuyen-khoan.html", logoBase64, false,
                "Đỗ Thị Quỳnh", "0987654321", "123 Đường Nguyễn Trãi, Thanh Xuân, Hà Nội",
                "DH001002", "10/11/2026 10:15", "Chuyển khoản ngân hàng (VietQR)", false, true,
                qrBase64, "MBBank", "0123456789", "F6 SHOP", "1.500.000 đ", "DH001002", "Trước 24 giờ kể từ khi đặt hàng",
                List.of(
                        new EmailItemView("Giày Bóng Rổ F6 Supreme High", "Kích cỡ: 42 · Màu: Đỏ Đen · Chất liệu: Da PU · Đế: Cao su", 1, "1.500.000 đ", "1.500.000 đ")
                ),
                "1.500.000 đ", null, null, "1.500.000 đ");

        // 6. bien-nhan-tai-quay.html (Không theo dõi giao hàng)
        renderDonHangXacNhan("bien-nhan-tai-quay.html", logoBase64, true,
                "Vũ Hải Nam", "0901234567", "",
                "DH001003", "10/11/2026 11:00", "Tiền mặt tại quầy", true, false,
                null, null, null, null, null, null, null,
                List.of(
                        new EmailItemView("Giày Lười Casual F6 Loafer", "Kích cỡ: 40 · Màu: Nâu · Chất liệu: Da bò · Đế: Phíp", 1, "1.200.000 đ", "1.200.000 đ")
                ),
                "1.200.000 đ", null, null, "1.200.000 đ");

        // 7. trang-thai-da-xac-nhan.html
        renderTrangThai("trang-thai-da-xac-nhan.html", logoBase64,
                "CONFIRMED", 2, "Cửa hàng đã xác nhận đơn của bạn và đang tiến hành chuẩn bị đóng gói.",
                null, null, null);

        // 8. trang-thai-dang-chuan-bi.html
        renderTrangThai("trang-thai-dang-chuan-bi.html", logoBase64,
                "PREPARING", 3, "Đơn hàng của bạn đang được nhân viên chuẩn bị và đóng gói cẩn thận.",
                null, null, null);

        // 9. trang-thai-dang-giao.html
        renderTrangThai("trang-thai-dang-giao.html", logoBase64,
                "SHIPPING", 4, "Đơn hàng đã được bàn giao cho đơn vị vận chuyển và đang trên đường giao đến bạn.",
                "Giao Hàng Nhanh (GHN)", "GHN9823412VN", null);

        // 10. trang-thai-da-giao.html
        renderTrangThai("trang-thai-da-giao.html", logoBase64,
                "DELIVERED", 5, "Đơn hàng đã được giao thành công. Cảm ơn bạn đã tin tưởng mua sắm tại F6 Shop!",
                "Giao Hàng Nhanh (GHN)", "GHN9823412VN", null);

        // 11. trang-thai-da-huy.html
        renderTrangThai("trang-thai-da-huy.html", logoBase64,
                "CANCELLED", 1, "Đơn hàng của bạn đã được hủy.",
                null, null, "Khách hàng đổi ý muốn thay đổi kích cỡ giày.");

        // 12. trang-thai-giao-that-bai.html
        renderTrangThai("trang-thai-giao-that-bai.html", logoBase64,
                "DELIVERY_FAILED", 4, "Giao hàng không thành công. Đơn hàng sẽ được chuyển hoàn về kho.",
                "Giao Hàng Nhanh (GHN)", "GHN9823412VN", "Shipper gọi 3 lần không liên lạc được với người nhận.");

        // 13. trang-thai-da-nhan-thanh-toan.html
        renderTrangThai("trang-thai-da-nhan-thanh-toan.html", logoBase64,
                "PAYMENT_CONFIRMED", 2, "Cửa hàng đã nhận được thanh toán chuyển khoản cho đơn hàng của bạn.",
                null, null, null);

        // Xác nhận cả 13 file đều tồn tại
        assertTrue(new File(PREVIEW_DIR, "tai-khoan-khach.html").exists());
        assertTrue(new File(PREVIEW_DIR, "tai-khoan-nhan-vien.html").exists());
        assertTrue(new File(PREVIEW_DIR, "tai-khoan-gui-lai.html").exists());
        assertTrue(new File(PREVIEW_DIR, "dat-hang-cod.html").exists());
        assertTrue(new File(PREVIEW_DIR, "dat-hang-chuyen-khoan.html").exists());
        assertTrue(new File(PREVIEW_DIR, "bien-nhan-tai-quay.html").exists());
        assertTrue(new File(PREVIEW_DIR, "trang-thai-da-xac-nhan.html").exists());
        assertTrue(new File(PREVIEW_DIR, "trang-thai-dang-chuan-bi.html").exists());
        assertTrue(new File(PREVIEW_DIR, "trang-thai-dang-giao.html").exists());
        assertTrue(new File(PREVIEW_DIR, "trang-thai-da-giao.html").exists());
        assertTrue(new File(PREVIEW_DIR, "trang-thai-da-huy.html").exists());
        assertTrue(new File(PREVIEW_DIR, "trang-thai-giao-that-bai.html").exists());
        assertTrue(new File(PREVIEW_DIR, "trang-thai-da-nhan-thanh-toan.html").exists());
    }

    private void renderTaiKhoan(String fileName, String logoBase64, String fullName, boolean isEmployee,
                                String position, String username, String password, boolean isResend) throws Exception {
        Context ctx = new Context(Locale.forLanguageTag("vi"));
        ctx.setVariable("emailSubject", "[F6 Shop] Tài khoản của bạn đã sẵn sàng");
        ctx.setVariable("preheader", "Tài khoản của bạn tại F6 Shop đã sẵn sàng.");
        ctx.setVariable("fullName", fullName);
        ctx.setVariable("isEmployee", isEmployee);
        ctx.setVariable("position", position);
        ctx.setVariable("username", username);
        ctx.setVariable("password", password);
        ctx.setVariable("isResend", isResend);
        ctx.setVariable("loginUrl", "http://localhost:8080/login");
        ctx.setVariable("shopName", "F6 Shop");
        ctx.setVariable("shopAddress", "123 Đường Cầu Giấy, Hà Nội");
        ctx.setVariable("shopHotline", "1900 1234");
        ctx.setVariable("shopSupportEmail", "support@f6shop.com");
        ctx.setVariable("publicUrl", "http://localhost:8080");

        String html = templateEngine.process("email/tai-khoan", ctx);
        if (!logoBase64.isBlank()) {
            html = html.replace("cid:f6s-logo", logoBase64);
        }
        Files.writeString(Paths.get(PREVIEW_DIR, fileName), html);
    }

    private void renderDonHangXacNhan(String fileName, String logoBase64, boolean isStorePickup,
                                      String receiverName, String receiverPhone, String shippingAddress,
                                      String orderCode, String orderTime, String paymentMethodText,
                                      boolean isPaid, boolean hasQrPayment, String qrBase64,
                                      String bankName, String bankAccount, String bankAccountName,
                                      String qrAmountFormatted, String qrContent, String paymentDeadline,
                                      List<EmailItemView> items, String subtotalFormatted,
                                      String voucherDiscount, String voucherCode, String totalFormatted) throws Exception {
        Context ctx = new Context(Locale.forLanguageTag("vi"));
        ctx.setVariable("emailSubject", isStorePickup
                ? "[F6 Shop] Cảm ơn bạn đã mua hàng - Đơn " + orderCode
                : "[F6 Shop] Cảm ơn bạn đã đặt hàng - Đơn " + orderCode);
        ctx.setVariable("preheader", "Xác nhận đơn hàng " + orderCode + " từ F6 Shop");
        ctx.setVariable("isStorePickup", isStorePickup);
        ctx.setVariable("receiverName", receiverName);
        ctx.setVariable("receiverPhone", receiverPhone);
        ctx.setVariable("shippingAddress", shippingAddress);
        ctx.setVariable("orderCode", orderCode);
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
        ctx.setVariable("subtotalFormatted", subtotalFormatted);
        ctx.setVariable("promotionDiscount", "0 đ");
        ctx.setVariable("voucherDiscount", voucherDiscount);
        ctx.setVariable("voucherCode", voucherCode);
        ctx.setVariable("totalFormatted", totalFormatted);
        ctx.setVariable("orderDetailUrl", "http://localhost:8080/orders/1");
        ctx.setVariable("shopName", "F6 Shop");
        ctx.setVariable("shopAddress", "123 Đường Cầu Giấy, Hà Nội");
        ctx.setVariable("shopHotline", "1900 1234");
        ctx.setVariable("shopSupportEmail", "support@f6shop.com");
        ctx.setVariable("publicUrl", "http://localhost:8080");

        String html = templateEngine.process("email/don-hang-xac-nhan", ctx);
        if (!logoBase64.isBlank()) {
            html = html.replace("cid:f6s-logo", logoBase64);
        }
        if (hasQrPayment && qrBase64 != null) {
            html = html.replace("cid:f6s-qr", qrBase64);
        }
        Files.writeString(Paths.get(PREVIEW_DIR, fileName), html);
    }

    private void renderTrangThai(String fileName, String logoBase64, String status, int step,
                                 String statusMessage, String carrier, String trackingCode, String note) throws Exception {
        boolean showProgress = !List.of("CANCELLED", "DELIVERY_FAILED").contains(status);
        String title = switch (status) {
            case "CONFIRMED" -> "Đã xác nhận";
            case "PREPARING" -> "Đang chuẩn bị hàng";
            case "SHIPPING" -> "Đang giao hàng";
            case "DELIVERED" -> "Đã giao hàng thành công";
            case "CANCELLED" -> "Đã hủy đơn hàng";
            case "DELIVERY_FAILED" -> "Giao hàng thất bại";
            case "PAYMENT_CONFIRMED" -> "Đã nhận thanh toán chuyển khoản";
            default -> status;
        };

        Context ctx = new Context(Locale.forLanguageTag("vi"));
        ctx.setVariable("emailSubject", "[F6 Shop] Cập nhật đơn hàng DH001001 - " + title);
        ctx.setVariable("preheader", "Đơn hàng DH001001 có cập nhật mới: " + title);
        ctx.setVariable("statusHeading", "Cập nhật đơn hàng: " + title);
        ctx.setVariable("receiverName", "Phạm Minh Đức");
        ctx.setVariable("statusMessage", statusMessage);
        ctx.setVariable("showProgress", showProgress);
        ctx.setVariable("step", step);
        ctx.setVariable("status", status);
        ctx.setVariable("carrier", carrier);
        ctx.setVariable("trackingCode", trackingCode);
        ctx.setVariable("note", note);
        ctx.setVariable("orderCode", "DH001001");
        ctx.setVariable("totalFormatted", "1.850.000 đ");
        ctx.setVariable("updateTime", "10/11/2026 14:00");
        ctx.setVariable("orderItems", List.of(
                new EmailItemView("Giày Sneaker Nam Sport Classic", "Kích cỡ: 41 · Màu: Trắng", 1, "950.000 đ", "950.000 đ"),
                new EmailItemView("Giày Chạy Bộ F6 Pro Air", "Kích cỡ: 42 · Màu: Đen", 1, "1.000.000 đ", "1.000.000 đ")
        ));
        ctx.setVariable("orderDetailUrl", "http://localhost:8080/orders/1");
        ctx.setVariable("shopName", "F6 Shop");
        ctx.setVariable("shopAddress", "123 Đường Cầu Giấy, Hà Nội");
        ctx.setVariable("shopHotline", "1900 1234");
        ctx.setVariable("shopSupportEmail", "support@f6shop.com");
        ctx.setVariable("publicUrl", "http://localhost:8080");

        String html = templateEngine.process("email/don-hang-trang-thai", ctx);
        if (!logoBase64.isBlank()) {
            html = html.replace("cid:f6s-logo", logoBase64);
        }
        Files.writeString(Paths.get(PREVIEW_DIR, fileName), html);
    }
}
