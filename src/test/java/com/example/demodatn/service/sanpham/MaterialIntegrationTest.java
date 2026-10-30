package com.example.demodatn.service.sanpham;
import com.example.demodatn.dto.donhang.CartItemDto;
import com.example.demodatn.dto.sanpham.VariantForm;
import com.example.demodatn.entity.sanpham.Material;
import com.example.demodatn.entity.sanpham.Product;
import com.example.demodatn.entity.sanpham.ProductVariant;
import com.example.demodatn.exception.CatalogValidationException;
import com.example.demodatn.repository.donhang.InvoiceRepository;
import com.example.demodatn.repository.nhanvien.EmployeeRepository;
import com.example.demodatn.repository.sanpham.AdminCatalogQuery;
import com.example.demodatn.repository.sanpham.MaterialRepository;
import com.example.demodatn.repository.sanpham.ProductRepository;
import com.example.demodatn.repository.sanpham.ProductVariantRepository;
import com.example.demodatn.repository.sanpham.SizeRepository;
import com.example.demodatn.service.donhang.OrderService;
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

@SpringBootTest @Transactional @WithMockUser(username="admin",roles="ADMIN")
class MaterialIntegrationTest {
    @org.springframework.beans.factory.annotation.Autowired com.example.demodatn.repository.sanpham.ColorRepository phase5Colors;
    @org.springframework.beans.factory.annotation.Autowired com.example.demodatn.repository.sanpham.SoleRepository phase5Soles;

    @Autowired MaterialRepository materials;
    @Autowired ProductRepository products;
    @Autowired ProductVariantRepository variants;
    @Autowired SizeRepository sizes;
    @Autowired AdminCatalogService catalog;
    @Autowired AdminCatalogQuery query;
    @Autowired ProductFilterService filter;
    @Autowired OrderService orders;
    @Autowired EmployeeRepository employees;
    @Autowired InvoiceRepository invoices;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;
    Material material(String name) { Material m=new Material();m.setName(name+UUID.randomUUID());return materials.saveAndFlush(m); }
    Product product() { Product p=new Product();p.setCode("MAT-"+UUID.randomUUID());p.setName("Giày kiểm thử chất liệu");return products.saveAndFlush(p); }
    VariantForm form(Product p,Material m,int price,int quantity) {var f=new VariantForm();f.setColorId(phase5Colors.findAll().get(0).getId());f.setSoleId(phase5Soles.findAll().get(0).getId());f.setProductId(p.getId());f.setSizeId(sizes.findAll().get(0).getId());f.setMaterialId(m.getId());f.setPrice(BigDecimal.valueOf(price));f.setQuantity(quantity);return f;}
    CartItemDto item(ProductVariant v) {return new CartItemDto(v.getProduct().getId(),v.getId(),v.getProduct().getName(),v.getSize().getSizeValue(),BigDecimal.ONE,1);}
    @BeforeEach void guard(){assertTrue((Set.of("shopbangiay_kt","shop_verify","shopbangiay_thu").contains(jdbc.queryForObject("SELECT DB_NAME()",String.class)) || jdbc.queryForObject("SELECT DB_NAME()",String.class).startsWith("shopbangiay_kt_")));}
    @Test void sameSizeDifferentMaterialsAreDistinctAndTripleDuplicateIsRejected(){
        var p=product();var m=material("Da kiểm thử ");var n=material("Vải kiểm thử ");
        var a=catalog.saveVariant(form(p,m,100000,5));var b=catalog.saveVariant(form(p,n,150000,7));
        assertNotEquals(a.getId(),b.getId());assertEquals(a.getSize().getId(),b.getSize().getId());
        assertEquals("materialId",assertThrows(CatalogValidationException.class,()->catalog.saveVariant(form(p,m,100000,5))).getField());
        assertTrue(catalog.deleteLookup("materials",m.getId()).startsWith("Không"));
        assertEquals(1,query.variantsPage(p.getId(),"all",0,"abc",m.getId()).getTotalElements());
        assertEquals(1,filter.filterProducts(p.getCode(),null,a.getSize().getId(),null,null,null,"newest",0,12,m.getId()).getTotalElements());
    }
    @Test void saleSnapshotsMaterialAndStockForEachVariantAndRenameDoesNotChangeHistory() throws Exception {
        var p=product();var m=material("Da trước đổi tên ");var n=material("Vải trước đổi tên ");String original=m.getName();
        var a=catalog.saveVariant(form(p,m,100000,5));var b=catalog.saveVariant(form(p,n,150000,7));
        var cart=List.of(item(a),item(b));var o=orders.createPosOrder(cart,null,employees.findByUser_Username("admin").orElseThrow(),null,"CASH","Tại quầy",BigDecimal.valueOf(300000));
        assertEquals(2,o.getOrderDetails().size());assertEquals(4,a.getQuantity());assertEquals(6,b.getQuantity());assertEquals(0,BigDecimal.valueOf(250000).compareTo(o.getFinalAmount()));
        m.setName("Tên mới "+UUID.randomUUID());materials.flush();
        assertEquals(original,jdbc.queryForObject("SELECT ten_chat_lieu FROM order_details WHERE order_id=? AND product_variant_id=?",String.class,o.getId(),a.getId()));
        var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        for(String url:List.of("/admin/orders/"+o.getId(),"/admin/invoices/"+invoices.findByOrder_Id(o.getId()).orElseThrow().getId()))mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString(original)));
    }
    @Test void inactiveMaterialIsNotSelectableAndMissingMaterialIsRejected() throws Exception {
        var p=product();var inactive=material("Chất liệu ngừng hoạt động ");inactive.setStatus(false);materials.flush();
        var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        mvc.perform(get("/admin/variants/create")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(inactive.getName()))));
        var r=mvc.perform(post("/admin/variants/save").with(csrf()).param("productId",p.getId().toString()).param("sizeId",sizes.findAll().get(0).getId().toString()).param("price","100000").param("quantity","1").param("status","true")).andExpect(status().isOk()).andReturn();
        assertTrue(r.getResponse().getContentAsString().contains("Chọn chất liệu."));
        assertThrows(CatalogValidationException.class,()->catalog.saveVariant(form(p,inactive,100000,1)));
    }
    @Test void unusedMaterialCanBeDeletedAndMaterialPagesUseConfiguredSize() throws Exception {
        var m=material("Xóa chất liệu chưa dùng ");catalog.deleteLookup("materials",m.getId());assertFalse(materials.existsById(m.getId()));
        for(int n=0;n<30;n++)material("Phân trang "+n+" ");
        var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        var response=mvc.perform(get("/admin/materials").param("page","1")).andExpect(status().isOk()).andReturn();
        var page=(org.springframework.data.domain.Page<?>)response.getModelAndView().getModel().get("items");
        assertEquals(12,page.getNumberOfElements());assertEquals(1,page.getNumber());
        for(String value:List.of("-1","abc","99999"))mvc.perform(get("/admin/materials").param("page",value)).andExpect(status().isOk());
    }
    @Test void materialCrudValidatesAndRequiresAdminAndCsrf() throws Exception {
        var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();var m=material("Chất liệu ");
        for(String url:List.of("/admin/materials","/admin/materials/create","/admin/materials/edit/"+m.getId(),"/admin/variants/create","/admin/inventory"))mvc.perform(get(url)).andExpect(status().isOk());
        mvc.perform(post("/admin/materials/save").with(csrf()).param("value",m.getName()).param("status","true")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Tên chất liệu đã tồn tại")));
        mvc.perform(post("/admin/materials/save").param("value","Không CSRF")).andExpect(status().isForbidden());
        mvc.perform(get("/admin/materials").with(user("ordinary").roles("USER"))).andExpect(status().isForbidden());
    }
}
