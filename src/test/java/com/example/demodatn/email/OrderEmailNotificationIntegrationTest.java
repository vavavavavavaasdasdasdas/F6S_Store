package com.example.demodatn.email;

import com.example.demodatn.TestDatabaseGuard;
import com.example.demodatn.dto.donhang.CartItemDto;
import com.example.demodatn.dto.donhang.CheckoutRequest;
import com.example.demodatn.entity.donhang.Order;
import com.example.demodatn.entity.donhang.OrderMailLog;
import com.example.demodatn.entity.donhang.Province;
import com.example.demodatn.entity.donhang.Ward;
import com.example.demodatn.entity.khachhang.Customer;
import com.example.demodatn.entity.nhanvien.Employee;
import com.example.demodatn.entity.sanpham.*;
import com.example.demodatn.repository.donhang.OrderMailLogRepository;
import com.example.demodatn.repository.donhang.OrderRepository;
import com.example.demodatn.repository.donhang.ProvinceRepository;
import com.example.demodatn.repository.donhang.WardRepository;
import com.example.demodatn.repository.khachhang.CustomerRepository;
import com.example.demodatn.repository.nhanvien.EmployeeRepository;
import com.example.demodatn.repository.sanpham.*;
import com.example.demodatn.service.donhang.CheckoutService;
import com.example.demodatn.service.donhang.OrderEmailService;
import com.example.demodatn.service.donhang.OrderService;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@WithMockUser(username = "admin", roles = "ADMIN")
public class OrderEmailNotificationIntegrationTest {

    static GreenMail smtp = new GreenMail(new ServerSetup(0, "127.0.0.1", ServerSetup.PROTOCOL_SMTP));
    static {
        smtp.start();
    }

    @DynamicPropertySource
    static void configureMail(DynamicPropertyRegistry r) {
        r.add("spring.mail.host", () -> "127.0.0.1");
        r.add("spring.mail.port", () -> smtp.getSmtp().getPort());
        r.add("spring.mail.username", () -> "");
        r.add("spring.mail.password", () -> "");
        r.add("spring.mail.properties.mail.smtp.auth", () -> false);
        r.add("spring.mail.properties.mail.smtp.starttls.enable", () -> false);
        r.add("spring.mail.properties.mail.smtp.starttls.required", () -> false);
        r.add("spring.mail.properties.mail.smtp.connectiontimeout", () -> 2000);
        r.add("spring.mail.properties.mail.smtp.timeout", () -> 2000);
        r.add("shop.mail.from", () -> "f6shop-test@example.com");
        r.add("shop.bank.bin", () -> "970422");
        r.add("shop.bank.account", () -> "0123456789");
        r.add("shop.bank.account-name", () -> "F6 SHOP");
        r.add("shop.bank.name", () -> "MBBank");
    }

    @Autowired private CheckoutService checkoutService;
    @Autowired private OrderService orderService;
    @Autowired private OrderEmailService orderEmailService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderMailLogRepository mailLogRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private SizeRepository sizeRepository;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private ColorRepository colorRepository;
    @Autowired private SoleRepository soleRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private ProvinceRepository provinceRepository;
    @Autowired private WardRepository wardRepository;
    @Autowired private JdbcTemplate jdbc;

    private Employee employee;
    private Product testProduct;
    private ProductVariant testVariant;
    private Province province;
    private Ward ward;

    private final List<Integer> createdOrderIds = new ArrayList<>();
    private final List<Integer> createdCustomerIds = new ArrayList<>();
    private final List<Integer> createdVariantIds = new ArrayList<>();
    private final List<Integer> createdProductIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        TestDatabaseGuard.check(jdbc);

        employee = employeeRepository.findByUser_Username("admin")
                .orElseGet(() -> employeeRepository.findAll().get(0));

        testProduct = new Product();
        testProduct.setName("Giày Test Email G27");
        testProduct.setCode("SP-G27-" + UUID.randomUUID().toString().substring(0, 8));
        testProduct = productRepository.save(testProduct);
        createdProductIds.add(testProduct.getId());

        Size size = sizeRepository.findAll().get(0);
        Material mat = materialRepository.findAll().get(0);
        Color color = colorRepository.findAll().get(0);
        Sole sole = soleRepository.findAll().get(0);

        testVariant = new ProductVariant();
        testVariant.setProduct(testProduct);
        testVariant.setSize(size);
        testVariant.setMaterial(mat);
        testVariant.setColor(color);
        testVariant.setSole(sole);
        testVariant.setPrice(new BigDecimal("500000"));
        testVariant.setQuantity(50);
        testVariant = variantRepository.saveAndFlush(testVariant);
        createdVariantIds.add(testVariant.getId());

        province = provinceRepository.findAll().get(0);
        ward = wardRepository.findAll().stream()
                .filter(w -> w.getProvince().getId().equals(province.getId()))
                .findFirst()
                .orElseGet(() -> wardRepository.findAll().get(0));
    }

    @AfterEach
    void tearDown() {
        // DỌN SẠCH DỮ LIỆU KIỂM THỬ ĐÃ TẠO
        for (Integer oid : createdOrderIds) {
            try {
                jdbc.update("DELETE FROM order_mail_log WHERE order_id = ?", oid);
                jdbc.update("DELETE FROM invoices WHERE order_id = ?", oid);
                jdbc.update("DELETE FROM order_status_history WHERE order_id = ?", oid);
                jdbc.update("DELETE FROM order_details WHERE order_id = ?", oid);
                jdbc.update("DELETE FROM orders WHERE id = ?", oid);
            } catch (Exception ignored) {}
        }
        for (Integer cid : createdCustomerIds) {
            try {
                jdbc.update("DELETE FROM customers WHERE id = ?", cid);
            } catch (Exception ignored) {}
        }
        for (Integer vid : createdVariantIds) {
            try {
                jdbc.update("DELETE FROM product_variants WHERE id = ?", vid);
            } catch (Exception ignored) {}
        }
        for (Integer pid : createdProductIds) {
            try {
                jdbc.update("DELETE FROM products WHERE id = ?", pid);
            } catch (Exception ignored) {}
        }
        createdOrderIds.clear();
        createdCustomerIds.clear();
        createdVariantIds.clear();
        createdProductIds.clear();
    }

    @AfterAll
    static void stopSmtp() {
        smtp.stop();
    }

    private CartItemDto createCartItem(ProductVariant v, int quantity) {
        return new CartItemDto(
                v.getProduct().getId(),
                v.getId(),
                v.getProduct().getName(),
                v.getSize().getSizeValue(),
                BigDecimal.ONE,
                quantity
        );
    }

    private Optional<OrderMailLog> awaitMailLog(Integer orderId, String status) {
        long end = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < end) {
            var opt = mailLogRepository.findByOrder_IdAndStatus(orderId, status);
            if (opt.isPresent()) return opt;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return mailLogRepository.findByOrder_IdAndStatus(orderId, status);
    }

    @Test
    void testWebCheckoutCodSendsSinglePlacedEmail() throws Exception {
        int before = smtp.getReceivedMessages().length;
        String testEmail = "buyer_cod_" + UUID.randomUUID().toString().substring(0, 6) + "@example.com";

        CheckoutRequest req = new CheckoutRequest();
        req.setReceiverName("Khách Mua COD <script>");
        req.setPhone("0912345678");
        req.setEmail(testEmail);
        req.setPaymentMethod("COD");
        req.setProvinceId(province.getId());
        req.setWardId(ward.getId());
        req.setStreetAddress("Số 10 Đường Test COD");

        Order order = checkoutService.place(req, List.of(createCartItem(testVariant, 1)), null, null);
        assertNotNull(order.getId());
        createdOrderIds.add(order.getId());

        // Kiểm tra nhận được thư PLACED
        assertTrue(smtp.waitForIncomingEmail(6000, before + 1), "GreenMail phải nhận được email đặt hàng COD");
        MimeMessage msg = smtp.getReceivedMessages()[before];

        // Tiêu đề
        assertEquals("[F6 Shop] Cảm ơn bạn đã đặt hàng - Đơn " + order.getOrderCode(), msg.getSubject());
        assertEquals(testEmail, msg.getAllRecipients()[0].toString());

        String html = getPart(msg, "text/html");
        String text = getPart(msg, "text/plain");

        assertTrue(html.contains("Thanh toán khi nhận hàng (COD)") || text.contains("Thanh toán khi nhận hàng (COD)"));
        assertTrue(html.contains("Phí vận chuyển do đơn vị vận chuyển tính và thu riêng") || text.contains("Phí vận chuyển do đơn vị vận chuyển tính và thu riêng"));
        assertTrue(html.contains("&lt;script&gt;"), "Tên khách chứa HTML phải được escape trong HTML");
        assertFalse(html.contains("<script>"), "HTML không được chứa tag script thô");

        // Kiểm tra đúng MỘT thư PLACED trong log
        var logOpt = awaitMailLog(order.getId(), "PLACED");
        assertTrue(logOpt.isPresent());
        assertTrue(logOpt.get().isThanhCong());
        assertEquals(1, mailLogRepository.findByOrder_Id(order.getId()).stream()
                .filter(l -> "PLACED".equals(l.getStatus())).count());
    }

    @Test
    void testWebCheckoutBankTransferHasVietQrAndCorrectContent() throws Exception {
        int before = smtp.getReceivedMessages().length;
        String testEmail = "buyer_qr_" + UUID.randomUUID().toString().substring(0, 6) + "@example.com";

        CheckoutRequest req = new CheckoutRequest();
        req.setReceiverName("Khách Mua VietQR");
        req.setPhone("0988776655");
        req.setEmail(testEmail);
        req.setPaymentMethod("TRANSFER");
        req.setProvinceId(province.getId());
        req.setWardId(ward.getId());
        req.setStreetAddress("Số 20 Đường Chuyển Khoản");

        Order order = checkoutService.place(req, List.of(createCartItem(testVariant, 2)), null, null);
        assertNotNull(order.getId());
        createdOrderIds.add(order.getId());

        assertTrue(smtp.waitForIncomingEmail(6000, before + 1), "GreenMail phải nhận được email đặt hàng VietQR");
        MimeMessage msg = smtp.getReceivedMessages()[before];

        String html = getPart(msg, "text/html");
        String text = getPart(msg, "text/plain");
        assertTrue(html.contains("Thông tin chuyển khoản qua VietQR") || text.contains("THÔNG TIN CHUYỂN KHOẢN"));
        assertTrue(html.contains(order.getOrderCode()) || text.contains(order.getOrderCode()), "Nội dung CK phải chứa mã đơn");

        // Tìm ảnh QR đính kèm inline (cid:f6s-qr) và giải mã bằng ZXing
        BufferedImage qrImage = extractInlineImage(msg, "f6s-qr");
        assertNotNull(qrImage, "Phải có ảnh QR đính kèm inline với Content-ID f6s-qr");

        // Giải mã QR code
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(qrImage)));
        var result = new MultiFormatReader().decode(bitmap);
        assertNotNull(result);
        String decodedText = result.getText();

        // Chuỗi VietQR payload phải chứa số tiền 1000000 và mã đơn order.getOrderCode()
        assertTrue(decodedText.contains("1000000"), "QR payload phải chứa số tiền đúng");
        assertTrue(decodedText.contains(order.getOrderCode()), "QR payload phải chứa mã đơn hàng");
    }

    @Test
    void testPosInStoreWithEmailSendsReceipt() throws Exception {
        int before = smtp.getReceivedMessages().length;
        String testEmail = "pos_receipt_" + UUID.randomUUID().toString().substring(0, 6) + "@example.com";

        Customer customer = new Customer();
        customer.setCustomerCode("KH-TEST-" + UUID.randomUUID().toString().substring(0, 6));
        customer.setFullName("Khách Mua Tại Quầy");
        customer.setPhone("0905123456");
        customer.setEmail(testEmail);
        customer.setStatus(true);
        customer = customerRepository.saveAndFlush(customer);
        createdCustomerIds.add(customer.getId());

        Order order = orderService.createPosOrder(
                List.of(createCartItem(testVariant, 1)),
                customer,
                employee,
                null,
                "CASH",
                "Tại quầy",
                new BigDecimal("500000")
        );
        createdOrderIds.add(order.getId());

        assertTrue(smtp.waitForIncomingEmail(6000, before + 1), "GreenMail phải nhận được email biên nhận tại quầy");
        MimeMessage msg = smtp.getReceivedMessages()[before];

        assertEquals("[F6 Shop] Cảm ơn bạn đã mua hàng - Đơn " + order.getOrderCode(), msg.getSubject());
        String html = getPart(msg, "text/html");
        assertTrue(html.contains("Cảm ơn bạn đã tin tưởng mua sắm trực tiếp tại cửa hàng F6 Shop"));
        // Biên nhận tại quầy không có thông tin địa chỉ giao hàng
        assertFalse(html.contains("Địa chỉ nhận hàng:"));
    }

    @Test
    void testPosInStoreWithoutEmailDoesNotSend() throws Exception {
        int before = smtp.getReceivedMessages().length;

        // Đơn tại quầy cho khách lẻ không có email
        Order order = orderService.createPosOrder(
                List.of(createCartItem(testVariant, 1)),
                null,
                employee,
                null,
                "CASH",
                "Tại quầy",
                new BigDecimal("500000")
        );
        createdOrderIds.add(order.getId());

        Thread.sleep(1000);
        assertEquals(before, smtp.getReceivedMessages().length, "Đơn tại quầy không email không được gửi thư");
    }

    @Test
    void testSmtpDownOrderStillCreatedAndLogRecordsError() throws Exception {
        int originalPort = smtp.getSmtp().getPort();
        smtp.stop(); // Tắt SMTP server

        try {
            CheckoutRequest req = new CheckoutRequest();
            req.setReceiverName("Khách Khi SMTP Sập");
            req.setPhone("0944332211");
            req.setEmail("smtp_down@example.com");
            req.setPaymentMethod("COD");
            req.setProvinceId(province.getId());
            req.setWardId(ward.getId());
            req.setStreetAddress("Số 99 Phố Mất Mạng");

            // Đơn hàng vẫn tạo thành công mà không bị quăng Exception ra ngoài
            Order order = assertDoesNotThrow(() ->
                    checkoutService.place(req, List.of(createCartItem(testVariant, 1)), null, null)
            );
            assertNotNull(order.getId());
            createdOrderIds.add(order.getId());

            // Kiểm tra log ghi nhận lỗi
            var logOpt = awaitMailLog(order.getId(), "PLACED");
            assertTrue(logOpt.isPresent(), "Phải có mail log ghi nhận thất bại");
            assertFalse(logOpt.get().isThanhCong(), "Trạng thái mail log phải là thất bại");
            assertNotNull(logOpt.get().getLoi(), "Phải có thông báo lỗi chi tiết");

        } finally {
            // Khởi động lại SMTP server
            smtp = new GreenMail(new ServerSetup(originalPort, "127.0.0.1", ServerSetup.PROTOCOL_SMTP));
            smtp.start();
        }
    }

    private BufferedImage extractInlineImage(MimeMessage message, String contentId) throws Exception {
        Object content = message.getContent();
        if (content instanceof MimeMultipart mp) {
            return findImageInMultipart(mp, contentId);
        }
        return null;
    }

    private BufferedImage findImageInMultipart(MimeMultipart mp, String contentId) throws Exception {
        for (int i = 0; i < mp.getCount(); i++) {
            BodyPart part = mp.getBodyPart(i);
            String cid = part.getHeader("Content-ID") != null ? part.getHeader("Content-ID")[0] : null;
            if (cid != null && (cid.equals("<" + contentId + ">") || cid.equals(contentId))) {
                try (InputStream is = part.getInputStream()) {
                    return ImageIO.read(is);
                }
            }
            if (part.getContent() instanceof MimeMultipart subMp) {
                BufferedImage img = findImageInMultipart(subMp, contentId);
                if (img != null) return img;
            }
        }
        return null;
    }

    private String getPart(Part part, String mimeType) throws Exception {
        if (part.isMimeType(mimeType) && part.getContent() instanceof String s) {
            return s;
        }
        if (part.getContent() instanceof Multipart mp) {
            for (int i = 0; i < mp.getCount(); i++) {
                BodyPart bp = mp.getBodyPart(i);
                String result = getPart(bp, mimeType);
                if (result != null && !result.isBlank()) {
                    return result;
                }
            }
        }
        return "";
    }
}
