package com.example.demodatn.service.sanpham;

import com.example.demodatn.dto.sanpham.*;
import com.example.demodatn.entity.sanpham.*;
import com.example.demodatn.exception.CatalogValidationException;
import com.example.demodatn.repository.sanpham.*;
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
class EditableVariantIntegrationTest {
    @Autowired AdminCatalogService catalog;
    @Autowired ProductRepository products;
    @Autowired ProductVariantRepository variants;
    @Autowired SizeRepository sizes;
    @Autowired ColorRepository colors;
    @Autowired MaterialRepository materials;
    @Autowired SoleRepository soles;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;
    MockMvc mvc;
    @BeforeEach void setup(){assertTrue(jdbc.queryForObject("SELECT DB_NAME()",String.class).startsWith("shopbangiay_kt_"));mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();}
    Product product(){var p=new Product();p.setCode("EDIT-"+UUID.randomUUID());p.setName("Giày chỉnh trực tiếp");p.setStatus(false);return products.saveAndFlush(p);}
    VariantForm form(Product p){var f=new VariantForm();f.setProductId(p.getId());f.setSizeId(sizes.findAll().get(0).getId());f.setColorId(colors.findAll().get(0).getId());f.setMaterialId(materials.findAll().get(0).getId());f.setSoleId(soles.findAll().get(0).getId());f.setPrice(BigDecimal.valueOf(180000));f.setQuantity(8);return f;}
    VariantBatchForm batch(Product p){var single=form(p);var f=new VariantBatchForm();f.setProductId(p.getId());f.setSizeIds(sizes.findAll().stream().limit(3).map(Size::getId).toList());f.setColorId(single.getColorId());f.setMaterialId(single.getMaterialId());f.setSoleId(single.getSoleId());f.setPrice(single.getPrice());f.setQuantity(4);return f;}
    @Test void editUsesInventoryAdjustmentAndKeepsNewPriceAndAttributesAfterRefresh(){
        var p=product();var f=form(p);var v=catalog.saveVariant(f);f.setId(v.getId());f.setExpectedQuantity(8);f.setQuantity(11);f.setPrice(BigDecimal.valueOf(222000));f.setColorId(colors.findAll().get(1).getId());f.setSoleId(soles.findAll().get(1).getId());f.setReason("Chỉnh trên trang sản phẩm");catalog.saveVariant(f);variants.flush();
        var row=jdbc.queryForMap("SELECT price,quantity,mau_sac_id,de_giay_id FROM product_variants WHERE id=?",v.getId());assertEquals(11,row.get("quantity"));assertEquals(0,new BigDecimal(row.get("price").toString()).compareTo(f.getPrice()));assertEquals(f.getColorId(),row.get("mau_sac_id"));assertEquals(f.getSoleId(),row.get("de_giay_id"));
        var log=jdbc.queryForMap("SELECT TOP 1 quantity_before,quantity_change,quantity_after,reason,actor FROM inventory_logs WHERE variant_id=? ORDER BY id DESC",v.getId());assertEquals(8,log.get("quantity_before"));assertEquals(3,log.get("quantity_change"));assertEquals(11,log.get("quantity_after"));assertEquals("admin",log.get("actor"));System.out.println("GD6 adjustment "+row+" log="+log);
    }
    @Test void staleExpectedQuantityRejectsAllFieldsBeforeWriting(){
        var p=product();var f=form(p);var v=catalog.saveVariant(f);f.setId(v.getId());f.setExpectedQuantity(7);f.setQuantity(12);f.setPrice(BigDecimal.valueOf(990000));f.setReason("Phiên cũ");assertEquals("Tồn kho đã thay đổi, tải lại",assertThrows(CatalogValidationException.class,()->catalog.saveVariant(f)).getMessage());assertEquals(8,v.getQuantity());assertEquals(0,v.getPrice().compareTo(BigDecimal.valueOf(180000)));
    }
    @Test void batchCreatesThreeSizesAndZeroInitialStockStillHasLogs(){
        var p=product();var f=batch(p);f.setQuantity(0);var result=catalog.createVariants(f);variants.flush();assertEquals(3,result.size());assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM inventory_logs l JOIN product_variants v ON v.id=l.variant_id WHERE v.product_id=? AND l.quantity_before=0 AND l.quantity_after=0",Integer.class,p.getId()));
    }
    @Test @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void mixedBatchDuplicateIsReportedBeforeAnyPartialCommitViaHttp()throws Exception{
        var p=product();var f=form(p);catalog.saveVariant(f);int before=jdbc.queryForObject("SELECT COUNT(*) FROM product_variants WHERE product_id=?",Integer.class,p.getId());
        mvc.perform(post("/admin/products-crud/"+p.getId()+"/variants/batch").with(csrf()).param("sizeIds",sizes.findAll().get(1).getId().toString(),f.getSizeId().toString()).param("colorId",f.getColorId().toString()).param("materialId",f.getMaterialId().toString()).param("soleId",f.getSoleId().toString()).param("price","180000").param("quantity","4")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.sizeIds").exists());
        assertEquals(before,jdbc.queryForObject("SELECT COUNT(*) FROM product_variants WHERE product_id=?",Integer.class,p.getId()));
    }
    @Test void integerMoneyRequiredButDecimalZerosRemainValid(){
        var p=product();var f=form(p);f.setPrice(new BigDecimal("123000.50"));assertEquals("price",assertThrows(CatalogValidationException.class,()->catalog.saveVariant(f)).getField());f.setPrice(new BigDecimal("123000.00"));assertNotNull(catalog.saveVariant(f).getId());
    }
    @Test void deleteUnsoldVariantRemovesOnlyItsInventoryLogs(){
        var p=product();var v=catalog.saveVariant(form(p));var id=v.getId();catalog.deleteVariant(p.getId(),id);assertFalse(variants.existsById(id));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM inventory_logs WHERE variant_id=?",Integer.class,id));
    }
    @Test void soldVariantDeleteRejectedWithStopSellingAdvice(){
        Integer id=jdbc.queryForObject("SELECT TOP 1 product_variant_id FROM order_details ORDER BY id",Integer.class);var v=catalog.variant(id);assertTrue(assertThrows(CatalogValidationException.class,()->catalog.deleteVariant(v.getProduct().getId(),id)).getMessage().contains("Ngừng bán"));
    }
    @Test void changingPriceKeepsOldOrderSnapshotsAndWarnsAnOpenCart()throws Exception{
        Integer id=jdbc.queryForObject("SELECT TOP 1 v.id FROM product_variants v JOIN products p ON p.id=v.product_id JOIN order_details d ON d.product_variant_id=v.id WHERE v.status=1 AND p.status=1 AND v.quantity>0 ORDER BY v.id",Integer.class);
        var v=catalog.variant(id);var snapshots=jdbc.queryForList("SELECT id,price,total_price FROM order_details WHERE product_variant_id=? ORDER BY id",id);
        var oldPrice=v.getPrice();var cart=new com.example.demodatn.dto.donhang.CartItemDto(v.getProduct().getId(),id,v.getProduct().getName(),v.getSize().getSizeValue(),oldPrice,1);
        var form=new VariantForm();form.setId(id);form.setProductId(v.getProduct().getId());form.setSizeId(v.getSize().getId());form.setColorId(v.getColor().getId());form.setMaterialId(v.getMaterial().getId());form.setSoleId(v.getSole().getId());form.setPrice(oldPrice.add(BigDecimal.valueOf(1000)));form.setQuantity(v.getQuantity());form.setExpectedQuantity(v.getQuantity());form.setStatus(true);catalog.saveVariant(form);variants.flush();
        assertEquals(snapshots,jdbc.queryForList("SELECT id,price,total_price FROM order_details WHERE product_variant_id=? ORDER BY id",id));
        var session=new org.springframework.mock.web.MockHttpSession();session.setAttribute("cart",new ArrayList<>(List.of(cart)));
        var model=mvc.perform(get("/cart").session(session)).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Giá sản phẩm đã thay đổi"))).andReturn().getModelAndView().getModel();
        var lines=(List<com.example.demodatn.controller.user.CartController.CartViewItem>)model.get("cartItems");assertTrue(lines.get(0).isPriceChanged());assertFalse((Boolean)model.get("hasWarning"));
    }
    @Test void eightSecurityGroupsAndCrossProductOwnership()throws Exception{
        var p=product();var v=catalog.saveVariant(form(p));String root="/admin/products-crud/"+p.getId()+"/variants";
        mvc.perform(get("/admin/products-crud/view/"+p.getId()).with(anonymous())).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/products-crud/view/"+p.getId()).with(user("ordinary").roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(get("/admin/products-crud/view/"+p.getId())).andExpect(status().isOk());
        mvc.perform(post(root+"/batch").with(anonymous()).with(csrf())).andExpect(status().is3xxRedirection());
        for(String path:List.of(root+"/inline-save",root+"/batch",root+"/"+v.getId()+"/delete",root+"/"+v.getId()+"/status")){
            mvc.perform(post(path).with(user("ordinary").roles("USER")).with(csrf())).andExpect(status().isForbidden());
            mvc.perform(post(path)).andExpect(status().isForbidden());
            mvc.perform(post(path).with(csrf().useInvalidToken())).andExpect(status().isForbidden());
        }
        var other=product();mvc.perform(post("/admin/products-crud/"+other.getId()+"/variants/"+v.getId()+"/delete").with(csrf())).andExpect(status().isNotFound());
    }
}
