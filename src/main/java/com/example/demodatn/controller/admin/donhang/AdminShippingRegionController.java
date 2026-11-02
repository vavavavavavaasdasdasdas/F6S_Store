package com.example.demodatn.controller.admin.donhang;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminShippingRegionController {

    @GetMapping({"/admin/shipping-config", "/admin/shipping-config/**", "/admin/shipping-times", "/admin/shipping-times/**"})
    public String legacyRedirect() {
        return "redirect:/admin/dashboard";
    }
}
