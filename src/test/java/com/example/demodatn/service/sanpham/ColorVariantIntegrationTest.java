package com.example.demodatn.service.sanpham;

import com.example.demodatn.dto.sanpham.*;
import com.example.demodatn.dto.donhang.CartItemDto;
import com.example.demodatn.entity.sanpham.*;
import com.example.demodatn.repository.sanpham.*;
import com.example.demodatn.exception.CatalogValidationException;
import com.example.demodatn.service.donhang.OrderService;
import com.example.demodatn.repository.nhanvien.EmployeeRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @Transactional @WithMockUser(username="admin",roles="ADMIN")
class ColorVariantIntegrationTest {
    @Autowired ColorRepository colors;
    @Autowired SoleRepository soles;
    @Autowired MaterialRepository materials;
    @Autowired SizeRepository sizes;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired ProductVariantRepository variants;
    @Autowired AdminCatalogService catalog;
    @Autowired ColorService colorService;
    @Autowired AdminCatalogQuery query;
    @Autowired OrderService orders;
    @Autowired EmployeeRepository employees;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;
    MockMvc mvc;
    @BeforeEach void setup() {
        assertTrue(jdbc.queryForObject("SELECT DB_NAME()",String.class).startsWith("shopbangiay_kt_"));
        mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    Color color(String name) {var f=new LookupForm();f.setValue(name);f.setHexCode("#123AbC");return colorService.save(f);}
    Product product() {var p=new Product();p.setCode("COLOR-"+UUID.randomUUID());p.setName("Giày hai màu Nguyễn Huệ");p.setCategory(categories.findAll().get(0));return products.saveAndFlush(p);}
    VariantForm form(Product p,Color c,Sole s) {
        var f=new VariantForm();f.setProductId(p.getId());f.setSizeId(sizes.findAll().get(0).getId());
        f.setColorId(c.getId());f.setSoleId(s.getId());f.setMaterialId(materials.findAll().get(0).getId());f.setPrice(BigDecimal.valueOf(123000));f.setQuantity(8);return f;
    }
    @Test void fiveAttributesPermitSameSizeDifferentColorAndSoleButRejectExactDuplicate() {
        var p=product();var c=color("Màu Ánh "+UUID.randomUUID());var c2=color("Màu Đỏ "+UUID.randomUUID());
        var s=soles.findAll();var a=catalog.saveVariant(form(p,c,s.get(0)));catalog.saveVariant(form(p,c2,s.get(0)));catalog.saveVariant(form(p,c,s.get(1)));
        assertEquals(3,variants.findByProduct_Id(p.getId()).size());
        assertEquals("materialId",assertThrows(CatalogValidationException.class,()->catalog.saveVariant(form(p,c,s.get(0)))).getField());
        assertEquals(8,a.getQuantity());
        System.out.println("GD5 five attributes: "+jdbc.queryForList("SELECT id,product_id,size_id,chat_lieu_id,mau_sac_id,de_giay_id,price,quantity FROM product_variants WHERE product_id=?",p.getId()));
    }
    @Test void colorNamesFoldCaseAndAccentsAndInvalidHexHasFieldError() {
        String suffix=UUID.randomUUID().toString();var c=color("Đỏ Ánh "+suffix);
        assertEquals(c.getId(),catalog.resolveOrCreateColor("do anh "+suffix.toUpperCase(Locale.ROOT),"colorId").getId());
        var f=new LookupForm();f.setValue("DO ANH "+suffix);assertThrows(CatalogValidationException.class,()->colorService.save(f));
        f.setValue("Màu khác "+suffix);f.setHexCode("#12345Z");assertEquals("hexCode",assertThrows(CatalogValidationException.class,()->colorService.save(f)).getField());
        assertEquals("#123ABC",c.getHexCode());
    }
    @Test void hiddenColorRetainedByExistingVariantButRejectedOnNewVariantAndCannotDeleteInUse() {
        var p=product();var c=color("Ẩn "+UUID.randomUUID());var f=form(p,c,soles.findAll().get(0));var v=catalog.saveVariant(f);
        colorService.toggle(c.getId());assertTrue(colorService.delete(c.getId()).startsWith("Không"));
        assertEquals("colorId",assertThrows(CatalogValidationException.class,()->catalog.saveVariant(form(p,c,soles.findAll().get(1)))).getField());
        f.setId(v.getId());f.setExpectedQuantity(v.getQuantity());catalog.saveVariant(f);assertEquals(c.getName(),v.getColor().getName());
        var unused=color("Chưa dùng "+UUID.randomUUID());colorService.delete(unused.getId());assertFalse(colors.existsById(unused.getId()));
    }
    @Test void hiddenSoleRemainsOnExistingVariantAndIsNotAnOptionForNewVariants() throws Exception {
        var sole=new Sole();sole.setName("Đế ẩn "+UUID.randomUUID());soles.saveAndFlush(sole);
        var p=product();var f=form(p,colors.findAll().get(0),sole);var v=catalog.saveVariant(f);
        sole.setStatus(false);soles.saveAndFlush(sole);
        var newForm=form(p,colors.findAll().get(0),sole);
        assertEquals("soleId",assertThrows(CatalogValidationException.class,()->catalog.saveVariant(newForm)).getField());
        f.setId(v.getId());f.setExpectedQuantity(v.getQuantity());catalog.saveVariant(f);
        var create=mvc.perform(get("/admin/variants/create")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        var edit=mvc.perform(get("/admin/variants/edit/"+v.getId())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertFalse(create.contains(sole.getName()));assertTrue(edit.contains(sole.getName()));
    }
    @Test void twoColorsKeepSeparateStockAndOrderSnapshotsAfterRename() throws Exception {
        var p=product();var red=color("Đỏ "+UUID.randomUUID());var blue=color("Xanh "+UUID.randomUUID());var sole=soles.findAll().get(0);
        var a=catalog.saveVariant(form(p,red,sole));var b=catalog.saveVariant(form(p,blue,sole));
        var cart=List.of(new CartItemDto(p.getId(),a.getId(),p.getName(),a.getSize().getSizeValue(),BigDecimal.ONE,1),new CartItemDto(p.getId(),b.getId(),p.getName(),b.getSize().getSizeValue(),BigDecimal.ONE,2));
        var order=orders.createPosOrder(cart,null,employees.findByUser_Username("admin").orElseThrow(),null,"CASH","Hai màu",BigDecimal.valueOf(500000));
        assertEquals(7,a.getQuantity());assertEquals(6,b.getQuantity());
        var names=jdbc.queryForList("SELECT product_variant_id,ten_mau_sac,ten_de_giay,quantity FROM order_details WHERE order_id=? ORDER BY product_variant_id",order.getId());
        assertEquals(red.getName(),names.get(0).get("ten_mau_sac"));assertEquals(blue.getName(),names.get(1).get("ten_mau_sac"));
        String old=red.getName();var f=new LookupForm();f.setId(red.getId());f.setValue("Đã đổi tên "+UUID.randomUUID());colorService.save(f);
        assertEquals(old,jdbc.queryForObject("SELECT ten_mau_sac FROM order_details WHERE order_id=? AND product_variant_id=?",String.class,order.getId(),a.getId()));
        System.out.println("GD5 snapshots and stock: "+names+" quantities="+a.getQuantity()+","+b.getQuantity());
    }
    @Test void filtersReadSellingVariantsAndIgnoreLegacyProductSoleHint() {
        var p=product();var c=color("Lọc "+UUID.randomUUID());var sole=soles.findAll().get(0);p.setSole(soles.findAll().get(1));products.saveAndFlush(p);
        var v=catalog.saveVariant(form(p,c,sole));
        var result=query.products(p.getCode(),null,null,"newest",0,v.getMaterial().getId(),sole.getId(),null,null,c.getId());
        assertEquals(1,result.getTotalElements());assertEquals(sole.getName(),result.getContent().get(0).get("sole_names"));
        catalog.variantStatus(v.getId(),false);
        variants.flush();
        assertEquals(0,query.products(p.getCode(),null,null,"newest",0,null,sole.getId(),null,null,c.getId()).getTotalElements());
    }
    @Test void eightSecurityCasesAndOldOrderDisplay() throws Exception {
        mvc.perform(get("/admin/colors").with(anonymous())).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/colors").with(user("ordinary").roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(get("/admin/colors")).andExpect(status().isOk());
        mvc.perform(post("/admin/colors/save").with(anonymous()).with(csrf()).param("value","Không được tạo")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/admin/colors/save").with(user("ordinary").roles("USER")).with(csrf()).param("value","Không được tạo")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/colors/save").param("value","Thiếu CSRF")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/colors/save").with(csrf().useInvalidToken()).param("value","Sai CSRF")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/variants/save").with(user("ordinary").roles("USER")).with(csrf())).andExpect(status().isForbidden());
        var detail=new com.example.demodatn.entity.donhang.OrderDetail();assertEquals("—",detail.getDisplayColorName());assertEquals("—",detail.getDisplaySoleName());
        System.out.println("GD5 SECURITY: guest GET=302 user GET=403 admin GET=200 guest POST+csrf=302 user POST+csrf=403 missing csrf=403 invalid csrf=403 user variant POST=403");
    }
}
