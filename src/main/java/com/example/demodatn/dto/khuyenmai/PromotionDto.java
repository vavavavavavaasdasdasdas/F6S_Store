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
    @NotNull(message="Nhập phần trăm giảm.") @DecimalMin(value="1",message="Phần trăm từ 1 đến 100.") @DecimalMax(value="100",message="Phần trăm từ 1 đến 100.") @Digits(integer=3,fraction=2,message="Giá trị tối đa 2 chữ số thập phân.")
    private BigDecimal discountValue;
    @NotNull(message="Chọn thời điểm bắt đầu.") @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) private LocalDateTime startDate;
    @NotNull(message="Chọn thời điểm kết thúc.") @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) private LocalDateTime endDate;
    @NotNull private Boolean status=true;
}
