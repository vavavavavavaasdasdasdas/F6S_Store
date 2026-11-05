package com.example.demodatn.controller.admin.sanpham;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping("/admin/inventory")
public class AdminInventoryController {

    @GetMapping
    public RedirectView list(HttpServletRequest request) {
        String qs = request.getQueryString();
        RedirectView rv = new RedirectView("/admin/variants" + (qs != null && !qs.isBlank() ? "?" + qs : ""), true);
        rv.setStatusCode(HttpStatus.FOUND); // 302
        return rv;
    }

    @GetMapping("/adjust/{id}")
    public RedirectView adjust(@PathVariable Integer id) {
        RedirectView rv = new RedirectView("/admin/variants/adjust/" + id, true);
        rv.setStatusCode(HttpStatus.FOUND); // 302
        return rv;
    }

    @PostMapping("/adjust")
    public RedirectView saveAdjust() {
        RedirectView rv = new RedirectView("/admin/variants", true);
        rv.setStatusCode(HttpStatus.FOUND); // 302
        return rv;
    }

    @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST})
    public RedirectView fallback(HttpServletRequest request) {
        String qs = request.getQueryString();
        RedirectView rv = new RedirectView("/admin/variants" + (qs != null && !qs.isBlank() ? "?" + qs : ""), true);
        rv.setStatusCode(HttpStatus.FOUND); // 302
        return rv;
    }
}
