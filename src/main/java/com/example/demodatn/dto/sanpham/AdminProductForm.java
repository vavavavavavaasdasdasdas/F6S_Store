package com.example.demodatn.dto.sanpham;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class AdminProductForm {
    private Integer id;
    private Integer soleId;
    private String newSoleName;
    private Integer initialMaterialId;
    private String newInitialMaterialName;
    private String newInitialSizeValue;
    private java.util.List<Integer> initialSizeIds = new java.util.ArrayList<>();
    @DecimalMin(value="0.01", message="Giá phải lớn hơn 0.")
    @Digits(integer=16,fraction=2,message="Giá tối đa 16 chữ số và 2 số thập phân.")
    private java.math.BigDecimal initialPrice;
    @Min(value=0,message="Tồn kho ban đầu không được âm.")
    private Integer initialQuantity = 0;
    @NotBlank(message="Nhập mã sản phẩm.") @Pattern(regexp="[A-Za-z0-9_-]{1,50}", message="Mã gồm 1–50 chữ, số, dấu - hoặc _.")
    private String code;
    @NotBlank(message="Nhập tên sản phẩm.") @jakarta.validation.constraints.Size(max=255,message="Tên tối đa 255 ký tự.")
    private String name;
    @NotNull(message="Chọn thương hiệu.") private Integer categoryId;
    private String newCategoryName;
    @jakarta.validation.constraints.Size(max=4000,message="Mô tả tối đa 4000 ký tự.") private String description;
    @NotNull(message="Chọn trạng thái.") private Boolean status = true;
    private MultipartFile image;
    private String imageUrl;
    private MultipartFile[] images;
    private java.util.List<Long> removedImageIds = new java.util.ArrayList<>();
    private java.util.List<String> imageOrder = new java.util.ArrayList<>();
    private String mainImageKey;
}
