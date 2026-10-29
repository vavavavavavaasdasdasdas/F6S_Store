package com.example.demodatn.service.donhang;
import com.example.demodatn.service.sanpham.ProductSalePolicy;
import com.example.demodatn.dto.donhang.PriceInfo;
import com.example.demodatn.entity.khuyenmai.Promotion;
import com.example.demodatn.entity.khuyenmai.PromotionDetail;
import com.example.demodatn.entity.sanpham.ProductVariant;
import com.example.demodatn.repository.khuyenmai.PromotionDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final PromotionDetailRepository promotionDetailRepository;
    private final com.example.demodatn.repository.sanpham.ProductVariantRepository variants;
    @org.springframework.beans.factory.annotation.Value("${shop.time-zone:Asia/Ho_Chi_Minh}") private String timeZone="Asia/Ho_Chi_Minh";

    public PriceInfo priceForVariant(Integer id) {
        return calculatePrice(variants.findById(id).orElseThrow(()->new IllegalArgumentException("Sản phẩm không tồn tại.")));
    }

    public BigDecimal refreshCart(List<com.example.demodatn.dto.donhang.CartItemDto> cart) {
        BigDecimal total=BigDecimal.ZERO;
        for(var item:cart) {
            var variant=variants.findById(item.getVariantId()).orElseThrow(()->new IllegalArgumentException("Sản phẩm không tồn tại."));
            if(!ProductSalePolicy.isSelling(variant) || item.getQuantity()==null || item.getQuantity()<=0 || variant.getQuantity()<item.getQuantity()) throw new IllegalArgumentException("Sản phẩm đã ngừng bán hoặc không đủ tồn kho: "+variant.getProduct().getName());
            item.setMaterialName(variant.getMaterial().getName());
            item.setPrice(calculatePrice(variant).getFinalPrice());
            total=total.add(item.getTotalPrice());
        }
        return total;
    }

    public PriceInfo calculatePrice(ProductVariant variant) {
        return calculatePrice(variant,promotionDetailRepository.findByProductId(variant.getProduct().getId()));
    }

    public java.util.Map<Integer,PriceInfo> calculatePrices(List<ProductVariant> items) {
        if(items.isEmpty()) return java.util.Map.of();
        var ids=items.stream().map(v->v.getProduct().getId()).distinct().toList();
        var details=promotionDetailRepository.findByProduct_IdIn(ids).stream()
                .collect(java.util.stream.Collectors.groupingBy(d->d.getProduct().getId()));
        return items.stream().collect(java.util.stream.Collectors.toMap(ProductVariant::getId,
                v->calculatePrice(v,details.getOrDefault(v.getProduct().getId(),List.of()))));
    }

    private PriceInfo calculatePrice(ProductVariant variant,List<PromotionDetail> details) {
        BigDecimal originalPrice = variant.getPrice();
        if (originalPrice == null) originalPrice = BigDecimal.ZERO;
        
        
        BigDecimal maxDiscountValue = BigDecimal.ZERO;
        int maxDiscountPercent = 0;
        boolean hasActivePromo = false;
        
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of(timeZone));
        
        for (PromotionDetail detail : details) {
            Promotion promo = detail.getPromotion();
            if (promo != null && Boolean.TRUE.equals(promo.getStatus()) &&
                promo.getStartDate()!=null && promo.getEndDate()!=null && promo.getDiscountValue()!=null &&
                !promo.getStartDate().isAfter(now) && now.isBefore(promo.getEndDate())) {
                
                BigDecimal discount = BigDecimal.ZERO;
                int percent = 0;
                
                if ("PERCENT".equalsIgnoreCase(promo.getDiscountType()) && promo.getDiscountValue().compareTo(BigDecimal.ONE)>=0 && promo.getDiscountValue().compareTo(BigDecimal.valueOf(100))<=0) {
                    discount = originalPrice.multiply(promo.getDiscountValue()).divide(new BigDecimal(100), 0, RoundingMode.HALF_UP);
                    percent = promo.getDiscountValue().intValue();
                } else if ("AMOUNT".equalsIgnoreCase(promo.getDiscountType())) {
                    discount = promo.getDiscountValue();
                    if (originalPrice.compareTo(BigDecimal.ZERO) > 0) {
                        percent = discount.multiply(new BigDecimal(100)).divide(originalPrice, 0, RoundingMode.HALF_UP).intValue();
                    }
                }
                
                if (discount.compareTo(maxDiscountValue) > 0) {
                    maxDiscountValue = discount;
                    maxDiscountPercent = percent;
                    hasActivePromo = true;
                }
            }
        }
        
        BigDecimal finalPrice = originalPrice.subtract(maxDiscountValue);
        if (finalPrice.compareTo(BigDecimal.ZERO) < 0) {
            finalPrice = BigDecimal.ZERO;
        }
        
        return new PriceInfo(originalPrice, finalPrice, maxDiscountPercent, hasActivePromo);
    }
}
