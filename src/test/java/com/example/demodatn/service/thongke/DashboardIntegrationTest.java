package com.example.demodatn.service.thongke;
import com.example.demodatn.controller.AdminPageAdvice;
import com.example.demodatn.repository.thongke.DashboardRepository;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional // All fixture writes roll back; no seed data persists.
class DashboardIntegrationTest {
    @Autowired DashboardRepository repository;
    @Autowired DashboardService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;

    @Test
    void revenueIncludesStartExcludesEndUnpaidAndCancelled() {
        LocalDate start = LocalDate.of(2040, 1, 1), end = start.plusDays(1);
        BigDecimal before = (BigDecimal) repository.revenue(start, end);
        addOrder("2040-01-01T00:00:00", "PAID", "DELIVERED", 12345, "CASH");
        addOrder("2040-01-01T23:59:59", "PAID", "CONFIRMED", 55, "BANK_TRANSFER");
        addOrder("2040-01-02T00:00:00", "PAID", "DELIVERED", 999, "CASH");
        addOrder("2040-01-01T12:00:00", "UNPAID", "PENDING", 999, "COD");
        addOrder("2040-01-01T12:00:00", "PAID", "CANCELLED", 999, "TRANSFER");
        addOrder("2040-01-01T12:00:00", "PAID", "DELIVERY_FAILED", 999, "COD");
        assertEquals(0, before.add(new BigDecimal("12400")).compareTo((BigDecimal) repository.revenue(start, end)));
    }

    @Test
    void cashPlusTransferEqualsTotalPeriodRevenue() {
        LocalDate start = LocalDate.of(2040, 5, 1), end = start.plusDays(5);
        addOrder("2040-05-01T10:00:00", "PAID", "DELIVERED", 150000, "CASH");
        addOrder("2040-05-02T11:00:00", "PAID", "DELIVERED", 250000, "COD");
        addOrder("2040-05-03T12:00:00", "PAID", "CONFIRMED", 300000, "BANK_TRANSFER");
        addOrder("2040-05-04T13:00:00", "PAID", "DELIVERED", 200000, "TRANSFER");

        BigDecimal total = (BigDecimal) repository.revenue(start, end);
        BigDecimal cash = (BigDecimal) repository.cashRevenue(start, end);
        BigDecimal transfer = (BigDecimal) repository.transferRevenue(start, end);

        assertEquals(0, new BigDecimal("400000").compareTo(cash));
        assertEquals(0, new BigDecimal("500000").compareTo(transfer));
        assertEquals(0, total.compareTo(cash.add(transfer)));
    }

    private void addOrder(String date, String payment, String status, int amount, String method) {
        jdbc.update("INSERT INTO orders(order_code,order_type,status,total_amount,final_amount,payment_status,payment_method,created_at) VALUES (?,'ONLINE',?,?,?,?,?,CAST(? AS datetime))",
                "AUDIT-" + UUID.randomUUID(), status, amount, amount, payment, method, date);
    }

    @Test
    void lowStockIncludesThresholdAndZeroAndChecksInventoryLogsForPeriod() {
        Integer product = jdbc.queryForObject("INSERT INTO products(code,name,status) OUTPUT INSERTED.id VALUES(?,N'Sản phẩm kiểm thử tồn kho',1)", Integer.class, "DASH-" + UUID.randomUUID());
        Integer id = jdbc.queryForObject("INSERT INTO product_variants(product_id,size_id,chat_lieu_id,mau_sac_id,de_giay_id,price,quantity,status) OUTPUT INSERTED.id SELECT ?,MIN(s.id),MIN(m.id),(SELECT MIN(id) FROM mau_sac),(SELECT MIN(id) FROM de_giay),100000,5,1 FROM sizes s CROSS JOIN chat_lieu m", Integer.class, product);
        assertNotNull(id);
        jdbc.update("UPDATE product_variants SET quantity=5,status=1 WHERE id=?", id);
        assertTrue(repository.lowStock(5).stream().anyMatch(row -> id.equals(row.get("id"))));
        assertFalse(repository.lowStock(4).stream().anyMatch(row -> id.equals(row.get("id"))));

        LocalDate periodStart = LocalDate.of(2040, 6, 1);
        LocalDate periodEnd = periodStart.plusDays(7);
        assertFalse(repository.lowStock(5, periodStart, periodEnd).stream().anyMatch(row -> id.equals(row.get("id"))));

        jdbc.update("INSERT INTO inventory_logs(variant_id,quantity_change,quantity_before,quantity_after,reason,actor,created_at) VALUES(?,-2,7,5,N'Bán hàng','admin',CAST('2040-06-03T10:00:00' AS datetime))", id);
        assertTrue(repository.lowStock(5, periodStart, periodEnd).stream().anyMatch(row -> id.equals(row.get("id"))));

        jdbc.update("UPDATE product_variants SET quantity=0 WHERE id=?", id);
        assertTrue(repository.lowStock(0, periodStart, periodEnd).stream().anyMatch(row -> id.equals(row.get("id"))));
        jdbc.update("UPDATE product_variants SET status=0 WHERE id=?", id);
        assertFalse(repository.lowStock(5, periodStart, periodEnd).stream().anyMatch(row -> id.equals(row.get("id"))));
    }

    @Test
    void topCustomersExcludesWalkInAndOrdersByTotalSpent() {
        LocalDate start = LocalDate.of(2040, 7, 1), end = start.plusDays(2);
        Integer c1 = jdbc.queryForObject("INSERT INTO customers(customer_code,full_name,phone,status,created_at) OUTPUT INSERTED.id VALUES(?,N'Khách VIP 1','0911111111',1,CAST('2040-07-01T08:00:00' AS datetime))", Integer.class, "KH-T1-" + UUID.randomUUID().toString().substring(0, 6));
        Integer c2 = jdbc.queryForObject("INSERT INTO customers(customer_code,full_name,phone,status,created_at) OUTPUT INSERTED.id VALUES(?,N'Khách VIP 2','0922222222',1,CAST('2040-07-01T09:00:00' AS datetime))", Integer.class, "KH-T2-" + UUID.randomUUID().toString().substring(0, 6));
        // Walk-in order (customer_id = NULL) with huge amount
        jdbc.update("INSERT INTO orders(order_code,order_type,status,total_amount,final_amount,payment_status,payment_method,created_at) VALUES (?,'POS','DELIVERED',9999999,9999999,'PAID','CASH',CAST('2040-07-01T10:00:00' AS datetime))", "WALKIN-" + UUID.randomUUID());
        // Customer 1 & 2 orders
        jdbc.update("INSERT INTO orders(order_code,order_type,customer_id,status,total_amount,final_amount,payment_status,payment_method,created_at) VALUES (?,'ONLINE',?,'DELIVERED',300000,300000,'PAID','COD',CAST('2040-07-01T11:00:00' AS datetime))", "C1-" + UUID.randomUUID(), c1);
        jdbc.update("INSERT INTO orders(order_code,order_type,customer_id,status,total_amount,final_amount,payment_status,payment_method,created_at) VALUES (?,'ONLINE',?,'DELIVERED',800000,800000,'PAID','TRANSFER',CAST('2040-07-01T12:00:00' AS datetime))", "C2-" + UUID.randomUUID(), c2);

        List<Map<String, Object>> top = repository.topCustomers(start, end);
        assertEquals(2, top.size());
        assertEquals(c2, top.get(0).get("id"));
        assertEquals(c1, top.get(1).get("id"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void ordinaryUserCannotAccessAnyAdminRouteOrNestedMutation() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        for (var item : AdminPageAdvice.NAV) mvc.perform(get(item.route())).andExpect(status().isForbidden());
        for (String path : new String[]{"/admin", "/admin/", "/admin/products", "/admin/orders/detail/1", "/admin/arbitrary/deep/path"}) {
            mvc.perform(get(path)).andExpect(status().isForbidden());
        }
        for (String path : new String[]{"/admin/pos/add", "/admin/pos/checkout", "/admin/orders/update-status/1", "/admin/products-crud/save"}) {
            mvc.perform(post(path).with(csrf())).andExpect(status().isForbidden());
        }
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employeeCannotAccessDashboardOrStatistics() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        mvc.perform(get("/admin/dashboard")).andExpect(status().isForbidden());
        mvc.perform(get("/admin/statistics")).andExpect(status().isForbidden());
        mvc.perform(get("/admin/statistics/revenue")).andExpect(status().isForbidden());
    }
}
