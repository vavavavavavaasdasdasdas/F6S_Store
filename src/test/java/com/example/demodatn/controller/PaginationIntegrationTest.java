package com.example.demodatn.controller;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.data.domain.Page;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest @Transactional
class PaginationIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;
    MockMvc mvc;
    List<Integer> categories=new ArrayList<>(),sizes=new ArrayList<>(),products=new ArrayList<>(),variants=new ArrayList<>();
    int customer,promotion;
    int insert(String sql,Object...args){return jdbc.queryForObject(sql,Integer.class,args);}
    @BeforeEach void thirtyRowsPerType() {
        assertTrue((Set.of("shopbangiay_kt","shop_verify","shopbangiay_thu").contains(jdbc.queryForObject("SELECT DB_NAME()",String.class)) || jdbc.queryForObject("SELECT DB_NAME()",String.class).startsWith("shopbangiay_kt_")),"Never run fixtures against the real DB");
        mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        for(String table:List.of("pos_held_items","pos_held_orders","invoices","order_details","order_status_history","orders","inventory_logs","promotion_details","product_images","product_variants","products"))jdbc.update("DELETE FROM "+table);
        jdbc.update("UPDATE vouchers SET customer_id=NULL");
        for(String table:List.of("customers","employees","vouchers","promotions","categories","sizes"))jdbc.update("DELETE FROM "+table);
        int role=jdbc.queryForObject("SELECT id FROM roles WHERE name='ROLE_USER'",Integer.class);
        int adminRole=jdbc.queryForObject("SELECT id FROM roles WHERE name='ROLE_ADMIN'",Integer.class);
        int admin=jdbc.queryForObject("SELECT id FROM users WHERE username='admin'",Integer.class);
        String run=UUID.randomUUID().toString().substring(0,8);
        for(int n=0;n<30;n++) {
            String name=String.format("PG+ Item %02d",n);
            categories.add(insert("INSERT categories(name,status) OUTPUT INSERTED.id VALUES(?,1)",name));
            sizes.add(insert("INSERT sizes(size_value) OUTPUT INSERTED.id VALUES(?)","PG"+n));
            int user=insert("INSERT users(username,password,role_id,status) OUTPUT INSERTED.id VALUES(?,'not-used-by-mock-login',?,1)","pg-"+run+"-"+n,role);
            int c=insert("INSERT customers(customer_code,full_name,phone,email,user_id,status) OUTPUT INSERTED.id VALUES(?,?,?,?,?,1)","PG-"+run+"-"+n,name,String.format("0911%06d",n),run+n+"@example.test",user);
            if(n==0)customer=c;
            int staff=n==0?admin:insert("INSERT users(username,password,role_id,status) OUTPUT INSERTED.id VALUES(?,'not-used-by-mock-login',?,1)","pg-staff-"+run+"-"+n,adminRole);
            insert("INSERT employees(employee_code,full_name,phone,email,user_id,status) OUTPUT INSERTED.id VALUES(?,?,?,?,?,1)","PG-"+run+"-"+n,name,String.format("0922%06d",n),"staff"+run+n+"@example.test",staff);
            products.add(insert("INSERT products(code,name,category_id,status) OUTPUT INSERTED.id VALUES(?,?,?,1)","PG-"+run+"-"+n,name,categories.get(0)));
            variants.add(insert("INSERT product_variants(product_id,size_id,chat_lieu_id,mau_sac_id,de_giay_id,price,quantity,status) OUTPUT INSERTED.id VALUES(?,?,(SELECT TOP(1) id FROM chat_lieu ORDER BY id),(SELECT TOP(1) id FROM mau_sac ORDER BY id),(SELECT TOP(1) id FROM de_giay ORDER BY id),100000,5,1)",products.get(n),sizes.get(0)));
            jdbc.update("INSERT inventory_logs(variant_id,quantity_change,quantity_before,quantity_after,reason,actor,created_at) VALUES(?,5,0,5,'Pagination fixture','test',GETDATE())",variants.get(n));
            int p=insert("INSERT promotions(name,discount_type,discount_value,start_date,end_date,status) OUTPUT INSERTED.id VALUES(?,'PERCENT',10,'2000-01-01','2050-01-01',1)",name);if(n==0)promotion=p;
            jdbc.update("INSERT vouchers(code,name,discount_type,discount_value,min_order_value,start_date,end_date,is_public,used_count,status) VALUES(?,?,'PERCENT',10,0,'2000-01-01','2050-01-01',1,0,1)","PG-"+run+"-"+n,name);
            int order=insert("INSERT orders(order_code,customer_id,order_type,status,payment_status,total_amount,final_amount,created_at) OUTPUT INSERTED.id VALUES(?,?,'ONLINE','DELIVERED','PAID',100000,100000,DATEADD(day,?,CAST('2048-01-01' AS datetime)))","PG-"+run+"-"+n,customer,n);
            jdbc.update("INSERT order_details(order_id,product_variant_id,price,quantity,total_price,product_name,size_name) VALUES(?,?,100000,1,100000,?,'PG0')",order,variants.get(n),name);
            jdbc.update("INSERT invoices(order_id,invoice_code,issued_at,customer_name) VALUES(?,?,GETDATE(),?)",order,"PG-"+run+"-"+n,name);
        }
        jdbc.update("INSERT promotion_details(promotion_id,product_id) SELECT ?,id FROM products",promotion);
    }
    String username(){return jdbc.queryForObject("SELECT u.username FROM users u JOIN customers c ON c.user_id=u.id WHERE c.id=?",String.class,customer);}
    MvcResult getPage(String url,String parameter,String value)throws Exception {
        return mvc.perform(get(url).param(parameter,value).with(user(url.startsWith("/admin")?"admin":username()).roles(url.startsWith("/admin")?"ADMIN":"USER")))
                .andExpect(status().isOk()).andReturn();
    }
    Page<?> page(MvcResult result,String path){Object value=result.getModelAndView().getModel();for(String part:path.split("\\."))value=((Map<?,?>)value).get(part);return (Page<?>)value;}
    void verify(String url,String model,String parameter)throws Exception {
        Set<String> ids=new HashSet<>();
        for(int n=0;n<3;n++) {
            var result=getPage(url,parameter,String.valueOf(n));Page<?> data=page(result,model);
            assertEquals(n==2?6:12,data.getNumberOfElements(),url);assertEquals(30,data.getTotalElements(),url);
            for(Object row:data.getContent()) {
                Object key=row instanceof Map<?,?> map?(map.containsKey("id")?map.get("id"):map.get("day")):new org.springframework.beans.BeanWrapperImpl(row).getPropertyValue("id");
                assertTrue(ids.add(String.valueOf(key)),"Duplicate "+url+" "+key);
            }
        }
        for(String invalid:List.of("-1","abc","99999")) {var data=page(getPage(url,parameter,invalid),model);assertEquals(invalid.equals("99999")?2:0,data.getNumber(),url);}
        
    }
    @Test void allListsReturnTwelveTwelveSixWithStableIds()throws Exception {
        for(var entry:Map.ofEntries(Map.entry("/admin/products-crud","productPage"),Map.entry("/admin/variants","dataPage"),Map.entry("/admin/categories","items"),Map.entry("/admin/sizes","items"),Map.entry("/admin/inventory","dataPage"),Map.entry("/admin/customers","peoplePage"),Map.entry("/admin/employees","peoplePage"),Map.entry("/admin/vouchers","dataPage"),Map.entry("/admin/promotions","dataPage"),Map.entry("/admin/orders","orders"),Map.entry("/admin/invoices","invoices"),Map.entry("/products","productPage"),Map.entry("/search?promotion=true","productPage"),Map.entry("/category/"+categories.get(0),"productPage"),Map.entry("/orders","orderPage"),Map.entry("/admin/customers/detail/"+customer,"orderPage"),Map.entry("/admin/promotions/"+promotion+"/products","productPage"),Map.entry("/admin/pos","productPage")).entrySet())verify(entry.getKey(),entry.getValue(),"page");
        verify("/admin/inventory","historyPage","historyPage");
        verify("/admin/pos/customers?customerQuery=PG","customerPage","customerPage");
        String stats="/admin/statistics?from=2048-01-01&to=2048-01-30&channel=ONLINE";
        verify(stats,"stats.dailyPage","page");String top=stats.replace("/admin/statistics?","/admin/statistics/best-sellers?");verify(top,"stats.topQuantityPage","quantityPage");verify(top,"stats.topRevenuePage","revenuePage");
    }
    @Test void productVariantSublistIsPagedAndLinksKeepFilters()throws Exception {
        jdbc.update("DELETE order_details");jdbc.update("DELETE inventory_logs");jdbc.update("DELETE product_variants");
        for(int size:sizes)jdbc.update("INSERT product_variants(product_id,size_id,chat_lieu_id,mau_sac_id,de_giay_id,price,quantity,status) VALUES(?,?,(SELECT TOP(1) id FROM chat_lieu ORDER BY id),(SELECT TOP(1) id FROM mau_sac ORDER BY id),(SELECT TOP(1) id FROM de_giay ORDER BY id),100000,5,1)",products.get(0),size);
        verify("/admin/products-crud/"+products.get(0)+"/variants","dataPage","page");
        verify("/admin/products-crud/view/"+products.get(0),"variantPage","page");
        String html=mvc.perform(get("/admin/products-crud").param("keyword","PG+").param("sort","name").param("status","true").with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("keyword=PG%2B"));assertTrue(html.contains("sort=name"));assertTrue(html.contains("status=true"));
    }
    @Test void exactTwelveHidesPagerAndDeletingLastRowClampsToPreviousPage()throws Exception {
        for(int n=25;n<30;n++)jdbc.update("DELETE categories WHERE id=?",categories.get(n));
        assertEquals(1,page(getPage("/admin/categories","page","2"),"items").getNumberOfElements());
        mvc.perform(post("/admin/categories/delete/"+categories.get(24)).header("Referer","http://localhost/admin/categories?page=2").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/categories?page=2"));
        assertEquals(1,page(getPage("/admin/categories","page","2"),"items").getNumber());
        for(int n=12;n<24;n++)jdbc.update("DELETE categories WHERE id=?",categories.get(n));
        String html=getPage("/admin/categories","page","0").getResponse().getContentAsString();
        assertFalse(html.contains("class=\"shared-pagination\""));
    }
    @Test void promotionSelectionPreservesOtherPages()throws Exception {
        jdbc.update("DELETE promotion_details WHERE promotion_id=?",promotion);
        jdbc.update("INSERT promotion_details(promotion_id,product_id) VALUES(?,?),(?,?)",promotion,products.get(0),promotion,products.get(24));
        var post=post("/admin/promotions/"+promotion+"/products").with(user("admin").roles("ADMIN")).with(csrf()).param("productIds",products.get(1).toString()).param("q","PG+");
        for(int id:products.subList(0,12))post.param("visibleIds",String.valueOf(id));
        mvc.perform(post).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/promotions/"+promotion+"/products?page=0&q=PG%2B"));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM promotion_details WHERE promotion_id=?",Integer.class,promotion));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM promotion_details WHERE promotion_id=? AND product_id=?",Integer.class,promotion,products.get(24)));
    }
}
