package com.example.demodatn.controller;
import com.example.demodatn.dto.sanpham.AdminProductForm;
import com.example.demodatn.repository.sanpham.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

// Multipart parsing may fail before a controller is selected, so this advice has no selector.
@ControllerAdvice
@RequiredArgsConstructor
public class ProductUploadExceptionAdvice {
    private final CategoryRepository categories;
    @Value("${shop.upload-max-bytes:5242880}") private long maxBytes;

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public String tooLarge(Model model) {
        AdminProductForm form = new AdminProductForm();
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "product");
        errors.rejectValue("image", "tooLarge", "Ảnh vượt giới hạn tải lên. Chọn JPG/PNG/WEBP tối đa "
                + maxBytes / 1024 / 1024 + " MB và nhập lại thông tin.");
        model.addAttribute("product", form);
        model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "product", errors);
        model.addAttribute("categories", categories.findAll());
        model.addAttribute("productImages", java.util.List.of());
        model.addAttribute("uploadMaxBytes", maxBytes);
        model.addAttribute("adminNav", AdminPageAdvice.NAV);
        model.addAttribute("adminActive", "products");
        model.addAttribute("adminTitle", "Chi tiết sản phẩm");
        return "admin/san-pham/bieu-mau";
    }
}
