package com.example.demodatn.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class RouteErrorIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;

    @Test void invalidPathIdentifiersRender404WhileInvalidQueryRemains400() throws Exception {
        com.example.demodatn.TestDatabaseGuard.check(jdbc);MockMvc mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        for(String prefix:List.of("/products/","/category/","/orders/","/orders/success/",
                "/admin/products-crud/view/","/admin/products-crud/edit/","/admin/variants/edit/",
                "/admin/variants/adjust/","/admin/orders/","/admin/invoices/",
                "/admin/customers/detail/","/admin/customers/edit/","/admin/employees/detail/",
                "/admin/employees/edit/","/admin/vouchers/edit/","/admin/promotions/edit/",
                "/admin/categories/edit/","/admin/sizes/edit/","/admin/materials/edit/")) {
            for(String id:List.of("abc","999999999999999999999"))
                mvc.perform(get(prefix+id).with(user("admin").roles("ADMIN")))
                        .andExpect(status().isNotFound()).andExpect(view().name("loi/404"));
        }
        mvc.perform(get("/products?categoryId=abc")).andExpect(status().isBadRequest());
    }

    @Test void productImagesRenderThroughThymeleafRestrictedUrlAttributes() throws Exception {
        MockMvc mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        for(String path:List.of("/","/products","/admin/products-crud","/admin/dashboard?from=2026-01-01&to=2026-12-31"))
            mvc.perform(get(path).with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
    }

    @Test void exportMappingsFromCompleteSpringContext() throws Exception {
        var rows=new ArrayList<Map<String,Object>>();
        context.getBean("requestMappingHandlerMapping",RequestMappingHandlerMapping.class).getHandlerMethods().forEach((info,handler)->{
            for(String path:info.getPatternValues()) {
                var row=new LinkedHashMap<String,Object>();
                row.put("methods",info.getMethodsCondition().getMethods().stream().map(Enum::name).sorted().toList());
                row.put("path",path);row.put("handler",handler.getBeanType().getName()+"#"+handler.getMethod().getName());rows.add(row);
            }
        });
        rows.sort(Comparator.comparing(r->r.get("path").toString()));
        assertTrue(rows.stream().anyMatch(r->r.get("path").equals("/error")));
        Files.createDirectories(Path.of("target"));
        new tools.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(Path.of("target/route-mappings.json").toFile(),rows);
        System.out.println("ROUTE_AUDIT full Spring context mappings="+rows.size());
    }
}
