package com.example.demodatn.dto.khuyenmai;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
@Data
public class VoucherDto {
    private Integer id;
    @Pattern(regexp="^[A-Za-z0-9_-]{0,50}$",message="Mã gồm 1–50 chữ, số, _ hoặc -.")
    private String code;
    @NotBlank(message="Nhập tên phiếu giảm giá.") @Size(max=100,message="Tên tối đa 100 ký tự.") private String name;
    @NotBlank @Pattern(regexp="PERCENT|FIXED",message="Chọn loại giảm hợp lệ.") private String discountType="PERCENT";
    @NotNull(message="Nhập giá trị giảm.") @DecimalMin(value="0",inclusive=false,message="Giá trị giảm phải lớn hơn 0.") @Digits(integer=16,fraction=2)
    private BigDecimal discountValue;
    @DecimalMin(value="0",message="Đơn tối thiểu không âm.") @Digits(integer=16,fraction=2) private BigDecimal minOrderValue=BigDecimal.ZERO;
    @DecimalMin(value="0",inclusive=false,message="Giảm tối đa phải lớn hơn 0; để trống nếu không giới hạn.") @Digits(integer=16,fraction=2) private BigDecimal maxDiscount;
    @NotNull(message="Chọn thời điểm bắt đầu.") @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) private LocalDateTime startDate;
    @NotNull(message="Chọn thời điểm kết thúc.") @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) private LocalDateTime endDate;
    private Integer usageLimit;
    private Boolean isPublic=true;
    private Integer customerId;
    @NotNull private Boolean status=true;
}
