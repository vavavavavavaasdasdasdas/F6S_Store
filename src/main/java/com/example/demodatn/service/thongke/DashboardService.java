package com.example.demodatn.service.thongke;

import com.example.demodatn.repository.donhang.RevenueCondition;
import com.example.demodatn.repository.thongke.DashboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final DashboardRepository repository;
    private final StatisticsService statisticsService;
    private final JdbcTemplate jdbc;
    private final com.example.demodatn.util.Pagination pagination;
    @Value("${shop.low-stock-threshold:5}") private int threshold;
    @Value("${shop.time-zone:Asia/Ho_Chi_Minh}") private String timeZone;

    private static final String CAT_EXISTS = " AND (? IS NULL OR EXISTS (SELECT 1 FROM order_details d_c JOIN product_variants v_c ON v_c.id=d_c.product_variant_id JOIN products p_c ON p_c.id=v_c.product_id WHERE d_c.order_id=o.id AND p_c.category_id=?))";
    private static final String FILTER = " o.created_at>=? AND o.created_at<? AND (?='ALL' OR o.order_type=?)" + CAT_EXISTS;
    private static final String ELIGIBLE = " " + RevenueCondition.SQL + " AND" + FILTER;

    @Transactional(readOnly = true)
    public Map<String, Object> load(int days) {
        return load(days, null, null);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> load(int days, LocalDate customFrom, LocalDate customTo) {
        return load(days, "", customFrom, customTo, "ALL", null, "0", "0", "0", "0", "0", "0", "0", "0");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> load(Integer days, String period, LocalDate customFrom, LocalDate customTo,
                                    String channel, Integer categoryId,
                                    Object channelPage, Object statusPage,
                                    Object quantityPage, Object revenuePage, Object customerPage,
                                    Object reviewPage, Object favoritePage, Object lowStockPage) {
        LocalDate today = LocalDate.now(ZoneId.of(timeZone));
        String ch = (channel == null || channel.isBlank()) ? "ALL" : channel.trim();
        String validCh = Set.of("ALL", "ONLINE", "POS").contains(ch) ? ch : "ALL";
        int validDays = (days != null && days == 30) ? 30 : 7;
        boolean hasCustomRange = customFrom != null || customTo != null || (period != null && !period.isBlank());
        LocalDate end = customTo != null ? customTo : today;
        LocalDate from;
        if (customFrom != null) {
            from = customFrom;
        } else if ("today".equalsIgnoreCase(period)) {
            from = end;
        } else if ("month".equalsIgnoreCase(period)) {
            from = end.withDayOfMonth(1);
        } else if ("last-month".equalsIgnoreCase(period)) {
            LocalDate lastMonth = today.minusMonths(1);
            from = lastMonth.withDayOfMonth(1);
            end = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth());
        } else {
            from = end.minusDays(validDays - 1);
        }
        if (end.isBefore(from)) {
            end = from;
        }
        LocalDate until = end.plusDays(1);
        long spanDays = Math.max(1, ChronoUnit.DAYS.between(from, end) + 1);

        // Load unified statistics for the period, channel, and brand
        Map<String, Object> stats = statisticsService.load(from, end, validCh, categoryId, quantityPage, revenuePage, customerPage);
        Map<String, Object> data = new LinkedHashMap<>(stats);

        // Operational counts filtered by period, channel, and brand where applicable
        Object[] filterArgs = {from, until, validCh, validCh, categoryId, categoryId};
        Long totalOrdersInPeriod = jdbc.queryForObject("SELECT COUNT_BIG(*) FROM orders o WHERE" + FILTER, Long.class, filterArgs);
        Long pendingOrdersInPeriod = jdbc.queryForObject("SELECT COUNT_BIG(*) FROM orders o WHERE o.status='PENDING' AND" + FILTER, Long.class, filterArgs);
        Long totalProducts = jdbc.queryForObject("SELECT COUNT_BIG(*) FROM products p WHERE (? IS NULL OR p.category_id=?)", Long.class, categoryId, categoryId);
        Long newCustomers = jdbc.queryForObject("SELECT COUNT_BIG(*) FROM customers c WHERE c.created_at>=? AND c.created_at<?", Long.class, from, until);

        Object[] todayArgs = {today, today.plusDays(1), validCh, validCh, categoryId, categoryId};
        Object[] monthArgs = {today.withDayOfMonth(1), today.plusDays(1), validCh, validCh, categoryId, categoryId};
        java.math.BigDecimal todayRev = jdbc.queryForObject("SELECT COALESCE(SUM(" + RevenueCondition.AMOUNT + "),0) FROM orders o WHERE" + ELIGIBLE, java.math.BigDecimal.class, todayArgs);
        java.math.BigDecimal monthRev = jdbc.queryForObject("SELECT COALESCE(SUM(" + RevenueCondition.AMOUNT + "),0) FROM orders o WHERE" + ELIGIBLE, java.math.BigDecimal.class, monthArgs);

        data.put("eligibleOrders", stats.get("orders"));
        data.put("orders", totalOrdersInPeriod);
        data.put("pending", pendingOrdersInPeriod);
        data.put("products", totalProducts);
        data.put("customers", newCustomers);
        data.put("todayRevenue", todayRev);
        data.put("monthRevenue", monthRev);
        data.put("periodRevenue", stats.get("revenue"));
        data.put("cashRevenue", stats.get("cashRevenue"));
        data.put("transferRevenue", stats.get("transferRevenue"));

        // Channel pagination (Block 2)
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> channelsList = (List<Map<String, Object>>) stats.get("channels");
        Page<Map<String, Object>> chPage = new PageImpl<>(channelsList, PageRequest.of(0, 12), channelsList.size());
        data.put("channelPage", chPage);

        // Status pagination (Block 3)
        String statusSql = "SELECT o.status AS id, o.status, COUNT_BIG(*) AS total FROM orders o WHERE" + FILTER + " GROUP BY o.status ORDER BY o.status";
        String statusCountSql = "SELECT COUNT_BIG(*) FROM (SELECT o.status FROM orders o WHERE" + FILTER + " GROUP BY o.status) s_ranks";
        Page<Map<String, Object>> stPage = pagination.sql(jdbc, statusSql, statusCountSql, Arrays.asList(filterArgs), statusPage);
        data.put("statusPage", stPage);
        data.put("statusLabels", com.example.demodatn.service.donhang.OrderStatusService.LABELS);

        // Legacy bestSellers alias (Block 4)
        data.put("bestSellers", stats.get("topQuantity"));

        // Reviews (Block 7) - filtered by date and brand; independent of sales channel
        Object[] prodFilterArgs = {from, until, categoryId, categoryId};
        data.put("reviewSummary", repository.reviewSummary(from, until, categoryId));
        data.put("topRatedProducts", repository.topRatedProducts(from, until, categoryId));
        data.put("recentReviews", repository.recentReviews(from, until, categoryId));
        String revSql = "SELECT d.id, d.ma, d.so_sao, d.noi_dung, d.created_at, p.id AS product_id, p.code AS product_code, p.name AS product_name, c.full_name AS customer_name FROM danh_gia d JOIN products p ON p.id=d.product_id JOIN customers c ON c.id=d.customer_id WHERE d.hien_thi=1 AND d.created_at>=? AND d.created_at<? AND (? IS NULL OR p.category_id=?) ORDER BY d.created_at DESC, d.id DESC";
        String revCountSql = "SELECT COUNT_BIG(*) FROM danh_gia d JOIN products p ON p.id=d.product_id WHERE d.hien_thi=1 AND d.created_at>=? AND d.created_at<? AND (? IS NULL OR p.category_id=?)";
        Page<Map<String, Object>> rvPage = pagination.sql(jdbc, revSql, revCountSql, Arrays.asList(prodFilterArgs), reviewPage);
        data.put("reviews", rvPage.getContent());
        data.put("reviewPage", rvPage);

        // Favorites (Block 8) - filtered by date and brand; independent of sales channel
        data.put("favoriteSummary", repository.favoriteSummary(from, until, categoryId));
        String favSql = "SELECT p.id, p.code, p.name, p.image_url, COUNT_BIG(*) AS favorite_count FROM yeu_thich y JOIN products p ON p.id=y.product_id WHERE y.created_at>=? AND y.created_at<? AND (? IS NULL OR p.category_id=?) GROUP BY p.id, p.code, p.name, p.image_url ORDER BY favorite_count DESC, p.id";
        String favCountSql = "SELECT COUNT_BIG(*) FROM (SELECT p.id FROM yeu_thich y JOIN products p ON p.id=y.product_id WHERE y.created_at>=? AND y.created_at<? AND (? IS NULL OR p.category_id=?) GROUP BY p.id) f_ranks";
        Page<Map<String, Object>> fvPage = pagination.sql(jdbc, favSql, favCountSql, Arrays.asList(prodFilterArgs), favoritePage);
        data.put("topFavoriteProducts", fvPage.getContent());
        data.put("favoritePage", fvPage);

        // Low stock (Block 9) - filtered by threshold, brand, and inventory movement in period; independent of sales channel
        int safeThreshold = Math.max(0, threshold);
        Object[] lowStockArgs = {safeThreshold, categoryId, categoryId, from, until};
        String lowSql = "SELECT v.id, v.ma AS variant_code, p.code, p.name, s.size_value, m.ten AS material_name, c.ten AS color_name, sole.ten AS sole_name, v.quantity FROM product_variants v JOIN products p ON p.id=v.product_id JOIN sizes s ON s.id=v.size_id JOIN chat_lieu m ON m.id=v.chat_lieu_id JOIN mau_sac c ON c.id=v.mau_sac_id JOIN de_giay sole ON sole.id=v.de_giay_id WHERE v.status=1 AND p.status=1 AND v.quantity <= ? AND (? IS NULL OR p.category_id=?) AND EXISTS (SELECT 1 FROM inventory_logs l WHERE l.variant_id=v.id AND l.created_at >= ? AND l.created_at < ?) ORDER BY v.quantity, v.id";
        String lowCountSql = "SELECT COUNT_BIG(*) FROM product_variants v JOIN products p ON p.id=v.product_id WHERE v.status=1 AND p.status=1 AND v.quantity <= ? AND (? IS NULL OR p.category_id=?) AND EXISTS (SELECT 1 FROM inventory_logs l WHERE l.variant_id=v.id AND l.created_at >= ? AND l.created_at < ?)";
        Page<Map<String, Object>> lsPage = pagination.sql(jdbc, lowSql, lowCountSql, Arrays.asList(lowStockArgs), lowStockPage);
        data.put("lowStock", lsPage.getContent());
        data.put("lowStockPage", lsPage);
        data.put("threshold", safeThreshold);

        data.put("days", hasCustomRange ? spanDays : validDays);
        data.put("selectedDays", (!hasCustomRange || (spanDays == 7 && end.equals(today))) ? 7 : (spanDays == 30 && end.equals(today) ? 30 : 0));
        data.put("from", from);
        data.put("to", end);
        data.put("channel", validCh);
        data.put("categoryId", categoryId);
        data.put("hasCustomRange", hasCustomRange);
        data.put("today", today.toString());
        return data;
    }
}
