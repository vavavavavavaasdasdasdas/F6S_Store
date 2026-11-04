package com.example.demodatn.service.hotro;

import com.example.demodatn.dto.hotro.ChatbotProductCardDto;
import com.example.demodatn.dto.sanpham.ProductDto;
import com.example.demodatn.entity.donhang.Order;
import com.example.demodatn.entity.hotro.ChatbotFaq;
import com.example.demodatn.entity.khachhang.Customer;
import com.example.demodatn.entity.khuyenmai.Promotion;
import com.example.demodatn.entity.sanpham.*;
import com.example.demodatn.repository.donhang.OrderRepository;
import com.example.demodatn.repository.hotro.ChatbotFaqRepository;
import com.example.demodatn.repository.khachhang.CustomerRepository;
import com.example.demodatn.repository.khuyenmai.PromotionRepository;
import com.example.demodatn.repository.sanpham.*;
import com.example.demodatn.service.donhang.OrderStatusService;
import com.example.demodatn.service.donhang.PricingService;
import com.example.demodatn.service.sanpham.ProductFilterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotToolService {

    private final ProductFilterService productFilterService;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final PricingService pricingService;
    private final CategoryRepository categoryRepository;
    private final SizeRepository sizeRepository;
    private final ColorRepository colorRepository;
    private final MaterialRepository materialRepository;
    private final SoleRepository soleRepository;
    private final PromotionRepository promotionRepository;
    private final ChatbotFaqRepository faqRepository;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;

    private static final DecimalFormat CURRENCY_FMT = new DecimalFormat("#,###");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static String removeAccents(String text) {
        if (text == null) return "";
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    // 1. Tìm kiếm sản phẩm
    @Transactional(readOnly = true)
    public List<ChatbotProductCardDto> searchProducts(String keyword, String brand, String color, String size,
                                                      String material, String sole, BigDecimal minPrice, BigDecimal maxPrice) {
        Integer categoryId = resolveCategoryId(brand);
        Integer sizeId = resolveSizeId(size);
        Integer colorId = resolveColorId(color);
        Integer materialId = resolveMaterialId(material);
        Integer soleId = resolveSoleId(sole);

        Page<ProductDto> page = productFilterService.filterProducts(
                keyword, categoryId, sizeId, minPrice, maxPrice, null, "newest", 0, 5, materialId, soleId, colorId
        );

        return page.getContent().stream().map(dto -> {
            ChatbotProductCardDto card = new ChatbotProductCardDto();
            card.setId(dto.getId());
            card.setCode(dto.getCode());
            card.setName(dto.getName());
            card.setImageUrl(dto.getImageUrl() != null ? dto.getImageUrl() : "/images/default-shoe.png");
            card.setOriginalPrice(dto.getMinOriginalPrice());
            card.setFinalPrice(dto.getMinFinalPrice() != null ? dto.getMinFinalPrice() : dto.getMinOriginalPrice());
            card.setDiscountPercent(dto.getDiscountPercentage());
            card.setStock(dto.getTotalStock());
            card.setProductUrl("/products/" + dto.getId());
            return card;
        }).collect(Collectors.toList());
    }

    // 2. Chi tiết sản phẩm và các biến thể còn hàng
    @Transactional(readOnly = true)
    public String getProductDetails(Integer productId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) return "Không tìm thấy thông tin sản phẩm này.";

        List<ProductVariant> variants = variantRepository.findByProduct_IdAndStatusTrue(productId);
        var prices = pricingService.calculatePrices(variants);

        StringBuilder sb = new StringBuilder();
        sb.append("Sản phẩm: ").append(product.getName()).append(" (Mã: ").append(product.getCode()).append(")\n");
        if (product.getCategory() != null) {
            sb.append("Thương hiệu: ").append(product.getCategory().getName()).append("\n");
        }
        sb.append("Các mẫu còn hàng:\n");

        int inStockCount = 0;
        for (ProductVariant v : variants) {
            if (v.getQuantity() != null && v.getQuantity() > 0) {
                inStockCount++;
                var pInfo = prices.get(v.getId());
                BigDecimal price = pInfo != null ? pInfo.getFinalPrice() : v.getPrice();
                sb.append("- Size: ").append(v.getSize() != null ? v.getSize().getSizeValue() : "—")
                  .append(", Màu: ").append(v.getColor() != null ? v.getColor().getName() : "—")
                  .append(", Chất liệu: ").append(v.getMaterial() != null ? v.getMaterial().getName() : "—")
                  .append(", Giá: ").append(CURRENCY_FMT.format(price)).append(" đ")
                  .append(" (Còn ").append(v.getQuantity()).append(" đôi)\n");
            }
        }
        if (inStockCount == 0) {
            sb.append("Hiện tại các kích cỡ của sản phẩm này đã tạm hết hàng.\n");
        }
        return sb.toString();
    }

    // 3. Khuyến mãi đang chạy
    @Transactional(readOnly = true)
    public String getActivePromotions() {
        LocalDateTime now = LocalDateTime.now();
        List<Promotion> all = promotionRepository.findAll();
        List<Promotion> active = all.stream()
                .filter(p -> Boolean.TRUE.equals(p.getStatus()) && !now.isBefore(p.getStartDate()) && !now.isAfter(p.getEndDate()))
                .collect(Collectors.toList());

        if (active.isEmpty()) {
            return "Hiện tại cửa hàng chưa có chương trình khuyến mãi nào đang áp dụng. Bạn hãy theo dõi để cập nhật ưu đãi sớm nhất nhé!";
        }

        StringBuilder sb = new StringBuilder("Các chương trình khuyến mãi đang diễn ra tại F6S Store:\n");
        for (Promotion p : active) {
            sb.append("🔥 **").append(p.getName()).append("** (Mã: ").append(p.getCode()).append("): ");
            if ("PERCENTAGE".equalsIgnoreCase(p.getDiscountType())) {
                sb.append("Giảm ").append(p.getDiscountValue().stripTrailingZeros().toPlainString()).append("%");
            } else {
                sb.append("Giảm ").append(CURRENCY_FMT.format(p.getDiscountValue())).append(" đ");
            }
            sb.append(" (Áp dụng đến hết ngày ").append(p.getEndDate().format(DATE_FMT)).append(")\n");
        }
        return sb.toString();
    }

    // 4. Tư vấn kích cỡ giày theo chiều dài bàn chân cm
    public String getSizeAdvice(Double footLengthCm) {
        if (footLengthCm == null || footLengthCm <= 0) {
            return "Bạn vui lòng cho mình biết chiều dài bàn chân (tính theo cm) để mình gợi ý cỡ giày chuẩn nhất nhé!";
        }

        String suggestedSize;
        if (footLengthCm < 22.0) {
            suggestedSize = "35";
        } else if (footLengthCm <= 22.5) {
            suggestedSize = "35 - 36";
        } else if (footLengthCm <= 23.0) {
            suggestedSize = "36 - 37";
        } else if (footLengthCm <= 23.5) {
            suggestedSize = "37";
        } else if (footLengthCm <= 24.0) {
            suggestedSize = "38";
        } else if (footLengthCm <= 24.5) {
            suggestedSize = "39";
        } else if (footLengthCm <= 25.0) {
            suggestedSize = "40";
        } else if (footLengthCm <= 25.5) {
            suggestedSize = "41";
        } else if (footLengthCm <= 26.0) {
            suggestedSize = "42";
        } else if (footLengthCm <= 26.5) {
            suggestedSize = "43";
        } else if (footLengthCm <= 27.0) {
            suggestedSize = "44";
        } else {
            suggestedSize = "45";
        }

        return String.format(
                "Với chiều dài bàn chân khoảng **%.1f cm**, kích cỡ giày phù hợp nhất với bạn là **Size %s**.\n\n" +
                "💡 *Lưu ý: Bảng kích cỡ chỉ mang tính tham khảo. Nếu mu bàn chân dày hoặc bề ngang bè, bạn nên tăng thêm 1/2 đến 1 size để đi êm chân và thoải mái nhất nhé!*",
                footLengthCm, suggestedSize
        );
    }

    // 5. Chính sách và hướng dẫn (từ chatbot_faq)
    @Transactional(readOnly = true)
    public String getPolicyOrFaq(String query) {
        List<ChatbotFaq> list = faqRepository.findByHienThiTrueAndDoiTuong("KHACH");
        if (list.isEmpty()) {
            return "Bạn có thể tham khảo chi tiết tại trang Chính sách của cửa hàng.";
        }

        String rawQuery = removeAccents(query);
        // Tìm câu hỏi khớp nhất dựa trên từ khóa hoặc câu hỏi
        ChatbotFaq bestMatch = null;
        int maxScore = 0;

        for (ChatbotFaq faq : list) {
            int score = 0;
            String rawQ = removeAccents(faq.getCauHoi());
            String rawKeywords = removeAccents(faq.getTuKhoa());

            for (String part : rawQuery.split("\\s+")) {
                if (part.length() >= 2) {
                    if (rawKeywords.contains(part)) score += 3;
                    if (rawQ.contains(part)) score += 2;
                }
            }

            if (score > maxScore) {
                maxScore = score;
                bestMatch = faq;
            }
        }

        if (bestMatch != null && maxScore >= 2) {
            return bestMatch.getTraLoi();
        }

        return null;
    }

    // 6. Tra cứu đơn hàng (CHỈ khi đã đăng nhập và CHỈ đơn của chính khách)
    @Transactional(readOnly = true)
    public String trackOrder(String orderCode, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return "Bạn vui lòng đăng nhập tài khoản để kiểm tra thông tin đơn hàng của mình nhé! (F6S Store bảo mật thông tin và không tra cứu đơn hàng qua số điện thoại).";
        }

        String username = authentication.getName();
        Customer customer = customerRepository.findByUser_Username(username).orElse(null);
        if (customer == null) {
            return "Không tìm thấy hồ sơ khách hàng liên kết với tài khoản này.";
        }

        if (orderCode != null && !orderCode.isBlank()) {
            String code = orderCode.trim();
            // Nếu người dùng nhập mã số như 1430, thử tìm DH001430 hoặc chính xác
            Optional<Order> orderOpt = orderRepository.findByOrderCode(code);
            if (orderOpt.isEmpty() && code.matches("\\d+")) {
                String padded = String.format("DH%06d", Integer.parseInt(code));
                orderOpt = orderRepository.findByOrderCode(padded);
            }

            if (orderOpt.isEmpty()) {
                return "Không tìm thấy đơn hàng mã #" + code + ". Bạn vui lòng kiểm tra lại mã đơn hàng trong mục Lịch sử đơn hàng.";
            }

            Order order = orderOpt.get();
            // KIỂM TRA QUYỀN SỞ HỮU: chỉ xem đơn của chính mình
            if (order.getCustomer() == null || !customer.getId().equals(order.getCustomer().getId())) {
                return "Không tìm thấy đơn hàng #" + code + " thuộc tài khoản của bạn.";
            }

            String statusLabel = OrderStatusService.LABELS.getOrDefault(order.getStatus(), order.getStatus());
            StringBuilder sb = new StringBuilder();
            sb.append("📦 **Thông tin đơn hàng #").append(order.getOrderCode()).append("**:\n");
            sb.append("- Ngày đặt: ").append(order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n");
            sb.append("- Trạng thái đơn: **").append(statusLabel).append("**\n");
            sb.append("- Thanh toán: ").append("PAID".equals(order.getPaymentStatus()) ? "Đã thanh toán" : "Chưa thanh toán");
            sb.append(" (").append(order.getPaymentMethod()).append(")\n");
            sb.append("- Tổng thanh toán: ").append(CURRENCY_FMT.format(order.getFinalAmount())).append(" đ\n");
            if (order.getReceiverName() != null && !order.getReceiverName().isBlank()) {
                sb.append("- Người nhận: ").append(order.getReceiverName()).append("\n");
            }
            if (order.getShippingAddress() != null && !order.getShippingAddress().isBlank()) {
                sb.append("- Địa chỉ giao: ").append(order.getShippingAddress()).append("\n");
            }
            return sb.toString();
        }

        // Nếu không nhập mã đơn cụ thể, hiển thị danh sách đơn gần đây của khách
        List<Order> recentOrders = orderRepository.findByCustomer_IdOrderByCreatedAtDescIdDesc(
                customer.getId(), org.springframework.data.domain.PageRequest.of(0, 3)).getContent();
        if (recentOrders.isEmpty()) {
            return "Tài khoản của bạn hiện chưa có đơn hàng nào.";
        }

        StringBuilder sb = new StringBuilder("Các đơn hàng gần nhất của bạn:\n");
        int count = 0;
        for (Order o : recentOrders) {
            if (count++ >= 3) break;
            String statusLabel = OrderStatusService.LABELS.getOrDefault(o.getStatus(), o.getStatus());
            sb.append("- Đơn **#").append(o.getOrderCode()).append("**: ").append(statusLabel)
              .append(" (").append(CURRENCY_FMT.format(o.getFinalAmount())).append(" đ)\n");
        }
        sb.append("Bạn có thể nhắn cho mình mã đơn cụ thể (VD: ").append(recentOrders.get(0).getOrderCode()).append(") để xem chi tiết nhé!");
        return sb.toString();
    }

    // Helper resolutions
    private Integer resolveCategoryId(String brand) {
        if (brand == null || brand.isBlank()) return null;
        String raw = removeAccents(brand);
        List<Category> all = categoryRepository.findAll();
        for (Category c : all) {
            if (removeAccents(c.getName()).contains(raw) || raw.contains(removeAccents(c.getName()))) {
                return c.getId();
            }
        }
        return null;
    }

    private Integer resolveSizeId(String size) {
        if (size == null || size.isBlank()) return null;
        String val = size.trim();
        List<Size> all = sizeRepository.findAll();
        for (Size s : all) {
            if (s.getSizeValue() != null && s.getSizeValue().equalsIgnoreCase(val)) {
                return s.getId();
            }
        }
        return null;
    }

    private Integer resolveColorId(String color) {
        if (color == null || color.isBlank()) return null;
        String raw = removeAccents(color);
        List<Color> all = colorRepository.findAll();
        for (Color c : all) {
            if (removeAccents(c.getName()).contains(raw) || raw.contains(removeAccents(c.getName()))) {
                return c.getId();
            }
        }
        return null;
    }

    private Integer resolveMaterialId(String material) {
        if (material == null || material.isBlank()) return null;
        String raw = removeAccents(material);
        List<Material> all = materialRepository.findAll();
        for (Material m : all) {
            if (removeAccents(m.getName()).contains(raw) || raw.contains(removeAccents(m.getName()))) {
                return m.getId();
            }
        }
        return null;
    }

    private Integer resolveSoleId(String sole) {
        if (sole == null || sole.isBlank()) return null;
        String raw = removeAccents(sole);
        List<Sole> all = soleRepository.findAll();
        for (Sole s : all) {
            if (removeAccents(s.getName()).contains(raw) || raw.contains(removeAccents(s.getName()))) {
                return s.getId();
            }
        }
        return null;
    }
}
