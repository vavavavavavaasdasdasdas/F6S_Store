package com.example.demodatn.controller.admin.donhang;

import com.example.demodatn.service.sanpham.ProductSalePolicy;
import com.example.demodatn.dto.donhang.CartItemDto;
import com.example.demodatn.dto.donhang.PosCheckoutRequest;
import com.example.demodatn.entity.sanpham.ProductVariant;
import com.example.demodatn.repository.donhang.InvoiceRepository;
import com.example.demodatn.repository.khachhang.CustomerRepository;
import com.example.demodatn.repository.nhanvien.EmployeeRepository;
import com.example.demodatn.repository.sanpham.*;
import com.example.demodatn.service.donhang.OrderService;
import com.example.demodatn.service.donhang.PosCatalogQuery;
import com.example.demodatn.service.donhang.PosHoldService;
import com.example.demodatn.service.donhang.PricingService;
import com.example.demodatn.service.khuyenmai.VoucherService;
import com.example.demodatn.service.donhang.VietQrService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.ResponseEntity;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.*;

@Controller
@RequiredArgsConstructor
public class AdminPosController {
    private final ProductVariantRepository variants;
    private final ProductRepository products;
    private final PricingService pricing;
    private final VietQrService vietQr;
    private final VoucherService vouchers;
    private final CustomerRepository customers;
    private final EmployeeRepository employees;
    private final OrderService orders;
    private final InvoiceRepository invoices;
    private final PosHoldService holds;
    private final PosCatalogQuery catalogQuery;
    private final CategoryRepository categories;
    private final ColorRepository colors;
    private final MaterialRepository materials;
    private final SoleRepository soles;
    private final SizeRepository sizes;
    private final com.example.demodatn.service.nhanvien.AccountProvisioningService accounts;
    private final com.example.demodatn.service.PeopleService people;
    private final com.example.demodatn.repository.donhang.ProvinceRepository provinces;
    private final com.example.demodatn.repository.khachhang.ShippingAddressRepository shippingAddresses;

    @GetMapping("/admin/ban-online")
    public String banOnlineRedirect() {
        return "redirect:/admin/pos";
    }

    private void clearCart(HttpSession session) {
        for (String key : List.of("posCart", "posCheckoutToken", "posLoadedHold", "posHeldRequest", "posHoldReview", "posPaymentReference")) {
            session.removeAttribute(key);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, BigDecimal> prices(HttpSession session) {
        var snapshots = (Map<Long, Map<Integer, BigDecimal>>) session.getAttribute("posHoldPrices");
        if (snapshots == null) {
            snapshots = new HashMap<>();
            session.setAttribute("posHoldPrices", snapshots);
        }
        return snapshots.computeIfAbsent((Long) session.getAttribute("posLoadedHold"), id -> new HashMap<>());
    }

    @SuppressWarnings("unchecked")
    private List<CartItemDto> cart(HttpSession session) {
        var cart = (List<CartItemDto>) session.getAttribute("posCart");
        if (cart == null) {
            cart = new ArrayList<>();
            session.setAttribute("posCart", cart);
        }
        return cart;
    }

    @GetMapping("/admin/pos")
    public String page(HttpSession session, Principal principal, Model model) {
        synchronized (session) {
            var cart = cart(session);
            model.addAttribute("categories", categories.findAll(org.springframework.data.domain.Sort.by("name", "id")));
            model.addAttribute("colors", colors.findAll(org.springframework.data.domain.Sort.by("name", "id")));
            model.addAttribute("materials", materials.findAll(org.springframework.data.domain.Sort.by("name", "id")));
            model.addAttribute("soles", soles.findAll(org.springframework.data.domain.Sort.by("name", "id")));
            model.addAttribute("sizes", sizes.findAll(org.springframework.data.domain.Sort.by("id")));
            model.addAttribute("posCart", cart);

            boolean valid = true;
            var lineWarnings = new HashMap<Integer, String>();
            Long loadedId = (Long) session.getAttribute("posLoadedHold");
            if (loadedId != null) {
                try {
                    holds.open(principal.getName(), loadedId, "POS");
                } catch (org.springframework.web.server.ResponseStatusException ex) {
                    clearCart(session);
                    cart = cart(session);
                    model.addAttribute("posCart", cart);
                    model.addAttribute("error", "Hóa đơn chờ đã hết hạn hoặc không còn tồn tại.");
                    loadedId = null;
                }
            }
            if (loadedId != null) {
                var baseline = prices(session);
                for (var item : cart) {
                    var v = variants.findById(item.getVariantId()).orElse(null);
                    if (!ProductSalePolicy.isSelling(v)) {
                        lineWarnings.put(item.getVariantId(), "Sản phẩm đã ngừng bán. Xóa dòng này để tiếp tục.");
                    } else {
                        var current = pricing.calculatePrice(v).getFinalPrice();
                        var previous = baseline.get(item.getVariantId());
                        if (previous != null && previous.compareTo(current) != 0) {
                            session.setAttribute("posHoldReview", true);
                            lineWarnings.put(item.getVariantId(), "Giá đã thay đổi. Xác nhận giá mới trước khi thanh toán.");
                        }
                        item.setPrice(current);
                        if (v.getQuantity() < item.getQuantity()) {
                            lineWarnings.put(item.getVariantId(), v.getQuantity() == 0 ? "Hết hàng. Xóa dòng hoặc chờ nhập hàng." : "Không đủ tồn kho. Giảm số lượng và lưu lại.");
                        }
                    }
                }
            }
            try {
                pricing.refreshCart(cart);
            } catch (IllegalArgumentException ex) {
                valid = false;
                model.addAttribute("error", ex.getMessage());
            }
            if (Boolean.TRUE.equals(session.getAttribute("posHoldReview"))) {
                valid = false;
            }
            model.addAttribute("lineWarnings", lineWarnings);
            model.addAttribute("holdNeedsReview", Boolean.TRUE.equals(session.getAttribute("posHoldReview")));
            model.addAttribute("loadedHoldId", loadedId);
            model.addAttribute("heldOrders", holds.list(principal.getName(), "POS"));
            model.addAttribute("cartValid", valid);
            BigDecimal subtotalAmount = cart.stream().map(CartItemDto::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal originalTotalAmount = cart.stream().map(CartItemDto::getOriginalTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal promoDiscountAmount = originalTotalAmount.subtract(subtotalAmount);
            if (promoDiscountAmount.compareTo(BigDecimal.ZERO) < 0) promoDiscountAmount = BigDecimal.ZERO;
            model.addAttribute("total", subtotalAmount);
            model.addAttribute("originalTotal", originalTotalAmount);
            model.addAttribute("promoDiscount", promoDiscountAmount);

            Map<Integer, Integer> physicalStockMap = new HashMap<>();
            for (var item : cart) {
                variants.findById(item.getVariantId()).ifPresent(v -> physicalStockMap.put(v.getId(), v.getQuantity()));
            }
            model.addAttribute("physicalStockMap", physicalStockMap);

            if (session.getAttribute("posCheckoutToken") == null) {
                session.setAttribute("posCheckoutToken", UUID.randomUUID().toString());
            }
            if (!model.containsAttribute("posCheckoutRequest")) {
                var request = (PosCheckoutRequest) session.getAttribute("posHeldRequest");
                if (request == null) {
                    request = new PosCheckoutRequest();
                    request.setPaymentMethod("CASH");
                }
                model.addAttribute("posCheckoutRequest", request);
            }
            model.addAttribute("checkoutToken", session.getAttribute("posCheckoutToken"));
            if (session.getAttribute("posPaymentReference") == null) {
                session.setAttribute("posPaymentReference", VietQrService.newReference());
            }
            model.addAttribute("paymentReference", session.getAttribute("posPaymentReference"));
            model.addAttribute("provinces", provinces.findAllByOrderByNameAsc());
        }
        return "admin/ban-tai-quay/ban-tai-quay";
    }

    public record ModalVariantDto(
            Integer id,
            String productName,
            String productCode,
            String variantCode,
            String imageUrl,
            int physicalQuantity,
            int availableQuantity,
            String sizeValue,
            String colorName,
            String colorHex,
            String materialName,
            String soleName,
            boolean hasPromotion,
            int discountPercentage,
            String originalPriceFormatted,
            String finalPriceFormatted,
            String promotionName,
            String discountLabel
    ) {
        public Integer getId() { return id; }
        public String getProductName() { return productName; }
        public String getProductCode() { return productCode; }
        public String getVariantCode() { return variantCode; }
        public String getImageUrl() { return imageUrl; }
        public int getPhysicalQuantity() { return physicalQuantity; }
        public int getAvailableQuantity() { return availableQuantity; }
        public String getSizeValue() { return sizeValue; }
        public String getColorName() { return colorName; }
        public String getColorHex() { return colorHex; }
        public String getMaterialName() { return materialName; }
        public String getSoleName() { return soleName; }
        public boolean isHasPromotion() { return hasPromotion; }
        public int getDiscountPercentage() { return discountPercentage; }
        public String getOriginalPriceFormatted() { return originalPriceFormatted; }
        public String getFinalPriceFormatted() { return finalPriceFormatted; }
        public String getPromotionName() { return promotionName; }
        public String getDiscountLabel() { return discountLabel; }
    }

    @GetMapping("/admin/pos/products-modal")
    public String productsModal(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer colorId,
            @RequestParam(required = false) Integer materialId,
            @RequestParam(required = false) Integer soleId,
            @RequestParam(required = false) Integer sizeId,
            @RequestParam(required = false) String priceRange,
            @RequestParam(defaultValue = "0") int page,
            HttpSession session,
            Model model) {
        var pageResult = catalogQuery.variants(keyword, categoryId, colorId, materialId, soleId, sizeId, priceRange, page);
        var nf = java.text.NumberFormat.getInstance(new Locale("vi", "VN"));
        var cart = cart(session);
        Map<Integer, Integer> inCartMap = new HashMap<>();
        for (var item : cart) {
            inCartMap.merge(item.getVariantId(), item.getQuantity(), Integer::sum);
        }

        var dtoList = new ArrayList<ModalVariantDto>();
        for (var v : pageResult.getContent()) {
            var pInfo = pricing.calculatePrice(v);
            int inCart = inCartMap.getOrDefault(v.getId(), 0);
            int available = Math.max(0, v.getQuantity() - inCart);
            dtoList.add(new ModalVariantDto(
                    v.getId(),
                    v.getProduct().getName(),
                    v.getProduct().getCode(),
                    v.getMa() != null ? v.getMa() : "—",
                    v.getProduct().getDisplayImageUrl(),
                    v.getQuantity(),
                    available,
                    v.getSize().getSizeValue(),
                    v.getColor().getName(),
                    v.getColor().getHexCode() != null ? v.getColor().getHexCode() : "#ccc",
                    v.getMaterial() != null ? v.getMaterial().getName() : "—",
                    v.getSole() != null ? v.getSole().getName() : "—",
                    pInfo.isHasPromotion(),
                    pInfo.getDiscountPercentage(),
                    nf.format(pInfo.getOriginalPrice()) + " đ",
                    nf.format(pInfo.getFinalPrice()) + " đ",
                    pInfo.isHasPromotion() && pInfo.getPromotionName() != null ? pInfo.getPromotionName() : "—",
                    pInfo.getDiscountLabel()
            ));
        }
        model.addAttribute("variants", dtoList);
        model.addAttribute("currentPage", pageResult.getNumber());
        model.addAttribute("totalPages", pageResult.getTotalPages());
        model.addAttribute("totalElements", pageResult.getTotalElements());
        model.addAttribute("isFirst", pageResult.isFirst());
        model.addAttribute("isLast", pageResult.isLast());
        model.addAttribute("isEmpty", pageResult.isEmpty());
        return "admin/ban-tai-quay/modal-san-pham :: modalTableAndPagination";
    }

    @PostMapping("/admin/pos/scan")
    @ResponseBody
    public ResponseEntity<?> scanBarcode(@RequestParam String barcode, HttpSession session) {
        if (barcode == null || barcode.trim().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Mã quét không được để trống."));
        }
        String code = barcode.trim();
        synchronized (session) {
            ProductVariant variant = variants.findByCodeIgnoreCase(code).orElse(null);
            if (variant == null) {
                var prodOpt = products.findByCodeIgnoreCase(code);
                if (prodOpt.isPresent()) {
                    var activeVariants = variants.findByProduct_IdAndStatusTrue(prodOpt.get().getId());
                    if (activeVariants.size() == 1) {
                        variant = activeVariants.get(0);
                    } else if (activeVariants.size() > 1) {
                        return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Sản phẩm có " + activeVariants.size() + " biến thể. Vui lòng bấm '+ Chọn sản phẩm' để chọn biến thể."));
                    } else {
                        return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Sản phẩm không có biến thể nào còn hoạt động."));
                    }
                }
            }
            if (variant == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Không tìm thấy sản phẩm hoặc biến thể phù hợp với mã: " + code));
            }
            if (!ProductSalePolicy.isSelling(variant)) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Sản phẩm đã ngừng bán: " + variant.getProduct().getName()));
            }
            if (variant.getQuantity() <= 0) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Biến thể đã hết hàng trong kho."));
            }

            final Integer targetVariantId = variant.getId();
            var cart = cart(session);
            var item = cart.stream().filter(i -> i.getVariantId().equals(targetVariantId)).findFirst().orElse(null);
            int currentQty = item == null ? 0 : item.getQuantity();
            if (currentQty + 1 > variant.getQuantity()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Số lượng vượt quá tồn kho hiện tại (" + variant.getQuantity() + ")."));
            }

            var pInfo = pricing.calculatePrice(variant);
            if (item == null) {
                var newItem = new CartItemDto(
                        variant.getProduct().getId(),
                        variant.getId(),
                        variant.getProduct().getName(),
                        variant.getSize().getSizeValue(),
                        variant.getMaterial().getName(),
                        variant.getColor().getName(),
                        variant.getSole().getName(),
                        pInfo.getFinalPrice(),
                        1
                );
                newItem.setProductCode(variant.getProduct().getCode());
                newItem.setVariantCode(variant.getMa());
                newItem.setOriginalPrice(pInfo.getOriginalPrice());
                newItem.setHasPromotion(pInfo.isHasPromotion());
                newItem.setPromotionName(pInfo.getPromotionName());
                newItem.setPromotionDiscountLabel(pInfo.getDiscountLabel());
                cart.add(newItem);
            } else {
                item.setQuantity(currentQty + 1);
                item.setPrice(pInfo.getFinalPrice());
                item.setOriginalPrice(pInfo.getOriginalPrice());
                item.setHasPromotion(pInfo.isHasPromotion());
                item.setPromotionName(pInfo.getPromotionName());
                item.setPromotionDiscountLabel(pInfo.getDiscountLabel());
            }
            session.removeAttribute("posCheckoutToken");
            pricing.refreshCart(cart);
            BigDecimal total = cart.stream().map(CartItemDto::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal originalTotal = cart.stream().map(CartItemDto::getOriginalTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal promoDiscount = originalTotal.subtract(total);
            if (promoDiscount.compareTo(BigDecimal.ZERO) < 0) promoDiscount = BigDecimal.ZERO;
            int totalItems = cart.stream().mapToInt(CartItemDto::getQuantity).sum();
            int newAvailable = Math.max(0, variant.getQuantity() - (currentQty + 1));
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã thêm " + variant.getProduct().getName() + " (" + variant.getSize().getSizeValue() + " - " + variant.getColor().getName() + ") vào giỏ hàng.",
                    "variantId", variant.getId(),
                    "availableQuantity", newAvailable,
                    "inCartQuantity", currentQty + 1,
                    "total", total,
                    "originalTotal", originalTotal,
                    "promoDiscount", promoDiscount,
                    "totalItems", totalItems
            ));
        }
    }

    @PostMapping("/admin/pos/add")
    public Object add(@RequestParam Integer variantId, @RequestParam(defaultValue = "1") Integer quantity,
                      @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
                      HttpSession session, RedirectAttributes flash) {
        synchronized (session) {
            try {
                var v = variants.findById(variantId).orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại."));
                if (quantity <= 0 || !ProductSalePolicy.isSelling(v)) {
                    throw new IllegalArgumentException("Sản phẩm hoặc số lượng không hợp lệ.");
                }
                var cart = cart(session);
                var item = cart.stream().filter(i -> i.getVariantId().equals(variantId)).findFirst().orElse(null);
                long currentQty = item == null ? 0L : item.getQuantity();
                long totalQty = quantity + currentQty;
                if (totalQty > v.getQuantity()) {
                    throw new IllegalArgumentException("Số lượng vượt quá tồn kho hiện tại (" + v.getQuantity() + ").");
                }
                var pInfo = pricing.calculatePrice(v);
                if (item == null) {
                    var newItem = new CartItemDto(
                        v.getProduct().getId(),
                        v.getId(),
                        v.getProduct().getName(),
                        v.getSize().getSizeValue(),
                        v.getMaterial().getName(),
                        v.getColor().getName(),
                        v.getSole().getName(),
                        pInfo.getFinalPrice(),
                        quantity
                    );
                    newItem.setProductCode(v.getProduct().getCode());
                    newItem.setVariantCode(v.getMa());
                    newItem.setOriginalPrice(pInfo.getOriginalPrice());
                    newItem.setHasPromotion(pInfo.isHasPromotion());
                    newItem.setPromotionName(pInfo.getPromotionName());
                    newItem.setPromotionDiscountLabel(pInfo.getDiscountLabel());
                    cart.add(newItem);
                } else {
                    item.setQuantity((int) totalQty);
                    item.setPrice(pInfo.getFinalPrice());
                    item.setOriginalPrice(pInfo.getOriginalPrice());
                    item.setHasPromotion(pInfo.isHasPromotion());
                    item.setPromotionName(pInfo.getPromotionName());
                    item.setPromotionDiscountLabel(pInfo.getDiscountLabel());
                }
                session.removeAttribute("posCheckoutToken");

                if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
                    pricing.refreshCart(cart);
                    BigDecimal subtotal = cart.stream().map(CartItemDto::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal originalTotal = cart.stream().map(CartItemDto::getOriginalTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal promoDiscount = originalTotal.subtract(subtotal);
                    if (promoDiscount.compareTo(BigDecimal.ZERO) < 0) promoDiscount = BigDecimal.ZERO;
                    int totalItems = cart.stream().mapToInt(CartItemDto::getQuantity).sum();
                    int newAvailable = Math.max(0, v.getQuantity() - (int) totalQty);

                    List<Map<String, Object>> cartList = cart.stream().map(ci -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("variantId", ci.getVariantId());
                        m.put("productId", ci.getProductId());
                        m.put("productCode", ci.getProductCode());
                        m.put("variantCode", ci.getVariantCode());
                        m.put("productName", ci.getProductName());
                        m.put("colorName", ci.getColorName());
                        m.put("materialName", ci.getMaterialName());
                        m.put("soleName", ci.getSoleName());
                        m.put("size", ci.getSize());
                        m.put("quantity", ci.getQuantity());
                        m.put("price", ci.getPrice());
                        m.put("originalPrice", ci.getOriginalPrice());
                        m.put("totalPrice", ci.getTotalPrice());
                        m.put("hasPromotion", ci.isHasPromotion());
                        m.put("promotionName", ci.getPromotionName() != null ? ci.getPromotionName() : "");
                        int phys = variants.findById(ci.getVariantId()).map(ProductVariant::getQuantity).orElse(99);
                        m.put("maxQuantity", phys);
                        return m;
                    }).toList();

                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Đã thêm " + v.getProduct().getName() + " (" + v.getSize().getSizeValue() + " - " + v.getColor().getName() + ") vào giỏ hàng.",
                            "variantId", v.getId(),
                            "availableQuantity", newAvailable,
                            "inCartQuantity", (int) totalQty,
                            "total", subtotal,
                            "originalTotal", originalTotal,
                            "promoDiscount", promoDiscount,
                            "totalItems", totalItems,
                            "cartItems", cartList
                    ));
                }
            } catch (IllegalArgumentException ex) {
                if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
                    return ResponseEntity.badRequest().body(Map.of("success", false, "message", ex.getMessage()));
                }
                flash.addFlashAttribute("error", ex.getMessage());
            }
        }
        return "redirect:/admin/pos";
    }

    @PostMapping("/admin/pos/quantity")
    public Object quantity(@RequestParam Integer variantId, @RequestParam Integer quantity,
                           @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
                           HttpSession session, RedirectAttributes flash) {
        synchronized (session) {
            try {
                var v = variants.findById(variantId).orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại."));
                if (quantity <= 0 || quantity > v.getQuantity()) {
                    throw new IllegalArgumentException("Số lượng phải từ 1 đến tồn kho hiện tại (" + v.getQuantity() + ").");
                }
                var item = cart(session).stream().filter(i -> i.getVariantId().equals(variantId)).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không còn trong giỏ."));
                item.setQuantity(quantity);
                session.removeAttribute("posCheckoutToken");
                pricing.refreshCart(cart(session));
                if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
                    BigDecimal total = cart(session).stream().map(CartItemDto::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal originalTotal = cart(session).stream().map(CartItemDto::getOriginalTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal promoDiscount = originalTotal.subtract(total);
                    if (promoDiscount.compareTo(BigDecimal.ZERO) < 0) promoDiscount = BigDecimal.ZERO;
                    int totalItems = cart(session).stream().mapToInt(CartItemDto::getQuantity).sum();
                    int newAvailable = Math.max(0, v.getQuantity() - quantity);
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "variantId", variantId,
                            "availableQuantity", newAvailable,
                            "total", total,
                            "originalTotal", originalTotal,
                            "promoDiscount", promoDiscount,
                            "totalItems", totalItems,
                            "itemTotal", item.getTotalPrice()
                    ));
                }
            } catch (IllegalArgumentException ex) {
                if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
                    return ResponseEntity.badRequest().body(Map.of("success", false, "message", ex.getMessage()));
                }
                flash.addFlashAttribute("error", ex.getMessage());
            }
        }
        return "redirect:/admin/pos";
    }

    @PostMapping("/admin/pos/remove")
    public Object remove(@RequestParam Integer variantId,
                         @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
                         HttpSession session) {
        synchronized (session) {
            cart(session).removeIf(i -> i.getVariantId().equals(variantId));
            session.removeAttribute("posCheckoutToken");
            pricing.refreshCart(cart(session));
            if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
                BigDecimal total = cart(session).stream().map(CartItemDto::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal originalTotal = cart(session).stream().map(CartItemDto::getOriginalTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal promoDiscount = originalTotal.subtract(total);
                if (promoDiscount.compareTo(BigDecimal.ZERO) < 0) promoDiscount = BigDecimal.ZERO;
                int totalItems = cart(session).stream().mapToInt(CartItemDto::getQuantity).sum();
                int physical = variants.findById(variantId).map(ProductVariant::getQuantity).orElse(0);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "variantId", variantId,
                        "availableQuantity", physical,
                        "total", total,
                        "originalTotal", originalTotal,
                        "promoDiscount", promoDiscount,
                        "totalItems", totalItems
                ));
            }
        }
        return "redirect:/admin/pos";
    }

    @PostMapping("/admin/pos/clear")
    public String clearCurrentCart(HttpSession session, RedirectAttributes flash) {
        synchronized (session) {
            clearCart(session);
            flash.addFlashAttribute("success", "Đã hủy hóa đơn hiện tại.");
        }
        return "redirect:/admin/pos";
    }

    @PostMapping("/admin/pos/validate-voucher")
    @ResponseBody
    public ResponseEntity<?> voucher(@RequestParam String code, @RequestParam(required = false) Integer customerId, HttpSession session) {
        synchronized (session) {
            try {
                if (cart(session).isEmpty()) throw new IllegalArgumentException("Giỏ hàng trống.");
                var customer = customerId == null ? null : customers.findById(customerId).orElseThrow(() -> new IllegalArgumentException("Khách hàng không tồn tại."));
                var subtotal = pricing.refreshCart(cart(session));
                var result = vouchers.validateAndCalculate(code, customer, subtotal);
                return ResponseEntity.ok(Map.of("valid", result.valid, "message", result.message, "discountAmount", result.discountAmount, "subtotal", subtotal, "shippingFee", BigDecimal.ZERO, "total", subtotal.subtract(result.discountAmount)));
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.badRequest().body(Map.of("valid", false, "message", ex.getMessage()));
            }
        }
    }

    @PostMapping("/admin/pos/payment-qr")
    @ResponseBody
    public VietQrService.PaymentQr paymentQr(@ModelAttribute PosCheckoutRequest request, HttpSession session) {
        synchronized (session) {
            try {
                BigDecimal total;
                if ("COMBINED".equals(request.getPaymentMethod())) {
                    if (request.getTransferAmount() == null || request.getTransferAmount().compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException("Nhập số tiền chuyển khoản hợp lệ (> 0).");
                    }
                    total = request.getTransferAmount();
                } else {
                    total = transferTotal(request, session);
                }
                if (session.getAttribute("posPaymentReference") == null) {
                    session.setAttribute("posPaymentReference", VietQrService.newReference());
                }
                return vietQr.create(total, (String) session.getAttribute("posPaymentReference"));
            } catch (IllegalArgumentException ex) {
                return new VietQrService.PaymentQr(false, ex.getMessage(), null, null, null, null, null, null);
            }
        }
    }

    private BigDecimal transferTotal(PosCheckoutRequest request, HttpSession session) {
        var cart = cart(session);
        if (cart.isEmpty()) throw new IllegalArgumentException("Giỏ hàng trống.");
        if (Boolean.TRUE.equals(session.getAttribute("posHoldReview"))) throw new IllegalArgumentException("Xác nhận lại giá hóa đơn chờ trước khi chuyển khoản.");
        var loaded = variants.findAllById(cart.stream().map(CartItemDto::getVariantId).toList());
        var byId = loaded.stream().collect(java.util.stream.Collectors.toMap(ProductVariant::getId, v -> v));
        var priceMap = pricing.calculatePrices(loaded);
        BigDecimal subtotal = BigDecimal.ZERO;
        for (var item : cart) {
            var variant = byId.get(item.getVariantId());
            if (!ProductSalePolicy.isSelling(variant) || item.getQuantity() == null || item.getQuantity() <= 0 || variant.getQuantity() < item.getQuantity()) {
                throw new IllegalArgumentException("Sản phẩm đã ngừng bán hoặc không đủ tồn kho.");
            }
            var price = priceMap.get(variant.getId()).getFinalPrice();
            if (session.getAttribute("posLoadedHold") != null && price.compareTo(item.getPrice()) != 0) {
                throw new IllegalArgumentException("Giá đã thay đổi. Xác nhận lại hóa đơn chờ trước khi chuyển khoản.");
            }
            subtotal = subtotal.add(price.multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        var customer = request.getCustomerId() == null ? null : customers.findById(request.getCustomerId()).filter(c -> Boolean.TRUE.equals(c.getStatus())).orElseThrow(() -> new IllegalArgumentException("Khách hàng không còn hoạt động."));
        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            var result = vouchers.validateAndCalculate(request.getVoucherCode(), customer, subtotal);
            if (!result.valid) throw new IllegalArgumentException(result.message);
            subtotal = subtotal.subtract(result.discountAmount);
        }
        return subtotal;
    }

    @PostMapping("/admin/pos/checkout")
    public String checkout(@Valid @ModelAttribute("posCheckoutRequest") PosCheckoutRequest request, BindingResult errors,
                           HttpSession session, Principal principal, RedirectAttributes flash) {
        synchronized (session) {
            try {
                if (errors.hasErrors()) throw new IllegalArgumentException("Kiểm tra lại hình thức thanh toán và các ô số tiền.");
                if (request.getCheckoutToken() == null || !request.getCheckoutToken().equals(session.getAttribute("posCheckoutToken"))) {
                    throw new IllegalArgumentException("Phiên thanh toán đã thay đổi. Kiểm tra giỏ rồi thanh toán lại.");
                }
                var cart = cart(session);
                if (cart.isEmpty()) throw new IllegalArgumentException("Giỏ hàng trống.");
                var employee = employees.findByUser_Username(principal.getName()).orElseThrow(() -> new IllegalArgumentException("Tài khoản chưa gắn với nhân viên."));
                var customer = request.getCustomerId() == null ? null : customers.findById(request.getCustomerId()).orElseThrow(() -> new IllegalArgumentException("Khách hàng không tồn tại."));
                if (customer != null && !Boolean.TRUE.equals(customer.getStatus())) throw new IllegalArgumentException("Khách hàng đã bị vô hiệu hóa.");

                BigDecimal subtotal = pricing.refreshCart(cart);
                BigDecimal discount = BigDecimal.ZERO;
                if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
                    var vRes = vouchers.validateAndCalculate(request.getVoucherCode(), customer, subtotal);
                    if (!vRes.valid) throw new IllegalArgumentException(vRes.message);
                    discount = vRes.discountAmount;
                }
                BigDecimal finalAmount = subtotal.subtract(discount);

                if (request.isDelivery()) {
                    if (!Set.of("COD", "TRANSFER").contains(request.getPaymentMethod())) {
                        throw new IllegalArgumentException("Đơn giao hàng chỉ hỗ trợ COD hoặc Chuyển khoản.");
                    }
                    if ("TRANSFER".equals(request.getPaymentMethod())) {
                        if (!request.isTransferReceived()) {
                            throw new IllegalArgumentException("Cần xác nhận đã nhận đủ số tiền chuyển khoản.");
                        }
                        request.setTransferAmount(finalAmount);
                    }
                    if (request.getReceiverName() == null || request.getReceiverName().isBlank()) {
                        throw new IllegalArgumentException("Vui lòng nhập họ tên người nhận.");
                    }
                    if (request.getReceiverPhone() == null || request.getReceiverPhone().isBlank()) {
                        throw new IllegalArgumentException("Vui lòng nhập số điện thoại người nhận.");
                    }
                    if (request.getProvinceId() == null) {
                        throw new IllegalArgumentException("Vui lòng chọn Tỉnh/Thành phố giao hàng.");
                    }
                    if (request.getWardId() == null) {
                        throw new IllegalArgumentException("Vui lòng chọn Phường/Xã giao hàng.");
                    }
                    if (request.getStreetAddress() == null || request.getStreetAddress().isBlank()) {
                        throw new IllegalArgumentException("Vui lòng nhập địa chỉ chi tiết (số nhà, tên đường).");
                    }
                } else {
                    if ("CASH".equals(request.getPaymentMethod())) {
                        if (request.getCashReceived() == null || request.getCashReceived().compareTo(finalAmount) < 0) {
                            throw new IllegalArgumentException("Tiền khách đưa phải đủ thanh toán tổng đơn hàng.");
                        }
                    } else if ("TRANSFER".equals(request.getPaymentMethod())) {
                        if (!request.isTransferReceived()) {
                            throw new IllegalArgumentException("Cần xác nhận đã nhận đủ số tiền chuyển khoản.");
                        }
                        if (request.getTransferAmount() != null && request.getTransferAmount().compareTo(finalAmount) != 0) {
                            throw new IllegalArgumentException("Tổng tiền đã thay đổi.");
                        }
                        request.setTransferAmount(finalAmount);
                    } else if ("COMBINED".equals(request.getPaymentMethod())) {
                        org.slf4j.LoggerFactory.getLogger(AdminPosController.class).info("COMBINED checkout: cashReceived={}, transferAmount={}, finalAmount={}, subtotal={}, discount={}, voucher={}",
                                request.getCashReceived(), request.getTransferAmount(), finalAmount, subtotal, discount, request.getVoucherCode());
                        if (request.getCashReceived() == null || request.getCashReceived().compareTo(BigDecimal.ZERO) <= 0) {
                            throw new IllegalArgumentException("Tiền mặt phải lớn hơn 0 khi thanh toán kết hợp.");
                        }
                        if (request.getTransferAmount() == null || request.getTransferAmount().compareTo(BigDecimal.ZERO) <= 0) {
                            throw new IllegalArgumentException("Tiền chuyển khoản phải lớn hơn 0 khi thanh toán kết hợp.");
                        }
                        if (request.getCashReceived().add(request.getTransferAmount()).compareTo(finalAmount) != 0) {
                            throw new IllegalArgumentException("Tổng tiền mặt và tiền chuyển khoản phải bằng đúng tổng thanh toán.");
                        }
                        if (!request.isTransferReceived()) {
                            throw new IllegalArgumentException("Cần xác nhận đã nhận đủ số tiền chuyển khoản.");
                        }
                    }
                }

                request.setPaymentReference((String) session.getAttribute("posPaymentReference"));
                Long heldId = (Long) session.getAttribute("posLoadedHold");
                if (heldId != null && Boolean.TRUE.equals(session.getAttribute("posHoldReview"))) {
                    throw new IllegalArgumentException("Xác nhận giá đã tính lại trước khi thanh toán.");
                }

                var order = heldId == null
                        ? (request.isDelivery()
                                ? orders.createPosDeliveryOrder(cart, customer, employee, request.getVoucherCode(), request.getPaymentMethod(),
                                        request.getReceiverName(), request.getReceiverPhone(), request.getReceiverEmail(),
                                        request.getProvinceId(), request.getWardId(), request.getStreetAddress(),
                                        request.getLatitude(), request.getLongitude(), request.getCoordinateSource(),
                                        request.isTransferReceived(), request.isConfirmNow(), request.getNote())
                                : orders.createPosOrder(cart, customer, employee, request.getVoucherCode(), request.getPaymentMethod(), "Tại quầy", request.getCashReceived(), request.getTransferAmount(), request.getPaymentReference(), request.getNote()))
                        : holds.checkout(principal.getName(), heldId, cart, request);
                clearCart(session);
                var invoice = invoices.findByOrder_Id(order.getId()).orElse(null);
                if (invoice != null) {
                    flash.addFlashAttribute("message", "Thanh toán thành công! Mã hóa đơn: " + invoice.getInvoiceCode());
                    return "redirect:/admin/invoices/" + invoice.getId();
                }
                flash.addFlashAttribute("message", "Thanh toán thành công!");
                return "redirect:/admin/invoices/" + order.getId();
            } catch (org.springframework.web.server.ResponseStatusException ex) {
                throw ex;
            } catch (IllegalArgumentException ex) {
                flash.addFlashAttribute("error", ex.getMessage());
            } catch (Exception ex) {
                flash.addFlashAttribute("error", "Không thể tạo đơn: " + ex.getMessage());
            }
            flash.addFlashAttribute("posCheckoutRequest", request);
            if (errors.hasErrors()) flash.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + "posCheckoutRequest", errors);
            return "redirect:/admin/pos";
        }
    }

    @PostMapping("/admin/pos/holds/new-tab")
    public String newHoldTab(@ModelAttribute PosCheckoutRequest request, HttpSession session, Principal principal, RedirectAttributes flash) {
        synchronized (session) {
            try {
                var existingHolds = holds.list(principal.getName(), "POS");
                Long currentLoadedId = (Long) session.getAttribute("posLoadedHold");

                // 1. Nếu đang mở 1 hóa đơn chờ, lưu lại nội dung hiện tại của hóa đơn đó
                if (currentLoadedId != null) {
                    var cart = cart(session);
                    holds.save(principal.getName(), cart, request, currentLoadedId, "POS", null);
                }

                // 2. Kiểm tra nếu đã đủ 5 hóa đơn chờ thì không cho tạo thêm
                if (existingHolds.size() >= 5) {
                    flash.addFlashAttribute("error", PosHoldService.LIMIT_MESSAGE);
                    return "redirect:/admin/pos";
                }

                // 3. Tạo ngay một hóa đơn chờ mới (không bắt buộc đơn trước phải có sản phẩm)
                var emptyCart = new ArrayList<CartItemDto>();
                var defaultReq = new PosCheckoutRequest();
                defaultReq.setPaymentMethod("CASH");
                Long newHoldId = holds.save(principal.getName(), emptyCart, defaultReq, null, "POS", null);

                // 4. Mở hóa đơn chờ mới tạo
                clearCart(session);
                session.setAttribute("posCart", new ArrayList<CartItemDto>());
                session.setAttribute("posLoadedHold", newHoldId);
                session.setAttribute("posHeldRequest", defaultReq);
                flash.addFlashAttribute("success", "Đã thêm hóa đơn chờ mới.");
            } catch (IllegalArgumentException ex) {
                flash.addFlashAttribute("error", ex.getMessage());
            }
        }
        return "redirect:/admin/pos";
    }

    @PostMapping("/admin/pos/holds/close")
    public String closeHoldToCurrent(@ModelAttribute PosCheckoutRequest request, HttpSession session, Principal principal) {
        synchronized (session) {
            Long loadedId = (Long) session.getAttribute("posLoadedHold");
            if (loadedId != null) {
                try {
                    holds.save(principal.getName(), cart(session), request, loadedId, "POS", null);
                } catch (Exception ignored) {}
            }
            clearCart(session);
        }
        return "redirect:/admin/pos";
    }

    @PostMapping("/admin/pos/holds")
    public String hold(@ModelAttribute PosCheckoutRequest request, HttpSession session, Principal principal, RedirectAttributes flash) {
        return newHoldTab(request, session, principal, flash);
    }

    @PostMapping("/admin/pos/holds/{id}/open")
    public String openHold(@PathVariable Long id, @RequestParam(defaultValue = "SAVE") String action,
                           @ModelAttribute PosCheckoutRequest request, HttpSession session, Principal principal, RedirectAttributes flash) {
        synchronized (session) {
            try {
                var loaded = holds.switchTo(principal.getName(), id, cart(session), request, (Long) session.getAttribute("posLoadedHold"), true, "POS", null);
                clearCart(session);
                session.setAttribute("posCart", loaded.cart());
                session.setAttribute("posLoadedHold", id);
                session.setAttribute("posHeldRequest", loaded.request());
                var baseline = prices(session);
                if (baseline.isEmpty()) {
                    loaded.cart().forEach(i -> baseline.put(i.getVariantId(), i.getPrice()));
                    session.setAttribute("posHoldReview", true);
                }
                var checkout = loaded.request();
                if (checkout.getCustomerId() != null && customers.findById(checkout.getCustomerId()).filter(c -> Boolean.TRUE.equals(c.getStatus())).isEmpty()) {
                    checkout.setCustomerId(null);
                    flash.addFlashAttribute("error", "Khách đã bị vô hiệu hóa. Chuyển về Khách lẻ.");
                }
                if (checkout.getVoucherCode() != null && !checkout.getVoucherCode().isBlank()) {
                    var customer = checkout.getCustomerId() == null ? null : customers.findById(checkout.getCustomerId()).orElse(null);
                    var subtotal = loaded.cart().stream().map(CartItemDto::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
                    var result = vouchers.validateAndCalculate(checkout.getVoucherCode(), customer, subtotal);
                    if (!result.valid) {
                        checkout.setVoucherCode(null);
                        flash.addFlashAttribute("error", "Đã bỏ phiếu giảm giá: " + result.message);
                    }
                }
                flash.addFlashAttribute("success", "Đã mở " + loaded.code() + ". Giá và tồn kho đã được tính lại.");
            } catch (IllegalArgumentException ex) {
                flash.addFlashAttribute("error", ex.getMessage());
            }
        }
        return "redirect:/admin/pos";
    }

    @PostMapping("/admin/pos/holds/{id}/delete")
    public String deleteHold(@PathVariable Long id, HttpSession session, Principal principal, RedirectAttributes flash) {
        synchronized (session) {
            holds.delete(principal.getName(), id, "POS");
            if (id.equals(session.getAttribute("posLoadedHold"))) {
                clearCart(session);
            }
        }
        flash.addFlashAttribute("success", "Đã xóa hóa đơn chờ.");
        return "redirect:/admin/pos";
    }

    @PostMapping("/admin/pos/holds/review")
    public String review(HttpSession session, Principal principal, RedirectAttributes flash) {
        synchronized (session) {
            Long id = (Long) session.getAttribute("posLoadedHold");
            if (id == null) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
            holds.open(principal.getName(), id, "POS");
            try {
                pricing.refreshCart(cart(session));
                var baseline = prices(session);
                baseline.clear();
                cart(session).forEach(i -> baseline.put(i.getVariantId(), i.getPrice()));
                session.removeAttribute("posHoldReview");
                flash.addFlashAttribute("success", "Đã xác nhận giá và tồn kho hiện tại.");
            } catch (IllegalArgumentException ex) {
                flash.addFlashAttribute("error", ex.getMessage());
            }
        }
        return "redirect:/admin/pos";
    }

    @PostMapping("/admin/pos/new")
    public String newOrder(HttpSession session) {
        synchronized (session) {
            clearCart(session);
        }
        return "redirect:/admin/pos";
    }

    @GetMapping("/admin/pos/customers")
    public String searchCustomersLegacy(
            @RequestParam(defaultValue = "") String customerQuery,
            @RequestParam(defaultValue = "0") String customerPage,
            HttpSession session,
            Principal principal,
            Model model) {
        String view = page(session, principal, model);
        var pageResult = catalogQuery.customers(customerQuery, customerPage);
        model.addAttribute("customerPage", pageResult);
        return view;
    }

    @GetMapping("/admin/pos/customers-json")
    @ResponseBody
    public List<Map<String, Object>> searchCustomersJson(@RequestParam(defaultValue = "") String q) {
        if (q == null || q.trim().length() < 2) return List.of();
        var page = catalogQuery.customers(q, "0");
        return page.getContent();
    }

    @PostMapping("/admin/pos/customers/quick-create")
    @ResponseBody
    public ResponseEntity<?> quickCreateCustomer(
            @RequestParam String fullName,
            @RequestParam String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Integer provinceId,
            @RequestParam(required = false) Integer wardId,
            @RequestParam(required = false) String streetAddress) {
        try {
            var form = new com.example.demodatn.dto.khachhang.CustomerDto();
            form.setFullName(fullName != null ? fullName.trim() : "");
            form.setPhone(phone != null ? phone.trim() : "");
            form.setEmail(email != null && !email.isBlank() ? email.trim() : null);
            String code = people.nextCustomerCode();
            form.setCustomerCode(code);
            form.setProvinceId(provinceId != null ? provinceId : 1);
            form.setWardId(wardId != null ? wardId : 1);
            form.setStreetAddress(streetAddress != null && !streetAddress.isBlank() ? streetAddress.trim() : "Tại quầy");
            form.setAddress(form.getStreetAddress());

            if (form.getEmail() != null && !form.getEmail().isBlank()) {
                var outcome = accounts.saveCustomer(form);
                var c = customers.findById(outcome.personId()).orElseThrow();
                return ResponseEntity.ok(Map.of(
                        "id", c.getId(),
                        "customerCode", c.getCustomerCode(),
                        "fullName", c.getFullName(),
                        "phone", c.getPhone(),
                        "email", c.getEmail() != null ? c.getEmail() : "",
                        "accountIssued", outcome.issued(),
                        "accountSent", outcome.sent(),
                        "message", outcome.message()
                ));
            } else {
                var c = people.saveCustomer(form);
                return ResponseEntity.ok(Map.of(
                        "id", c.getId(),
                        "customerCode", c.getCustomerCode(),
                        "fullName", c.getFullName(),
                        "phone", c.getPhone(),
                        "email", "",
                        "accountIssued", false,
                        "accountSent", false,
                        "message", "Đã lưu thông tin khách hàng."
                ));
            }
        } catch (com.example.demodatn.exception.PeopleValidationException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi tạo khách hàng: " + ex.getMessage()));
        }
    }

    @GetMapping("/admin/pos/customers/{id}/addresses")
    @ResponseBody
    public List<Map<String, Object>> getCustomerAddresses(@PathVariable Integer id) {
        var customer = customers.findById(id).orElse(null);
        if (customer == null) return List.of();
        var list = shippingAddresses.findByCustomer_IdOrderBySlotAsc(id);
        List<Map<String, Object>> res = new ArrayList<>();
        for (var addr : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", addr.getId());
            map.put("receiverName", addr.getReceiverName());
            map.put("phone", addr.getPhone());
            map.put("email", customer.getEmail() != null ? customer.getEmail() : (customer.getUser() != null ? customer.getUser().getEmail() : ""));
            map.put("isDefault", addr.isDefaultAddress());
            map.put("provinceId", addr.getProvince() != null ? addr.getProvince().getId() : null);
            map.put("provinceName", addr.getProvince() != null ? addr.getProvince().getName() : "");
            map.put("wardId", addr.getWard() != null ? addr.getWard().getId() : null);
            map.put("wardName", addr.getWard() != null ? addr.getWard().getName() : "");
            map.put("streetAddress", addr.getDisplayStreetAddress());
            map.put("fullAddress", addr.getFullAddress());
            map.put("latitude", addr.getLatitude());
            map.put("longitude", addr.getLongitude());
            map.put("coordinateSource", addr.getCoordinateSource());
            res.add(map);
        }
        if (res.isEmpty() && customer.getAddress() != null && !customer.getAddress().isBlank()) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", 0);
            map.put("receiverName", customer.getFullName());
            map.put("phone", customer.getPhone());
            map.put("email", customer.getEmail() != null ? customer.getEmail() : (customer.getUser() != null ? customer.getUser().getEmail() : ""));
            map.put("isDefault", true);
            map.put("provinceId", null);
            map.put("provinceName", "");
            map.put("wardId", null);
            map.put("wardName", "");
            map.put("streetAddress", customer.getAddress());
            map.put("fullAddress", customer.getAddress());
            res.add(map);
        }
        return res;
    }

    @GetMapping("/admin/pos/success/{id}")
    public String success(@PathVariable Integer id) {
        var invoice = invoices.findByOrder_Id(id).orElse(null);
        if (invoice != null) {
            return "redirect:/admin/invoices/" + invoice.getId();
        }
        return "redirect:/admin/invoices/" + id;
    }
}
