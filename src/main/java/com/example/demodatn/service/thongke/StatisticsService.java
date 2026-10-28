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
    public Map<String,Object> load(LocalDate from,LocalDate to,String channel){return load(from,to,channel,null,"0","0","0");}

    @Transactional(readOnly=true)
    public Map<String,Object> load(LocalDate from, LocalDate to, String channel,Object page,Object quantityPage,Object revenuePage) {
        return load(from, to, channel, null, page, quantityPage, revenuePage);
    }

    @Transactional(readOnly=true)
    public Map<String,Object> load(LocalDate from, LocalDate to, String channel, Integer categoryId, Object page, Object quantityPage, Object revenuePage) {
        Object[] args = {from, to.plusDays(1), channel, channel, categoryId, categoryId};
        Map<String,Object> result = new LinkedHashMap<>(jdbc.queryForMap(
                "SELECT COALESCE(SUM(o.final_amount),0) AS revenue,COUNT_BIG(*) AS orders FROM orders o WHERE" + ELIGIBLE, args));
        BigDecimal revenue = (BigDecimal) result.get("revenue");
        long count = ((Number) result.get("orders")).longValue();
        result.put("average",count==0 ? BigDecimal.ZERO : revenue.divide(BigDecimal.valueOf(count),2,RoundingMode.HALF_UP));
        var rows = jdbc.queryForList("SELECT CAST(o.created_at AS date) AS day,SUM(o.final_amount) AS amount,COUNT_BIG(*) AS orders FROM orders o WHERE" + ELIGIBLE + " GROUP BY CAST(o.created_at AS date) ORDER BY day",args);
        Map<LocalDate,Map<String,Object>> days = new HashMap<>();
        rows.forEach(row -> {
            LocalDate day = LocalDate.parse(row.get("day").toString());
            row.put("day",day.toString());
            days.put(day,row);
        });
        List<Map<String,Object>> daily = new ArrayList<>();
        for(LocalDate day=from;!day.isAfter(to);day=day.plusDays(1))
            daily.add(days.getOrDefault(day,Map.of("day",day.toString(),"amount",BigDecimal.ZERO,"orders",0)));
        result.put("daily",daily);
        result.put("statuses",jdbc.queryForList("SELECT o.status,COUNT_BIG(*) AS total FROM orders o WHERE" + FILTER + " GROUP BY o.status ORDER BY o.status",args));
        String products = "SELECT p.id,p.code,p.name,p.image_url,SUM(CAST(d.quantity AS bigint)) AS quantity,SUM(d.total_price) AS amount FROM order_details d JOIN orders o ON o.id=d.order_id JOIN product_variants v ON v.id=d.product_variant_id JOIN products p ON p.id=v.product_id WHERE" + PRODUCT_ELIGIBLE + " GROUP BY p.id,p.code,p.name,p.image_url ORDER BY ";
        String countSql="SELECT COUNT_BIG(*) FROM (SELECT p.id FROM order_details d JOIN orders o ON o.id=d.order_id JOIN product_variants v ON v.id=d.product_variant_id JOIN products p ON p.id=v.product_id WHERE"+PRODUCT_ELIGIBLE+" GROUP BY p.id) ranks";
        var quantities=pagination.sql(jdbc,products+"quantity DESC,amount DESC,p.id",countSql,Arrays.asList(args),quantityPage);
        var revenues=pagination.sql(jdbc,products+"amount DESC,quantity DESC,p.id",countSql,Arrays.asList(args),revenuePage);
        result.put("topQuantity",quantities.getContent());result.put("topQuantityPage",quantities);
        result.put("topRevenue",revenues.getContent());result.put("topRevenuePage",revenues);
        long daysCount=java.time.temporal.ChronoUnit.DAYS.between(from,to)+1;
        var request=pagination.request(page,daysCount,org.springframework.data.domain.Sort.by("day"));
        LocalDate first=from.plusDays(request.getOffset()),last=first.plusDays(request.getPageSize()-1);
        if(last.isAfter(to))last=to;
        String calendar="WITH days AS (SELECT CAST(? AS date) AS day UNION ALL SELECT DATEADD(day,1,day) FROM days WHERE day<?) SELECT CONVERT(varchar(10),days.day,23) AS day,COUNT_BIG(o.id) AS orders,COALESCE(SUM(o.final_amount),0) AS amount FROM days LEFT JOIN orders o ON o.created_at>=days.day AND o.created_at<DATEADD(day,1,days.day) AND " + RevenueCondition.SQL + " AND (?='ALL' OR o.order_type=?)" + CAT_EXISTS + " GROUP BY days.day ORDER BY days.day OPTION(MAXRECURSION 0)";
        var dayRows=jdbc.queryForList(calendar,first,last,channel,channel,categoryId,categoryId);
        result.put("dailyPage",new org.springframework.data.domain.PageImpl<>(dayRows,request,daysCount));
        return result;
    }
}
