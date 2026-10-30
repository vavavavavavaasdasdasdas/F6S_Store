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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest @Transactional @WithMockUser(username="admin",roles="ADMIN")
class SoleIntegrationTest {
    @org.springframework.beans.factory.annotation.Autowired com.example.demodatn.repository.sanpham.ColorRepository phase5Colors;
    @org.springframework.beans.factory.annotation.Autowired com.example.demodatn.repository.sanpham.SoleRepository phase5Soles;

    @Autowired SoleService service;
    @Autowired SoleRepository soles;
    @Autowired ProductRepository products;
    @Autowired AdminCatalogService catalog;
    @Autowired AdminCatalogQuery query;
    @Autowired ProductFilterService filters;
    @Autowired CategoryRepository categories;
    @Autowired ProductVariantRepository variants;
    @Autowired SizeRepository sizes;
    @Autowired MaterialRepository materials;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;

    @BeforeEach void databaseGuard(){String name=jdbc.queryForObject("SELECT DB_NAME()",String.class);assertTrue(name.equals("shop_verify")||name.startsWith("shopbangiay_kt"));}
    private Sole sole(boolean active){var form=new LookupForm();form.setValue("Đế Nguyễn Huệ "+UUID.randomUUID());form.setStatus(active);return service.save(form);}
    private AdminProductForm form(Integer soleId){var f=new AdminProductForm();f.setInitialColorId(phase5Colors.findAll().get(0).getId());f.setInitialSoleId(phase5Soles.findAll().get(0).getId());f.setCode("SG1-"+UUID.randomUUID());f.setName("Giày thử đế Nguyễn Huệ");f.setCategoryId(categories.findAll().get(0).getId());f.setSoleId(soleId);f.setStatus(false);return f;}

    @Test void productFormIgnoresLegacySolePostsAndRetainsExistingHint(){
        var first=sole(true);var second=sole(true);var f=form(first.getId());
        var p=catalog.saveProduct(f);assertNull(p.getSole());
        p.setSole(first);products.saveAndFlush(p);
        f.setId(p.getId());f.setSoleId(second.getId());catalog.saveProduct(f);
        assertEquals(first.getId(),jdbc.queryForObject("SELECT de_giay_id FROM products WHERE id=?",Integer.class,p.getId()));
    }

    @Test void unicodeCaseInsensitiveUniquenessIsEnforcedByServiceAndDatabase(){
        var s=sole(true);String upper=s.getName().toUpperCase(Locale.ROOT);var duplicate=new LookupForm();duplicate.setValue(upper);
        assertThrows(CatalogValidationException.class,()->service.save(duplicate));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("INSERT de_giay(ten,trang_thai) VALUES(?,1)",upper));
        assertEquals(s.getName(),jdbc.queryForObject("SELECT ten FROM de_giay WHERE id=?",String.class,s.getId()));
        assertTrue(jdbc.queryForObject("SELECT collation_name FROM sys.columns WHERE object_id=OBJECT_ID('de_giay') AND name='ten'",String.class).contains("_CI_"));
    }

    @Test void usedSoleCannotBeDeletedAndBothProductFiltersKeepTwelveRowPagination()throws Exception{
        var s=sole(true);var other=sole(true);
        for(int i=0;i<14;i++){
            var p=new Product();p.setCode("SG1-"+UUID.randomUUID());p.setName("Giày Nguyễn Huệ "+i);p.setCategory(categories.findAll().get(0));p.setSole(i==13?other:s);p.setImageUrl("/anh/anh-san-pham-thay-the.svg");products.saveAndFlush(p);
            var v=new ProductVariant();v.setProduct(p);v.setSize(sizes.findAll().get(0));v.setMaterial(materials.findAll().get(0));v.setColor(phase5Colors.findAll().get(0));v.setSole(i==13?other:s);v.setPrice(BigDecimal.valueOf(123000));v.setQuantity(3);variants.saveAndFlush(v);
        }
        var admin=query.products("",null,null,"newest",0,null,s.getId());assertEquals(13,admin.getTotalElements());assertEquals(12,admin.getContent().size());assertTrue(admin.stream().allMatch(p->s.getName().equals(p.get("sole_names"))));
        var user=filters.filterProducts(null,null,null,null,null,null,"newest",0,12,null,s.getId());assertEquals(13,user.getTotalElements());assertEquals(12,user.getContent().size());
        assertEquals(1,filters.filterProducts(null,null,null,null,null,null,"newest",1,12,null,s.getId()).getNumberOfElements());
        assertTrue(service.delete(s.getId()).startsWith("Không"));assertTrue(soles.existsById(s.getId()));
        var unused=sole(true);service.delete(unused.getId());assertFalse(soles.existsById(unused.getId()));
        service.toggle(s.getId());assertEquals(13,filters.filterProducts(null,null,null,null,null,null,"newest",0,12,null,s.getId()).getTotalElements());
        var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        mvc.perform(get("/products").param("soleId",s.getId().toString())).andExpect(status().isOk()).andExpect(content().string(containsString("soleId="+s.getId())));
    }

    @Test void labelsAndSoleCrudRoutesUseExistingRoleAndCsrfProtection()throws Exception{
        var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        mvc.perform(get("/admin/soles")).andExpect(status().isOk()).andExpect(content().string(containsString("Đế giày")));
        mvc.perform(get("/admin/categories")).andExpect(status().isOk()).andExpect(content().string(containsString("Thương hiệu")));
        mvc.perform(get("/admin/account")).andExpect(status().isOk()).andExpect(content().string(containsString("Hồ sơ của tôi")));
        mvc.perform(get("/admin/soles").with(anonymous())).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/soles").with(user("ordinary").roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(post("/admin/soles/save").param("value","Đế thiếu CSRF")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/soles/save").with(csrf().useInvalidToken()).param("value","Đế CSRF sai")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/soles/save").with(csrf()).param("value","")).andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("lookup","value"));
    }
}
