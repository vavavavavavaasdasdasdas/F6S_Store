package com.example.demodatn.dto.khuyenmai;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
@Data
public class PromotionDto {
    private String code;
    private Integer id;
    @NotBlank(message="Nhập tên đợt giảm giá.") @Size(max=100,message="Tên tối đa 100 ký tự.") private String name;
    @NotBlank @Pattern(regexp="PERCENT|AMOUNT",message="Chọn loại giảm hợp lệ.") private String discountType="PERCENT";
    @NotNull(message="Nhập giá trị giảm.") @DecimalMin(value="1",message="Giá trị phải lớn hơn 0.") @Digits(integer=16,fraction=2,message="Nhập số nguyên, tối đa 16 chữ số.")
    private BigDecimal discountValue;
    @NotNull(message="Chọn thời điểm bắt đầu.") @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) private LocalDateTime startDate;
    @NotNull(message="Chọn thời điểm kết thúc.") @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) private LocalDateTime endDate;
    @NotNull private Boolean status=true;
}
