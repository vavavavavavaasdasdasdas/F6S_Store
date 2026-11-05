package com.example.demodatn.service.hotro;

import com.example.demodatn.entity.donhang.Order;
import com.example.demodatn.entity.hotro.ChatbotFaq;
import com.example.demodatn.entity.hotro.SupportTicket;
import com.example.demodatn.entity.sanpham.Product;
import com.example.demodatn.entity.sanpham.ProductVariant;
import com.example.demodatn.repository.donhang.OrderRepository;
import com.example.demodatn.repository.donhang.RevenueCondition;
import com.example.demodatn.repository.hotro.ChatbotFaqRepository;
import com.example.demodatn.repository.hotro.SupportTicketRepository;
import com.example.demodatn.repository.sanpham.ProductRepository;
import com.example.demodatn.repository.sanpham.ProductVariantRepository;
import com.example.demodatn.security.AdminAccess;
import com.example.demodatn.service.donhang.OrderStatusService;
import com.example.demodatn.service.thongke.DashboardService;
import com.example.demodatn.service.thongke.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminChatbotToolService {

    private final DashboardService dashboardService;
    private final StatisticsService statisticsService;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final OrderRepository orderRepository;
    private final SupportTicketRepository ticketRepository;
    private final ChatbotFaqRepository faqRepository;
    private final AdminAccess adminAccess;
    private final JdbcTemplate jdbc;

    @Value("${shop.time-zone:Asia/Ho_Chi_Minh}")
    private String timeZone;

    @Value("${shop.low-stock-threshold:5}")
    private int lowStockThreshold;

    private static final DecimalFormat CURRENCY_FMT = new DecimalFormat("#,###");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ==========================================
    // 1. DOANH THU (CHỈ DÀNH RIÊNG CHO ROLE_ADMIN)
    // ==========================================
    @Transactional(readOnly = true)
    public String getRevenue(String period, LocalDate customFrom, LocalDate customTo, Authentication auth) {
        if (!adminAccess.isAdmin(auth)) {
            return "Xin lỗi, thông tin doanh thu và tài chính chỉ dành riêng cho Quản trị viên (Admin). " +
                   "Bạn có thể tra cứu số lượng tồn kho, trạng thái đơn hàng hoặc hướng dẫn thao tác nhé!";
        }

        LocalDate today = LocalDate.now(ZoneId.of(timeZone));
        LocalDate from;
        LocalDate to;
        String periodLabel;

        if ("yesterday".equalsIgnoreCase(period)) {
            from = today.minusDays(1);
            to = today.minusDays(1);
            periodLabel = "hôm qua (" + from.format(DATE_FMT) + ")";
        } else if ("month".equalsIgnoreCase(period)) {
            from = today.withDayOfMonth(1);
            to = today;
            periodLabel = "tháng này (" + from.format(DATE_FMT) + " đến " + to.format(DATE_FMT) + ")";
        } else if ("week".equalsIgnoreCase(period) || "7".equalsIgnoreCase(period)) {
            from = today.minusDays(6);
            to = today;
            periodLabel = "tuần này (" + from.format(DATE_FMT) + " đến " + to.format(DATE_FMT) + ")";
        } else if (customFrom != null && customTo != null) {
            from = customFrom;
            to = customTo;
            periodLabel = "từ " + from.format(DATE_FMT) + " đến " + to.format(DATE_FMT);
        } else { // default "today"
            from = today;
            to = today;
            periodLabel = "hôm nay (" + today.format(DATE_FMT) + ")";
        }

        // Gọi đúng StatisticsService mà Dashboard đang dùng (khớp 100%)
        Map<String, Object> stats = statisticsService.load(from, to, "ALL");
        BigDecimal revenue = (BigDecimal) stats.get("revenue");
        long ordersCount = ((Number) stats.get("orders")).longValue();
        BigDecimal cashRev = (BigDecimal) stats.get("cashRevenue");
        BigDecimal transferRev = (BigDecimal) stats.get("transferRevenue");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> channels = (List<Map<String, Object>>) stats.get("channels");
        BigDecimal onlineRev = BigDecimal.ZERO;
        long onlineOrders = 0;
        BigDecimal posRev = BigDecimal.ZERO;
        long posOrders = 0;

        if (channels != null) {
            for (Map<String, Object> ch : channels) {
                String type = (String) ch.get("type");
                if ("ONLINE".equals(type)) {
                    onlineRev = (BigDecimal) ch.get("revenue");
                    onlineOrders = ((Number) ch.get("orders")).longValue();
                } else if ("POS".equals(type)) {
                    posRev = (BigDecimal) ch.get("revenue");
                    posOrders = ((Number) ch.get("orders")).longValue();
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📊 **Báo cáo doanh thu ").append(periodLabel).append("**:\n")
          .append("- **Tổng doanh thu**: **").append(CURRENCY_FMT.format(revenue)).append(" đ** (từ **").append(ordersCount).append("** đơn hợp lệ)\n")
          .append("- **Theo phương thức thanh toán**:\n")
          .append("  + Tiền mặt: **").append(CURRENCY_FMT.format(cashRev)).append(" đ**\n")
          .append("  + Chuyển khoản: **").append(CURRENCY_FMT.format(transferRev)).append(" đ**\n")
          .append("- **Theo kênh bán hàng**:\n")
          .append("  + Bán tại quầy (POS): **").append(CURRENCY_FMT.format(posRev)).append(" đ** (").append(posOrders).append(" đơn)\n")
          .append("  + Trực tuyến (Online): **").append(CURRENCY_FMT.format(onlineRev)).append(" đ** (").append(onlineOrders).append(" đơn)\n\n")
          .append("👉 [Xem chi tiết trên Dashboard](/admin/dashboard?from=").append(from).append("&to=").append(to).append(")");

        return sb.toString();
    }

    // ==========================================
    // 2. SỐ ĐƠN THEO TRẠNG THÁI
    // ==========================================
    @Transactional(readOnly = true)
    public String getOrderStatusCounts() {
        String sql = "SELECT o.status, COUNT_BIG(*) AS total FROM orders o GROUP BY o.status ORDER BY total DESC";
        List<Map<String, Object>> rows = jdbc.queryForList(sql);

        if (rows.isEmpty()) {
            return "Hệ thống hiện chưa có đơn hàng nào.\n\n👉 [Xem Quản lý hóa đơn](/admin/invoices)";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📦 **Thống kê số lượng đơn hàng theo trạng thái**:\n");
        long grandTotal = 0;
        for (Map<String, Object> r : rows) {
            String status = (String) r.get("status");
            long total = ((Number) r.get("total")).longValue();
            grandTotal += total;
            String label = OrderStatusService.LABELS.getOrDefault(status, status);
            sb.append("- ").append(label).append(" (").append(status).append("): **").append(total).append("** đơn\n");
        }
        sb.append("\n**Tổng cộng**: **").append(grandTotal).append("** đơn hàng.\n\n")
          .append("👉 [Xem chi tiết tại Quản lý hóa đơn](/admin/invoices)");

        return sb.toString();
    }

    // ==========================================
    // 3. DANH SÁCH ĐƠN CHỜ XỬ LÝ (PENDING)
    // ==========================================
    @Transactional(readOnly = true)
    public String getPendingOrders() {
        List<Order> pendingList = orderRepository.findByStatusOrderByCreatedAtDesc("PENDING");

        if (pendingList.isEmpty()) {
            return "Tuyệt vời! Hiện tại không có đơn hàng nào đang chờ xử lý.\n\n👉 [Xem Quản lý hóa đơn](/admin/invoices)";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("⏳ **Hiện có ").append(pendingList.size()).append(" đơn hàng đang chờ xử lý**:\n");

        int limit = Math.min(pendingList.size(), 5);
        for (int i = 0; i < limit; i++) {
            Order o = pendingList.get(i);
            String customerName = o.getReceiverName() != null ? o.getReceiverName()
                    : (o.getCustomer() != null ? o.getCustomer().getFullName() : "Khách lẻ");
            String phone = o.getPhoneNumber() != null ? o.getPhoneNumber() : "—";
            sb.append(i + 1).append(". **#").append(o.getOrderCode()).append("** - Khách: ")
              .append(customerName).append(" (SĐT: ").append(phone).append(") - ")
              .append(CURRENCY_FMT.format(o.getFinalAmount())).append(" đ - ")
              .append("[Mở hóa đơn](/admin/invoices/").append(o.getId()).append(")\n");
        }

        if (pendingList.size() > limit) {
            sb.append("... và còn ").append(pendingList.size() - limit).append(" đơn chờ xử lý khác.\n");
        }

        sb.append("\n👉 [Xem tất cả đơn chờ xử lý](/admin/invoices?trangThai=PENDING)");
        return sb.toString();
    }

    // ==========================================
    // 4. TỒN KHO THEO MÃ HOẶC TÊN SẢN PHẨM
    // ==========================================
    @Transactional(readOnly = true)
    public String getProductStock(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "Bạn vui lòng cho mình biết mã hoặc tên sản phẩm cần kiểm tra tồn kho nhé! (VD: 'tồn kho giày Nike AF1').";
        }

        String kw = keyword.trim().toLowerCase();
        List<Product> products = productRepository.findAll().stream()
                .filter(p -> (p.getCode() != null && p.getCode().toLowerCase().contains(kw))
                          || (p.getName() != null && p.getName().toLowerCase().contains(kw)))
                .limit(5)
                .toList();

        if (products.isEmpty()) {
            return "Không tìm thấy sản phẩm nào khớp với từ khóa '" + keyword + "'. Bạn vui lòng kiểm tra lại mã hoặc tên sản phẩm.\n\n👉 [Xem Danh sách biến thể](/admin/variants)";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("👟 **Thông tin tồn kho sản phẩm khớp với '").append(keyword).append("'**:\n\n");

        for (Product p : products) {
            List<ProductVariant> variants = variantRepository.findByProduct_IdAndStatusTrue(p.getId());
            int totalStock = variants.stream().mapToInt(v -> v.getQuantity() != null ? v.getQuantity() : 0).sum();

            sb.append("📦 **").append(p.getName()).append("** (Mã: `").append(p.getCode()).append("`)\n")
              .append("- **Tổng tồn kho**: **").append(totalStock).append("** đôi\n");

            if (!variants.isEmpty()) {
                sb.append("- Chi tiết biến thể:\n");
                for (ProductVariant v : variants) {
                    String size = v.getSize() != null ? v.getSize().getSizeValue() : "—";
                    String color = v.getColor() != null ? v.getColor().getName() : "—";
                    sb.append("  + Size ").append(size).append(" / ").append(color)
                      .append(" (").append(v.getMa() != null ? v.getMa() : "—").append("): **")
                      .append(v.getQuantity() != null ? v.getQuantity() : 0).append("** đôi\n");
                }
            }
            sb.append("\n");
        }

        sb.append("👉 [Xem chi tiết tại Danh sách biến thể](/admin/variants?keyword=").append(kw).append(")");
        return sb.toString();
    }

    // ==========================================
    // 5. SẢN PHẨM SẮP HẾT HÀNG
    // ==========================================
    @Transactional(readOnly = true)
    public String getLowStockProducts() {
        int threshold = Math.max(0, lowStockThreshold);
        String sql = "SELECT v.id, v.ma AS variant_code, p.code AS product_code, p.name AS product_name, " +
                     "s.size_value, c.ten AS color_name, v.quantity " +
                     "FROM product_variants v " +
                     "JOIN products p ON p.id = v.product_id " +
                     "JOIN sizes s ON s.id = v.size_id " +
                     "JOIN mau_sac c ON c.id = v.mau_sac_id " +
                     "WHERE v.status = 1 AND p.status = 1 AND v.quantity <= ? " +
                     "ORDER BY v.quantity ASC, v.id ASC";

        List<Map<String, Object>> rows = jdbc.queryForList(sql, threshold);

        if (rows.isEmpty()) {
            return "Tất cả sản phẩm hiện đều có lượng tồn an toàn (trên " + threshold + " đôi).\n\n👉 [Xem Danh sách biến thể](/admin/variants)";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("⚠️ **Cảnh báo ").append(rows.size()).append(" biến thể sản phẩm sắp hết hàng (tồn <= ").append(threshold).append(")**:\n");

        int limit = Math.min(rows.size(), 8);
        for (int i = 0; i < limit; i++) {
            Map<String, Object> r = rows.get(i);
            sb.append(i + 1).append(". **").append(r.get("product_name")).append("** (Size ")
              .append(r.get("size_value")).append(" - ").append(r.get("color_name")).append(") - ")
              .append("Mã: `").append(r.get("variant_code")).append("` - ")
              .append("Tồn còn: **").append(r.get("quantity")).append("** đôi\n");
        }

        if (rows.size() > limit) {
            sb.append("... và còn ").append(rows.size() - limit).append(" biến thể khác sắp hết hàng.\n");
        }

        sb.append("\n👉 [Kiểm tra tại Danh sách biến thể](/admin/variants?stock=low)");
        return sb.toString();
    }

    // ==========================================
    // 6. TOP SẢN PHẨM BÁN CHẠY
    // ==========================================
    @Transactional(readOnly = true)
    public String getTopProducts(Authentication auth) {
        LocalDate today = LocalDate.now(ZoneId.of(timeZone));
        LocalDate from = today.minusDays(6);
        boolean isAdmin = adminAccess.isAdmin(auth);

        Map<String, Object> stats = statisticsService.load(from, today, "ALL");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> topList = (List<Map<String, Object>>) stats.get("topQuantity");

        if (topList == null || topList.isEmpty()) {
            return "Chưa có dữ liệu sản phẩm bán chạy trong 7 ngày qua.\n\n👉 [Xem Dashboard](/admin/dashboard)";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("🏆 **Top sản phẩm bán chạy trong tuần này (7 ngày gần nhất)**:\n");

        int rank = 1;
        for (Map<String, Object> item : topList) {
            if (rank > 5) break;
            sb.append(rank).append(". **").append(item.get("name")).append("** (Mã: `")
              .append(item.get("code")).append("`) - Đã bán: **")
              .append(item.get("quantity")).append("** đôi");

            // Chỉ admin mới thấy số tiền doanh thu
            if (isAdmin && item.get("amount") != null) {
                BigDecimal amount = (BigDecimal) item.get("amount");
                sb.append(" - Doanh thu: **").append(CURRENCY_FMT.format(amount)).append(" đ**");
            }
            sb.append("\n");
            rank++;
        }

        sb.append("\n👉 [Xem báo cáo đầy đủ trên Dashboard](/admin/dashboard)");
        return sb.toString();
    }

    // ==========================================
    // 7. TOP KHÁCH HÀNG (CHỈ DÀNH RIÊNG CHO ROLE_ADMIN)
    // ==========================================
    @Transactional(readOnly = true)
    public String getTopCustomers(Authentication auth) {
        if (!adminAccess.isAdmin(auth)) {
            return "Xin lỗi, thông tin xếp hạng khách hàng và chi tiêu chỉ dành riêng cho Quản trị viên (Admin). " +
                   "Bạn có thể tra cứu tồn kho, đơn hàng hoặc hướng dẫn nghiệp vụ nhé!";
        }

        LocalDate today = LocalDate.now(ZoneId.of(timeZone));
        LocalDate from = today.minusDays(6);

        Map<String, Object> stats = statisticsService.load(from, today, "ALL");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> topCust = (List<Map<String, Object>>) stats.get("topCustomers");

        if (topCust == null || topCust.isEmpty()) {
            return "Chưa có dữ liệu khách hàng chi tiêu trong 7 ngày qua.\n\n👉 [Xem Dashboard](/admin/dashboard)";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("🌟 **Top khách hàng chi tiêu nhiều nhất trong 7 ngày qua**:\n");

        int rank = 1;
        for (Map<String, Object> c : topCust) {
            if (rank > 5) break;
            sb.append(rank).append(". **").append(c.get("full_name")).append("** (Mã: `")
              .append(c.get("code")).append("`) - Số đơn: **")
              .append(c.get("order_count")).append("** - Tổng chi tiêu: **")
              .append(CURRENCY_FMT.format((BigDecimal) c.get("total_spent"))).append(" đ**\n");
            rank++;
        }

        sb.append("\n👉 [Xem chi tiết tại Khách hàng](/admin/customers)");
        return sb.toString();
    }

    // ==========================================
    // 8. TRA CỨU HÓA ĐƠN / ĐƠN THEO MÃ HOẶC SĐT
    // ==========================================
    @Transactional(readOnly = true)
    public String trackInvoiceOrOrder(String query) {
        if (query == null || query.isBlank()) {
            return "Vui lòng nhập mã hóa đơn, mã đơn hàng hoặc số điện thoại để tra cứu (VD: 'tra đơn DH000002' hoặc 'tìm hóa đơn SĐT 0987654321').";
        }

        String q = query.trim();
        List<Order> orders = new ArrayList<>();

        // Thử tìm theo mã đơn chính xác
        orderRepository.findByOrderCode(q).ifPresent(orders::add);
        if (orders.isEmpty() && q.matches("\\d+")) {
            String padded = String.format("DH%06d", Integer.parseInt(q));
            orderRepository.findByOrderCode(padded).ifPresent(orders::add);
        }

        // Thử tìm theo số điện thoại
        if (orders.isEmpty()) {
            List<Order> byPhone = orderRepository.findAll().stream()
                    .filter(o -> q.equals(o.getPhoneNumber()))
                    .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                    .limit(3)
                    .toList();
            orders.addAll(byPhone);
        }

        if (orders.isEmpty()) {
            return "Không tìm thấy hóa đơn hoặc đơn hàng nào khớp với '" + q + "'.\n\n👉 [Xem Quản lý hóa đơn](/admin/invoices)";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("🔍 **Kết quả tra cứu đơn hàng / hóa đơn cho '").append(q).append("'**:\n\n");

        for (Order o : orders) {
            String statusLabel = OrderStatusService.LABELS.getOrDefault(o.getStatus(), o.getStatus());
            String customerName = o.getReceiverName() != null ? o.getReceiverName()
                    : (o.getCustomer() != null ? o.getCustomer().getFullName() : "Khách lẻ");

            sb.append("📋 **Đơn hàng #").append(o.getOrderCode()).append("**:\n")
              .append("- Khách hàng: **").append(customerName).append("**\n")
              .append("- Số điện thoại: **").append(o.getPhoneNumber() != null ? o.getPhoneNumber() : "—").append("**\n")
              .append("- Địa chỉ giao: ").append(o.getShippingAddress() != null ? o.getShippingAddress() : "Tại cửa hàng").append("\n")
              .append("- Kênh bán: ").append(o.getChannelLabel()).append("\n")
              .append("- Trạng thái: **").append(statusLabel).append("**\n")
              .append("- Thanh toán: ").append("PAID".equals(o.getPaymentStatus()) ? "Đã thanh toán" : "Chưa thanh toán")
              .append(" (").append(o.getPaymentMethod()).append(")\n")
              .append("- Tổng tiền: **").append(CURRENCY_FMT.format(o.getFinalAmount())).append(" đ**\n")
              .append("- 👉 [Mở chi tiết hóa đơn](/admin/invoices/").append(o.getId()).append(")\n\n");
        }

        return sb.toString();
    }

    // ==========================================
    // 9. SỐ PHIẾU HỖ TRỢ MỚI
    // ==========================================
    @Transactional(readOnly = true)
    public String getNewSupportTickets() {
        var counts=jdbc.queryForMap("SELECT COALESCE(SUM(chua_doc_nhan_vien),0) unread,COALESCE(SUM(CASE WHEN assigned_user_id IS NULL AND trang_thai<>N'DA_DONG' THEN 1 ELSE 0 END),0) unassigned FROM chat_hoi_thoai WHERE customer_id IS NOT NULL OR khach_id IS NULL OR legacy_ticket_id IS NOT NULL");
        return "Hiện có "+counts.get("unread")+" tin nhắn chưa đọc và "+counts.get("unassigned")+" hội thoại chưa ai nhận.\n\n👉 [Xem chi tiết](/admin/chat)";
    }

    // ==========================================
    // 10. HƯỚNG DẪN THAO TÁC NỘI BỘ
    // ==========================================
    @Transactional(readOnly = true)
    public String getInternalFaq(String query) {
        List<ChatbotFaq> list = faqRepository.findByHienThiTrueAndDoiTuong("NOI_BO");
        if (list.isEmpty()) {
            return "Hiện chưa có tài liệu hướng dẫn nội bộ.";
        }

        String rawQuery = ChatbotToolService.removeAccents(query);
        ChatbotFaq bestMatch = null;
        int maxScore = 0;

        for (ChatbotFaq faq : list) {
            int score = 0;
            String rawQ = ChatbotToolService.removeAccents(faq.getCauHoi());
            String rawKeywords = faq.getTuKhoa() != null ? ChatbotToolService.removeAccents(faq.getTuKhoa()) : "";

            for (String word : rawQuery.split("\\s+")) {
                if (word.length() <= 1) continue;
                if (rawQ.contains(word)) score += 2;
                if (rawKeywords.contains(word)) score += 3;
            }

            if (score > maxScore) {
                maxScore = score;
                bestMatch = faq;
            }
        }

        if (bestMatch != null && maxScore >= 2) {
            String link = "";
            if (bestMatch.getMa().equals("KB_ADMIN_01")) link = "\n\n👉 [Mở trang Bán hàng tại quầy](/admin/pos)";
            else if (bestMatch.getMa().equals("KB_ADMIN_02")) link = "\n\n👉 [Mở trang Quản lý hóa đơn](/admin/invoices)";
            else if (bestMatch.getMa().equals("KB_ADMIN_03")) link = "\n\n👉 [Mở trang Danh sách biến thể](/admin/variants)";
            else if (bestMatch.getMa().equals("KB_ADMIN_04")) link = "\n\n👉 [Mở trang Chat với khách](/admin/chat)";

            return "📖 **" + bestMatch.getCauHoi() + "**:\n\n" + bestMatch.getTraLoi() + link;
        }

        return null;
    }
}
