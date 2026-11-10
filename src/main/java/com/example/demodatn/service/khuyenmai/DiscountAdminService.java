package com.example.demodatn.service.khuyenmai;
import com.example.demodatn.dto.khuyenmai.PromotionDto;
import com.example.demodatn.dto.khuyenmai.VoucherDto;
import com.example.demodatn.entity.khuyenmai.Promotion;
import com.example.demodatn.entity.khuyenmai.PromotionDetail;
import com.example.demodatn.entity.khuyenmai.Voucher;
import com.example.demodatn.exception.DiscountValidationException;
import com.example.demodatn.repository.donhang.OrderRepository;
import com.example.demodatn.repository.khachhang.CustomerRepository;
import com.example.demodatn.repository.khuyenmai.PromotionDetailRepository;
import com.example.demodatn.repository.khuyenmai.PromotionRepository;
import com.example.demodatn.repository.khuyenmai.VoucherRepository;
import com.example.demodatn.repository.sanpham.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Locale;

@Service @RequiredArgsConstructor
public class DiscountAdminService {
    private final VoucherRepository vouchers;
    private final PromotionRepository promotions;
    private final PromotionDetailRepository details;
    private final CustomerRepository customers;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public synchronized String nextVoucherCode() {
        Integer maxNum = null;
        try {
            maxNum = jdbc.queryForObject(
                "SELECT MAX(TRY_CAST(SUBSTRING(code, 4, 20) AS INT)) FROM vouchers WHERE code LIKE 'PGG[0-9]%'",
                Integer.class
            );
        } catch (Exception ignored) {
        }
        int next = (maxNum != null ? maxNum : 0) + 1;
        String candidate = String.format(Locale.ROOT, "PGG%05d", next);
        while (vouchers.existsByCodeIgnoreCaseAndIdNot(candidate, -1)) {
            next++;
            candidate = String.format(Locale.ROOT, "PGG%05d", next);
        }
        return candidate;
    }

    @Transactional(readOnly = true)
    public synchronized String nextPromotionCode() {
        Integer maxNum = null;
        try {
            maxNum = jdbc.queryForObject(
                "SELECT MAX(TRY_CAST(SUBSTRING(ma, 3, 20) AS INT)) FROM promotions WHERE ma LIKE 'KM[0-9]%'",
                Integer.class
            );
        } catch (Exception ignored) {
        }
        int next = (maxNum != null ? maxNum : 0) + 1;
        return String.format(Locale.ROOT, "KM%03d", next);
    }

    public Voucher voucher(Integer id) { return vouchers.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND)); }
    public Promotion promotion(Integer id) { return promotions.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND)); }
    private void dates(LocalDateTime start,LocalDateTime end) {
        if(start==null || end==null || !end.isAfter(start)) throw new DiscountValidationException("endDate","Kết thúc phải sau thời điểm bắt đầu.");
    }
    @Transactional
    public Voucher saveVoucher(VoucherDto f) {
        if(f.getStartDate()==null||f.getEndDate()==null||f.getEndDate().toLocalDate().isBefore(f.getStartDate().toLocalDate()))throw new DiscountValidationException("endDate","Ngày kết thúc không được trước ngày bắt đầu.");
        String rawCode = f.getCode();
        String code;
        if (rawCode == null || rawCode.trim().isEmpty()) {
            code = nextVoucherCode();
        } else {
            code = VoucherService.normalize(rawCode);
        }
        if(vouchers.existsByCodeIgnoreCaseAndIdNot(code,f.getId()==null ? -1 : f.getId())) throw new DiscountValidationException("code","Mã phiếu giảm giá đã tồn tại.");
        if(!Set.of("PERCENT","FIXED").contains(f.getDiscountType())) throw new DiscountValidationException("discountType","Loại phiếu giảm giá không hợp lệ.");
        if(f.getDiscountValue()==null || f.getDiscountValue().signum()<=0 || ("PERCENT".equals(f.getDiscountType()) && (f.getDiscountValue().compareTo(BigDecimal.ONE)<0 || f.getDiscountValue().compareTo(BigDecimal.valueOf(100))>0))) throw new DiscountValidationException("discountValue","Phần trăm từ 1–100; số tiền phải lớn hơn 0.");
        if(f.getMaxDiscount()!=null && f.getMaxDiscount().signum()<=0) throw new DiscountValidationException("maxDiscount","Giảm tối đa phải lớn hơn 0.");
        Voucher v=f.getId()==null ? new Voucher() : vouchers.findForUpdate(f.getId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        v.setCode(code);v.setName(f.getName().trim());v.setDiscountType(f.getDiscountType());v.setDiscountValue(f.getDiscountValue());
        v.setMinOrderValue(f.getMinOrderValue()==null ? BigDecimal.ZERO : f.getMinOrderValue());
        v.setMaxDiscount("PERCENT".equals(f.getDiscountType()) ? f.getMaxDiscount() : null);
        v.setStartDate(f.getStartDate());v.setEndDate(f.getEndDate().toLocalDate().atStartOfDay());v.setIsPublic(true);v.setCustomer(null);v.setStatus(f.getStatus());
        // usedCount is deliberately not bindable from the Admin form.
        return vouchers.saveAndFlush(v);
    }
    @Transactional
    public String deleteVoucher(Integer id) {
        throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED,"Không được xóa phiếu giảm giá. Hãy tạm dừng hoạt động.");
    }
    @Transactional
    public Promotion savePromotion(PromotionDto f) {
        dates(f.getStartDate(),f.getEndDate());
        if(!Set.of("PERCENT","AMOUNT").contains(f.getDiscountType()))throw new DiscountValidationException("discountType","Chọn loại giảm hợp lệ.");
        if(f.getDiscountValue()==null||f.getDiscountValue().signum()<=0||f.getDiscountValue().stripTrailingZeros().scale()>0||f.getDiscountValue().precision()-f.getDiscountValue().scale()>16||("PERCENT".equals(f.getDiscountType())&&f.getDiscountValue().compareTo(BigDecimal.valueOf(100))>0))throw new DiscountValidationException("discountValue","Giá trị là số nguyên dương; phần trăm từ 1 đến 100.");
        if (f.getId() == null) {
            try {
                Integer maxNum = jdbc.queryForObject(
                    "SELECT MAX(TRY_CAST(SUBSTRING(ma, 3, 20) AS INT)) FROM promotions WHERE ma LIKE 'KM[0-9]%'",
                    Integer.class
                );
                int nextVal = (maxNum != null ? maxNum : 0) + 1;
                Long currentSeq = jdbc.queryForObject(
                    "SELECT CAST(current_value AS BIGINT) FROM sys.sequences WHERE name = 'promotions_ma_seq'",
                    Long.class
                );
                if (currentSeq != null && currentSeq < nextVal) {
                    jdbc.execute("ALTER SEQUENCE dbo.promotions_ma_seq RESTART WITH " + nextVal);
                }
            } catch (Exception ignored) {
            }
        }
        Promotion p=f.getId()==null ? new Promotion() : promotions.findForUpdate(f.getId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        p.setName(f.getName().trim());p.setDiscountType(f.getDiscountType());p.setDiscountValue(f.getDiscountValue());p.setStartDate(f.getStartDate());p.setEndDate(f.getEndDate());p.setStatus(f.getStatus());
        return promotions.saveAndFlush(p);
    }
    @Transactional
    public void selectProducts(Integer id,List<Integer> ids) {
        Promotion p=promotions.findForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        Set<Integer> unique=new HashSet<>(ids);
        var chosen=products.findAllById(unique);
        if(chosen.size()!=unique.size()) throw new IllegalArgumentException("Có sản phẩm không tồn tại; tải lại danh sách.");
        details.deleteByPromotion_Id(id);details.flush();
        for(var product:chosen) { var d=new PromotionDetail();d.setPromotion(p);d.setProduct(product);details.save(d); }
    }
    @Transactional
    public void selectProductPage(Integer id,List<Integer> visible,List<Integer> chosen) {
        Promotion promotion=promotions.findForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(!visible.containsAll(chosen))throw new IllegalArgumentException("Danh sách sản phẩm không hợp lệ.");
        var selected=products.findAllById(new HashSet<>(chosen));
        if(selected.size()!=new HashSet<>(chosen).size()||selected.stream().anyMatch(p->!Boolean.TRUE.equals(p.getStatus())))throw new IllegalArgumentException("Chỉ chọn sản phẩm đang bán. Tải lại danh sách.");
        details.deleteAll(details.findByPromotion_IdAndProduct_IdIn(id,visible));details.flush();
        for(var product:selected){var row=new PromotionDetail();row.setPromotion(promotion);row.setProduct(product);details.save(row);}
    }
    @Transactional
    public Voucher voucherStatus(Integer id,boolean active){var v=vouchers.findForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));v.setStatus(active);return v;}
    @Transactional
    public Promotion promotionStatus(Integer id,boolean active){var p=promotions.findForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));p.setStatus(active);return p;}
    @Transactional
    public void deletePromotion(Integer id) {
        Promotion p=promotions.findForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        details.deleteByPromotion_Id(id);details.flush();promotions.delete(p);
    }
}
