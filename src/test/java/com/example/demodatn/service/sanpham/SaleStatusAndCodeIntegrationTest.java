package com.example.demodatn.service.sanpham;

import com.example.demodatn.dto.sanpham.AdminProductForm;
import com.example.demodatn.dto.donhang.*;
import com.example.demodatn.repository.sanpham.*;
import com.example.demodatn.repository.nhanvien.EmployeeRepository;
import com.example.demodatn.service.donhang.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @WithMockUser(username="admin",roles="ADMIN")
class SaleStatusAndCodeIntegrationTest {
    @Autowired AdminCatalogService catalog; @Autowired ProductRepository products;
    @Autowired ProductVariantRepository variants; @Autowired CategoryRepository categories;
    @Autowired SizeRepository sizes; @Autowired MaterialRepository materials;
    @Autowired ProductFilterService filter; @Autowired OrderService orders;
    @Autowired EmployeeRepository employees; @Autowired PosHoldService holds;
    @Autowired JdbcTemplate jdbc; @Autowired WebApplicationContext context;
    @Autowired PlatformTransactionManager transactions;
    MockMvc mvc;
    @BeforeEach void setup() {
        assertTrue(jdbc.queryForObject("SELECT DB_NAME()",String.class).startsWith("shopbangiay_kt_"));
        mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    AdminProductForm form(boolean withVariants) {
        var f=new AdminProductForm();f.setName("Giày bổ sung "+UUID.randomUUID());f.setCode("FORGED-CODE");f.setStatus(withVariants);f.setCategoryId(categories.findAll().get(0).getId());
        if(withVariants){
            try {var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",bytes);f.setImage(new org.springframework.mock.web.MockMultipartFile("image","fixture.png","image/png",bytes.toByteArray()));}
            catch(java.io.IOException ex){throw new IllegalStateException(ex);}
            f.setInitialSizeIds(sizes.findAll().stream().limit(2).map(s->s.getId()).toList());f.setInitialMaterialId(materials.findByStatusTrueOrderByNameAscIdAsc().get(0).getId());f.setInitialPrice(new BigDecimal("190000"));f.setInitialQuantity(12);}
        return f;
    }
    CartItemDto item(com.example.demodatn.entity.sanpham.ProductVariant v) {
        return new CartItemDto(v.getProduct().getId(),v.getId(),v.getProduct().getName(),v.getSize().getSizeValue(),v.getMaterial().getName(),v.getPrice(),1);
    }
    @Test @Transactional void postedCodeIsIgnoredOnCreateAndEdit() {
        var p=catalog.saveProduct(form(false));assertTrue(p.getCode().matches("SP[0-9]{3,}"));assertNotEquals("FORGED-CODE",p.getCode());
        String original=p.getCode();var edit=form(false);edit.setId(p.getId());edit.setCode("CHANGED-FORGERY");
        assertEquals(original,catalog.saveProduct(edit).getCode());
    }
    @Test @Transactional void stopPreservesVariantsStockHistoryAndBlocksEverySaleEntry() throws Exception {
        var p=catalog.saveProduct(form(true));var children=variants.findByProduct_Id(p.getId());var v=children.get(0);
        catalog.variantStatus(children.get(1).getId(),false);
        var basket=new ArrayList<>(List.of(item(v)));var request=new PosCheckoutRequest();request.setPaymentMethod("CASH");
        Long hold=holds.save("admin",basket,request,null);
        var old=orders.createPosOrder(List.of(item(v)),null,employees.findByUser_Username("admin").orElseThrow(),null,"CASH","Tại quầy",new BigDecimal("999999"));
        var before=jdbc.queryForList("SELECT id,status,quantity FROM product_variants WHERE product_id=? ORDER BY id",p.getId());
        var historical=jdbc.queryForList("SELECT id,status,payment_status,total_amount,final_amount FROM orders WHERE id=?",old.getId());
        int logs=jdbc.queryForObject("SELECT COUNT(*) FROM inventory_logs",Integer.class);
        catalog.productStatus(p.getId(),false);catalog.productStatus(p.getId(),false);
        assertEquals(before,jdbc.queryForList("SELECT id,status,quantity FROM product_variants WHERE product_id=? ORDER BY id",p.getId()));
        assertEquals(logs,jdbc.queryForObject("SELECT COUNT(*) FROM inventory_logs",Integer.class));
        assertEquals(historical,jdbc.queryForList("SELECT id,status,payment_status,total_amount,final_amount FROM orders WHERE id=?",old.getId()));
        assertTrue(filter.filterProducts(p.getCode(),null,null,null,null,null,"newest",0,12).isEmpty());
        mvc.perform(get("/products/"+p.getId())).andExpect(status().isNotFound());
        var session=new MockHttpSession();session.setAttribute("cart",basket);
        mvc.perform(get("/cart").session(session)).andExpect(content().string(org.hamcrest.Matchers.containsString("Sản phẩm đã ngừng bán")));
        mvc.perform(post("/cart/add-ajax").with(csrf()).param("variantId",v.getId().toString()).param("quantity","1")).andExpect(status().isBadRequest());
        mvc.perform(get("/checkout").session(session)).andExpect(redirectedUrl("/cart"));
        var checkout=new CheckoutRequest();checkout.setPaymentMethod("COD");
        assertThrows(IllegalArgumentException.class,()->orders.placeOrder(checkout,basket,null,BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,()->holds.checkout("admin",hold,basket,request));
        catalog.productStatus(p.getId(),true);assertTrue(ProductSalePolicy.isSelling(v));assertFalse(ProductSalePolicy.isSelling(children.get(1)));
        assertFalse(filter.filterProducts(p.getCode(),null,null,null,null,null,"newest",0,12).isEmpty());
    }
    @Test @Transactional void variantStatusIsIdempotentAndKeepsQuantity() {
        var p=catalog.saveProduct(form(true));var v=variants.findByProduct_Id(p.getId()).get(0);int stock=v.getQuantity();
        catalog.variantStatus(v.getId(),false);catalog.variantStatus(v.getId(),false);assertFalse(v.getStatus());assertEquals(stock,v.getQuantity());
        catalog.variantStatus(v.getId(),true);assertTrue(v.getStatus());assertEquals(stock,v.getQuantity());assertTrue(p.getStatus());
    }
    @Test void twentyConcurrentPostRequestsAllocateDistinctCodes() throws Exception {
        int category=categories.findAll().get(0).getId();var pool=Executors.newFixedThreadPool(20);var start=new CountDownLatch(1);
        try {
            List<Future<Integer>> futures=new ArrayList<>();
            for(int i=0;i<20;i++)futures.add(pool.submit(()->{start.await();var r=mvc.perform(post("/admin/products-crud/save").with(user("admin").roles("ADMIN")).with(csrf())
                .param("name","Giày yêu cầu đồng thời "+UUID.randomUUID()).param("categoryId",String.valueOf(category)).param("status","false").param("code","SAME-FORGED-CODE"))
                .andExpect(status().isFound()).andReturn().getResponse();return Integer.valueOf(r.getRedirectedUrl().replaceAll(".*/",""));}));
            start.countDown();Set<String> codes=new HashSet<>();for(var future:futures){String code=products.findById(future.get(45,TimeUnit.SECONDS)).orElseThrow().getCode();assertTrue(code.matches("SP[0-9]{3,}"));assertTrue(codes.add(code));}
            assertEquals(20,codes.size());
        } finally {pool.shutdownNow();}
    }
    @Test void rollbackLeavesAGapAndNeverReusesItsNumber() {
        long before=products.count();var consumed=new AtomicReference<String>();
        new TransactionTemplate(transactions).executeWithoutResult(s->{consumed.set(catalog.saveProduct(form(false)).getCode());s.setRollbackOnly();});
        assertEquals(before,products.count());assertFalse(products.existsByCodeIgnoreCaseAndIdNot(consumed.get(),-1));
        String next=catalog.saveProduct(form(false)).getCode();assertTrue(Long.parseLong(next.substring(2))>Long.parseLong(consumed.get().substring(2)));
    }
    @Test void migrationNeverRewindsConsumedNumbers() throws Exception {
        new TransactionTemplate(transactions).executeWithoutResult(s->{catalog.saveProduct(form(false));s.setRollbackOnly();});
        Long before=jdbc.queryForObject("SELECT CONVERT(BIGINT,current_value) FROM sys.sequences WHERE name='product_code_seq'",Long.class);
        String script=Files.readString(Path.of("database/migrations/012_ma_san_pham_tu_sinh.sql"));
        jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) connection->{
            try(var statement=connection.createStatement()) {
                boolean previous;
                try(var options=statement.executeQuery("SELECT CASE WHEN @@OPTIONS & 16384=16384 THEN 1 ELSE 0 END")){options.next();previous=options.getBoolean(1);}
                try {statement.execute(script);statement.execute(script);}
                finally {statement.execute(previous?"SET XACT_ABORT ON":"SET XACT_ABORT OFF");}
            }
            return null;
        });
        assertEquals(before,jdbc.queryForObject("SELECT CONVERT(BIGINT,current_value) FROM sys.sequences WHERE name='product_code_seq'",Long.class));
    }
    @Test void statusRoutesRequireAdministratorAndCsrf() throws Exception {
        for(String path:new String[]{"/admin/products-crud/status/1","/admin/variants/status/1"}) {
            mvc.perform(post(path).with(user("user").roles("USER")).with(csrf()).param("selling","false")).andExpect(status().isForbidden());
            mvc.perform(post(path).param("selling","false")).andExpect(status().isForbidden());
            mvc.perform(post(path).with(csrf().useInvalidToken()).param("selling","false")).andExpect(status().isForbidden());
        }
    }
}
