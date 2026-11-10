package com.example.demodatn.controller.admin.nhanvien;

import com.example.demodatn.dto.nhanvien.AccountPasswordForm;
import com.example.demodatn.dto.nhanvien.AccountProfileForm;
import com.example.demodatn.service.nhanvien.AccountMailService;
import com.example.demodatn.service.nhanvien.AdminAccountService;
import java.security.Principal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AdminPageController {
    private final AdminAccountService accounts;
    private final AccountMailService mailService;

    private void populate(Principal principal, Model model) {
        var account = accounts.user(principal.getName());
        var profile = accounts.profile(account);
        model.addAttribute("account", account);
        model.addAttribute("accountProfile", profile);
        model.addAttribute("mailConfigured", mailService.isConfigured());
        model.addAttribute("mailStatusText", mailService.getStatusText());
        model.addAttribute("mailHost", mailService.getResolvedHost());
        model.addAttribute("mailPort", mailService.getResolvedPort());
        model.addAttribute("mailFrom", mailService.getResolvedFrom());
        model.addAttribute("adminEmail", profile != null && profile.getEmail() != null && !profile.getEmail().isBlank()
                ? profile.getEmail().trim()
                : (account.getEmail() != null ? account.getEmail() : ""));
        if (!model.containsAttribute("profileForm")) model.addAttribute("profileForm", profile);
        if (!model.containsAttribute("passwordForm")) model.addAttribute("passwordForm", new AccountPasswordForm());
    }

    @GetMapping("/admin/account")
    public String account(Principal principal, Model model) {
        populate(principal, model);
        return "admin/tai-khoan/tai-khoan";
    }

    @PostMapping("/admin/account/profile")
    public String profile(@Valid @ModelAttribute("profileForm") AccountProfileForm form, BindingResult errors,
                          Principal principal, Model model, RedirectAttributes flash) {
        accounts.update(principal.getName(), form, errors);
        if (errors.hasErrors()) {
            populate(principal, model);
            return "admin/tai-khoan/tai-khoan";
        }
        flash.addFlashAttribute("success", "Đã cập nhật thông tin tài khoản.");
        return "redirect:/admin/account";
    }

    @PostMapping("/admin/account/password")
    public String password(@Valid @ModelAttribute("passwordForm") AccountPasswordForm form, BindingResult errors,
                           Principal principal, Model model, RedirectAttributes flash) {
        if (!errors.hasErrors()) accounts.password(principal.getName(), form, errors);
        if (errors.hasErrors()) {
            populate(principal, model);
            return "admin/tai-khoan/tai-khoan";
        }
        flash.addFlashAttribute("success", "Đổi mật khẩu thành công.");
        return "redirect:/admin/account";
    }

    @PostMapping("/admin/account/test-email")
    public String testEmail(@RequestParam(value = "recipientEmail", required = false) String recipientEmail,
                            Principal principal, RedirectAttributes flash) {
        if (principal == null) return "redirect:/login";
        var user = accounts.user(principal.getName());
        if (user == null || user.getRole() == null || !"ROLE_ADMIN".equals(user.getRole().getName())) {
            throw new org.springframework.security.access.AccessDeniedException("Chỉ quản trị viên mới được gửi email thử.");
        }
        String targetEmail = recipientEmail;
        if (targetEmail == null || targetEmail.isBlank()) {
            var profile = accounts.profile(user);
            targetEmail = profile != null && profile.getEmail() != null && !profile.getEmail().isBlank()
                    ? profile.getEmail().trim()
                    : user.getEmail();
        }
        var result = mailService.sendTestMail(targetEmail);
        if (result.success()) {
            flash.addFlashAttribute("success", result.message());
        } else {
            flash.addFlashAttribute("error", result.message());
        }
        return "redirect:/admin/account";
    }
}
