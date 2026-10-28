package com.example.demodatn.repository.thongke;

import lombok.RequiredArgsConstructor;
import com.example.demodatn.repository.donhang.RevenueCondition;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class DashboardRepository {
    private final JdbcTemplate jdbc;
    // Revenue is final order value (including shipping), excluding failed/cancelled sales,
    // attributed to creation date because this schema has no paid_at timestamp.
    public static final String REVENUE = "SELECT COALESCE(SUM(o.final_amount),0) FROM orders o WHERE " + RevenueCondition.SQL + " AND o.created_at >= ? AND o.created_at < ?";
    public Number revenue(LocalDate from, LocalDate to) {
        return jdbc.queryForObject(REVENUE, java.math.BigDecimal.class, from, to);
    }
    public Map<String, Object> counts() {
        return jdbc.queryForMap("SELECT (SELECT COUNT_BIG(*) FROM orders) AS orders, (SELECT COUNT_BIG(*) FROM orders WHERE status='PENDING') AS pending, (SELECT COUNT_BIG(*) FROM products) AS products, (SELECT COUNT_BIG(*) FROM customers) AS customers");
    }
    public Map<String, Object> counts(LocalDate from, LocalDate to) {
        return jdbc.queryForMap(
                "SELECT (SELECT COUNT_BIG(*) FROM orders WHERE created_at >= ? AND created_at < ?) AS orders, " +
                "(SELECT COUNT_BIG(*) FROM orders WHERE status='PENDING' AND created_at >= ? AND created_at < ?) AS pending, " +
                "(SELECT COUNT_BIG(*) FROM products) AS products, " +
                "(SELECT COUNT_BIG(*) FROM customers WHERE created_at >= ? AND created_at < ?) AS customers",
                from, to, from, to, from, to);
    }
    public List<Map<String, Object>> daily(LocalDate from, LocalDate to) {
        return jdbc.queryForList("SELECT CAST(o.created_at AS date) AS day, SUM(o.final_amount) AS amount FROM orders o WHERE " + RevenueCondition.SQL + " AND o.created_at >= ? AND o.created_at < ? GROUP BY CAST(o.created_at AS date) ORDER BY day", from, to);
    }
    public List<Map<String, Object>> statuses() {
        return jdbc.queryForList("SELECT status, COUNT_BIG(*) AS total FROM orders GROUP BY status ORDER BY status");
    }
    public List<Map<String, Object>> statuses(LocalDate from, LocalDate to) {
        return jdbc.queryForList("SELECT status, COUNT_BIG(*) AS total FROM orders WHERE created_at >= ? AND created_at < ? GROUP BY status ORDER BY status", from, to);
    }
    public List<Map<String, Object>> bestSellers(LocalDate from, LocalDate to) {
        return jdbc.queryForList("SELECT TOP (5) p.id, p.code, p.name, SUM(CAST(d.quantity AS bigint)) AS quantity FROM order_details d JOIN orders o ON o.id=d.order_id JOIN product_variants v ON v.id=d.product_variant_id JOIN products p ON p.id=v.product_id WHERE " + RevenueCondition.SQL + " AND o.created_at >= ? AND o.created_at < ? GROUP BY p.id,p.code,p.name ORDER BY quantity DESC,p.id", from, to);
    }
    public List<Map<String, Object>> recent() {
        return jdbc.queryForList("SELECT TOP (10) o.id,o.order_code,o.status,o.final_amount,o.created_at,COALESCE(c.full_name,o.receiver_name,N'Khách lẻ') AS customer FROM orders o LEFT JOIN customers c ON c.id=o.customer_id ORDER BY o.created_at DESC,o.id DESC");
    }
    public List<Map<String, Object>> recent(LocalDate from, LocalDate to) {
        return jdbc.queryForList("SELECT TOP (10) o.id,o.order_code,o.status,o.final_amount,o.created_at,COALESCE(c.full_name,o.receiver_name,N'Khách lẻ') AS customer FROM orders o LEFT JOIN customers c ON c.id=o.customer_id WHERE o.created_at >= ? AND o.created_at < ? ORDER BY o.created_at DESC,o.id DESC", from, to);
    }
    public List<Map<String, Object>> lowStock(int threshold) {
        return jdbc.queryForList("SELECT v.id,p.code,p.name,s.size_value,m.ten AS material_name,v.quantity FROM product_variants v JOIN products p ON p.id=v.product_id JOIN sizes s ON s.id=v.size_id JOIN chat_lieu m ON m.id=v.chat_lieu_id WHERE v.status=1 AND p.status=1 AND v.quantity <= ? ORDER BY v.quantity,v.id", threshold);
    }
}
