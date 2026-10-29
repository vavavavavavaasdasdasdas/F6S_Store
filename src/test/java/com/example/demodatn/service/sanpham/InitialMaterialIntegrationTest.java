package com.example.demodatn.service.sanpham;

import com.example.demodatn.dto.sanpham.AdminProductForm;
import com.example.demodatn.entity.sanpham.*;
import com.example.demodatn.repository.sanpham.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@WithMockUser(username="admin",roles="ADMIN")
class InitialMaterialIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;
    @Autowired AdminCatalogService catalog;
    @Autowired MaterialRepository materials;
    @Autowired ProductRepository products;
    @Autowired ProductVariantRepository variants;
    @Autowired SizeRepository sizes;
    @Autowired CategoryRepository categories;
    @Autowired PlatformTransactionManager transactionManager;
    MockMvc mvc;
    @BeforeEach void setup() {
        assertTrue((Set.of("shopbangiay_kt","shop_verify","shopbangiay_thu").contains(jdbc.queryForObject("SELECT DB_NAME()",String.class)) || jdbc.queryForObject("SELECT DB_NAME()",String.class).startsWith("shopbangiay_kt_")));
        mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    AdminProductForm form() {
        var f=new AdminProductForm();f.setCode("INIT-"+UUID.randomUUID());f.setName("Giày thử khởi tạo");
        f.setCategoryId(categories.findAll().get(0).getId());f.setStatus(false);
        f.setInitialSizeIds(sizes.findAll().stream().limit(2).map(Size::getId).toList());
        f.setInitialMaterialId(materials.findByStatusTrueOrderByNameAscIdAsc().get(0).getId());
        f.setInitialPrice(new BigDecimal("321000.50"));f.setInitialQuantity(7);return f;
    }
    @Test @org.springframework.transaction.annotation.Transactional
    void formPostCreatesTwoVariantsAndExactInitializationLogs() throws Exception {
        var f=form();
        var saved=mvc.perform(multipart("/admin/products-crud/save").with(csrf()).param("code",f.getCode()).param("name",f.getName())
            .param("categoryId",f.getCategoryId().toString()).param("status","false")
            .param("initialMaterialId",f.getInitialMaterialId().toString()).param("initialSizeIds",f.getInitialSizeIds().stream().map(Object::toString).toArray(String[]::new))
            .param("initialPrice","321000.50").param("initialQuantity","7")).andExpect(status().is3xxRedirection()).andReturn();
        int savedId=Integer.parseInt(saved.getResponse().getRedirectedUrl().replaceAll(".*/",""));
        var rows=jdbc.queryForList("SELECT v.price,v.quantity,v.chat_lieu_id,l.quantity_before,l.quantity_change,l.quantity_after,l.reason FROM product_variants v JOIN products p ON p.id=v.product_id JOIN inventory_logs l ON l.variant_id=v.id WHERE p.id=?",savedId);
        assertEquals(2,rows.size());
        for(var r:rows) {assertEquals(0,new BigDecimal(r.get("price").toString()).compareTo(f.getInitialPrice()));assertEquals(7,r.get("quantity"));assertEquals(f.getInitialMaterialId(),r.get("chat_lieu_id"));assertEquals(0,r.get("quantity_before"));assertEquals(7,r.get("quantity_change"));assertEquals(7,r.get("quantity_after"));assertEquals("Khởi tạo",r.get("reason"));}
        System.out.println("PHASE_A SQL two sizes: "+rows);
    }
    @Test @org.springframework.transaction.annotation.Transactional
    void missingMaterialAndPriceShowErrorsBesideBothFieldsWithoutCreatingProduct() throws Exception {
        var f=form();long beforeProducts=products.count();mvc.perform(multipart("/admin/products-crud/save").with(csrf()).param("code",f.getCode()).param("name",f.getName())
            .param("categoryId",f.getCategoryId().toString()).param("status","false").param("initialSizeIds",f.getInitialSizeIds().get(0).toString()))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("product","initialMaterialId","initialPrice"))
            .andExpect(content().string(containsString("Chọn chất liệu khi chọn kích cỡ ban đầu.")));
        assertEquals(beforeProducts,products.count());
    }
    @Test @org.springframework.transaction.annotation.Transactional
    void emptySizesKeepProductOnlyAndZeroStockStillWritesInitialization() {
        var f=form();f.setInitialSizeIds(List.of());f.setInitialMaterialId(null);f.setInitialPrice(null);
        assertTrue(variants.findByProduct_Id(catalog.saveProduct(f).getId()).isEmpty());
        f=form();f.setInitialQuantity(0);int id=catalog.saveProduct(f).getId();
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM inventory_logs l JOIN product_variants v ON v.id=l.variant_id WHERE v.product_id=? AND l.quantity_before=0 AND l.quantity_after=0 AND l.reason=N'Khởi tạo'",Integer.class,id));
    }
    @Test void rollbackUndoesProductVariantsAndLogsTogether() {
        var f=form();int before=jdbc.queryForObject("SELECT COUNT(*) FROM inventory_logs",Integer.class);
        assertThrows(IllegalStateException.class,()->new TransactionTemplate(transactionManager).execute(s->{
            int id=catalog.saveProduct(f).getId();assertEquals(2,variants.findByProduct_Id(id).size());throw new IllegalStateException("simulate transaction failure");
        }));
        assertFalse(products.existsByCodeIgnoreCaseAndIdNot(f.getCode(),-1));
        assertEquals(before,jdbc.queryForObject("SELECT COUNT(*) FROM inventory_logs",Integer.class));
        System.out.println("PHASE_A rollback product=0 variants=0 logs before="+before+" after="+before);
    }
    @Test @org.springframework.transaction.annotation.Transactional
    void materialColumnFiltersLabelsUsageAndPreselectionMatchDatabase() throws Exception {
        var f=form();int id=catalog.saveProduct(f).getId();String name=materials.findById(f.getInitialMaterialId()).orElseThrow().getName();
        mvc.perform(get("/admin/products-crud").param("keyword",f.getCode()).param("materialId",f.getInitialMaterialId().toString()))
            .andExpect(status().isOk()).andExpect(content().string(containsString(name))).andExpect(content().string(containsString(f.getName())));
        mvc.perform(get("/admin/products-crud/edit/"+id)).andExpect(status().isOk()).andExpect(content().string(containsString(name))).andExpect(content().string(containsString("Quản lý biến thể")));
        mvc.perform(get("/admin/materials")).andExpect(status().isOk()).andExpect(content().string(containsString("Đang dùng")));
        var r=mvc.perform(get("/admin/variants/create").param("productId",""+id)).andExpect(status().isOk()).andReturn();
        assertEquals(id,((com.example.demodatn.dto.sanpham.VariantForm)r.getModelAndView().getModel().get("variant")).getProductId());
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM product_variants WHERE product_id=? AND chat_lieu_id=?",Integer.class,id,f.getInitialMaterialId()));
    }
    @Test @org.springframework.transaction.annotation.Transactional
    void hiddenMaterialIsAbsentForCreationButExistingVariantCanBeEdited() throws Exception {
        var m=new Material();m.setName("Chất liệu ẩn "+UUID.randomUUID());materials.saveAndFlush(m);
        var f=form();f.setInitialMaterialId(m.getId());int id=catalog.saveProduct(f).getId();
        mvc.perform(post("/admin/materials/toggle-status/"+m.getId()).with(csrf())).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/products-crud/create")).andExpect(content().string(not(containsString(m.getName()))));
        mvc.perform(get("/admin/variants/create")).andExpect(content().string(not(containsString(m.getName()))));
        var v=variants.findByProduct_Id(id).get(0);
        mvc.perform(get("/admin/variants/edit/"+v.getId())).andExpect(content().string(containsString(m.getName())));
        mvc.perform(post("/admin/variants/save").with(csrf()).param("id",v.getId().toString()).param("productId",""+id).param("sizeId",v.getSize().getId().toString())
            .param("materialId",m.getId().toString()).param("price","321000.50").param("quantity","7").param("expectedQuantity","7").param("status","true"))
            .andExpect(status().is3xxRedirection());
        assertTrue(catalog.deleteLookup("materials",m.getId()).startsWith("Không"));
    }
    @Test @org.springframework.transaction.annotation.Transactional
    void summaryCapsThreeActiveMaterialsAndPaginationKeepsMaterialFilter() throws Exception {
        var f=form();int id=catalog.saveProduct(f).getId();
        for(int n=0;n<4;n++) {
            var m=new Material();m.setName("Tóm tắt "+n+" "+UUID.randomUUID());materials.saveAndFlush(m);
            var v=new ProductVariant();v.setProduct(products.findById(id).orElseThrow());v.setSize(sizes.findAll().get(0));
            v.setMaterial(m);v.setPrice(BigDecimal.TEN);v.setQuantity(1);v.setStatus(n<3);variants.saveAndFlush(v);
        }
        var r=mvc.perform(get("/admin/products-crud").param("keyword",f.getCode())).andExpect(status().isOk()).andReturn();
        var page=(org.springframework.data.domain.Page<?>)r.getModelAndView().getModel().get("productPage");
        String summary=((Map<?,?>)page.getContent().get(0)).get("material_names").toString();
        assertTrue(summary.endsWith(" +1"),summary);
        assertFalse(summary.contains("Tóm tắt 3"));
        String code="PAGEA-"+UUID.randomUUID().toString().substring(0,8);
        for(int n=0;n<14;n++){f=form();f.setCode(code+"-"+n);f.setName(code+" "+n);catalog.saveProduct(f);}
        r=mvc.perform(get("/admin/products-crud").param("keyword",code).param("materialId",f.getInitialMaterialId().toString()))
                .andExpect(status().isOk()).andReturn();
        String markup=r.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(markup.contains("materialId="+f.getInitialMaterialId()));
        assertTrue(markup.contains("page=1"));
        page=(org.springframework.data.domain.Page<?>)r.getModelAndView().getModel().get("productPage");
        assertEquals(14,page.getTotalElements());
        System.out.println("PHASE_A material summary="+summary+"; filter total=14; pagination materialId="+f.getInitialMaterialId()+" retained");
    }
    @Test @org.springframework.transaction.annotation.Transactional
    void eightSecurityScenarios() throws Exception {
        mvc.perform(get("/admin/materials").with(anonymous())).andExpect(status().is3xxRedirection());System.out.println("SECURITY 1 anonymous admin GET=302");
        mvc.perform(get("/admin/products-crud").with(user("customer").roles("USER"))).andExpect(status().isForbidden());System.out.println("SECURITY 2 USER admin GET=403");
        mvc.perform(post("/admin/products-crud/save")).andExpect(status().isForbidden());System.out.println("SECURITY 3 product POST missing CSRF=403");
        mvc.perform(post("/admin/materials/toggle-status/1").with(csrf().useInvalidToken())).andExpect(status().isForbidden());System.out.println("SECURITY 4 hide invalid CSRF=403");
        mvc.perform(post("/admin/variants/save").with(csrf()).with(user("customer").roles("USER"))).andExpect(status().isForbidden());System.out.println("SECURITY 5 USER variant POST=403");
        var f=form();mvc.perform(multipart("/admin/products-crud/save").with(csrf()).param("code",f.getCode()).param("name",f.getName()).param("status","false").param("categoryId",f.getCategoryId().toString())
            .param("initialSizeIds","-1").param("initialMaterialId",f.getInitialMaterialId().toString()).param("initialPrice","1").param("initialQuantity","1"))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("product","initialSizeIds"));System.out.println("SECURITY 6 forged size rejected HTTP=200 field error");
        f=form();mvc.perform(multipart("/admin/products-crud/save").with(csrf()).param("code",f.getCode()).param("name",f.getName()).param("status","false").param("categoryId",f.getCategoryId().toString())
            .param("initialSizeIds",f.getInitialSizeIds().get(0).toString()).param("initialMaterialId",f.getInitialMaterialId().toString()).param("initialPrice","-1").param("initialQuantity","-1"))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("product","initialPrice","initialQuantity"));System.out.println("SECURITY 7 negative price/stock rejected HTTP=200 field errors");
        mvc.perform(get("/admin/products-crud").param("keyword","%' OR 1=1--").param("sort","p.id;DROP TABLE products"))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Không tìm thấy sản phẩm")));System.out.println("SECURITY 8 SQL injection filter/sort HTTP=200 zero products");
    }
}
