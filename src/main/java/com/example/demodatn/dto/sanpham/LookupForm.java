package com.example.demodatn.dto.sanpham;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LookupForm {
    private Integer id;
    @NotBlank(message="Không được để trống.")
    @jakarta.validation.constraints.Size(max=100,message="Tối đa 100 ký tự.")
    private String value;
    private String hexCode;
    private Boolean status = true;
}

