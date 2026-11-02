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
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class StatisticsIntegrationTest {
    @Autowired StatisticsService service;
    @Autowired DashboardService dashboardService;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;

    private int order(String channel, String status, String payment, String date, String amount, String method) {
        return jdbc.queryForObject("INSERT INTO orders(order_code,order_type,status,payment_status,payment_method,total_amount,final_amount,created_at) OUTPUT INSERTED.id VALUES(?,?,?,?,?,?,?,CAST(? AS datetime))", Integer.class, "P10-" + UUID.randomUUID(), channel, status, payment, method, new BigDecimal(amount), new BigDecimal(amount), date);
    }
    private int orderWithCustomer(int customerId, String channel, String status, String payment, String date, String amount, String method) {
        return jdbc.queryForObject("INSERT INTO orders(order_code,customer_id,order_type,status,payment_status,payment_method,total_amount,final_amount,created_at) OUTPUT INSERTED.id VALUES(?,?,?,?,?,?,?,?,CAST(? AS datetime))", Integer.class, "P16-" + UUID.randomUUID(), customerId, channel, status, payment, method, new BigDecimal(amount), new BigDecimal(amount), date);
    }
    private void line(int order, int variant, int quantity, String amount) {
        jdbc.update("INSERT INTO order_details(order_id,product_variant_id,price,quantity,total_price) VALUES(?,?,?,?,?)", order, variant, new BigDecimal(amount).divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP), quantity, new BigDecimal(amount));
    }
    private int variant() {
        int product = jdbc.queryForObject("INSERT INTO products(code,name,status) OUTPUT INSERTED.id VALUES(?,N'Sản phẩm kiểm thử thống kê',1)", Integer.class, "STAT-" + UUID.randomUUID());
        return jdbc.queryForObject("INSERT INTO product_variants(product_id,size_id,chat_lieu_id,mau_sac_id,de_giay_id,price,quantity,status) OUTPUT INSERTED.id SELECT ?,MIN(s.id),MIN(m.id),(SELECT MIN(id) FROM mau_sac),(SELECT MIN(id) FROM de_giay),100000,5,1 FROM sizes s CROSS JOIN chat_lieu m", Integer.class, product);
    }
    private int variantWithCategory(int categoryId, String name) {
        int product = jdbc.queryForObject("INSERT INTO products(code,name,category_id,status) OUTPUT INSERTED.id VALUES(?,?,?,1)", Integer.class, "STAT-" + UUID.randomUUID().toString().substring(0, 8), name, categoryId);
        return jdbc.queryForObject("INSERT INTO product_variants(product_id,size_id,chat_lieu_id,mau_sac_id,de_giay_id,price,quantity,status) OUTPUT INSERTED.id SELECT ?,MIN(s.id),MIN(m.id),(SELECT MIN(id) FROM mau_sac),(SELECT MIN(id) FROM de_giay),100000,4,1 FROM sizes s CROSS JOIN chat_lieu m", Integer.class, product);
    }

    @Test
    void metricsRankingsBoundariesChannelsAndStatusesUseSnapshotMoney() {
        var day = LocalDate.of(2042, 1, 1);
        int online = order("ONLINE", "DELIVERED", "PAID", "2042-01-01T00:00:00", "90.50", "COD");
        int pos = order("POS", "CONFIRMED", "PAID", "2042-01-01T23:59:59", "210.25", "BANK_TRANSFER");
        order("POS", "DELIVERED", "UNPAID", "2042-01-01T12:00:00", "999", "CASH");
        order("POS", "CANCELLED", "PAID", "2042-01-01T12:00:00", "999", "TRANSFER");
        int failed = order("ONLINE", "DELIVERY_FAILED", "PAID", "2042-01-01T12:00:00", "999", "COD");
        order("POS", "DELIVERED", "PAID", "2042-01-02T00:00:00", "999", "CASH");
        var ids = List.of(variant(), variant());
        line(online, ids.get(0), 2, "100.50");
        line(pos, ids.get(1), 1, "220.25");
        line(failed, ids.get(0), 10, "999.00");
        var all = service.load(day, day, "ALL");
        assertEquals(2L, ((Number) all.get("orders")).longValue());
        assertEquals("2042-01-01", ((List<Map<String, Object>>) all.get("daily")).get(0).get("day"));
        assertEquals(new BigDecimal("300.75"), all.get("revenue"));
        assertEquals(new BigDecimal("150.38"), all.get("average"));
        assertEquals(new BigDecimal("90.50"), all.get("cashRevenue"));
        assertEquals(new BigDecimal("210.25"), all.get("transferRevenue"));
        assertEquals(0, ((BigDecimal) all.get("revenue")).compareTo(((BigDecimal) all.get("cashRevenue")).add((BigDecimal) all.get("transferRevenue"))));
        assertEquals(new BigDecimal("90.50"), service.load(day, day, "ONLINE").get("revenue"));
        assertEquals(new BigDecimal("210.25"), service.load(day, day, "POS").get("revenue"));
        var quantities = (List<Map<String, Object>>) all.get("topQuantity");
        var revenues = (List<Map<String, Object>>) all.get("topRevenue");
        assertEquals(2L, ((Number) quantities.get(0).get("quantity")).longValue());
        assertEquals(new BigDecimal("220.25"), revenues.get(0).get("amount"));
        var statuses = (List<Map<String, Object>>) all.get("statuses");
        assertEquals(5L, statuses.stream().mapToLong(s -> ((Number) s.get("total")).longValue()).sum());
        var empty = service.load(day.minusDays(2), day.minusDays(1), "ALL");
        assertEquals(0L, ((Number) empty.get("orders")).longValue());
        assertEquals(BigDecimal.ZERO, empty.get("average"));
        assertEquals(2, ((List<?>) empty.get("daily")).size());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void routesRedirectOldUrlsToDashboardAndRenderUnifiedDashboardPageAndStrangePagesNever500() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        mvc.perform(get("/admin/dashboard")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Doanh thu theo kênh")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Top sản phẩm theo số lượng")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Top sản phẩm theo doanh thu")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Top khách hàng theo chi tiêu")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Đánh giá sản phẩm")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sản phẩm yêu thích")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sản phẩm sắp hết hàng")));

        for (String url : List.of("/admin/statistics", "/admin/statistics/revenue", "/admin/statistics/best-sellers", "/admin/reports", "/admin/reports/revenue", "/admin/reports/best-sellers")) {
            mvc.perform(get(url)).andExpect(status().isFound()).andExpect(redirectedUrl("/admin/dashboard"));
            mvc.perform(get(url + "?from=2026-01-01&channel=ONLINE")).andExpect(status().isFound()).andExpect(redirectedUrl("/admin/dashboard?from=2026-01-01&channel=ONLINE"));
        }

        mvc.perform(get("/admin/dashboard").param("from", "2041-01-01").param("to", "2041-01-02"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa có doanh thu")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Doanh thu theo ngày"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Đơn hàng gần đây"))));

        mvc.perform(get("/admin/dashboard").param("from", "2041-02-02").param("to", "2041-02-01")).andExpect(status().isOk()).andExpect(model().attribute("filterError", "Ngày kết thúc phải từ ngày bắt đầu."));
        mvc.perform(get("/admin/dashboard").param("channel", "INVALID")).andExpect(status().isOk()).andExpect(model().attribute("filterError", "Kênh bán không hợp lệ."));
        for (String p : List.of("-1", "abc", "999999")) {
            mvc.perform(get("/admin/dashboard").param("quantityPage", p).param("revenuePage", p).param("customerPage", p).param("reviewPage", p).param("favoritePage", p).param("lowStockPage", p).param("statusPage", p))
                    .andExpect(status().isOk());
        }
    }

    @Test @WithMockUser(roles = "ADMIN")
    void independentSqlMatchesEveryUnifiedDashboardNumberAcrossAllFilterModes() throws Exception {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        int cat1 = jdbc.queryForObject("INSERT INTO categories(name,status) OUTPUT INSERTED.id VALUES(N'Thương hiệu GD16 A',1)", Integer.class);
        int cat2 = jdbc.queryForObject("INSERT INTO categories(name,status) OUTPUT INSERTED.id VALUES(N'Thương hiệu GD16 B',1)", Integer.class);
        int v1 = variantWithCategory(cat1, "Giày GD16 Alpha");
        int v2 = variantWithCategory(cat2, "Giày GD16 Beta");
        int p1 = jdbc.queryForObject("SELECT product_id FROM product_variants WHERE id=?", Integer.class, v1);

        int cust1 = jdbc.queryForObject("INSERT INTO customers(customer_code,full_name,phone,status,created_at) OUTPUT INSERTED.id VALUES(?,N'Khách GD16 Một','0909111222',1,CAST(? AS datetime))", Integer.class, "KH16A-" + UUID.randomUUID().toString().substring(0, 5), today.minusDays(2) + "T10:00:00");
        int cust2 = jdbc.queryForObject("INSERT INTO customers(customer_code,full_name,phone,status,created_at) OUTPUT INSERTED.id VALUES(?,N'Khách GD16 Hai','0909333444',1,CAST(? AS datetime))", Integer.class, "KH16B-" + UUID.randomUUID().toString().substring(0, 5), today.minusDays(15) + "T10:00:00");
        int cust3 = jdbc.queryForObject("INSERT INTO customers(customer_code,full_name,phone,status,created_at) OUTPUT INSERTED.id VALUES(?,N'Khách GD16 Ba','0909555666',1,CAST(? AS datetime))", Integer.class, "KH16C-" + UUID.randomUUID().toString().substring(0, 5), today.minusDays(3) + "T10:00:00");

        int o1 = orderWithCustomer(cust1, "ONLINE", "DELIVERED", "PAID", today.minusDays(2) + "T12:00:00", "500000", "COD");
        line(o1, v1, 5, "500000");
        int o2 = orderWithCustomer(cust3, "POS", "DELIVERED", "PAID", today.minusDays(4) + "T14:00:00", "700000", "BANK_TRANSFER");
        line(o2, v2, 2, "700000");
        int o3 = orderWithCustomer(cust2, "ONLINE", "DELIVERED", "PAID", today.minusDays(18) + "T15:00:00", "900000", "TRANSFER");
        line(o3, v2, 9, "900000");

        jdbc.update("INSERT INTO danh_gia(product_id,customer_id,order_id,so_sao,noi_dung,hien_thi,created_at,updated_at) VALUES(?,?,?,5,N'Rất tốt 1',1,CAST(? AS datetime),CAST(? AS datetime))", p1, cust1, o1, today.minusDays(1) + "T09:00:00", today.minusDays(1) + "T09:00:00");
        jdbc.update("INSERT INTO danh_gia(product_id,customer_id,order_id,so_sao,noi_dung,hien_thi,created_at,updated_at) VALUES(?,?,?,4,N'Rất tốt 2',1,CAST(? AS datetime),CAST(? AS datetime))", p1, cust2, o3, today.minusDays(1) + "T10:00:00", today.minusDays(1) + "T10:00:00");
        jdbc.update("INSERT INTO danh_gia(product_id,customer_id,order_id,so_sao,noi_dung,hien_thi,created_at,updated_at) VALUES(?,?,?,5,N'Rất tốt 3',1,CAST(? AS datetime),CAST(? AS datetime))", p1, cust3, o2, today.minusDays(1) + "T11:00:00", today.minusDays(1) + "T11:00:00");

        jdbc.update("INSERT INTO yeu_thich(customer_id,product_id,created_at) VALUES(?,?,CAST(? AS datetime))", cust1, p1, today.minusDays(1) + "T08:00:00");
        jdbc.update("INSERT INTO yeu_thich(customer_id,product_id,created_at) VALUES(?,?,CAST(? AS datetime))", cust2, p1, today.minusDays(2) + "T08:00:00");

        jdbc.update("INSERT INTO inventory_logs(variant_id,quantity_change,quantity_before,quantity_after,reason,actor,created_at) VALUES(?,-1,5,4,N'Xuất bán GD16','admin',CAST(? AS datetime))", v1, today.minusDays(2) + "T12:05:00");

        StringBuilder json = new StringBuilder("{\n");
        String revExpr = com.example.demodatn.repository.donhang.RevenueCondition.AMOUNT;
        String revCond = com.example.demodatn.repository.donhang.RevenueCondition.SQL;
        String catExists = " AND (? IS NULL OR EXISTS (SELECT 1 FROM order_details d_c JOIN product_variants v_c ON v_c.id=d_c.product_variant_id JOIN products p_c ON p_c.id=v_c.product_id WHERE d_c.order_id=o.id AND p_c.category_id=?))";

        record Scenario(String name, Integer days, String period, LocalDate from, LocalDate to, String ch, Integer cat) {}
        List<Scenario> scenarios = List.of(
                new Scenario("default_7d", null, "", null, null, "ALL", null),
                new Scenario("quick_7d", 7, "", null, null, "ALL", null),
                new Scenario("quick_30d", 30, "", null, null, "ALL", null),
                new Scenario("empty_range", null, "", LocalDate.of(2041, 1, 1), LocalDate.of(2041, 1, 2), "ALL", null),
                new Scenario("channel_ONLINE", 30, "", null, null, "ONLINE", null),
                new Scenario("channel_POS", 30, "", null, null, "POS", null),
                new Scenario("brand_A", 30, "", null, null, "ALL", cat1),
                new Scenario("brand_B", 30, "", null, null, "ALL", cat2)
        );

        for (int i = 0; i < scenarios.size(); i++) {
            Scenario s = scenarios.get(i);
            var dash = dashboardService.load(s.days(), s.period(), s.from(), s.to(), s.ch(), s.cat(), "0", "0", "0", "0", "0", "0", "0", "0");
            LocalDate from = (LocalDate) dash.get("from");
            LocalDate until = ((LocalDate) dash.get("to")).plusDays(1);
            Object[] args = {from, until, s.ch(), s.ch(), s.cat(), s.cat()};

            BigDecimal sqlRev = jdbc.queryForObject("SELECT COALESCE(SUM(" + revExpr + "),0) FROM orders o WHERE " + revCond + " AND o.created_at>=? AND o.created_at<? AND (?='ALL' OR o.order_type=?)" + catExists, BigDecimal.class, args);
            BigDecimal sqlCash = jdbc.queryForObject("SELECT COALESCE(SUM(" + revExpr + "),0) FROM orders o WHERE " + revCond + " AND o.payment_method IN ('CASH','COD') AND o.created_at>=? AND o.created_at<? AND (?='ALL' OR o.order_type=?)" + catExists, BigDecimal.class, args);
            BigDecimal sqlTrans = jdbc.queryForObject("SELECT COALESCE(SUM(" + revExpr + "),0) FROM orders o WHERE " + revCond + " AND o.payment_method IN ('BANK_TRANSFER','TRANSFER') AND o.created_at>=? AND o.created_at<? AND (?='ALL' OR o.order_type=?)" + catExists, BigDecimal.class, args);
            Long sqlEligibleOrders = jdbc.queryForObject("SELECT COUNT_BIG(*) FROM orders o WHERE " + revCond + " AND o.created_at>=? AND o.created_at<? AND (?='ALL' OR o.order_type=?)" + catExists, Long.class, args);
            Long sqlTotalOrders = jdbc.queryForObject("SELECT COUNT_BIG(*) FROM orders o WHERE o.created_at>=? AND o.created_at<? AND (?='ALL' OR o.order_type=?)" + catExists, Long.class, args);
            Long sqlFavs = jdbc.queryForObject("SELECT COUNT_BIG(*) FROM yeu_thich y JOIN products p ON p.id=y.product_id WHERE y.created_at>=? AND y.created_at<? AND (? IS NULL OR p.category_id=?)", Long.class, from, until, s.cat(), s.cat());

            assertEquals(0, sqlRev.compareTo((BigDecimal) dash.get("periodRevenue")));
            assertEquals(0, sqlCash.compareTo((BigDecimal) dash.get("cashRevenue")));
            assertEquals(0, sqlTrans.compareTo((BigDecimal) dash.get("transferRevenue")));
            assertEquals(0, sqlRev.compareTo(sqlCash.add(sqlTrans)));
            assertEquals(sqlEligibleOrders, ((Number) dash.get("eligibleOrders")).longValue());
            assertEquals(sqlTotalOrders, ((Number) dash.get("orders")).longValue());
            assertEquals(sqlFavs, ((Number) ((Map<?, ?>) dash.get("favoriteSummary")).get("totalFavorites")).longValue());

            json.append(String.format("  \"%s\": {\"from\":\"%s\",\"to\":\"%s\",\"channel\":\"%s\",\"categoryId\":%s,\"pageRevenue\":%s,\"sqlRevenue\":%s,\"pageCash\":%s,\"sqlCash\":%s,\"pageTransfer\":%s,\"sqlTransfer\":%s,\"pageEligibleOrders\":%s,\"sqlEligibleOrders\":%s,\"pageTotalOrders\":%s,\"sqlTotalOrders\":%s,\"pageFavorites\":%s,\"sqlFavorites\":%s}%s\n",
                    s.name(), from, dash.get("to"), s.ch(), s.cat(),
                    dash.get("periodRevenue"), sqlRev,
                    dash.get("cashRevenue"), sqlCash,
                    dash.get("transferRevenue"), sqlTrans,
                    dash.get("eligibleOrders"), sqlEligibleOrders,
                    dash.get("orders"), sqlTotalOrders,
                    ((Map<?, ?>) dash.get("favoriteSummary")).get("totalFavorites"), sqlFavs,
                    i == scenarios.size() - 1 ? "" : ","));
        }
        json.append("}\n");

        Path outDir = Path.of("docs/evidence/gd16");
        Files.createDirectories(outDir);
        Files.writeString(outDir.resolve("dashboard-sql-pairs.json"), json.toString(), StandardCharsets.UTF_8);
    }

    @Test @WithMockUser(roles = "USER")
    void userIsForbidden() throws Exception {
        MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build().perform(get("/admin/dashboard")).andExpect(status().isForbidden());
        MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build().perform(get("/admin/statistics")).andExpect(status().isForbidden());
    }
    @Test @WithMockUser(roles = "EMPLOYEE")
    void employeeIsForbidden() throws Exception {
        MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build().perform(get("/admin/dashboard")).andExpect(status().isForbidden());
        MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build().perform(get("/admin/statistics")).andExpect(status().isForbidden());
    }
}
