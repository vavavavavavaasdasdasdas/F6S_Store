package com.example.demodatn.service.sanpham;
import com.example.demodatn.dto.donhang.CartItemDto;
import com.example.demodatn.dto.donhang.CheckoutRequest;
import com.example.demodatn.dto.sanpham.AdminProductForm;
import com.example.demodatn.dto.sanpham.InventoryForm;
import com.example.demodatn.dto.sanpham.VariantForm;
import com.example.demodatn.entity.donhang.Order;
import com.example.demodatn.entity.sanpham.Product;
import com.example.demodatn.entity.sanpham.ProductVariant;
import com.example.demodatn.exception.CatalogValidationException;
import com.example.demodatn.repository.donhang.OrderRepository;
import com.example.demodatn.repository.sanpham.AdminCatalogQuery;
import com.example.demodatn.repository.sanpham.CategoryRepository;
import com.example.demodatn.repository.sanpham.InventoryLogRepository;
import com.example.demodatn.repository.sanpham.MaterialRepository;
import com.example.demodatn.repository.sanpham.ProductRepository;
import com.example.demodatn.repository.sanpham.ProductVariantRepository;
import com.example.demodatn.repository.sanpham.SizeRepository;
import com.example.demodatn.service.donhang.OrderService;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@WithMockUser(username="phase6-test",roles="ADMIN")
class CatalogIntegrationTest {
    @org.springframework.beans.factory.annotation.Autowired com.example.demodatn.repository.sanpham.ColorRepository phase5Colors;
    @org.springframework.beans.factory.annotation.Autowired com.example.demodatn.repository.sanpham.SoleRepository phase5Soles;

    @Autowired AdminCatalogService catalog;
    @Autowired InventoryService inventory;
    @Autowired InventoryLogRepository logs;
    @Autowired ProductRepository products;
    @Autowired ProductVariantRepository variants;
    @Autowired CategoryRepository categories;
    @Autowired SizeRepository sizes;
    @Autowired MaterialRepository materials;
    @Autowired OrderService orders;
    @Autowired com.example.demodatn.repository.nhanvien.EmployeeRepository employees;
    @Autowired OrderRepository orderRepository;
    @Autowired AdminCatalogQuery query;
    @Autowired Validator validator;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;
    @Autowired PlatformTransactionManager transactionManager;
    @TempDir Path temp;

    private AdminProductForm productForm(String code) {
        AdminProductForm f=new AdminProductForm();f.setInitialColorId(phase5Colors.findAll().get(0).getId());f.setInitialSoleId(phase5Soles.findAll().get(0).getId());
        f.setCode(code);f.setName("Kiểm thử Phase 6");f.setStatus(true);f.setCategoryId(categories.findAll().get(0).getId());
        try {
            var bytes=new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",bytes);
            f.setImage(new MockMultipartFile("image","fixture.png","image/png",bytes.toByteArray()));
        } catch(java.io.IOException ex) { throw new IllegalStateException(ex); }
        return f;
    }
    private Product product() { return catalog.saveProduct(productForm("T6-"+UUID.randomUUID())); }
    private VariantForm variantForm(Product p,int quantity) {
        VariantForm f=new VariantForm();f.setColorId(phase5Colors.findAll().get(0).getId());f.setSoleId(phase5Soles.findAll().get(0).getId());f.setProductId(p.getId());f.setSizeId(sizes.findAll().get(0).getId());f.setMaterialId(materials.findAll().get(0).getId());
        f.setPrice(new BigDecimal("150000"));f.setQuantity(quantity);return f;
    }
    private InventoryForm adjustment(ProductVariant v,String mode,int amount,String reason) {
        InventoryForm f=new InventoryForm();f.setVariantId(v.getId());f.setMode(mode);f.setQuantity(amount);
        f.setExpectedQuantity(v.getQuantity());f.setReason(reason);return f;
    }
    private CartItemDto item(ProductVariant v,int quantity) {
        return new CartItemDto(v.getProduct().getId(),v.getId(),v.getProduct().getName(),v.getSize().getSizeValue(),BigDecimal.ONE,quantity);
    }
    @Test @Transactional
    void stockLedgerMatchesEveryAdjustmentAndRejectsStaleRepeat() {
        ProductVariant v=catalog.saveVariant(variantForm(product(),10));
        InventoryForm change=adjustment(v,"ADD",5,"Nhập hàng thử");
        inventory.adjust(change);
        assertEquals(15,v.getQuantity());
        var history=query.history(v.getId());
        assertEquals(2,history.size());
        assertEquals(5,history.get(0).get("quantity_change"));
        assertEquals("phase6-test",history.get(0).get("actor"));
        assertThrows(CatalogValidationException.class,()->inventory.adjust(change));
        assertEquals(15,v.getQuantity());
    }
    @Test @Transactional
    void unchangedAdjustmentDoesNotClaimToWriteALog() {
        ProductVariant v=catalog.saveVariant(variantForm(product(),10));
        assertThrows(CatalogValidationException.class,()->inventory.adjust(adjustment(v,"SET",10,"Không đổi")));
        assertEquals(1,query.history(v.getId()).size());
    }
    @Test @Transactional @WithMockUser(username="admin",roles="ADMIN")
    void guestPosCheckoutWritesOrderWithoutCreatingDuplicateGuestCustomer() throws Exception {
        ProductVariant v=catalog.saveVariant(variantForm(product(),10));
        var session=new org.springframework.mock.web.MockHttpSession();
        session.setAttribute("posCart",new ArrayList<>(List.of(item(v,2))));
        session.setAttribute("posCheckoutToken","test-token");
        long before=orderRepository.count();
        var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        mvc.perform(post("/admin/pos/checkout").session(session).with(csrf()).param("paymentMethod","CASH").param("cashReceived","300000").param("checkoutToken","test-token"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("/admin/pos/success/*"));
        assertEquals(before+1,orderRepository.count());
        assertEquals(8,v.getQuantity());
        assertNull(session.getAttribute("posCart"));
    }
    @Test @WithMockUser(roles="USER")
    void ordinaryUserCannotReadOrWriteCatalogAdminRoutes() throws Exception {
        var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        for(String path:List.of("/admin/products-crud","/admin/products-crud/create","/admin/variants","/admin/categories","/admin/sizes","/admin/inventory"))
            mvc.perform(get(path)).andExpect(status().isForbidden());
        for(String path:List.of("/admin/products-crud/save","/admin/variants/save","/admin/categories/save","/admin/sizes/save","/admin/inventory/adjust"))
            mvc.perform(post(path).with(csrf())).andExpect(status().isForbidden());
    }
    @Test @Transactional
    void posUsesCurrentPriceAndCancelRestoresExactlyOnce() {
        ProductVariant v=catalog.saveVariant(variantForm(product(),10));
        Order order=orders.createPosOrder(List.of(item(v,2)),null,employees.findByUser_Username("admin").orElseThrow(),null,"CASH","Quầy",new BigDecimal("300000"));
        assertEquals(0,new BigDecimal("300000").compareTo(order.getFinalAmount()));
        assertEquals(8,v.getQuantity());
        assertEquals("Kiểm thử Phase 6",order.getOrderDetails().get(0).getProductName());
        orders.updateStatus(order.getId(),"CANCELLED");
        orders.updateStatus(order.getId(),"CANCELLED");
        assertEquals(10,v.getQuantity());
        assertEquals(3,query.history(v.getId()).size());
        assertThrows(IllegalArgumentException.class,()->orders.updateStatus(order.getId(),"PENDING"));
    }
    @Test @Transactional
    void onlineSaleAndCancellationShareTheStockLedger() {
        ProductVariant v=catalog.saveVariant(variantForm(product(),5));
        CheckoutRequest request=new CheckoutRequest();request.setProvinceId(jdbc.queryForObject("SELECT TOP 1 id FROM tinh_thanh ORDER BY id",Integer.class));request.setWardId(jdbc.queryForObject("SELECT TOP 1 id FROM phuong_xa WHERE tinh_thanh_id=? ORDER BY id",Integer.class,request.getProvinceId()));request.setStreetAddress("Số 12, đường Nguyễn Huệ");
        request.setPaymentMethod("COD");request.setReceiverName("Phase 6 test");
        request.setPhone("0900000000");request.setShippingAddress("Địa chỉ kiểm thử");request.setStreetAddress("Địa chỉ kiểm thử");
        Order order=orders.placeOrder(request,List.of(item(v,2)),null,BigDecimal.ZERO);
        assertEquals(5,v.getQuantity());
        orders.updateStatus(order.getId(),"CONFIRMED");
        assertEquals(3,v.getQuantity());
        assertTrue(query.history(v.getId()).get(0).get("reason").toString().contains(order.getOrderCode()));
        orders.updateStatus(order.getId(),"CANCELLED");
        assertEquals(5,v.getQuantity());
        assertEquals(3,query.history(v.getId()).size());
    }
    @Test @Transactional
    void referencedDeleteOnlyStopsSellingAndSnapshotsRemain() {
        Product p=product();ProductVariant v=catalog.saveVariant(variantForm(p,10));
        Order order=orders.createPosOrder(List.of(item(v,1)),null,employees.findByUser_Username("admin").orElseThrow(),null,"CASH","Quầy",new BigDecimal("150000"));
        var detail=order.getOrderDetails().get(0);
        String name=detail.getProductName();BigDecimal price=detail.getPrice();
        assertTrue(catalog.deleteProduct(p.getId()).contains("ngừng bán"));
        assertFalse(products.findById(p.getId()).orElseThrow().getStatus());
        assertEquals(name,detail.getProductName());assertEquals(price,detail.getPrice());
        assertTrue(variants.existsById(v.getId()));
    }
    @Test @Transactional
    void postedProductCodeIsIgnoredAndDuplicateVariantHasFieldError() throws Exception {
        Product p=product();catalog.saveVariant(variantForm(p,10));
        assertNotEquals(p.getCode(),catalog.saveProduct(productForm(p.getCode())).getCode());
        assertEquals("materialId",assertThrows(CatalogValidationException.class,()->catalog.saveVariant(variantForm(p,1))).getField());
        VariantForm invalid=variantForm(p,0);invalid.setPrice(new BigDecimal("-1"));
        assertTrue(validator.validate(invalid).stream().anyMatch(v->v.getPropertyPath().toString().equals("price")));
        var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        mvc.perform(post("/admin/variants/save").with(csrf()).param("productId",p.getId().toString())
                .param("sizeId",sizes.findAll().get(1).getId().toString()).param("price","-1").param("quantity","0").param("status","true"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("variant","price"));
    }
    @Test @Transactional
    void referencedLookupsCannotBeDeletedAndInactiveVariantsAreHidden() {
        Product p=product();ProductVariant v=catalog.saveVariant(variantForm(p,10));
        assertTrue(catalog.deleteLookup("categories",p.getCategory().getId()).startsWith("Không"));
        assertTrue(catalog.deleteLookup("sizes",v.getSize().getId()).startsWith("Không"));
        v.setStatus(false);variants.flush();
        assertTrue(variants.findByProduct_IdAndStatusTrue(p.getId()).isEmpty());
    }
    @Test @Transactional
    void productPaginationFilteringAndSortRunInSql() {
        String prefix="P6PAGE-"+UUID.randomUUID().toString().substring(0,8);
        for(int i=0;i<30;i++){AdminProductForm f=productForm(prefix+"-"+String.format("%02d",i));f.setName(prefix+" "+i);catalog.saveProduct(f);}
        var first=query.products(prefix,null,true,"code",0);
        var second=query.products(prefix,null,true,"code",1);
        assertEquals(30,first.getTotalElements());assertEquals(12,first.getNumberOfElements());assertEquals(12,second.getNumberOfElements());
        assertTrue(first.getContent().get(0).get("code").toString().matches("SP[0-9]{3,}"));
        assertEquals(2,query.products(prefix,null,true,"code",999).getNumber());
    }
    @Test
    void failedCheckoutRollsBackProductStockOrderAndLogsTogether() {
        String code="T6-rollback-"+UUID.randomUUID().toString().substring(0,8);
        long ordersBefore=orderRepository.count(), logsBefore=logs.count(), productsBefore=products.count();
        TransactionTemplate tx=new TransactionTemplate(transactionManager);
        assertThrows(IllegalArgumentException.class,()->tx.execute(status->{
            Product p=catalog.saveProduct(productForm(code));
            ProductVariant v=catalog.saveVariant(variantForm(p,1));
            return orders.createPosOrder(List.of(item(v,1),item(v,1)),null,employees.findByUser_Username("admin").orElseThrow(),null,"CASH","Quầy",new BigDecimal("300000"));
        }));
        assertEquals(productsBefore,products.count());
        assertEquals(ordersBefore,orderRepository.count());assertEquals(logsBefore,logs.count());
    }
    @Test
    void uploadChecksContentTypeSizeAndReencodesWithSafeName() throws Exception {
        ProductImageStorage storage=new ProductImageStorage(temp.toString(),1024);
        assertThrows(CatalogValidationException.class,()->storage.store(new MockMultipartFile("image","bad.png","image/png","not an image".getBytes())));
        assertThrows(CatalogValidationException.class,()->storage.store(new MockMultipartFile("image","bad.svg","image/svg+xml","<svg/>".getBytes())));
        assertThrows(CatalogValidationException.class,()->storage.store(new MockMultipartFile("image","big.png","image/png",new byte[1025])));
        var image=new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",bytes);
        String url=storage.store(new MockMultipartFile("image","../../safe.png","image/png",bytes.toByteArray()));
        assertTrue(url.matches("/uploads/products/[a-f0-9-]+\\.png"));
        assertEquals(1,Files.list(temp).count());
    }
}
