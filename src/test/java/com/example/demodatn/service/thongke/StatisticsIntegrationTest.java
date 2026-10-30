package com.example.demodatn.service.thongke;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class StatisticsIntegrationTest {
    @Autowired StatisticsService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;
    private int order(String channel,String status,String payment,String date,String amount) {
        return jdbc.queryForObject("INSERT INTO orders(order_code,order_type,status,payment_status,total_amount,final_amount,created_at) OUTPUT INSERTED.id VALUES(?,?,?,?,?,?,CAST(? AS datetime))",Integer.class,"P10-"+UUID.randomUUID(),channel,status,payment,new BigDecimal(amount),new BigDecimal(amount),date);
    }
    private void line(int order,int variant,int quantity,String amount) {
        jdbc.update("INSERT INTO order_details(order_id,product_variant_id,price,quantity,total_price) VALUES(?,?,?,?,?)",order,variant,new BigDecimal(amount).divide(BigDecimal.valueOf(quantity)),quantity,new BigDecimal(amount));
    }
    private int variant() {
        int product=jdbc.queryForObject("INSERT INTO products(code,name,status) OUTPUT INSERTED.id VALUES(?,N'Sản phẩm kiểm thử thống kê',1)",Integer.class,"STAT-"+UUID.randomUUID());
        return jdbc.queryForObject("INSERT INTO product_variants(product_id,size_id,chat_lieu_id,mau_sac_id,de_giay_id,price,quantity,status) OUTPUT INSERTED.id SELECT ?,MIN(s.id),MIN(m.id),(SELECT MIN(id) FROM mau_sac),(SELECT MIN(id) FROM de_giay),100000,5,1 FROM sizes s CROSS JOIN chat_lieu m",Integer.class,product);
    }
    @Test
    void metricsRankingsBoundariesChannelsAndStatusesUseSnapshotMoney() {
        var day=LocalDate.of(2042,1,1);
        int online=order("ONLINE","DELIVERED","PAID","2042-01-01T00:00:00","90.50");
        int pos=order("POS","CONFIRMED","PAID","2042-01-01T23:59:59","210.25");
        order("POS","DELIVERED","UNPAID","2042-01-01T12:00:00","999");
        order("POS","CANCELLED","PAID","2042-01-01T12:00:00","999");
        int failed=order("ONLINE","DELIVERY_FAILED","PAID","2042-01-01T12:00:00","999");
        order("POS","DELIVERED","PAID","2042-01-02T00:00:00","999");
        var ids=List.of(variant(),variant());
        line(online,ids.get(0),2,"100.50");line(pos,ids.get(1),1,"220.25");
        line(failed,ids.get(0),10,"999.00");
        var all=service.load(day,day,"ALL");assertEquals(2L,((Number)all.get("orders")).longValue());
        assertEquals("2042-01-01",((List<Map<String,Object>>)all.get("daily")).get(0).get("day"));
        assertEquals(new BigDecimal("300.75"),all.get("revenue"));assertEquals(new BigDecimal("150.38"),all.get("average"));
        assertEquals(new BigDecimal("90.50"),service.load(day,day,"ONLINE").get("revenue"));
        assertEquals(new BigDecimal("210.25"),service.load(day,day,"POS").get("revenue"));
        var quantities=(List<Map<String,Object>>)all.get("topQuantity");var revenues=(List<Map<String,Object>>)all.get("topRevenue");
        assertEquals(2L,((Number)quantities.get(0).get("quantity")).longValue());assertEquals(new BigDecimal("220.25"),revenues.get(0).get("amount"));
        var statuses=(List<Map<String,Object>>)all.get("statuses");assertEquals(5L,statuses.stream().mapToLong(s->((Number)s.get("total")).longValue()).sum());
        var empty=service.load(day.minusDays(2),day.minusDays(1),"ALL");assertEquals(0L,((Number)empty.get("orders")).longValue());assertEquals(BigDecimal.ZERO,empty.get("average"));assertEquals(2,((List<?>)empty.get("daily")).size());
    }
    @Test @WithMockUser(roles="ADMIN")
    void routesTabsEmptyAndInvalidFiltersRenderWithoutBrokenCharts() throws Exception {
        var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        for(String url:List.of("/admin/statistics","/admin/statistics/revenue","/admin/statistics/best-sellers"))mvc.perform(get(url)).andExpect(status().isOk());
        mvc.perform(get("/admin/statistics").param("from","2041-01-01").param("to","2041-01-02"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa có doanh thu")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("id=\"statistics-revenue\""))));
        mvc.perform(get("/admin/statistics").param("from","2041-02-02").param("to","2041-02-01")).andExpect(status().isOk()).andExpect(model().attribute("filterError","Ngày kết thúc phải từ ngày bắt đầu."));
        mvc.perform(get("/admin/statistics").param("channel","INVALID")).andExpect(status().isOk()).andExpect(model().attribute("filterError","Kênh bán không hợp lệ."));
    }
    @Test @WithMockUser(roles="USER")
    void userIsForbidden() throws Exception {
        MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build().perform(get("/admin/statistics")).andExpect(status().isForbidden());
    }
}
