package com.example.demodatn.service.donhang;

import com.example.demodatn.dto.donhang.CartItemDto;
import com.example.demodatn.dto.donhang.PriceInfo;
import com.example.demodatn.entity.donhang.Invoice;
import com.example.demodatn.entity.donhang.Order;
import com.example.demodatn.entity.khuyenmai.Promotion;
import com.example.demodatn.entity.khuyenmai.PromotionDetail;
import com.example.demodatn.entity.sanpham.Product;
import com.example.demodatn.entity.sanpham.ProductVariant;
import com.example.demodatn.repository.donhang.InvoiceRepository;
import com.example.demodatn.repository.donhang.OrderRepository;
import com.example.demodatn.repository.khuyenmai.PromotionDetailRepository;
import com.example.demodatn.repository.khuyenmai.PromotionRepository;
import com.example.demodatn.repository.nhanvien.EmployeeRepository;
import com.example.demodatn.repository.sanpham.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class PosModalPricingIntegrationTest {

    @Autowired WebApplicationContext context;
    @Autowired ProductRepository products;
    @Autowired ProductVariantRepository variants;
    @Autowired SizeRepository sizes;
    @Autowired MaterialRepository materials;
    @Autowired ColorRepository colors;
    @Autowired SoleRepository soles;
    @Autowired PromotionRepository promotions;
    @Autowired PromotionDetailRepository promotionDetails;
    @Autowired PricingService pricingService;
    @Autowired OrderRepository orders;
    @Autowired InvoiceRepository invoices;
    @Autowired EmployeeRepository employees;

    private ProductVariant createTestVariant(String code, BigDecimal price, int qty) {
        Product p = new Product();
        p.setName("Sản phẩm GD23 " + code);
        p.setCode("P23-" + UUID.randomUUID().toString().substring(0, 8));
        products.save(p);

        ProductVariant v = new ProductVariant();
        v.setProduct(p);
        v.setSize(sizes.findAll().get(0));
        v.setMaterial(materials.findAll().get(0));
        v.setColor(colors.findAll().get(0));
        v.setSole(soles.findAll().get(0));
        v.setPrice(price);
        v.setQuantity(qty);
        return variants.saveAndFlush(v);
    }

    @Test
    @Transactional
    void testPricingServicePercentAndAmountPromotions() {
        ProductVariant v1 = createTestVariant("V1", new BigDecimal("100000"), 10);
        ProductVariant v2 = createTestVariant("V2", new BigDecimal("200000"), 10);

        // 1. Khuyến mãi theo phần trăm
        Promotion promoPercent = new Promotion(null, "Khuyến mãi hè 20%", "PERCENT", new BigDecimal("20"),
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(2), true);
        promotions.save(promoPercent);

        PromotionDetail d1 = new PromotionDetail();
        d1.setPromotion(promoPercent);
        d1.setProduct(v1.getProduct());
        promotionDetails.save(d1);

        PriceInfo info1 = pricingService.calculatePrice(v1);
        assertTrue(info1.isHasPromotion());
        assertEquals(0, new BigDecimal("80000").compareTo(info1.getFinalPrice()));
        assertEquals(0, new BigDecimal("100000").compareTo(info1.getOriginalPrice()));
        assertEquals("Khuyến mãi hè 20%", info1.getPromotionName());
        assertEquals("-20%", info1.getDiscountLabel());

        // 2. Khuyến mãi theo số tiền
        Promotion promoAmount = new Promotion(null, "Giảm ngay 50k", "AMOUNT", new BigDecimal("50000"),
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(2), true);
        promotions.save(promoAmount);

        PromotionDetail d2 = new PromotionDetail();
        d2.setPromotion(promoAmount);
        d2.setProduct(v2.getProduct());
        promotionDetails.save(d2);

        PriceInfo info2 = pricingService.calculatePrice(v2);
        assertTrue(info2.isHasPromotion());
        assertEquals(0, new BigDecimal("150000").compareTo(info2.getFinalPrice()));
        assertEquals(0, new BigDecimal("200000").compareTo(info2.getOriginalPrice()));
        assertEquals("Giảm ngay 50k", info2.getPromotionName());
        assertEquals("-50.000 đ", info2.getDiscountLabel());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @Transactional
    void testPosAvailableQuantityCalculationsInSession() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        ProductVariant v = createTestVariant("STOCK", new BigDecimal("150000"), 5);
        MockHttpSession session = new MockHttpSession();

        // Ban đầu tồn kho thật = 5, giỏ hàng trống -> tồn khả dụng = 5
        mvc.perform(get("/admin/pos/products-modal").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attribute("variants", hasItem(hasProperty("availableQuantity", is(5)))));

        // Thêm 1 lần vào giỏ
        mvc.perform(post("/admin/pos/add").session(session).with(csrf())
                        .header("X-Requested-With", "XMLHttpRequest")
                        .param("variantId", v.getId().toString())
                        .param("quantity", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.availableQuantity").value(4));

        // DB physical quantity không được thay đổi (vẫn = 5)
        ProductVariant freshV = variants.findById(v.getId()).orElseThrow();
        assertEquals(5, freshV.getQuantity(), "Tồn kho thật trong DB không được trừ khi thêm vào giỏ");

        // Thêm tiếp lần 2
        mvc.perform(post("/admin/pos/add").session(session).with(csrf())
                        .header("X-Requested-With", "XMLHttpRequest")
                        .param("variantId", v.getId().toString())
                        .param("quantity", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(3));

        // Thêm tiếp lần 3 (tổng cộng 3 trong giỏ)
        mvc.perform(post("/admin/pos/add").session(session).with(csrf())
                        .header("X-Requested-With", "XMLHttpRequest")
                        .param("variantId", v.getId().toString())
                        .param("quantity", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(2));

        // DB physical quantity vẫn là 5
        freshV = variants.findById(v.getId()).orElseThrow();
        assertEquals(5, freshV.getQuantity(), "Tồn kho thật trong DB vẫn là 5 sau 3 lần thêm giỏ");

        // Xóa khỏi giỏ hàng -> tồn khả dụng được hoàn trả lại thành 5
        mvc.perform(post("/admin/pos/remove").session(session).with(csrf())
                        .header("X-Requested-With", "XMLHttpRequest")
                        .param("variantId", v.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.availableQuantity").value(5));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @Transactional
    void testCheckoutRedirectionAndRealInventoryDeduction() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        ProductVariant v = createTestVariant("BUY", new BigDecimal("200000"), 10);
        MockHttpSession session = new MockHttpSession();

        // Chuẩn bị giỏ hàng với 2 sản phẩm
        List<CartItemDto> cart = new ArrayList<>();
        cart.add(new CartItemDto(v.getProduct().getId(), v.getId(), v.getProduct().getName(),
                v.getSize().getSizeValue(), v.getPrice(), 2));
        session.setAttribute("posCart", cart);
        session.setAttribute("posCheckoutToken", "TOKEN-TEST-23");

        // Thực hiện thanh toán tiền mặt 500k
        var result = mvc.perform(post("/admin/pos/checkout").session(session).with(csrf())
                        .param("checkoutToken", "TOKEN-TEST-23")
                        .param("paymentMethod", "CASH")
                        .param("cashReceived", "500000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/invoices/*"))
                .andExpect(flash().attributeExists("message"))
                .andReturn();

        String redirectUrl = result.getResponse().getRedirectedUrl();
        assertNotNull(redirectUrl);
        Integer invoiceId = Integer.parseInt(redirectUrl.substring(redirectUrl.lastIndexOf('/') + 1));
        Invoice invoice = invoices.findById(invoiceId).orElseThrow();
        assertEquals(0, new BigDecimal("400000").compareTo(invoice.getOrder().getFinalAmount()));

        // Tồn kho thật trong DB chỉ bị trừ 1 lần duy nhất khi checkout: 10 - 2 = 8
        ProductVariant updatedV = variants.findById(v.getId()).orElseThrow();
        assertEquals(8, updatedV.getQuantity());

        // Kiểm tra endpoint cũ /admin/pos/success/{orderId} chuyển hướng 302 về /admin/invoices/{invoiceId}
        mvc.perform(get("/admin/pos/success/" + invoice.getOrder().getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/invoices/" + invoiceId));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @Transactional
    void testConcurrentStockDepletionRollbacksCheckout() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        ProductVariant v = createTestVariant("DEPLETED", new BigDecimal("100000"), 2);
        MockHttpSession session = new MockHttpSession();

        // Khách hàng có 2 sản phẩm trong giỏ
        List<CartItemDto> cart = new ArrayList<>();
        cart.add(new CartItemDto(v.getProduct().getId(), v.getId(), v.getProduct().getName(),
                v.getSize().getSizeValue(), v.getPrice(), 2));
        session.setAttribute("posCart", cart);
        session.setAttribute("posCheckoutToken", "TOKEN-CONCURRENT");

        // Trước khi bấm thanh toán, người khác đã mua 1 chiếc -> tồn thật trong DB giảm xuống 1
        v.setQuantity(1);
        variants.saveAndFlush(v);

        long orderCountBefore = orders.count();

        // Bấm thanh toán 2 chiếc -> server kiểm tra tồn kho thật và phát hiện thiếu
        mvc.perform(post("/admin/pos/checkout").session(session).with(csrf())
                        .param("checkoutToken", "TOKEN-CONCURRENT")
                        .param("paymentMethod", "CASH")
                        .param("cashReceived", "200000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/pos"))
                .andExpect(flash().attributeExists("error"));

        // Rollback hoàn toàn: không tạo đơn hàng mới, tồn kho thật vẫn là 1
        assertEquals(orderCountBefore, orders.count());
        ProductVariant unchangedV = variants.findById(v.getId()).orElseThrow();
        assertEquals(1, unchangedV.getQuantity());
    }

    @Test
    @WithMockUser(username = "nv01", roles = "EMPLOYEE")
    void testEmployeeRoleAccessToInvoiceDetail() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        // Lấy 1 hóa đơn bất kỳ
        Invoice invoice = invoices.findAll().stream().findFirst().orElse(null);
        if (invoice != null) {
            mvc.perform(get("/admin/invoices/" + invoice.getId()))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Bán đơn mới")))
                    .andExpect(content().string(containsString("In hóa đơn")));
        }
    }
}
