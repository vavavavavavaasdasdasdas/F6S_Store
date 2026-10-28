package com.example.demodatn.service.thongke;
import com.example.demodatn.repository.thongke.DashboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${shop.low-stock-threshold:5}") private int threshold;
    @Value("${shop.time-zone:Asia/Ho_Chi_Minh}") private String timeZone;

    @Transactional(readOnly = true)
    public Map<String, Object> load(int days) {
        return load(days, null, null);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> load(int days, LocalDate customFrom, LocalDate customTo) {
        LocalDate today = LocalDate.now(ZoneId.of(timeZone));
        int validDays = days == 30 ? 30 : 7;
        boolean hasCustomRange = customFrom != null || customTo != null;
        LocalDate end = customTo != null ? customTo : today;
        LocalDate from = customFrom != null ? customFrom : end.minusDays(validDays - 1);
        if (end.isBefore(from)) {
            end = from;
        }
        LocalDate until = end.plusDays(1);
        long spanDays = Math.max(1, ChronoUnit.DAYS.between(from, end) + 1);

        Map<String, Object> data = new LinkedHashMap<>(hasCustomRange ? repository.counts(from, until) : repository.counts());
        data.put("todayRevenue", repository.revenue(today, today.plusDays(1)));
        data.put("monthRevenue", hasCustomRange ? repository.revenue(from, until) : repository.revenue(today.withDayOfMonth(1), today.plusDays(1)));
        data.put("periodRevenue", repository.revenue(from, until));
        Map<LocalDate, Object> amounts = new HashMap<>();
        repository.daily(from, until).forEach(row -> amounts.put(LocalDate.parse(row.get("day").toString()), row.get("amount")));
        List<String> labels = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (LocalDate day = from; day.isBefore(until); day = day.plusDays(1)) {
            labels.add(day.toString());
            values.add(amounts.getOrDefault(day, 0));
        }
        data.put("labels", labels);
        data.put("values", values);
        data.put("statuses", hasCustomRange ? repository.statuses(from, until) : repository.statuses());
        data.put("statusLabels",com.example.demodatn.service.donhang.OrderStatusService.LABELS);
        data.put("bestSellers", repository.bestSellers(from, until));
        data.put("recent", hasCustomRange ? repository.recent(from, until) : repository.recent());
        data.put("lowStock", repository.lowStock(Math.max(0, threshold)));
        data.put("threshold", Math.max(0, threshold));
        data.put("days", hasCustomRange ? spanDays : validDays);
        data.put("from", from);
        data.put("to", end);
        data.put("hasCustomRange", hasCustomRange);
        data.put("today", today.toString());
        return data;
    }
}
