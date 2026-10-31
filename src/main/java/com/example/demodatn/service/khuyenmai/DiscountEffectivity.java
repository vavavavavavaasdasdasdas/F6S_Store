package com.example.demodatn.service.khuyenmai;

import com.example.demodatn.entity.khuyenmai.*;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.*;

@Service @RequiredArgsConstructor
public class DiscountEffectivity {
    private final Clock clock;
    public LocalDateTime now(){return LocalDateTime.now(clock.withZone(ZoneId.of("Asia/Ho_Chi_Minh")));}
    private String state(Boolean enabled,LocalDateTime start,LocalDateTime end,boolean endOfDay){
        if(!Boolean.TRUE.equals(enabled))return "INACTIVE";
        if(start==null||end==null)return "INVALID";
        LocalDateTime expiry=endOfDay?end.toLocalDate().plusDays(1).atStartOfDay():end;
        if(!expiry.isAfter(start))return "INVALID";
        var current=now();
        if(current.isBefore(start))return "UPCOMING";
        return current.isBefore(expiry)?"RUNNING":"EXPIRED";
    }
    public String voucherState(Voucher voucher){return state(voucher.getStatus(),voucher.getStartDate(),voucher.getEndDate(),true);}
    public String promotionState(Promotion promotion){return state(promotion.getStatus(),promotion.getStartDate(),promotion.getEndDate(),false);}
    public boolean active(Promotion promotion){return promotion!=null&&"RUNNING".equals(promotionState(promotion));}
    public String label(String state){return switch(state){case "RUNNING"->"Đang chạy";case "UPCOMING"->"Sắp diễn ra";case "EXPIRED"->"Hết hạn";case "INACTIVE"->"Tạm dừng";default->"Thời hạn không hợp lệ";};}
    public Predicate predicate(Root<?> root,CriteriaBuilder cb,String state,boolean endOfDay){
        var current=now();var expiryThreshold=endOfDay?current.toLocalDate().atStartOfDay():current;
        return switch(state==null?"":state){
            case "RUNNING"->cb.and(cb.isTrue(root.get("status")),cb.lessThanOrEqualTo(root.get("startDate"),current),endOfDay?cb.greaterThanOrEqualTo(root.get("endDate"),expiryThreshold):cb.greaterThan(root.get("endDate"),expiryThreshold));
            case "UPCOMING"->cb.and(cb.isTrue(root.get("status")),cb.greaterThan(root.get("startDate"),current));
            case "EXPIRED"->endOfDay?cb.lessThan(root.get("endDate"),expiryThreshold):cb.lessThanOrEqualTo(root.get("endDate"),expiryThreshold);
            case "INACTIVE"->cb.or(cb.isNull(root.get("status")),cb.isFalse(root.get("status")));
            default->cb.conjunction();
        };
    }
}
