package com.example.demodatn.service.hotro;

import com.example.demodatn.dto.hotro.AdminChatbotResult;
import com.example.demodatn.dto.hotro.ChatbotMessageResponse;
import com.example.demodatn.security.AdminAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service("adminRulesChatbotEngine")
@RequiredArgsConstructor
public class AdminRulesChatbotEngine implements AdminChatbotEngine {

    private final AdminChatbotToolService toolService;
    private final AdminAccess adminAccess;

    private static final List<String> ADMIN_QUICK_REPLIES = List.of(
            "Doanh thu hôm nay",
            "Đơn chờ xử lý",
            "Sản phẩm sắp hết hàng",
            "Top bán chạy tuần này",
            "Khách chi tiêu nhiều nhất",
            "Tin nhắn mới",
            "Hướng dẫn thao tác POS"
    );

    private static final List<String> EMPLOYEE_QUICK_REPLIES = List.of(
            "Đơn chờ xử lý",
            "Sản phẩm sắp hết hàng",
            "Top bán chạy tuần này",
            "Tin nhắn mới",
            "Hướng dẫn thao tác POS",
            "Kiểm tra tồn kho"
    );

    private static final Pattern ORDER_CODE_PATTERN = Pattern.compile("(?i)(DH\\d{1,8})");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b(0[3|5|7|8|9]\\d{8})\\b");
    private static final Pattern DATE_RANGE_PATTERN = Pattern.compile("(?i)(?:từ|tu)\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})\\s*(?:đến|den)\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})");

    @Override
    public AdminChatbotResult process(String message, String sessionId, Authentication authentication) {
        List<String> quickReplies = adminAccess.isAdmin(authentication) ? ADMIN_QUICK_REPLIES : EMPLOYEE_QUICK_REPLIES;

        if (message == null || message.isBlank()) {
            return AdminChatbotResult.builder()
                    .response(ChatbotMessageResponse.builder()
                            .text("Xin chào! Tôi là Trợ lý cửa hàng nội bộ F6S Store 🤖. Tôi có thể hỗ trợ gì cho bạn hôm nay?")
                            .quickReplies(quickReplies)
                            .build())
                    .toolName("GREETING")
                    .build();
        }

        String raw = ChatbotToolService.removeAccents(message).toLowerCase();

        // 1. KIỂM TRA BẢO MẬT & CHỐNG PROMPT INJECTION / SQL INJECTION / VƯỢT QUYỀN
        if (isSecurityViolation(raw)) {
            return AdminChatbotResult.builder()
                    .response(ChatbotMessageResponse.builder()
                            .text("Xin lỗi, tôi là Trợ lý cửa hàng nội bộ và chỉ hỗ trợ tra cứu thông tin theo đúng phân quyền và nghiệp vụ của hệ thống.")
                            .quickReplies(quickReplies)
                            .build())
                    .toolName("SECURITY_VIOLATION")
                    .build();
        }

        // 2. LỜI CHÀO HỎI
        if (isGreeting(raw)) {
            return AdminChatbotResult.builder()
                    .response(ChatbotMessageResponse.builder()
                            .text("Xin chào bạn! Tôi là Trợ lý cửa hàng F6S Store 🤖. Tôi có thể hỗ trợ tra cứu doanh thu, theo dõi đơn chờ xử lý, kiểm tra tồn kho, cảnh báo sắp hết hàng và hướng dẫn thao tác nghiệp vụ.")
                            .quickReplies(quickReplies)
                            .build())
                    .toolName("GREETING")
                    .build();
        }

        // 3. DOANH THU & BÁO CÁO TÀI CHÍNH (Kiểm tra phân quyền ngay tại ToolService)
        if (isRevenueIntent(raw)) {
            String period = "today";
            LocalDate customFrom = null;
            LocalDate customTo = null;

            Matcher dateMatcher = DATE_RANGE_PATTERN.matcher(message);
            if (dateMatcher.find()) {
                try {
                    customFrom = parseDate(dateMatcher.group(1));
                    customTo = parseDate(dateMatcher.group(2));
                    period = "custom";
                } catch (Exception ignored) {
                }
            } else if (raw.contains("hom qua")) {
                period = "yesterday";
            } else if (raw.contains("thang")) {
                period = "month";
            } else if (raw.contains("tuan") || raw.contains("7 ngay")) {
                period = "week";
            }

            String result = toolService.getRevenue(period, customFrom, customTo, authentication);
            return AdminChatbotResult.builder()
                    .response(ChatbotMessageResponse.builder().text(result).quickReplies(quickReplies).build())
                    .toolName("getRevenue")
                    .build();
        }

        // 4. DANH SÁCH ĐƠN CHỜ XỬ LÝ (PENDING)
        if (isPendingOrdersIntent(raw)) {
            String result = toolService.getPendingOrders();
            return AdminChatbotResult.builder()
                    .response(ChatbotMessageResponse.builder().text(result).quickReplies(quickReplies).build())
                    .toolName("getPendingOrders")
                    .build();
        }

        // 5. SỐ ĐƠN THEO TRẠNG THÁI / THỐNG KÊ ĐƠN
        if (isOrderStatusIntent(raw)) {
            String result = toolService.getOrderStatusCounts();
            return AdminChatbotResult.builder()
                    .response(ChatbotMessageResponse.builder().text(result).quickReplies(quickReplies).build())
                    .toolName("getOrderStatusCounts")
                    .build();
        }

        // 6. SẢN PHẨM SẮP HẾT HÀNG (CẢNH BÁO TỒN)
        if (isLowStockIntent(raw)) {
            String result = toolService.getLowStockProducts();
            return AdminChatbotResult.builder()
                    .response(ChatbotMessageResponse.builder().text(result).quickReplies(quickReplies).build())
                    .toolName("getLowStockProducts")
                    .build();
        }

        // 7. TOP SẢN PHẨM BÁN CHẠY
        if (isTopProductsIntent(raw)) {
            String result = toolService.getTopProducts(authentication);
            return AdminChatbotResult.builder()
                    .response(ChatbotMessageResponse.builder().text(result).quickReplies(quickReplies).build())
                    .toolName("getTopProducts")
                    .build();
        }

        // 8. TOP KHÁCH HÀNG CHI TIÊU NHIỀU NHẤT
        if (isTopCustomersIntent(raw)) {
            String result = toolService.getTopCustomers(authentication);
            return AdminChatbotResult.builder()
                    .response(ChatbotMessageResponse.builder().text(result).quickReplies(quickReplies).build())
                    .toolName("getTopCustomers")
                    .build();
        }

        // 9. TRA CỨU HÓA ĐƠN HOẶC ĐƠN HÀNG (THEO MÃ HOẶC SĐT)
        if (isOrderTrackingIntent(raw, message)) {
            String query = extractTrackingQuery(message, raw);
            String result = toolService.trackInvoiceOrOrder(query);
            return AdminChatbotResult.builder()
                    .response(ChatbotMessageResponse.builder().text(result).quickReplies(quickReplies).build())
                    .toolName("trackInvoiceOrOrder")
                    .build();
        }

        // 10. SỐ PHIẾU HỖ TRỢ MỚI
        if (isSupportTicketIntent(raw)) {
            String result = toolService.getNewSupportTickets();
            return AdminChatbotResult.builder()
                    .response(ChatbotMessageResponse.builder().text(result).quickReplies(quickReplies).build())
                    .toolName("getNewSupportTickets")
                    .build();
        }

        // 11. TỒN KHO THEO MÃ HOẶC TÊN SẢN PHẨM
        if (isStockCheckIntent(raw)) {
            String kw = extractProductKeyword(message, raw);
            String result = toolService.getProductStock(kw);
            return AdminChatbotResult.builder()
                    .response(ChatbotMessageResponse.builder().text(result).quickReplies(quickReplies).build())
                    .toolName("getProductStock")
                    .build();
        }

        // 12. HƯỚNG DẪN THAO TÁC NỘI BỘ (POS, HÓA ĐƠN, TỒN KHO, PHIẾU HỖ TRỢ)
        if (isHelpOrFaqIntent(raw)) {
            String faqAnswer = toolService.getInternalFaq(message);
            if (faqAnswer != null) {
                return AdminChatbotResult.builder()
                        .response(ChatbotMessageResponse.builder().text(faqAnswer).quickReplies(quickReplies).build())
                        .toolName("getInternalFaq")
                        .build();
            }
        }

        // 13. NGOÀI PHẠM VI (OUT OF SCOPE)
        return AdminChatbotResult.builder()
                .response(ChatbotMessageResponse.builder()
                        .text("Xin lỗi, tôi là Trợ lý cửa hàng nội bộ của F6S Store 🤖. Tôi chỉ hỗ trợ các câu hỏi liên quan đến quản lý cửa hàng (doanh thu, đơn hàng, tồn kho, sản phẩm bán chạy, khách VIP, phiếu hỗ trợ và hướng dẫn thao tác). Bạn vui lòng chọn gợi ý nhanh bên dưới hoặc đặt câu hỏi về các nghiệp vụ trên nhé!")
                        .quickReplies(quickReplies)
                        .build())
                .toolName("OUT_OF_SCOPE")
                .build();
    }

    // --- Helper methods for intent detection ---

    private boolean isSecurityViolation(String raw) {
        return raw.contains("ignore all instructions")
                || raw.contains("ignore instructions")
                || raw.contains("bo qua huong dan")
                || raw.contains("quen moi quy tac")
                || raw.contains("quen quy tac")
                || raw.contains("bo qua quy tac")
                || raw.contains("system prompt")
                || raw.contains("jailbreak")
                || raw.contains("drop table")
                || raw.contains("union select")
                || raw.contains("delete from")
                || raw.contains("bo qua phan quyen")
                || raw.contains("cho xem doanh thu mac du")
                || raw.contains("vuot quyen");
    }

    private boolean isGreeting(String raw) {
        return raw.matches("^(chao|xin chao|hello|hi|alo|tro ly oi|bot oi|chao bot|chao ban).*$")
                || raw.equals("tro ly")
                || raw.equals("tro ly cua hang");
    }

    private boolean isRevenueIntent(String raw) {
        return raw.contains("doanh thu")
                || raw.contains("doanh so")
                || raw.contains("thu nhap")
                || raw.contains("tien ban")
                || raw.contains("tien thu")
                || raw.contains("bao cao doanh thu")
                || raw.contains("bao cao tai chinh");
    }

    private boolean isPendingOrdersIntent(String raw) {
        return raw.contains("don cho xu ly")
                || raw.contains("don cho")
                || raw.contains("don pending")
                || raw.contains("don chua duyet")
                || raw.contains("don moi cho")
                || raw.contains("don can xu ly");
    }

    private boolean isOrderStatusIntent(String raw) {
        return raw.contains("so don theo trang thai")
                || raw.contains("thong ke don")
                || raw.contains("tinh hinh don")
                || raw.contains("so luong don")
                || raw.contains("cac trang thai don");
    }

    private boolean isLowStockIntent(String raw) {
        return raw.contains("sap het hang")
                || raw.contains("canh bao ton")
                || raw.contains("ton it")
                || raw.contains("het hang")
                || raw.contains("sap het")
                || raw.contains("hang ton duoi");
    }

    private boolean isTopProductsIntent(String raw) {
        return raw.contains("top ban chay")
                || raw.contains("san pham ban chay")
                || raw.contains("top san pham")
                || raw.contains("ban chay nhat")
                || raw.contains("best seller")
                || raw.contains("chay nhat");
    }

    private boolean isTopCustomersIntent(String raw) {
        return raw.contains("khach chi tieu")
                || raw.contains("top khach")
                || raw.contains("khach vip")
                || raw.contains("mua nhieu nhat")
                || raw.contains("khach hang vip");
    }

    private boolean isSupportTicketIntent(String raw) {
        return raw.contains("tin nhan moi") || raw.contains("hoi thoai") || raw.contains("phieu ho tro")
                || raw.contains("support ticket")
                || raw.contains("ticket moi")
                || raw.contains("khach can tro giup")
                || raw.contains("yeu cau ho tro")
                || raw.contains("ho tro moi");
    }

    private boolean isOrderTrackingIntent(String raw, String message) {
        if (ORDER_CODE_PATTERN.matcher(message).find() || PHONE_PATTERN.matcher(message).find()) {
            return true;
        }
        return raw.contains("tra hoa don")
                || raw.contains("tim hoa don")
                || raw.contains("tra don")
                || raw.contains("tim don")
                || raw.contains("kiem tra don")
                || raw.contains("kiem tra hoa don");
    }

    private String extractTrackingQuery(String message, String raw) {
        Matcher mCode = ORDER_CODE_PATTERN.matcher(message);
        if (mCode.find()) return mCode.group(1);

        Matcher mPhone = PHONE_PATTERN.matcher(message);
        if (mPhone.find()) return mPhone.group(1);

        // Bỏ các từ khóa dẫn dắt
        String cleaned = message.replaceAll("(?i)(tra hóa đơn|tìm hóa đơn|tra đơn hàng|tra đơn|tìm đơn|kiểm tra đơn|kiểm tra hóa đơn|hóa đơn|mã|sđt|số điện thoại|cho|của|với)", "").trim();
        return cleaned.isEmpty() ? message.trim() : cleaned;
    }

    private boolean isStockCheckIntent(String raw) {
        return raw.contains("ton kho")
                || raw.contains("kiem tra ton")
                || raw.contains("con bao nhieu")
                || raw.contains("so luong con")
                || raw.contains("check ton")
                || raw.contains("kiem kho")
                || raw.contains("con hang khong");
    }

    private String extractProductKeyword(String message, String raw) {
        String cleaned = message.replaceAll("(?i)(kiểm tra tồn kho của|kiểm tra tồn kho|tồn kho của|tồn kho|kiểm kho|check tồn|còn bao nhiêu đôi|còn bao nhiêu|số lượng còn của|số lượng còn|sản phẩm)", "").trim();
        return cleaned;
    }

    private boolean isHelpOrFaqIntent(String raw) {
        return raw.contains("huong dan")
                || raw.contains("thao tac")
                || raw.contains("cach dung")
                || raw.contains("lam sao de")
                || raw.contains("quy trinh")
                || raw.contains("cach ban hang")
                || raw.contains("cach nhap kho")
                || raw.contains("cach xu ly don")
                || raw.contains("pos");
    }

    private LocalDate parseDate(String str) {
        String s = str.trim().replace('/', '-');
        String[] parts = s.split("-");
        if (parts.length == 3) {
            int d = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            if (y < 100) y += 2000;
            return LocalDate.of(y, m, d);
        }
        return LocalDate.now();
    }
}
