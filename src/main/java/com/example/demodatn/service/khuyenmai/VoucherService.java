package com.example.demodatn.service.khuyenmai;
import com.example.demodatn.entity.khachhang.Customer;
import com.example.demodatn.entity.khuyenmai.Voucher;
import com.example.demodatn.repository.khuyenmai.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

@Service @RequiredArgsConstructor
public class VoucherService {
    private final VoucherRepository voucherRepository;
    private final DiscountEffectivity effectivity;

    public static class VoucherResult {
        public boolean valid;
        public String message;
        public Voucher voucher;
        public BigDecimal discountAmount=BigDecimal.ZERO;
    }
    public static String normalize(String code) { return code==null ? "" : code.trim().toUpperCase(Locale.ROOT); }

    public record CheckoutVoucher(String code, String name, String discountType, BigDecimal discountValue,
                                  BigDecimal maxDiscount, BigDecimal minOrderValue, BigDecimal discountAmount,
                                  BigDecimal missingAmount, boolean eligible, boolean best, String message) {}

    @Transactional(readOnly=true)
    public java.util.List<CheckoutVoucher> checkoutOptions(Customer customer, BigDecimal subtotal) {
        var active = voucherRepository.findAll().stream()
                .filter(v -> "RUNNING".equals(effectivity.voucherState(v))).toList();
        var calculated = new java.util.LinkedHashMap<Voucher, VoucherResult>();
        active.forEach(v -> calculated.put(v, calculate(v, customer, subtotal)));
        BigDecimal best = calculated.values().stream().filter(r -> r.valid).map(r -> r.discountAmount)
                .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        return calculated.entrySet().stream().map(entry -> {
            var v = entry.getKey(); var r = entry.getValue();
            BigDecimal min = v.getMinOrderValue() == null ? BigDecimal.ZERO : v.getMinOrderValue();
            return new CheckoutVoucher(v.getCode(), v.getName(), v.getDiscountType(), v.getDiscountValue(),
                    v.getMaxDiscount(), min, r.discountAmount, min.subtract(subtotal).max(BigDecimal.ZERO),
                    r.valid, r.valid && best.signum() > 0 && r.discountAmount.compareTo(best) == 0, r.message);
        }).sorted(java.util.Comparator.comparing(CheckoutVoucher::eligible).reversed()
                .thenComparing(CheckoutVoucher::discountAmount, java.util.Comparator.reverseOrder())
                .thenComparing(CheckoutVoucher::code)).toList();
    }

    @Transactional(readOnly=true)
    public VoucherResult validateAndCalculate(String code, Customer customer, BigDecimal subtotal) {
        return calculate(voucherRepository.findByCodeIgnoreCase(normalize(code)).orElse(null),customer,subtotal);
    }
    // Caller must own the order transaction: failures roll back usage, order and stock together.
    @Transactional(propagation=Propagation.MANDATORY)
    public VoucherResult consume(String code, Customer customer, BigDecimal subtotal) {
        Voucher voucher=voucherRepository.findByCodeForUpdate(normalize(code)).orElse(null);
        VoucherResult result=calculate(voucher,customer,subtotal);
        if(!result.valid) throw new IllegalArgumentException(result.message);
        voucher.setUsedCount((voucher.getUsedCount()==null ? 0 : voucher.getUsedCount())+1);
        voucherRepository.save(voucher);
        return result;
    }
    private VoucherResult failure(String message) {
        VoucherResult r=new VoucherResult();r.message=message;return r;
    }
    private VoucherResult calculate(Voucher voucher, Customer customer, BigDecimal subtotal) {
        if(subtotal==null || subtotal.signum()<0) return failure("Giá trị đơn hàng không hợp lệ.");
        if(voucher==null || !Boolean.TRUE.equals(voucher.getStatus())) return failure("Phiếu giảm giá không tồn tại hoặc đã bị khóa.");
        String state=effectivity.voucherState(voucher);
        if("UPCOMING".equals(state))return failure("Phiếu giảm giá chưa tới ngày sử dụng.");
        if("EXPIRED".equals(state))return failure("Phiếu giảm giá đã hết hạn.");
        if(!"RUNNING".equals(state))return failure("Thời hạn phiếu giảm giá không hợp lệ.");
        if(voucher.getMinOrderValue()!=null && subtotal.compareTo(voucher.getMinOrderValue())<0) return failure("Đơn hàng chưa đạt giá trị tối thiểu "+voucher.getMinOrderValue().toPlainString()+" đ.");
        BigDecimal value=voucher.getDiscountValue(),discount;
        if(value==null || value.signum()<=0) return failure("Giá trị giảm của phiếu giảm giá không hợp lệ.");
        if("PERCENT".equals(voucher.getDiscountType())) {
            if(value.compareTo(BigDecimal.ONE)<0 || value.compareTo(BigDecimal.valueOf(100))>0) return failure("Phần trăm phiếu giảm giá không hợp lệ.");
            discount=subtotal.multiply(value).divide(BigDecimal.valueOf(100),0,RoundingMode.HALF_UP);
            if(voucher.getMaxDiscount()!=null) {
                if(voucher.getMaxDiscount().signum()<=0) return failure("Giảm tối đa của phiếu giảm giá không hợp lệ.");
                discount=discount.min(voucher.getMaxDiscount());
            }
        } else if("FIXED".equals(voucher.getDiscountType())) discount=value;
        else return failure("Loại phiếu giảm giá không hợp lệ.");
        VoucherResult result=new VoucherResult();result.valid=true;result.voucher=voucher;
        result.discountAmount=discount.min(subtotal);
        result.message="Áp dụng thành công.";return result;
    }
}
