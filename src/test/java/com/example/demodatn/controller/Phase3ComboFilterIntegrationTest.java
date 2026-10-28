package com.example.demodatn.controller;

import com.example.demodatn.dto.nhanvien.EmployeeDto;
import com.example.demodatn.dto.sanpham.AdminProductForm;
import com.example.demodatn.dto.sanpham.VariantForm;
import com.example.demodatn.exception.CatalogValidationException;
import com.example.demodatn.repository.nhanvien.EmployeeRepository;
import com.example.demodatn.repository.sanpham.CategoryRepository;
import com.example.demodatn.repository.sanpham.MaterialRepository;
import com.example.demodatn.repository.sanpham.SizeRepository;
import com.example.demodatn.repository.sanpham.SoleRepository;
import com.example.demodatn.service.PeopleService;
import com.example.demodatn.service.sanpham.AdminCatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class Phase3ComboFilterIntegrationTest {

    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired AdminCatalogService catalogService;
    @Autowired PeopleService peopleService;
    @Autowired CategoryRepository categories;
    @Autowired SoleRepository soles;
    @Autowired MaterialRepository materials;
    @Autowired SizeRepository sizes;
    @Autowired EmployeeRepository employees;

    MockMvc mvc;

    @BeforeEach
    void setup() {
        String dbName = jdbc.queryForObject("SELECT DB_NAME()", String.class);
        assertTrue(Set.of("shopbangiay_kt", "shop_verify", "shopbangiay_thu").contains(dbName)
                || (dbName != null && dbName.startsWith("shopbangiay_kt_")),
                "Never run fixtures against the real DB");
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void creatableCatalogFieldsCreateAndDeduplicateWithoutAccents() {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        String catName = "Thương hiệu Phố " + suffix;
        String soleName = "Đế Cao Su Đúc " + suffix;
        String matName = "Da Lộn Mềm " + suffix;
        String sizeVal = "45" + suffix.substring(0, 2);

        AdminProductForm pForm = new AdminProductForm();
        pForm.setCode("SP-P3-" + suffix);
        pForm.setName("Giày Thể Thao " + suffix);
        pForm.setNewCategoryName("  " + catName + "  ");
        pForm.setNewSoleName("  " + soleName + "  ");
        pForm.setNewInitialMaterialName("  " + matName + "  ");
        pForm.setStatus(false);

        var product = catalogService.saveProduct(pForm);
        assertNotNull(product.getCategory());
        assertEquals(catName, product.getCategory().getName());
        assertNotNull(product.getSole());
        assertEquals(soleName, product.getSole().getName());
        assertNotNull(pForm.getInitialMaterialId());

        // Deduplicate when typing without accents / different case
        AdminProductForm pForm2 = new AdminProductForm();
        pForm2.setCode("SP-P3B-" + suffix);
        pForm2.setName("Giày Chạy Bộ " + suffix);
        pForm2.setNewCategoryName("thuong hieu pho " + suffix.toUpperCase());
        pForm2.setNewSoleName("de cao su duc " + suffix.toUpperCase());
        pForm2.setNewInitialMaterialName("da lon mem " + suffix.toUpperCase());
        pForm2.setStatus(false);

        var product2 = catalogService.saveProduct(pForm2);
        assertEquals(product.getCategory().getId(), product2.getCategory().getId(), "Should reuse existing category accent-insensitively");
        assertEquals(product.getSole().getId(), product2.getSole().getId(), "Should reuse existing sole accent-insensitively");
        assertEquals(pForm.getInitialMaterialId(), pForm2.getInitialMaterialId(), "Should reuse existing material accent-insensitively");

        // Creatable Size & Material on VariantForm
        VariantForm vForm = new VariantForm();
        vForm.setProductId(product.getId());
        vForm.setNewSizeValue(" " + sizeVal + " ");
        vForm.setNewMaterialName("da lon mem " + suffix);
        vForm.setPrice(new BigDecimal("450000"));
        vForm.setQuantity(10);
        vForm.setStatus(true);

        var variant = catalogService.saveVariant(vForm);
        assertEquals(sizeVal, variant.getSize().getSizeValue());
        assertEquals(pForm.getInitialMaterialId(), variant.getMaterial().getId());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void creatableEmployeePositionTrimsAndDeduplicates() {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        EmployeeDto dto1 = new EmployeeDto();
        dto1.setEmployeeCode("NV-P3A-" + suffix);
        dto1.setFullName("Nguyễn Văn Test " + suffix);
        dto1.setPhone("0971" + String.format("%06d", Math.abs(suffix.hashCode()) % 1000000));
        dto1.setEmail("emp1_" + suffix + "@example.test");
        dto1.setNewPosition("  Trưởng Ca Kho " + suffix + "  ");
        dto1.setUsername("emp1_" + suffix);
        dto1.setPassword("Abcd@1234");

        peopleService.saveEmployee(dto1);
        String pos1 = jdbc.queryForObject("SELECT position FROM employees WHERE employee_code=?", String.class, "NV-P3A-" + suffix);
        assertEquals("Trưởng Ca Kho " + suffix, pos1);

        EmployeeDto dto2 = new EmployeeDto();
        dto2.setEmployeeCode("NV-P3B-" + suffix);
        dto2.setFullName("Trần Thị Test " + suffix);
        dto2.setPhone("0972" + String.format("%06d", Math.abs(suffix.hashCode()) % 1000000));
        dto2.setEmail("emp2_" + suffix + "@example.test");
        dto2.setNewPosition("truong ca kho " + suffix.toUpperCase());
        dto2.setUsername("emp2_" + suffix);
        dto2.setPassword("Abcd@1234");

        peopleService.saveEmployee(dto2);
        String pos2 = jdbc.queryForObject("SELECT position FROM employees WHERE employee_code=?", String.class, "NV-P3B-" + suffix);
        assertEquals("Trưởng Ca Kho " + suffix, pos2, "Should reuse existing position case/accent-insensitively");
    }

    @Test
    @WithMockUser(username = "staffuser", roles = "STAFF")
    void nonAdminCannotCreateNewLookupRecords() {
        AdminProductForm pForm = new AdminProductForm();
        pForm.setCode("SP-STAFF-1");
        pForm.setName("Giày Staff");
        pForm.setNewCategoryName("Thương hiệu Trái Phép");
        pForm.setInitialMaterialId(1);
        pForm.setStatus(true);
        assertThrows(AccessDeniedException.class, () -> catalogService.saveProduct(pForm));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void forgedForeignIdsAreRejected() throws Exception {
        // 1. Product form with forged categoryId
        AdminProductForm pForm = new AdminProductForm();
        pForm.setCode("SP-FORGE-1");
        pForm.setName("Giày Giả Mạo");
        pForm.setCategoryId(999999);
        pForm.setInitialMaterialId(1);
        pForm.setStatus(true);
        CatalogValidationException ex1 = assertThrows(CatalogValidationException.class,
                () -> catalogService.saveProduct(pForm));
        assertEquals("categoryId", ex1.getField());

        // 2. Variant form with forged sizeId and materialId
        VariantForm vForm = new VariantForm();
        vForm.setProductId(999999);
        vForm.setSizeId(999999);
        vForm.setMaterialId(999999);
        vForm.setPrice(new BigDecimal("200000"));
        vForm.setQuantity(5);
        vForm.setStatus(true);
        CatalogValidationException ex2 = assertThrows(CatalogValidationException.class,
                () -> catalogService.saveVariant(vForm));
        assertTrue(Set.of("productId", "sizeId", "materialId").contains(ex2.getField()));

        // 3. Voucher form with forged customerId via POST
        mvc.perform(post("/admin/vouchers/save")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN"))
                        .param("code", "FORGEVOUCHER")
                        .param("name", "Voucher Giả")
                        .param("discountType", "FIXED")
                        .param("discountValue", "10000")
                        .param("minOrderValue", "50000")
                        .param("startDate", "2026-01-01T00:00")
                        .param("endDate", "2026-12-31T23:59")
                        .param("usageLimit", "10")
                        .param("isPublic", "false")
                        .param("customerId", "999999")
                        .param("status", "true"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("voucher", "customerId"));

        // 4. POS add with forged variantId
        mvc.perform(post("/admin/pos/add")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN"))
                        .param("variantId", "999999")
                        .param("quantity", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void allFilterScreensRenderSuccessfullyWithFilters() throws Exception {
        String[] adminUrls = {
                "/admin/products-crud?q=Giày&status=true&minPrice=100000&maxPrice=2000000",
                "/admin/variants?q=SP&status=true",
                "/admin/categories?q=Nike&status=true&inUse=true",
                "/admin/sizes?q=40&inUse=true",
                "/admin/materials?q=Da&status=true&inUse=true",
                "/admin/soles?q=Cao&status=true&inUse=true",
                "/admin/inventory?q=Giày&stock=in&changeType=INCREASE",
                "/admin/customers?q=Nguyễn&status=true&from=2020-01-01&to=2030-12-31",
                "/admin/employees?q=admin&status=true&from=2020-01-01&to=2030-12-31",
                "/admin/vouchers?q=GIAM&discountType=PERCENT&state=RUNNING&from=2020-01-01&to=2030-12-31",
                "/admin/promotions?q=Sale&state=RUNNING&from=2020-01-01&to=2030-12-31",
                "/admin/orders?q=ORD&type=ONLINE&status=PENDING&paymentStatus=UNPAID&paymentMethod=COD&from=2020-01-01&to=2030-12-31",
                "/admin/invoices?q=INV&channel=POS&from=2020-01-01&to=2030-12-31",
                "/admin/pos?q=Giày",
                "/admin/statistics/revenue?from=2025-01-01&to=2026-12-31&channel=ONLINE",
                "/admin/statistics/best-sellers?from=2025-01-01&to=2026-12-31&channel=POS",
                "/admin/dashboard?from=2025-01-01&to=2026-12-31&days=30"
        };
        for (String url : adminUrls) {
            mvc.perform(get(url).with(user("admin").roles("ADMIN")))
                    .andExpect(status().isOk());
        }

        mvc.perform(get("/products?keyword=Giày&minPrice=100000&maxPrice=2000000&sort=price_asc"))
                .andExpect(status().isOk());
    }
}
