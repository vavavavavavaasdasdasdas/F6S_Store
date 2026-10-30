package com.example.demodatn.service.khuyenmai;
import com.example.demodatn.dto.donhang.CartItemDto;
import com.example.demodatn.dto.donhang.CheckoutRequest;
import com.example.demodatn.dto.khuyenmai.PromotionDto;
import com.example.demodatn.dto.khuyenmai.VoucherDto;
import com.example.demodatn.entity.sanpham.Product;
import com.example.demodatn.entity.sanpham.ProductVariant;
import com.example.demodatn.exception.DiscountValidationException;
import com.example.demodatn.repository.donhang.OrderRepository;
import com.example.demodatn.repository.khachhang.CustomerRepository;
import com.example.demodatn.repository.khuyenmai.PromotionDetailRepository;
import com.example.demodatn.repository.khuyenmai.PromotionRepository;
import com.example.demodatn.repository.khuyenmai.VoucherRepository;
import com.example.demodatn.repository.sanpham.MaterialRepository;
import com.example.demodatn.repository.sanpham.ProductRepository;
import com.example.demodatn.repository.sanpham.ProductVariantRepository;
import com.example.demodatn.repository.sanpham.SizeRepository;
import com.example.demodatn.service.donhang.OrderService;
import com.example.demodatn.service.donhang.PricingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import jakarta.validation.Validator;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @WithMockUser(roles="ADMIN")
class DiscountIntegrationTest {
    @org.springframework.beans.factory.annotation.Autowired com.example.demodatn.repository.sanpham.ColorRepository phase5Colors;
    @org.springframework.beans.factory.annotation.Autowired com.example.demodatn.repository.sanpham.SoleRepository phase5Soles;

    @Autowired DiscountAdminService admin;
    @Autowired VoucherService vouchers;
    @Autowired VoucherRepository voucherRepo;
    @Autowired PromotionRepository promotions;
    @Autowired PromotionDetailRepository details;
    @Autowired ProductRepository products;
    @Autowired ProductVariantRepository variants;
    @Autowired SizeRepository sizes;
    @Autowired MaterialRepository materials;
    @Autowired CustomerRepository customers;
    @Autowired PricingService pricing;
    @Autowired OrderService orders;
    @Autowired com.example.demodatn.repository.nhanvien.EmployeeRepository employees;
    @Autowired OrderRepository orderRepo;
    @Autowired PlatformTransactionManager manager;
    @Autowired JdbcTemplate jdbc;
    @Autowired Validator validator;
    @Autowired WebApplicationContext context;

    private com.example.demodatn.entity.khachhang.Customer customer() {
        var customer = new com.example.demodatn.entity.khachhang.Customer();
        customer.setCustomerCode("P8-" + UUID.randomUUID().toString().substring(0, 15));
        customer.setFullName("Khách kiểm thử voucher");
        customer.setPhone("P8-" + UUID.randomUUID().toString().substring(0, 15));
        customer.setEmail(UUID.randomUUID() + "@example.invalid");
        return customers.saveAndFlush(customer);
    }

    private VoucherDto voucherForm() {
        var f=new VoucherDto();f.setCode("P8-"+UUID.randomUUID().toString().substring(0,8));f.setName("Kiểm thử voucher");f.setDiscountValue(new BigDecimal("20"));f.setStartDate(LocalDateTime.now().minusDays(1));f.setEndDate(LocalDateTime.now().plusDays(1));f.setUsageLimit(5);return f;
    }
    private ProductVariant variant() {
        var p=new Product();p.setCode("P8-"+UUID.randomUUID());p.setName("Kiểm thử Phase 8");p=products.saveAndFlush(p);
        var v=new ProductVariant();v.setProduct(p);v.setSize(sizes.findAll().get(0));v.setMaterial(materials.findAll().get(0));v.setColor(phase5Colors.findAll().get(0));v.setSole(phase5Soles.findAll().get(0));v.setPrice(new BigDecimal("200000"));v.setQuantity(10);return variants.saveAndFlush(v);
    }
    private CartItemDto item(ProductVariant v) { return new CartItemDto(v.getProduct().getId(),v.getId(),v.getProduct().getName(),v.getSize().getSizeValue(),BigDecimal.ONE,1); }
    private CheckoutRequest request(String code) { var r=new CheckoutRequest();r.setProvinceId(jdbc.queryForObject("SELECT TOP 1 id FROM tinh_thanh ORDER BY id",Integer.class));r.setWardId(jdbc.queryForObject("SELECT TOP 1 id FROM phuong_xa WHERE tinh_thanh_id=? ORDER BY id",Integer.class,r.getProvinceId()));r.setStreetAddress("Số 12, đường Nguyễn Huệ");r.setReceiverName("Kiểm thử");r.setPhone("0987000000");r.setEmail("p8@example.invalid");r.setShippingAddress("Địa chỉ kiểm thử");r.setStreetAddress("Địa chỉ kiểm thử");r.setPaymentMethod("COD");r.setVoucherCode(code);return r; }
    private void money(String expected,BigDecimal actual) { assertEquals(0,new BigDecimal(expected).compareTo(actual)); }

    @Test @Transactional
    void voucherCasesHaveSpecificReasonsAndCorrectCapsWithoutConsuming() {
        var f=voucherForm();f.setMaxDiscount(new BigDecimal("30000"));var v=admin.saveVoucher(f);
        var result=vouchers.validateAndCalculate("  "+v.getCode().toLowerCase()+"  ",null,new BigDecimal("200000"));assertTrue(result.valid);money("30000",result.discountAmount);assertEquals(0,v.getUsedCount());
        v.setEndDate(LocalDateTime.now().minusHours(1));v.setStartDate(LocalDateTime.now().minusDays(2));voucherRepo.flush();assertTrue(vouchers.validateAndCalculate(v.getCode(),null,new BigDecimal("200000")).message.contains("hết hạn"));
        v.setStartDate(LocalDateTime.now().plusHours(1));v.setEndDate(LocalDateTime.now().plusDays(2));voucherRepo.flush();assertTrue(vouchers.validateAndCalculate(v.getCode(),null,new BigDecimal("200000")).message.contains("chưa tới"));
        v.setStartDate(LocalDateTime.now().minusDays(1));v.setMinOrderValue(new BigDecimal("300000"));voucherRepo.flush();assertTrue(vouchers.validateAndCalculate(v.getCode(),null,new BigDecimal("200000")).message.contains("tối thiểu"));
        v.setMinOrderValue(BigDecimal.ZERO);v.setUsedCount(5);voucherRepo.flush();assertTrue(vouchers.validateAndCalculate(v.getCode(),null,new BigDecimal("200000")).message.contains("hết lượt"));
        v.setUsedCount(0);v.setIsPublic(false);v.setCustomer(customer());voucherRepo.flush();assertTrue(vouchers.validateAndCalculate(v.getCode(),null,new BigDecimal("200000")).message.contains("khách hàng"));assertTrue(vouchers.validateAndCalculate(v.getCode(),v.getCustomer(),new BigDecimal("200000")).valid);
        v.setIsPublic(true);v.setCustomer(null);v.setDiscountType("FIXED");v.setDiscountValue(new BigDecimal("500000"));voucherRepo.flush();money("200000",vouchers.validateAndCalculate(v.getCode(),null,new BigDecimal("200000")).discountAmount);
        money("200000.50",vouchers.validateAndCalculate(v.getCode(),null,new BigDecimal("200000.50")).discountAmount);
        v.setDiscountType("PERCENT");v.setDiscountValue(new BigDecimal("20"));v.setMaxDiscount(new BigDecimal("30000.50"));voucherRepo.flush();money("30000.50",vouchers.validateAndCalculate(v.getCode(),null,new BigDecimal("200000.50")).discountAmount);
        v.setStatus(false);voucherRepo.flush();assertFalse(vouchers.validateAndCalculate(v.getCode(),null,new BigDecimal("200000")).valid);
    }
    @Test @Transactional
    void adminValidatesCrossFieldsAndProtectsUsageCountOnEdit() {
        var f=voucherForm();var v=admin.saveVoucher(f);v.setUsedCount(2);voucherRepo.flush();
        var duplicate=voucherForm();duplicate.setCode(v.getCode().toLowerCase());assertEquals("code",assertThrows(DiscountValidationException.class,()->admin.saveVoucher(duplicate)).getField());
        f.setId(v.getId());f.setUsageLimit(1);assertEquals("usageLimit",assertThrows(DiscountValidationException.class,()->admin.saveVoucher(f)).getField());f.setUsageLimit(3);admin.saveVoucher(f);assertEquals(2,v.getUsedCount());
        f.setIsPublic(false);assertEquals("customerId",assertThrows(DiscountValidationException.class,()->admin.saveVoucher(f)).getField());f.setIsPublic(true);
        f.setEndDate(f.getStartDate());assertEquals("endDate",assertThrows(DiscountValidationException.class,()->admin.saveVoucher(f)).getField());
        f.setEndDate(LocalDateTime.now().plusDays(1));f.setDiscountValue(new BigDecimal("101"));assertThrows(DiscountValidationException.class,()->admin.saveVoucher(f));
        f.setDiscountValue(BigDecimal.ZERO);assertFalse(validator.validate(f).isEmpty());
    }
    @Test @Transactional
    void promotionSelectionAndExpirationRepriceCartAndOrderSnapshotsRemain() {
        var a=variant();var b=variant();var f=new PromotionDto();f.setName("20% hai sản phẩm");f.setDiscountValue(new BigDecimal("20"));f.setStartDate(LocalDateTime.now().minusDays(1));f.setEndDate(LocalDateTime.now().plusDays(1));var p=admin.savePromotion(f);
        admin.selectProducts(p.getId(),List.of(a.getProduct().getId(),b.getProduct().getId(),a.getProduct().getId()));assertEquals(2,details.findByPromotion_Id(p.getId()).size());
        money("160000",pricing.calculatePrice(a).getFinalPrice());money("160000",pricing.calculatePrice(b).getFinalPrice());
        var cart=new ArrayList<>(List.of(item(a),item(b)));money("320000",pricing.refreshCart(cart));
        var o=orders.placeOrder(request(null),cart,customer(),BigDecimal.ZERO);money("320000",o.getTotalAmount());money("320000",o.getFinalAmount());
        p.setEndDate(LocalDateTime.now().minusHours(1));promotions.flush();money("400000",pricing.refreshCart(cart));assertFalse(pricing.calculatePrice(a).isHasPromotion());money("320000",orderRepo.findById(o.getId()).orElseThrow().getTotalAmount());
        p.setEndDate(LocalDateTime.now().plusDays(1));p.setStatus(false);promotions.flush();money("200000",pricing.calculatePrice(a).getFinalPrice());
        admin.deletePromotion(p.getId());assertEquals(0,details.findByPromotion_Id(p.getId()).size());
    }
    @Test @Transactional
    void checkoutAndPosConsumeSamePercentageFormulaAndReferencedDeleteLocksVoucher() {
        var a=variant();var f=voucherForm();f.setMaxDiscount(new BigDecimal("30000"));var v=admin.saveVoucher(f);
        var online=orders.placeOrder(request(v.getCode()),List.of(item(a)),customer(),BigDecimal.ZERO);money("30000",online.getDiscountAmount());money("225000",online.getFinalAmount());
        var pos=orders.createPosOrder(List.of(item(a)),null,employees.findByUser_Username("admin").orElseThrow(),v.getCode(),"CASH","Tại quầy",new BigDecimal("200000"));money("30000",pos.getDiscountAmount());money("170000",pos.getFinalAmount());assertEquals(2,v.getUsedCount());
        assertTrue(admin.deleteVoucher(v.getId()).contains("giữ"));assertTrue(voucherRepo.existsById(v.getId()));assertFalse(v.getStatus());money("30000",online.getDiscountAmount());
    }
    @Test
    void failedStockAfterVoucherConsumptionRollsBackUsageAndOrder() {
        var tx=new TransactionTemplate(manager);Integer[] ids=tx.execute(s->{var a=variant();var f=voucherForm();var v=admin.saveVoucher(f);return new Integer[]{a.getId(),a.getProduct().getId(),v.getId()};});
        try {
            var a=variants.findById(ids[0]).orElseThrow();var v=voucherRepo.findById(ids[2]).orElseThrow();var i=item(a);i.setQuantity(11);long before=orderRepo.count();
            assertThrows(IllegalArgumentException.class,()->orders.placeOrder(request(v.getCode()),List.of(i),null,BigDecimal.ZERO));assertEquals(before,orderRepo.count());assertEquals(0,voucherRepo.findById(v.getId()).orElseThrow().getUsedCount());assertEquals(10,variants.findById(a.getId()).orElseThrow().getQuantity());
        } finally { cleanup(ids[2],List.of(ids[0]),List.of(ids[1])); }
    }
    @Test
    void twoConcurrentOrdersWithOneRemainingUseAllowExactlyOneCommit() throws Exception {
        var tx=new TransactionTemplate(manager);Integer[] ids=tx.execute(s->{var a=variant();var b=variant();var f=voucherForm();f.setUsageLimit(1);var v=admin.saveVoucher(f);return new Integer[]{a.getId(),a.getProduct().getId(),b.getId(),b.getProduct().getId(),v.getId()};});
        var pool=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var start=new CountDownLatch(1);
        try {
            String code=voucherRepo.findById(ids[4]).orElseThrow().getCode();var futures=new ArrayList<Future<String>>();
            for(int id:List.of(ids[0],ids[2])) futures.add(pool.submit(()->{var a=variants.findById(id).orElseThrow();var i=item(a);ready.countDown();start.await();try { orders.placeOrder(request(code),List.of(i),null,BigDecimal.ZERO);return "success"; } catch(IllegalArgumentException ex) { return ex.getMessage(); }}));
            assertTrue(ready.await(10,TimeUnit.SECONDS));start.countDown();var results=List.of(futures.get(0).get(30,TimeUnit.SECONDS),futures.get(1).get(30,TimeUnit.SECONDS));
            assertEquals(1,results.stream().filter("success"::equals).count());assertEquals(1,results.stream().filter(r->r.contains("hết lượt")).count());assertEquals(1,voucherRepo.findById(ids[4]).orElseThrow().getUsedCount());
            assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE voucher_id=?",Integer.class,ids[4]));assertEquals(20,jdbc.queryForObject("SELECT SUM(quantity) FROM product_variants WHERE id IN (?,?)",Integer.class,ids[0],ids[2]));
            
        } finally { start.countDown();pool.shutdownNow();cleanup(ids[4],List.of(ids[0],ids[2]),List.of(ids[1],ids[3])); }
    }
    private void cleanup(Integer voucherId,List<Integer> variantIds,List<Integer> productIds) {
        new TransactionTemplate(manager).executeWithoutResult(s->{jdbc.update("DELETE FROM invoices WHERE order_id IN (SELECT id FROM orders WHERE voucher_id=?)",voucherId);jdbc.update("DELETE FROM order_details WHERE order_id IN (SELECT id FROM orders WHERE voucher_id=?)",voucherId);jdbc.update("DELETE FROM order_status_history WHERE order_id IN (SELECT id FROM orders WHERE voucher_id=?)",voucherId);jdbc.update("DELETE FROM orders WHERE voucher_id=?",voucherId);jdbc.update("DELETE FROM vouchers WHERE id=?",voucherId);for(int id:variantIds){jdbc.update("DELETE FROM inventory_logs WHERE variant_id=?",id);jdbc.update("DELETE FROM product_variants WHERE id=?",id);}for(int id:productIds)jdbc.update("DELETE FROM products WHERE id=?",id);});
    }
    @Test @WithMockUser(roles="USER")
    void ordinaryUserCannotManageDiscounts() throws Exception {
        var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        for(String path:List.of("/admin/vouchers","/admin/promotions","/admin/promotions/1/products"))mvc.perform(get(path)).andExpect(status().isForbidden());
        mvc.perform(post("/admin/vouchers/save").with(csrf())).andExpect(status().isForbidden());mvc.perform(post("/admin/promotions/1/products").with(csrf())).andExpect(status().isForbidden());
    }
    @Test @Transactional
    void batchPricingMatchesSinglePricingAndRefreshesAfterPromotionChanges() {
        var a=variant();var b=variant();var plain=variant();
        var f=new PromotionDto();f.setName("Giảm giá nhiều chất liệu");f.setDiscountValue(new BigDecimal("20"));
        f.setStartDate(LocalDateTime.now().minusDays(1));f.setEndDate(LocalDateTime.now().plusDays(1));
        var promo=admin.savePromotion(f);admin.selectProducts(promo.getId(),List.of(a.getProduct().getId(),b.getProduct().getId()));
        var items=List.of(a,b,plain);var prices=pricing.calculatePrices(items);
        for(var v:items){var single=pricing.calculatePrice(v);var batch=prices.get(v.getId());
            money(single.getFinalPrice().toPlainString(),batch.getFinalPrice());
            assertEquals(single.getDiscountPercentage(),batch.getDiscountPercentage());assertEquals(single.isHasPromotion(),batch.isHasPromotion());}
        promo.setStatus(false);promotions.flush();
        var refreshed=pricing.calculatePrices(items);money("200000",refreshed.get(a.getId()).getFinalPrice());
        assertFalse(refreshed.get(a.getId()).isHasPromotion());assertTrue(pricing.calculatePrices(List.of()).isEmpty());
    }

    @Test @Transactional
    void decimalColumnPercentageCanBeEditedButFractionalPercentageIsRejected() {
        var f=new PromotionDto();f.setName("Kiểm thử sửa đợt giảm giá");f.setDiscountValue(new BigDecimal("20.00"));f.setStartDate(LocalDateTime.now().minusDays(1));f.setEndDate(LocalDateTime.now().plusDays(1));
        assertTrue(validator.validate(f).isEmpty());var p=admin.savePromotion(f);f.setId(p.getId());f.setName("Tên đã sửa");admin.savePromotion(f);assertEquals("Tên đã sửa",p.getName());
        f.setDiscountValue(new BigDecimal("20.50"));assertEquals("discountValue",assertThrows(DiscountValidationException.class,()->admin.savePromotion(f)).getField());
    }
    @Test @Transactional
    void invalidProductSelectionRollsBackAndMissingIdsAre404() throws Exception {
        var f=new PromotionDto();f.setName("Kiểm thử");f.setDiscountValue(BigDecimal.TEN);f.setStartDate(LocalDateTime.now().minusDays(1));f.setEndDate(LocalDateTime.now().plusDays(1));var p=admin.savePromotion(f);var a=variant();admin.selectProducts(p.getId(),List.of(a.getProduct().getId()));assertThrows(IllegalArgumentException.class,()->admin.selectProducts(p.getId(),List.of(-1)));assertEquals(1,details.findByPromotion_Id(p.getId()).size());
        var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();mvc.perform(get("/admin/vouchers/edit/-1")).andExpect(status().isNotFound());mvc.perform(get("/admin/promotions/delete/"+p.getId())).andExpect(status().isMethodNotAllowed());
    }
}
