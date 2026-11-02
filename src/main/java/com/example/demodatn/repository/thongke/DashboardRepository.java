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
    // Legacy revenue stays unchanged; new shipping-policy orders exclude shipping.
    // attributed to creation date because this schema has no paid_at timestamp.
    public static final String REVENUE = "SELECT COALESCE(SUM(" + RevenueCondition.AMOUNT + "),0) FROM orders o WHERE " + RevenueCondition.SQL + " AND o.created_at >= ? AND o.created_at < ?";
    public static final String CASH_REVENUE = "SELECT COALESCE(SUM(" + RevenueCondition.AMOUNT + "),0) FROM orders o WHERE " + RevenueCondition.SQL + " AND o.payment_method IN ('CASH','COD') AND o.created_at >= ? AND o.created_at < ?";
    public static final String TRANSFER_REVENUE = "SELECT COALESCE(SUM(" + RevenueCondition.AMOUNT + "),0) FROM orders o WHERE " + RevenueCondition.SQL + " AND o.payment_method IN ('BANK_TRANSFER','TRANSFER') AND o.created_at >= ? AND o.created_at < ?";

    public Number revenue(LocalDate from, LocalDate to) {
        return jdbc.queryForObject(REVENUE, java.math.BigDecimal.class, from, to);
    }
    public Number cashRevenue(LocalDate from, LocalDate to) {
        return jdbc.queryForObject(CASH_REVENUE, java.math.BigDecimal.class, from, to);
    }
    public Number transferRevenue(LocalDate from, LocalDate to) {
        return jdbc.queryForObject(TRANSFER_REVENUE, java.math.BigDecimal.class, from, to);
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
        return jdbc.queryForList("SELECT CAST(o.created_at AS date) AS day, SUM(" + RevenueCondition.AMOUNT + ") AS amount FROM orders o WHERE " + RevenueCondition.SQL + " AND o.created_at >= ? AND o.created_at < ? GROUP BY CAST(o.created_at AS date) ORDER BY day", from, to);
    }
    public List<Map<String, Object>> statuses() {
        return jdbc.queryForList("SELECT status, COUNT_BIG(*) AS total FROM orders GROUP BY status ORDER BY status");
    }
    public List<Map<String, Object>> statuses(LocalDate from, LocalDate to) {
        return jdbc.queryForList("SELECT status, COUNT_BIG(*) AS total FROM orders WHERE created_at >= ? AND created_at < ? GROUP BY status ORDER BY status", from, to);
    }
    public List<Map<String, Object>> bestSellers(LocalDate from, LocalDate to) {
        return jdbc.queryForList("SELECT TOP (5) p.id, p.code, p.name, p.image_url, SUM(CAST(d.quantity AS bigint)) AS quantity, COALESCE(SUM(d.total_price),0) AS revenue FROM order_details d JOIN orders o ON o.id=d.order_id JOIN product_variants v ON v.id=d.product_variant_id JOIN products p ON p.id=v.product_id WHERE " + RevenueCondition.SQL + " AND o.created_at >= ? AND o.created_at < ? GROUP BY p.id,p.code,p.name,p.image_url ORDER BY quantity DESC,revenue DESC,p.id", from, to);
    }
    public List<Map<String, Object>> topCustomers(LocalDate from, LocalDate to) {
        return jdbc.queryForList("SELECT TOP (5) c.id, c.customer_code AS code, c.full_name, COUNT_BIG(DISTINCT o.id) AS order_count, SUM(" + RevenueCondition.AMOUNT + ") AS total_spent FROM orders o JOIN customers c ON c.id=o.customer_id WHERE " + RevenueCondition.SQL + " AND o.created_at >= ? AND o.created_at < ? GROUP BY c.id,c.customer_code,c.full_name ORDER BY total_spent DESC,order_count DESC,c.id", from, to);
    }
    public Map<String, Object> reviewSummary(LocalDate from, LocalDate to) {
        return reviewSummary(from, to, null);
    }
    public Map<String, Object> reviewSummary(LocalDate from, LocalDate to, Integer categoryId) {
        return jdbc.queryForMap("SELECT COUNT_BIG(*) AS totalReviews, COALESCE(ROUND(AVG(CAST(d.so_sao AS float)),1),0.0) AS avgScore FROM danh_gia d JOIN products p ON p.id=d.product_id WHERE d.hien_thi=1 AND d.created_at >= ? AND d.created_at < ? AND (? IS NULL OR p.category_id=?)", from, to, categoryId, categoryId);
    }
    public List<Map<String, Object>> topRatedProducts(LocalDate from, LocalDate to) {
        return topRatedProducts(from, to, null);
    }
    public List<Map<String, Object>> topRatedProducts(LocalDate from, LocalDate to, Integer categoryId) {
        return jdbc.queryForList("SELECT TOP (5) p.id, p.code, p.name, p.image_url, COUNT_BIG(*) AS review_count, ROUND(AVG(CAST(d.so_sao AS float)),1) AS avg_score FROM danh_gia d JOIN products p ON p.id=d.product_id WHERE d.hien_thi=1 AND d.created_at >= ? AND d.created_at < ? AND (? IS NULL OR p.category_id=?) GROUP BY p.id,p.code,p.name,p.image_url HAVING COUNT_BIG(*) >= 3 ORDER BY avg_score DESC,review_count DESC,p.id", from, to, categoryId, categoryId);
    }
    public List<Map<String, Object>> recentReviews(LocalDate from, LocalDate to) {
        return recentReviews(from, to, null);
    }
    public List<Map<String, Object>> recentReviews(LocalDate from, LocalDate to, Integer categoryId) {
        return jdbc.queryForList("SELECT TOP (5) d.id, d.ma, d.so_sao, d.noi_dung, d.created_at, p.id AS product_id, p.code AS product_code, p.name AS product_name, c.full_name AS customer_name FROM danh_gia d JOIN products p ON p.id=d.product_id JOIN customers c ON c.id=d.customer_id WHERE d.hien_thi=1 AND d.created_at >= ? AND d.created_at < ? AND (? IS NULL OR p.category_id=?) ORDER BY d.created_at DESC,d.id DESC", from, to, categoryId, categoryId);
    }
    public Map<String, Object> favoriteSummary(LocalDate from, LocalDate to) {
        return favoriteSummary(from, to, null);
    }
    public Map<String, Object> favoriteSummary(LocalDate from, LocalDate to, Integer categoryId) {
        return jdbc.queryForMap("SELECT COUNT_BIG(*) AS totalFavorites FROM yeu_thich y JOIN products p ON p.id=y.product_id WHERE y.created_at >= ? AND y.created_at < ? AND (? IS NULL OR p.category_id=?)", from, to, categoryId, categoryId);
    }
    public List<Map<String, Object>> topFavoriteProducts(LocalDate from, LocalDate to) {
        return topFavoriteProducts(from, to, null);
    }
    public List<Map<String, Object>> topFavoriteProducts(LocalDate from, LocalDate to, Integer categoryId) {
        return jdbc.queryForList("SELECT TOP (5) p.id, p.code, p.name, p.image_url, COUNT_BIG(*) AS favorite_count FROM yeu_thich y JOIN products p ON p.id=y.product_id WHERE y.created_at >= ? AND y.created_at < ? AND (? IS NULL OR p.category_id=?) GROUP BY p.id,p.code,p.name,p.image_url ORDER BY favorite_count DESC,p.id", from, to, categoryId, categoryId);
    }
    public List<Map<String, Object>> recent() {
        return jdbc.queryForList("SELECT TOP (10) o.id,o.order_code,o.status,o.final_amount,o.created_at,COALESCE(c.full_name,o.receiver_name,N'Khách lẻ') AS customer FROM orders o LEFT JOIN customers c ON c.id=o.customer_id ORDER BY o.created_at DESC,o.id DESC");
    }
    public List<Map<String, Object>> recent(LocalDate from, LocalDate to) {
        return jdbc.queryForList("SELECT TOP (10) o.id,o.order_code,o.status,o.final_amount,o.created_at,COALESCE(c.full_name,o.receiver_name,N'Khách lẻ') AS customer FROM orders o LEFT JOIN customers c ON c.id=o.customer_id WHERE o.created_at >= ? AND o.created_at < ? ORDER BY o.created_at DESC,o.id DESC", from, to);
    }
    public List<Map<String, Object>> lowStock(int threshold) {
        return jdbc.queryForList("SELECT v.id,v.ma AS variant_code,p.code,p.name,s.size_value,m.ten AS material_name,c.ten AS color_name,sole.ten AS sole_name,v.quantity FROM product_variants v JOIN products p ON p.id=v.product_id JOIN sizes s ON s.id=v.size_id JOIN chat_lieu m ON m.id=v.chat_lieu_id JOIN mau_sac c ON c.id=v.mau_sac_id JOIN de_giay sole ON sole.id=v.de_giay_id WHERE v.status=1 AND p.status=1 AND v.quantity <= ? ORDER BY v.quantity,v.id", threshold);
    }
    public List<Map<String, Object>> lowStock(int threshold, LocalDate from, LocalDate to) {
        return lowStock(threshold, from, to, null);
    }
    public List<Map<String, Object>> lowStock(int threshold, LocalDate from, LocalDate to, Integer categoryId) {
        return jdbc.queryForList("SELECT v.id,v.ma AS variant_code,p.code,p.name,s.size_value,m.ten AS material_name,c.ten AS color_name,sole.ten AS sole_name,v.quantity FROM product_variants v JOIN products p ON p.id=v.product_id JOIN sizes s ON s.id=v.size_id JOIN chat_lieu m ON m.id=v.chat_lieu_id JOIN mau_sac c ON c.id=v.mau_sac_id JOIN de_giay sole ON sole.id=v.de_giay_id WHERE v.status=1 AND p.status=1 AND v.quantity <= ? AND (? IS NULL OR p.category_id=?) AND EXISTS (SELECT 1 FROM inventory_logs l WHERE l.variant_id=v.id AND l.created_at >= ? AND l.created_at < ?) ORDER BY v.quantity,v.id", threshold, categoryId, categoryId, from, to);
    }
}
