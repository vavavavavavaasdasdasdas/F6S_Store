package com.example.demodatn.service.donhang;
import com.example.demodatn.dto.donhang.CartItemDto;
import com.example.demodatn.entity.khuyenmai.Voucher;
import com.example.demodatn.entity.sanpham.Product;
import com.example.demodatn.entity.sanpham.ProductVariant;
import com.example.demodatn.repository.khuyenmai.VoucherRepository;
import com.example.demodatn.repository.sanpham.MaterialRepository;
import com.example.demodatn.repository.sanpham.ProductRepository;
import com.example.demodatn.repository.sanpham.ProductVariantRepository;
import com.example.demodatn.repository.sanpham.SizeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class OrderRollbackIntegrationTest {
    @org.springframework.beans.factory.annotation.Autowired com.example.demodatn.repository.sanpham.ColorRepository phase5Colors;
    @org.springframework.beans.factory.annotation.Autowired com.example.demodatn.repository.sanpham.SoleRepository phase5Soles;

    @Autowired ProductRepository products;
    @Autowired ProductVariantRepository variants;
    @Autowired SizeRepository sizes;
    @Autowired MaterialRepository materials;
    @Autowired VoucherRepository vouchers;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;
    @MockitoSpyBean PricingService pricing;

    // No test transaction: only OrderService can roll back the partial sale.
    private record Fixture(Product product, ProductVariant first, ProductVariant second, Voucher voucher) {}

    private Fixture fixture() {
        var product = new Product();
        product.setCode("P12-TX-" + UUID.randomUUID());
        product.setName("Phase 12 transaction regression");
        product = products.saveAndFlush(product);
        var sizeList = sizes.findAll();
        var first = new ProductVariant();
        first.setProduct(product); first.setSize(sizeList.get(0)); first.setMaterial(materials.findAll().get(0));first.setColor(phase5Colors.findAll().get(0));first.setSole(phase5Soles.findAll().get(0)); first.setPrice(new BigDecimal("200000")); first.setQuantity(10); first.setStatus(true);
        first = variants.saveAndFlush(first);
        var second = new ProductVariant();
        second.setProduct(product); second.setSize(sizeList.get(1)); second.setMaterial(materials.findAll().get(0));second.setColor(phase5Colors.findAll().get(0));second.setSole(phase5Soles.findAll().get(0)); second.setPrice(new BigDecimal("200000")); second.setQuantity(10); second.setStatus(true);
        second = variants.saveAndFlush(second);
        var voucher = new Voucher();
        voucher.setCode("P12-TX-" + UUID.randomUUID()); voucher.setName("Transaction fixture");
        voucher.setDiscountType("FIXED"); voucher.setDiscountValue(new BigDecimal("10000"));
        voucher.setStartDate(LocalDateTime.now().minusDays(1)); voucher.setEndDate(LocalDateTime.now().plusDays(1)); voucher.setUsageLimit(10);
        voucher = vouchers.saveAndFlush(voucher);
        return new Fixture(product, first, second, voucher);
    }

    private ArrayList<CartItemDto> items(Fixture f) {
        var list = new ArrayList<CartItemDto>();
        for (var v : List.of(f.first(), f.second()))
            list.add(new CartItemDto(f.product().getId(), v.getId(), f.product().getName(), v.getSize().getSizeValue(), BigDecimal.ONE, 2));
        return list;
    }

    private Map<String, Object> snapshot(Fixture f) {
        return jdbc.queryForMap("SELECT (SELECT COUNT(*) FROM orders) AS orders, (SELECT COUNT(*) FROM order_details) AS details, (SELECT COUNT(*) FROM invoices) AS invoices, (SELECT COUNT(*) FROM inventory_logs) AS logs, (SELECT quantity FROM product_variants WHERE id=?) AS first_stock, (SELECT quantity FROM product_variants WHERE id=?) AS second_stock, (SELECT used_count FROM vouchers WHERE id=?) AS used_count", f.first().getId(), f.second().getId(), f.voucher().getId());
    }

    private void cleanup(Fixture f) {
        jdbc.update("DELETE FROM invoices WHERE order_id IN (SELECT id FROM orders WHERE voucher_id=?)", f.voucher().getId());
        jdbc.update("DELETE FROM order_details WHERE order_id IN (SELECT id FROM orders WHERE voucher_id=?)", f.voucher().getId());
        jdbc.update("DELETE FROM orders WHERE voucher_id=?", f.voucher().getId());
        jdbc.update("DELETE FROM vouchers WHERE id=?", f.voucher().getId());
        jdbc.update("DELETE FROM inventory_logs WHERE variant_id IN (?,?)", f.first().getId(), f.second().getId());
        jdbc.update("DELETE FROM product_variants WHERE product_id=?", f.product().getId());
        jdbc.update("DELETE FROM products WHERE id=?", f.product().getId());
    }

    @Test
    @WithMockUser(username="user", roles="USER")
    void checkoutRaceRollsBackOrderVoucherAndFirstStockWrite() throws Exception {
        var f = fixture();
        try {
            var before = new AtomicReference<Map<String, Object>>();
            doAnswer(invocation -> {
                var subtotal = invocation.callRealMethod();
                jdbc.update("UPDATE product_variants SET quantity=1 WHERE id=?", f.second().getId());
                before.set(snapshot(f));
                return subtotal;
            }).when(pricing).refreshCart(anyList());
            var session = new MockHttpSession(); session.setAttribute("cart", items(f));
            var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
            mvc.perform(post("/checkout/place").session(session).with(csrf())
                    .param("receiverName", "QA rollback").param("phone", "0901234567").param("email", "qa@example.test")
                    .param("shippingAddress", "QA address").param("paymentMethod", "BANK_TRANSFER").param("voucherCode", f.voucher().getCode()))
                    .andExpect(status().isFound()).andExpect(redirectedUrl("/checkout"))
                    .andExpect(flash().attribute("error", org.hamcrest.Matchers.containsString("Không đủ tồn kho")));
            assertNotNull(before.get());
            var after = snapshot(f);
            
            assertEquals(before.get(), after);
        } finally { cleanup(f); }
    }

    @Test
    @WithMockUser(username="admin", roles="ADMIN")
    void posShortageRollsBackOrderVoucherAndFirstStockWrite() throws Exception {
        var f = fixture();
        try {
            jdbc.update("UPDATE product_variants SET quantity=1 WHERE id=?", f.second().getId());
            var before = snapshot(f);
            var session = new MockHttpSession(); session.setAttribute("posCart", items(f)); session.setAttribute("posCheckoutToken", "P12-test");
            var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
            mvc.perform(post("/admin/pos/checkout").session(session).with(csrf()).param("checkoutToken", "P12-test")
                    .param("paymentMethod", "CASH").param("cashReceived", "1000000").param("voucherCode", f.voucher().getCode()))
                    .andExpect(status().isFound()).andExpect(redirectedUrl("/admin/pos"))
                    .andExpect(flash().attribute("error", org.hamcrest.Matchers.containsString("Không đủ tồn kho")));
            var after = snapshot(f);
            
            assertEquals(before, after);
        } finally { cleanup(f); }
    }
}
