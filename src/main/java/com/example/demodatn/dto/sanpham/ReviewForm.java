package com.example.demodatn.dto.sanpham;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class ReviewForm {
 private Integer id;
 private Integer orderId;
 @NotNull(message="Chọn số sao.") @Min(value=1,message="Số sao từ 1 đến 5.") @Max(value=5,message="Số sao từ 1 đến 5.") private Integer stars;
 @Size(max=1000,message="Nội dung tối đa 1000 ký tự.") private String content;
}
