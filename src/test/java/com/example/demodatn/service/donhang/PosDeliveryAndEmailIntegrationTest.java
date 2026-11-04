package com.example.demodatn.service.donhang;

import com.example.demodatn.dto.donhang.CartItemDto;
import com.example.demodatn.entity.donhang.Order;
import com.example.demodatn.entity.donhang.OrderMailLog;
import com.example.demodatn.entity.donhang.Province;
import com.example.demodatn.entity.donhang.Ward;
import com.example.demodatn.entity.nhanvien.Employee;
import com.example.demodatn.entity.sanpham.Color;
import com.example.demodatn.entity.sanpham.Material;
import com.example.demodatn.entity.sanpham.Product;
import com.example.demodatn.entity.sanpham.ProductVariant;
import com.example.demodatn.entity.sanpham.Size;
import com.example.demodatn.entity.sanpham.Sole;
import com.example.demodatn.repository.donhang.OrderMailLogRepository;
import com.example.demodatn.repository.donhang.OrderRepository;
import com.example.demodatn.repository.donhang.ProvinceRepository;
import com.example.demodatn.repository.donhang.WardRepository;
import com.example.demodatn.repository.nhanvien.EmployeeRepository;
import com.example.demodatn.repository.sanpham.ColorRepository;
import com.example.demodatn.repository.sanpham.MaterialRepository;
import com.example.demodatn.repository.sanpham.ProductRepository;
import com.example.demodatn.repository.sanpham.ProductVariantRepository;
import com.example.demodatn.repository.sanpham.SizeRepository;
import com.example.demodatn.repository.sanpham.SoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@WithMockUser(username = "admin", roles = "ADMIN")
class PosDeliveryAndEmailIntegrationTest {

    @Autowired private OrderService orderService;
    @Autowired private OrderStatusService orderStatusService;
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
    @Autowired private ProvinceRepository provinceRepository;
    @Autowired private WardRepository wardRepository;

    private Employee employee;
    private ProductVariant testVariant;
    private Province province;
    private Ward ward;

    @BeforeEach
    void setup() {
        employee = employeeRepository.findByUser_Username("admin")
                .orElseGet(() -> employeeRepository.findAll().get(0));

        Product product = new Product();
        product.setName("Giày Test GD24");
        product.setCode("SP-GD24-" + UUID.randomUUID().toString().substring(0, 8));
        productRepository.save(product);

        Size size = sizeRepository.findAll().get(0);
        Material mat = materialRepository.findAll().get(0);
        Color color = colorRepository.findAll().get(0);
        Sole sole = soleRepository.findAll().get(0);

        testVariant = new ProductVariant();
        testVariant.setProduct(product);
        testVariant.setSize(size);
        testVariant.setMaterial(mat);
        testVariant.setColor(color);
        testVariant.setSole(sole);
        testVariant.setPrice(new BigDecimal("200000"));
        testVariant.setQuantity(20);
        testVariant = variantRepository.saveAndFlush(testVariant);

        province = provinceRepository.findAll().get(0);
        ward = wardRepository.findAll().stream()
                .filter(w -> w.getProvince().getId().equals(province.getId()))
                .findFirst()
                .orElseGet(() -> wardRepository.findAll().get(0));
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
        long end = System.currentTimeMillis() + 4000;
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
    void testPosDeliveryOrderCodDoesNotDeductUntilConfirmed() {
        int initialQty = testVariant.getQuantity();

        List<CartItemDto> items = List.of(createCartItem(testVariant, 2));

        Order order = orderService.createPosDeliveryOrder(
                items, null, employee, null, "COD",
                "Khách Hàng Test COD", "0987654321", "khachhang_cod@example.com",
                province.getId(), ward.getId(), "123 Đường Test",
                null, null, null, false, false, "Ghi chú COD"
        );

        // Verify order type and shipping fee
        assertEquals("ONLINE", order.getOrderType());
        assertEquals(0, BigDecimal.ZERO.compareTo(order.getShippingFee()));
        assertEquals(employee.getId(), order.getEmployee().getId());
        assertEquals("Khách Hàng Test COD", order.getReceiverName());
        assertEquals("khachhang_cod@example.com", order.getEmail());

        // Stock must NOT be deducted yet for COD before confirmation
        ProductVariant refreshed = variantRepository.findById(testVariant.getId()).orElseThrow();
        assertEquals(initialQty, refreshed.getQuantity());

        // Now confirm the order
        orderStatusService.transition(order.getId(), "CONFIRMED", "Duyệt đơn COD", null, null);

        // Now stock MUST be deducted
        ProductVariant deducted = variantRepository.findById(testVariant.getId()).orElseThrow();
        assertEquals(initialQty - 2, deducted.getQuantity());

        // Verify email notification log
        var logOpt = awaitMailLog(order.getId(), "CONFIRMED");
        assertTrue(logOpt.isPresent(), "Phải có mail log cho trạng thái CONFIRMED");
        assertEquals("khachhang_cod@example.com", logOpt.get().getEmail());
    }

    @Test
    void testPosDeliveryOrderTransferDeductsImmediately() {
        int initialQty = testVariant.getQuantity();

        List<CartItemDto> items = List.of(createCartItem(testVariant, 3));

        Order order = orderService.createPosDeliveryOrder(
                items, null, employee, null, "TRANSFER",
                "Khách Hàng Chuyển Khoản", "0912345678", "khachhang_ck@example.com",
                province.getId(), ward.getId(), "456 Phố Chuyển Khoản",
                null, null, null, true, false, "Ghi chú CK"
        );

        assertEquals("ONLINE", order.getOrderType());
        assertEquals("PAID", order.getPaymentStatus());

        // Stock must be deducted immediately
        ProductVariant refreshed = variantRepository.findById(testVariant.getId()).orElseThrow();
        assertEquals(initialQty - 3, refreshed.getQuantity());

        // Verify PAYMENT_CONFIRMED mail log
        var logOpt = awaitMailLog(order.getId(), "PAYMENT_CONFIRMED");
        assertTrue(logOpt.isPresent(), "Phải có mail log cho PAYMENT_CONFIRMED");
    }

    @Test
    void testPosDeliveryOrderConfirmNowDeductsImmediately() {
        int initialQty = testVariant.getQuantity();

        List<CartItemDto> items = List.of(createCartItem(testVariant, 1));

        Order order = orderService.createPosDeliveryOrder(
                items, null, employee, null, "COD",
                "Khách Hàng Confirm Now", "0900000000", "confirmnow@example.com",
                province.getId(), ward.getId(), "789 Đường Hỏa Tốc",
                null, null, null, false, true, "Xác nhận ngay"
        );

        // Status should be CONFIRMED immediately
        assertEquals("CONFIRMED", order.getStatus());

        // Stock deducted immediately
        ProductVariant refreshed = variantRepository.findById(testVariant.getId()).orElseThrow();
        assertEquals(initialQty - 1, refreshed.getQuantity());

        var logOpt = awaitMailLog(order.getId(), "CONFIRMED");
        assertTrue(logOpt.isPresent());
    }

    @Test
    void testPosDeliveryWithoutEmailLogsNoEmail() {
        List<CartItemDto> items = List.of(createCartItem(testVariant, 1));

        Order order = orderService.createPosDeliveryOrder(
                items, null, employee, null, "COD",
                "Khách Hàng Không Mail", "0933333333", "",
                province.getId(), ward.getId(), "Số 1 Đường Không Tên",
                null, null, null, false, false, "Không email"
        );

        // Change status to CONFIRMED
        orderStatusService.transition(order.getId(), "CONFIRMED", "Xác nhận đơn không email", null, null);

        var logOpt = awaitMailLog(order.getId(), "CONFIRMED");
        assertTrue(logOpt.isPresent());
        assertFalse(logOpt.get().isThanhCong());
        assertEquals("Không có email", logOpt.get().getLoi());
    }

    @Test
    void testRegularPosOrderDoesNotSendStatusEmail() {
        List<CartItemDto> items = List.of(createCartItem(testVariant, 1));

        // Regular POS in-store sale
        Order order = orderService.createPosOrder(
                items,
                null,
                employee,
                null,
                "CASH",
                "Tại quầy",
                new BigDecimal("500000")
        );

        assertEquals("POS", order.getOrderType());

        // Calling sendNotification directly or via status change should be ignored
        orderEmailService.sendNotification(order.getId(), "DELIVERED", null, null, null, false);

        var logs = mailLogRepository.findByOrder_Id(order.getId());
        assertTrue(logs.isEmpty(), "Đơn tại quầy không được có mail log");
    }

    @Test
    void testFullDeliveryLifecycleEmails() {
        List<CartItemDto> items = List.of(createCartItem(testVariant, 1));

        Order order = orderService.createPosDeliveryOrder(
                items, null, employee, null, "COD",
                "Khách Vòng Đời", "0988776655", "vongdoi@example.com",
                province.getId(), ward.getId(), "Số 99 Đường Đời",
                null, null, null, false, false, "Đơn theo dõi"
        );

        // 1. CONFIRMED
        orderStatusService.transition(order.getId(), "CONFIRMED", "Admin duyệt", null, null);
        assertTrue(awaitMailLog(order.getId(), "CONFIRMED").isPresent());

        // 2. PREPARING
        orderStatusService.transition(order.getId(), "PREPARING", "Đang gói hàng", null, null);
        assertTrue(awaitMailLog(order.getId(), "PREPARING").isPresent());

        // 3. SHIPPING with carrier & tracking
        orderStatusService.transition(order.getId(), "SHIPPING", "Giao shipper", "GiaoHangNhanh", "GHN123456VN");
        assertTrue(awaitMailLog(order.getId(), "SHIPPING").isPresent());

        // 4. DELIVERED
        orderStatusService.transition(order.getId(), "DELIVERED", "Khách đã nhận", null, null);
        assertTrue(awaitMailLog(order.getId(), "DELIVERED").isPresent());

        // Verify resend
        orderEmailService.resend(order.getId(), "DELIVERED");
        var log = awaitMailLog(order.getId(), "DELIVERED");
        assertTrue(log.isPresent());
    }
}
