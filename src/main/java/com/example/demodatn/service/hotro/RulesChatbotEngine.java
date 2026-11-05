package com.example.demodatn.service.hotro;

import com.example.demodatn.dto.hotro.ChatbotMessageResponse;
import com.example.demodatn.dto.hotro.ChatbotProductCardDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service("rulesChatbotEngine")
@RequiredArgsConstructor
public class RulesChatbotEngine implements ChatbotEngine {

    private final ChatbotToolService toolService;

    // Quản lý số lần liên tiếp không hiểu theo sessionId
    private final Map<String, Integer> consecutiveUnknownMap = new ConcurrentHashMap<>();

    private static final List<String> DEFAULT_QUICK_REPLIES = List.of(
            "Tìm giày",
            "Tư vấn size",
            "Chính sách đổi trả",
            "Kiểm tra đơn hàng",
            "Nói chuyện với nhân viên"
    );

    @Override
    public ChatbotMessageResponse process(String message, String sessionId, Authentication authentication) {
        if (message == null || message.isBlank()) {
            return ChatbotMessageResponse.builder()
                    .text("Xin chào! Mình có thể giúp gì cho bạn?")
                    .quickReplies(DEFAULT_QUICK_REPLIES)
                    .build();
        }

        String raw = ChatbotToolService.removeAccents(message);

        // 1. KIỂM TRA BẢO MẬT & CHỐNG PROMPT INJECTION / HỎI DỮ LIỆU NỘI BỘ
        if (isSecurityViolation(raw)) {
            resetUnknown(sessionId);
            return ChatbotMessageResponse.builder()
                    .text("Xin lỗi bạn, mình là trợ lý ảo bán hàng của F6S Store và chỉ có thể hỗ trợ các thông tin công khai về sản phẩm, chính sách và đơn hàng của chính bạn.")
                    .quickReplies(DEFAULT_QUICK_REPLIES)
                    .build();
        }

        // 2. NÓI CHUYỆN VỚI NHÂN VIÊN
        if (isHumanSupportIntent(raw)) {
            resetUnknown(sessionId);
            return ChatbotMessageResponse.builder()
                    .text("Dạ, bạn vui lòng điền thông tin bên dưới để gửi yêu cầu hỗ trợ. Đội ngũ nhân viên F6S Store sẽ liên hệ lại với bạn sớm nhất có thể ạ!")
                    .suggestSupport(true)
                    .build();
        }

        // 3. LỜI CHÀO
        if (isGreeting(raw)) {
            resetUnknown(sessionId);
            return ChatbotMessageResponse.builder()
                    .text("Xin chào bạn! Mình là trợ lý ảo của F6S Store. Mình có thể giúp bạn tìm kiếm giày thể thao, tư vấn size, cập nhật khuyến mãi hoặc kiểm tra đơn hàng. Bạn cần mình hỗ trợ gì ạ?")
                    .quickReplies(DEFAULT_QUICK_REPLIES)
                    .build();
        }

        // 4. KIỂM TRA ĐƠN HÀNG
        if (isOrderTrackingIntent(raw)) {
            resetUnknown(sessionId);
            String orderCode = extractOrderCode(message);
            String trackResult = toolService.trackOrder(orderCode, authentication);
            boolean requireLogin = trackResult.contains("đăng nhập tài khoản");
            return ChatbotMessageResponse.builder()
                    .text(trackResult)
                    .requireLogin(requireLogin)
                    .quickReplies(List.of("Tìm giày", "Tư vấn size", "Nói chuyện với nhân viên"))
                    .build();
        }

        // 5. TƯ VẤN KÍCH CỠ GIÀY
        if (isSizeAdviceIntent(raw)) {
            resetUnknown(sessionId);
            Double footLength = extractFootLength(message);
            if (footLength != null) {
                return ChatbotMessageResponse.builder()
                        .text(toolService.getSizeAdvice(footLength))
                        .quickReplies(List.of("Tìm giày size " + Math.round(footLength + 16), "Chính sách đổi trả", "Nói chuyện với nhân viên"))
                        .build();
            } else {
                return ChatbotMessageResponse.builder()
                        .text("Để tư vấn cỡ giày chuẩn nhất, bạn vui lòng cho mình biết chiều dài bàn chân (tính theo cm, ví dụ: 'chân mình dài 25.5 cm') nhé!")
                        .quickReplies(List.of("Chân dài 24 cm", "Chân dài 25 cm", "Chân dài 25.5 cm", "Chân dài 26 cm"))
                        .build();
            }
        }

        // 6. KHUYẾN MÃI
        if (isPromotionIntent(raw)) {
            resetUnknown(sessionId);
            return ChatbotMessageResponse.builder()
                    .text(toolService.getActivePromotions())
                    .quickReplies(List.of("Tìm giày khuyến mãi", "Chính sách giao hàng", "Nói chuyện với nhân viên"))
                    .build();
        }

        // 7. CHÍNH SÁCH VÀ FAQ
        String faqAnswer = toolService.getPolicyOrFaq(message);
        if (faqAnswer != null && !faqAnswer.isBlank()) {
            resetUnknown(sessionId);
            return ChatbotMessageResponse.builder()
                    .text(faqAnswer)
                    .quickReplies(DEFAULT_QUICK_REPLIES)
                    .build();
        }

        // 8. TÌM KIẾM SẢN PHẨM
        if (isProductSearchIntent(raw)) {
            resetUnknown(sessionId);
            return handleProductSearch(raw, message);
        }

        // 9. KHÔNG HIỂU (FALLBACK & 2 LẦN LIÊN TIẾP)
        int unknownCount = consecutiveUnknownMap.getOrDefault(sessionId, 0) + 1;
        consecutiveUnknownMap.put(sessionId, unknownCount);

        if (unknownCount >= 2) {
            consecutiveUnknownMap.put(sessionId, 0); // reset sau khi gợi ý
            return ChatbotMessageResponse.builder()
                    .text("Có vẻ câu hỏi của bạn cần chuyên viên tư vấn hỗ trợ chi tiết hơn. Bạn có muốn gửi yêu cầu để nhân viên F6S Store liên hệ hỗ trợ trực tiếp không ạ?")
                    .suggestSupport(true)
                    .quickReplies(DEFAULT_QUICK_REPLIES)
                    .build();
        }

        return ChatbotMessageResponse.builder()
                .text("Xin lỗi, mình chưa hiểu rõ câu hỏi của bạn. Bạn có thể diễn đạt lại hoặc chọn các chủ đề gợi ý bên dưới nhé:")
                .quickReplies(DEFAULT_QUICK_REPLIES)
                .build();
    }

    /** The support inbox only invokes automation for explicit public shopping intents. */
    public boolean supportsSupportMessage(String message) {
        String raw=ChatbotToolService.removeAccents(message).toLowerCase(Locale.ROOT);
        return !isSecurityViolation(raw) && !isHumanSupportIntent(raw)
            && raw.matches(".*\\b(giay|sneaker|nike|adidas|puma|converse|vans|size|kich co|ban chan|chan dai|chinh sach|doi tra|bao hanh|giao hang|thanh toan|don hang|tra don|kiem tra don|dh[0-9]+)\\b.*");
    }

    private void resetUnknown(String sessionId) {
        if (sessionId != null) consecutiveUnknownMap.put(sessionId, 0);
    }

    private boolean isSecurityViolation(String raw) {
        return raw.contains("bo qua moi huong dan") ||
               raw.contains("ignore all instructions") ||
               raw.contains("doanh thu") ||
               raw.contains("doanh so") ||
               raw.contains("gia von") ||
               raw.contains("loi nhuan") ||
               raw.contains("mat khau") ||
               raw.contains("password") ||
               raw.contains("thong tin noi bo") ||
               raw.contains("khach hang khac") ||
               raw.contains("don cua nguoi khac") ||
               raw.contains("don hang cua nguoi khac") ||
               raw.contains("xem database") ||
               raw.contains("drop table");
    }

    private boolean isHumanSupportIntent(String raw) {
        return raw.contains("nhan vien") ||
               raw.contains("noi chuyen voi nhan vien") ||
               raw.contains("gap nhan vien") ||
               raw.contains("tu van vien") ||
               raw.contains("ho tro truc tiep") ||
               raw.contains("nguoi that");
    }

    private boolean isGreeting(String raw) {
        return raw.equals("xin chao") || raw.equals("chao") || raw.equals("hello") || raw.equals("hi") ||
               raw.startsWith("xin chao") || raw.startsWith("chao shop") || raw.startsWith("chao ban") ||
               raw.equals("hey");
    }

    private boolean isOrderTrackingIntent(String raw) {
        return raw.contains("don hang") || raw.contains("tra don") || raw.contains("kiem tra don") ||
               raw.contains("tinh trang don") || raw.contains("dh0") || raw.matches(".*dh\\d+.*");
    }

    private boolean isSizeAdviceIntent(String raw) {
        return raw.contains("tu van size") || raw.contains("chon size") || raw.contains("size bao nhieu") ||
               raw.contains("co giay") || raw.contains("kich co") || raw.contains("ban chan") || raw.contains("chan dai") ||
               (raw.contains("size") && raw.contains("chan"));
    }

    private boolean isPromotionIntent(String raw) {
        return raw.contains("khuyen mai") || raw.contains("giam gia") || raw.contains("voucher") ||
               raw.contains("uu dai") || raw.contains("sale");
    }

    private boolean isProductSearchIntent(String raw) {
        return raw.contains("giay") || raw.contains("tim") || raw.contains("mua") || raw.contains("sneaker") ||
               raw.contains("nike") || raw.contains("adidas") || raw.contains("puma") || raw.contains("converse") ||
               raw.contains("vans") || raw.contains("mau") || raw.contains("size") || raw.contains("trieu") || raw.contains("k");
    }

    private String extractOrderCode(String text) {
        Matcher m = Pattern.compile("(?i)(ORD-[A-Za-z0-9]+|DH\\d+|DH-[A-Za-z0-9]+|\\b\\d{4,6}\\b)").matcher(text);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private Double extractFootLength(String text) {
        Matcher m = Pattern.compile("(\\d{2}(?:[.,]\\d+)?)\\s*(?:cm|centimet)?").matcher(text);
        while (m.find()) {
            try {
                double val = Double.parseDouble(m.group(1).replace(',', '.'));
                if (val >= 18.0 && val <= 35.0) {
                    return val;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private ChatbotMessageResponse handleProductSearch(String raw, String originalMessage) {
        String brand = extractBrand(raw);
        String color = extractColor(raw);
        String size = extractSize(raw);
        String material = extractMaterial(raw);
        String sole = extractSole(raw);
        BigDecimal maxPrice = extractMaxPrice(raw);
        BigDecimal minPrice = extractMinPrice(raw);

        // Từ khóa tìm kiếm: loại bỏ các từ thông thường
        String keyword = null;
        if (brand != null) {
            keyword = brand;
        }

        List<ChatbotProductCardDto> cards = toolService.searchProducts(
                keyword, brand, color, size, material, sole, minPrice, maxPrice
        );

        if (cards.isEmpty()) {
            // Thử nới lỏng tìm kiếm (chỉ theo thương hiệu hoặc từ khóa chung)
            cards = toolService.searchProducts(brand != null ? brand : "giày", null, null, null, null, null, null, null);
            if (cards.isEmpty()) {
                return ChatbotMessageResponse.builder()
                        .text("Hiện tại cửa hàng chưa có sản phẩm khớp với tiêu chí tìm kiếm của bạn. Bạn có thể tham khảo thêm các mẫu giày khác hoặc chọn mục Tư vấn size nhé!")
                        .quickReplies(DEFAULT_QUICK_REPLIES)
                        .build();
            } else {
                return ChatbotMessageResponse.builder()
                        .text("Chưa tìm thấy mẫu chính xác theo mọi tiêu chí, nhưng dưới đây là một số mẫu giày nổi bật bạn có thể tham khảo:")
                        .products(cards)
                        .quickReplies(List.of("Tư vấn size", "Khuyến mãi đang có", "Nói chuyện với nhân viên"))
                        .build();
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Tìm thấy ").append(cards.size()).append(" sản phẩm phù hợp với bạn:\n");
        return ChatbotMessageResponse.builder()
                .text(sb.toString())
                .products(cards)
                .quickReplies(List.of("Tư vấn size", "Chính sách đổi trả", "Nói chuyện với nhân viên"))
                .build();
    }

    private String extractBrand(String raw) {
        List<String> brands = List.of("nike", "adidas", "puma", "converse", "vans", "mlb", "new balance");
        for (String b : brands) {
            if (raw.contains(b)) return b;
        }
        return null;
    }

    private String extractColor(String raw) {
        Map<String, String> colors = Map.of(
                "den", "đen",
                "trang", "trắng",
                "do", "đỏ",
                "xanh", "xanh",
                "vang", "vàng",
                "xam", "xám",
                "nau", "nâu",
                "be", "be"
        );
        for (var entry : colors.entrySet()) {
            if (raw.contains(entry.getKey())) return entry.getValue();
        }
        return null;
    }

    private String extractSize(String raw) {
        Matcher m = Pattern.compile("(?:size|co|sz)\\s*(\\d{2})").matcher(raw);
        if (m.find()) return m.group(1);
        Matcher m2 = Pattern.compile("\\b(3[5-9]|4[0-6])\\b").matcher(raw);
        if (m2.find()) return m2.group(1);
        return null;
    }

    private String extractMaterial(String raw) {
        if (raw.contains("da that")) return "da thật";
        if (raw.contains("da tong hop")) return "da tổng hợp";
        if (raw.contains("da")) return "da";
        if (raw.contains("vai") || raw.contains("canvas")) return "vải";
        return null;
    }

    private String extractSole(String raw) {
        if (raw.contains("cao su")) return "cao su";
        if (raw.contains("dem khi")) return "đệm khí";
        return null;
    }

    private BigDecimal extractMaxPrice(String raw) {
        Matcher m = Pattern.compile("(?:duoi|<|nho hon|toi da)\\s*(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|k|nghin|d|vnd)?").matcher(raw);
        if (m.find()) {
            try {
                double val = Double.parseDouble(m.group(1).replace(',', '.'));
                String unit = m.group(2) != null ? m.group(2) : "";
                if (unit.startsWith("tr") || unit.startsWith("trieu")) {
                    return BigDecimal.valueOf((long) (val * 1_000_000));
                } else if (unit.startsWith("k") || unit.startsWith("nghin")) {
                    return BigDecimal.valueOf((long) (val * 1_000));
                } else if (val < 100) { // e.g. "dưới 3"
                    return BigDecimal.valueOf((long) (val * 1_000_000));
                } else {
                    return BigDecimal.valueOf((long) val);
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private BigDecimal extractMinPrice(String raw) {
        Matcher m = Pattern.compile("(?:tren|>|lon hon|tu)\\s*(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|k|nghin|d|vnd)?").matcher(raw);
        if (m.find()) {
            try {
                double val = Double.parseDouble(m.group(1).replace(',', '.'));
                String unit = m.group(2) != null ? m.group(2) : "";
                if (unit.startsWith("tr") || unit.startsWith("trieu")) {
                    return BigDecimal.valueOf((long) (val * 1_000_000));
                } else if (unit.startsWith("k") || unit.startsWith("nghin")) {
                    return BigDecimal.valueOf((long) (val * 1_000));
                } else if (val < 100) {
                    return BigDecimal.valueOf((long) (val * 1_000_000));
                } else {
                    return BigDecimal.valueOf((long) val);
                }
            } catch (Exception ignored) {}
        }
        return null;
    }
}
