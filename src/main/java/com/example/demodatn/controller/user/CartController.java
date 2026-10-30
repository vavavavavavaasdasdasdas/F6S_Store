package com.example.demodatn.controller.user;
import com.example.demodatn.service.sanpham.ProductSalePolicy;
import com.example.demodatn.dto.donhang.CartItemDto;
import com.example.demodatn.dto.donhang.PriceInfo;
import com.example.demodatn.entity.sanpham.ProductVariant;
import com.example.demodatn.service.donhang.PricingService;
import com.example.demodatn.service.sanpham.ProductService;
import jakarta.servlet.http.HttpSession;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final ProductService productService;
    private final PricingService pricingService;

    @Value("${shop.shipping.fee:30000}")
    private BigDecimal shippingFee;

    @Value("${shop.shipping.free-threshold:500000}")
    private BigDecimal freeShippingThreshold;

    @GetMapping
    public String viewCart(HttpSession session, Model model) {
        List<CartItemDto> sessionCart = getCart(session);
        List<CartViewItem> viewCart = new ArrayList<>();
        BigDecimal subTotal = BigDecimal.ZERO;
        boolean hasWarning = false;

        for (CartItemDto dto : sessionCart) {
            CartViewItem item = new CartViewItem();
            item.setVariantId(dto.getVariantId());
            item.setProductId(dto.getProductId());
            item.setProductName(dto.getProductName());
            item.setSize(dto.getSize());
            item.setQuantity(dto.getQuantity());

            ProductVariant variant = productService.getVariantById(dto.getVariantId());
            item.setProductCode(variant == null || variant.getProduct() == null ? null : variant.getProduct().getCode());
            item.setMaterialName(variant == null ? dto.getMaterialName() : variant.getMaterial().getName());
            item.setColorName(variant==null?dto.getColorName():variant.getColor().getName());
            item.setSoleName(variant==null?dto.getSoleName():variant.getSole().getName());
            if (!ProductSalePolicy.isSelling(variant)) {
                item.setWarningMessage("Sản phẩm đã ngừng bán");
                item.setPrice(dto.getPrice());
                hasWarning = true;
            } else {
                item.setImageUrl(variant.getProduct().getImageUrl());
                int stock = variant.getQuantity() != null ? variant.getQuantity() : 0;
                item.setMaxQuantity(stock);

                PriceInfo priceInfo = pricingService.calculatePrice(variant);
                item.setPrice(priceInfo.getFinalPrice());
                item.setOriginalPrice(priceInfo.getOriginalPrice());
                
                if (priceInfo.getOriginalPrice().compareTo(priceInfo.getFinalPrice()) > 0) {
                    item.setHasPromotion(true);
                }

                if (stock == 0) {
                    item.setWarningMessage("Sản phẩm đã hết hàng.");
                    hasWarning = true;
                } else if (item.getQuantity() > stock) {
                    item.setWarningMessage("Chỉ còn " + stock + " sản phẩm.");
                    hasWarning = true;
                } else if (priceInfo.getFinalPrice().compareTo(dto.getPrice()) != 0) {
                    // Update session price
                    dto.setPrice(priceInfo.getFinalPrice());
                    session.setAttribute("cart", sessionCart);
                }

                if (item.getWarningMessage() == null) {
                    subTotal = subTotal.add(item.getPrice().multiply(new BigDecimal(item.getQuantity())));
                }
            }
            viewCart.add(item);
        }

        BigDecimal fee = subTotal.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : 
                        (subTotal.compareTo(freeShippingThreshold) >= 0 ? BigDecimal.ZERO : shippingFee);
        BigDecimal total = subTotal.add(fee);

        model.addAttribute("cartItems", viewCart);
        model.addAttribute("subTotal", subTotal);
        model.addAttribute("shippingFee", fee);
        model.addAttribute("total", total);
        model.addAttribute("freeShippingThreshold", freeShippingThreshold);
        model.addAttribute("hasWarning", hasWarning);
        
        return "user/gio-hang";
    }

    @PostMapping("/add-ajax")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addToCartAjax(
            @RequestParam(required = false) Integer variantId,
            @RequestParam(required = false) Integer quantity,
            HttpSession session) {
        String error = addItem(session, variantId, quantity);
        if (error != null) {
            return ResponseEntity.badRequest().body(Map.of("error", error, "totalQuantity", totalQuantity(getCart(session))));
        }
        return ResponseEntity.ok(Map.of("totalQuantity", totalQuantity(getCart(session)), "message", "Đã thêm vào giỏ"));
    }

    @PostMapping("/update-ajax")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateCartAjax(
            @RequestParam Integer variantId,
            @RequestParam Integer quantity,
            HttpSession session) {
        if (quantity == null || quantity <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Số lượng không hợp lệ"));
        }
        ProductVariant variant = productService.getVariantById(variantId);
        if (!ProductSalePolicy.isSelling(variant)) {
            return ResponseEntity.badRequest().body(Map.of("error", ProductSalePolicy.STOPPED));
        }
        int stock = variant.getQuantity() != null ? variant.getQuantity() : 0;
        if (quantity > stock) {
            return ResponseEntity.badRequest().body(Map.of("error", "Chỉ còn " + stock + " sản phẩm kích cỡ " + variant.getSize().getSizeValue()));
        }

        List<CartItemDto> cart = getCart(session);
        for (CartItemDto item : cart) {
            if (item.getVariantId().equals(variantId)) {
                item.setQuantity(quantity);
                break;
            }
        }
        session.setAttribute("cart", cart);
        return ResponseEntity.ok(Map.of("message", "Cập nhật thành công", "totalQuantity", totalQuantity(cart)));
    }

    @PostMapping("/remove-ajax")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeCartAjax(
            @RequestParam Integer variantId,
            HttpSession session) {
        List<CartItemDto> cart = getCart(session);
        cart.removeIf(item -> item.getVariantId().equals(variantId));
        session.setAttribute("cart", cart);
        return ResponseEntity.ok(Map.of("message", "Đã xóa sản phẩm khỏi giỏ", "totalQuantity", totalQuantity(cart)));
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam(required = false) Integer variantId,
                            @RequestParam(required = false) Integer quantity,
                            @RequestParam(required = false) Boolean buyNow,
                            @RequestHeader(value = "Referer", required = false) String referer,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        String error = addItem(session, variantId, quantity);
        ProductVariant variant = variantId == null ? null : productService.getVariantById(variantId);
        String back = variant != null && variant.getProduct() != null
                ? "/products/" + variant.getProduct().getId()
                : (referer != null ? referer : "/products");
        if (error != null) {
            redirectAttributes.addFlashAttribute("error", error);
            return "redirect:" + back;
        }
        if (Boolean.TRUE.equals(buyNow)) {
            return "redirect:/checkout";
        }
        redirectAttributes.addFlashAttribute("message", "Đã thêm vào giỏ hàng");
        return "redirect:" + back;
    }

    private String addItem(HttpSession session, Integer variantId, Integer quantity) {
        if (variantId == null || quantity == null || quantity <= 0) {
            return "Vui lòng chọn kích cỡ và số lượng hợp lệ!";
        }
        ProductVariant variant = productService.getVariantById(variantId);
        if (!ProductSalePolicy.isSelling(variant)) {
            return "Sản phẩm không tồn tại hoặc đã ngừng bán";
        }
        int stock = variant.getQuantity() == null ? 0 : variant.getQuantity();
        if (stock <= 0) {
            return "Sản phẩm đã hết hàng";
        }

        List<CartItemDto> cart = getCart(session);
        CartItemDto existing = cart.stream()
                .filter(i -> i.getVariantId().equals(variantId))
                .findFirst().orElse(null);
        int inCart = existing == null ? 0 : existing.getQuantity();
        if (inCart + quantity > stock) {
            return "Vượt quá tồn kho (còn " + stock + ", trong giỏ đã có " + inCart + ")";
        }

        BigDecimal finalPrice = pricingService.calculatePrice(variant).getFinalPrice();
        if (existing != null) {
            existing.setQuantity(inCart + quantity);
            existing.setPrice(finalPrice);
        } else {
            cart.add(new CartItemDto(
                    variant.getProduct().getId(),
                    variant.getId(),
                    variant.getProduct().getName(),
                    variant.getSize() != null ? variant.getSize().getSizeValue() : "",
                    variant.getMaterial().getName(),variant.getColor().getName(),variant.getSole().getName(),
                    finalPrice,
                    quantity));
        }
        session.setAttribute("cart", cart);
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<CartItemDto> getCart(HttpSession session) {
        Object c = session.getAttribute("cart");
        return c instanceof List ? (List<CartItemDto>) c : new ArrayList<>();
    }

    private int totalQuantity(List<CartItemDto> cart) {
        return cart.stream().mapToInt(i -> i.getQuantity() == null ? 0 : i.getQuantity()).sum();
    }

    @Data
    public static class CartViewItem {
        private Integer variantId;
        private Integer productId;
        private String productName;
        private String productCode;
        private String imageUrl;
        private String size;
        private String materialName;
        private String colorName;
        private String soleName;
        private BigDecimal price;
        private BigDecimal originalPrice;
        private boolean hasPromotion;
        private Integer quantity;
        private Integer maxQuantity;
        private String warningMessage;

        public BigDecimal getTotal() {
            if (price != null && quantity != null) {
                return price.multiply(new BigDecimal(quantity));
            }
            return BigDecimal.ZERO;
        }
    }
}
