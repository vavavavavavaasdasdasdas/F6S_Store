package com.example.demodatn.controller;

import com.example.demodatn.entity.sanpham.*;
import com.example.demodatn.repository.sanpham.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SpringBootTest @Transactional @WithMockUser(username="admin",roles="ADMIN")
class TablePresentationIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired ProductRepository products;
    @Autowired ProductVariantRepository variants;
    @Autowired MaterialRepository materials;
    @Autowired CategoryRepository categories;
    @Autowired SizeRepository sizes;

    @Test void serialNumbersContinueAtThirteenAndFieldsRemainSeparate() throws Exception {
        String database=jdbc.queryForObject("SELECT DB_NAME()",String.class);
        assertTrue(database.equals("shop_verify")||database.startsWith("shopbangiay_kt_"));
        String marker="SG2-"+UUID.randomUUID();
        var material=new Material();material.setName(marker);material=materials.saveAndFlush(material);
        for(int i=0;i<14;i++) {
            var product=new Product();product.setCode(marker+"-"+i);product.setName("Giày bảng Nguyễn Huệ "+i);
            product.setCategory(categories.findAll().get(0));products.saveAndFlush(product);
            var variant=new ProductVariant();variant.setProduct(product);variant.setMaterial(material);
            variant.setSize(sizes.findAll().get(0));variant.setPrice(BigDecimal.valueOf(123456));variant.setQuantity(7);variants.saveAndFlush(variant);
        }
        var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        String first=mvc.perform(get("/admin/products-crud").param("keyword",marker)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String second=mvc.perform(get("/admin/products-crud").param("keyword",marker).param("page","1")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(Pattern.compile("<tbody>\\s*<tr[^>]*>\\s*<td[^>]*>1</td>").matcher(first).find());
        assertTrue(Pattern.compile("<tbody>\\s*<tr[^>]*>\\s*<td[^>]*>13</td>").matcher(second).find());
        assertTrue(second.contains("<th>Mã</th><th>Tên sản phẩm</th>"));
        assertFalse(second.contains("Mã / Tên"));
        assertTrue(second.contains("keyword="+marker));
        String inventory=mvc.perform(get("/admin/inventory").param("materialId",material.getId().toString()).param("page","1"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(Pattern.compile("<tbody>\\s*<tr[^>]*>\\s*<td[^>]*>13</td>").matcher(inventory).find());
        assertTrue(inventory.contains("<th>Mã</th><th>Tên sản phẩm</th><th>Kích cỡ</th><th>Chất liệu</th>"));
        assertEquals(14,products.findAll().stream().filter(p->p.getCode().startsWith(marker)).count());
    }
}
