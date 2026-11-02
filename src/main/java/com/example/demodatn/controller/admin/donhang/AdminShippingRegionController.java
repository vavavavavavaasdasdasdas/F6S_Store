package com.example.demodatn.controller.admin.donhang;
import com.example.demodatn.dto.donhang.ShippingSettingsForm;
import com.example.demodatn.service.donhang.ShippingFeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AdminShippingRegionController {
    private final ShippingFeeService shipping;

    @ModelAttribute("shippingSettings")
    public ShippingSettingsForm settingsForm() {
        var form = new ShippingSettingsForm();
        BeanUtils.copyProperties(shipping.settings(), form);
        return form;
    }

    @GetMapping("/admin/shipping-config")
    public String config(Model model) {
        return "admin/giao-hang/cau-hinh";
    }

    @PostMapping({"/admin/shipping-config", "/admin/shipping-config/settings"})
    public String settings(@Valid @ModelAttribute("shippingSettings") ShippingSettingsForm form,
                           BindingResult errors,
                           Model model,
                           RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                shipping.update(form);
                flash.addFlashAttribute("message", "Đã cập nhật kho và các nấc phí. Đơn cũ giữ phí đã lưu.");
                return "redirect:/admin/shipping-config";
            } catch (com.example.demodatn.exception.PeopleValidationException ex) {
                errors.rejectValue(ex.getField(), "invalid", ex.getMessage());
            } catch (IllegalArgumentException ex) {
                errors.rejectValue("secondLimitKm", "order", ex.getMessage());
            }
        }
        return "admin/giao-hang/cau-hinh";
    }

    @GetMapping({"/admin/shipping-times", "/admin/shipping-times/**"})
    public String legacyRedirect() {
        return "redirect:/admin/shipping-config";
    }
}
