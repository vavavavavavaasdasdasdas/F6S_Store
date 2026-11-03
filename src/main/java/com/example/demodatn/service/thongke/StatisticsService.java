package com.example.demodatn.service.thongke;

import lombok.RequiredArgsConstructor;
import com.example.demodatn.repository.donhang.RevenueCondition;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StatisticsService {
    private final JdbcTemplate jdbc;
    private final com.example.demodatn.util.Pagination pagination;
    private static final String CAT_EXISTS = " AND (? IS NULL OR EXISTS (SELECT 1 FROM order_details d_c JOIN product_variants v_c ON v_c.id=d_c.product_variant_id JOIN products p_c ON p_c.id=v_c.product_id WHERE d_c.order_id=o.id AND p_c.category_id=?))";
    private static final String FILTER = " o.created_at>=? AND o.created_at<? AND (?='ALL' OR o.order_type=?)" + CAT_EXISTS;
    private static final String ELIGIBLE = " " + RevenueCondition.SQL + " AND" + FILTER;
    private static final String PRODUCT_ELIGIBLE = " " + RevenueCondition.SQL + " AND o.created_at>=? AND o.created_at<? AND (?='ALL' OR o.order_type=?) AND (? IS NULL OR p.category_id=?)";

    @Transactional(readOnly = true)
    public Map<String, Object> load(LocalDate from, LocalDate to, String channel) {
        return load(from, to, channel, null, "0", "0", "0");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> load(LocalDate from, LocalDate to, String channel, Object page, Object quantityPage, Object revenuePage) {
        return load(from, to, channel, null, quantityPage == null ? page : quantityPage, revenuePage == null ? page : revenuePage, "0");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> load(LocalDate from, LocalDate to, String channel, Integer categoryId, Object quantityPage, Object revenuePage, Object customerPage) {
        Object[] args = {from, to.plusDays(1), channel, channel, categoryId, categoryId};
        Map<String, Object> result = new LinkedHashMap<>(jdbc.queryForMap(
                "SELECT COALESCE(SUM(" + RevenueCondition.AMOUNT + "),0) AS revenue, COUNT_BIG(*) AS orders FROM orders o WHERE" + ELIGIBLE, args));
        BigDecimal revenue = (BigDecimal) result.get("revenue");
        long count = ((Number) result.get("orders")).longValue();
        result.put("average", count == 0 ? BigDecimal.ZERO : revenue.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP));

        BigDecimal cashRevenue = jdbc.queryForObject(
                "SELECT COALESCE(SUM(o.tien_mat),0) FROM orders o WHERE" + ELIGIBLE,
                BigDecimal.class, args);
        result.put("cashRevenue", cashRevenue);

        BigDecimal transferRevenue = jdbc.queryForObject(
                "SELECT COALESCE(SUM(o.tien_chuyen_khoan),0) FROM orders o WHERE" + ELIGIBLE,
                BigDecimal.class, args);
        result.put("transferRevenue", transferRevenue);

        // Daily revenue list (kept for internal backwards compatibility)
        var rows = jdbc.queryForList("SELECT CAST(o.created_at AS date) AS day, SUM(" + RevenueCondition.AMOUNT + ") AS amount, COUNT_BIG(*) AS orders FROM orders o WHERE" + ELIGIBLE + " GROUP BY CAST(o.created_at AS date) ORDER BY day", args);
        Map<LocalDate, Map<String, Object>> days = new HashMap<>();
        rows.forEach(row -> {
            LocalDate day = LocalDate.parse(row.get("day").toString());
            row.put("day", day.toString());
            days.put(day, row);
        });
        List<Map<String, Object>> daily = new ArrayList<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            daily.add(days.getOrDefault(day, Map.of("day", day.toString(), "amount", BigDecimal.ZERO, "orders", 0)));
        }
        result.put("daily", daily);

        // Channels (Online vs POS)
        List<Map<String, Object>> channelRows = jdbc.queryForList(
                "SELECT o.order_type, COUNT_BIG(*) AS orders, COALESCE(SUM(" + RevenueCondition.AMOUNT + "),0) AS revenue " +
                "FROM orders o WHERE" + ELIGIBLE + " GROUP BY o.order_type ORDER BY o.order_type", args);
        Map<String, Map<String, Object>> channelMap = new LinkedHashMap<>();
        for (Map<String, Object> r : channelRows) {
            channelMap.put((String) r.get("order_type"), r);
        }
        List<Map<String, Object>> channels = new ArrayList<>();
        for (String ch : List.of("ONLINE", "POS")) {
            if (!"ALL".equals(channel) && !channel.equals(ch)) continue;
            Map<String, Object> r = channelMap.get(ch);
            BigDecimal chRev = r != null ? (BigDecimal) r.get("revenue") : BigDecimal.ZERO;
            long chOrders = r != null ? ((Number) r.get("orders")).longValue() : 0L;
            double ratio = revenue.compareTo(BigDecimal.ZERO) > 0
                    ? chRev.multiply(BigDecimal.valueOf(100)).divide(revenue, 1, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", ch);
            item.put("name", "ONLINE".equals(ch) ? "Trực tuyến (Online)" : "Tại quầy (POS)");
            item.put("orders", chOrders);
            item.put("revenue", chRev);
            item.put("ratio", ratio);
            channels.add(item);
        }
        result.put("channels", channels);

        // Statuses
        result.put("statuses", jdbc.queryForList("SELECT o.status, COUNT_BIG(*) AS total FROM orders o WHERE" + FILTER + " GROUP BY o.status ORDER BY o.status", args));

        // Top products by quantity and revenue
        String products = "SELECT p.id, p.code, p.name, p.image_url, SUM(CAST(d.quantity AS bigint)) AS quantity, SUM(d.total_price) AS amount FROM order_details d JOIN orders o ON o.id=d.order_id JOIN product_variants v ON v.id=d.product_variant_id JOIN products p ON p.id=v.product_id WHERE" + PRODUCT_ELIGIBLE + " GROUP BY p.id,p.code,p.name,p.image_url ORDER BY ";
        String countSql = "SELECT COUNT_BIG(*) FROM (SELECT p.id FROM order_details d JOIN orders o ON o.id=d.order_id JOIN product_variants v ON v.id=d.product_variant_id JOIN products p ON p.id=v.product_id WHERE" + PRODUCT_ELIGIBLE + " GROUP BY p.id) ranks";
        var quantities = pagination.sql(jdbc, products + "quantity DESC,amount DESC,p.id", countSql, Arrays.asList(args), quantityPage);
        var revenues = pagination.sql(jdbc, products + "amount DESC,quantity DESC,p.id", countSql, Arrays.asList(args), revenuePage);
        result.put("topQuantity", quantities.getContent());
        result.put("topQuantityPage", quantities);
        result.put("topRevenue", revenues.getContent());
        result.put("topRevenuePage", revenues);

        // Top customers
        String customers = "SELECT c.id, c.customer_code AS code, c.full_name, COUNT_BIG(DISTINCT o.id) AS order_count, SUM(" + RevenueCondition.AMOUNT + ") AS total_spent FROM orders o JOIN customers c ON c.id=o.customer_id WHERE" + ELIGIBLE + " GROUP BY c.id,c.customer_code,c.full_name ORDER BY total_spent DESC,order_count DESC,c.id";
        String countCustomers = "SELECT COUNT_BIG(*) FROM (SELECT c.id FROM orders o JOIN customers c ON c.id=o.customer_id WHERE" + ELIGIBLE + " GROUP BY c.id) ranks";
        var topCustomers = pagination.sql(jdbc, customers, countCustomers, Arrays.asList(args), customerPage);
        result.put("topCustomers", topCustomers.getContent());
        result.put("topCustomersPage", topCustomers);

        return result;
    }
}
