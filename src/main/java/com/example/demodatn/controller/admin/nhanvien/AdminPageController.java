package com.example.demodatn.controller.admin.nhanvien;

import com.example.demodatn.dto.nhanvien.AccountPasswordForm;
import com.example.demodatn.dto.nhanvien.AccountProfileForm;
import com.example.demodatn.dto.nhanvien.EmailConfigDto;
import com.example.demodatn.service.nhanvien.AccountMailService;
import com.example.demodatn.service.nhanvien.AdminAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class AdminPageController {
    private final AdminAccountService accounts;
    private final AccountMailService mailService;

    private boolean isAdmin(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private void populate(Principal principal, Model model) {
        var account = accounts.user(principal.getName());
        model.addAttribute("account", account);
        model.addAttribute("accountProfile", accounts.profile(account));
        model.addAttribute("mailConfigured", mailService.isConfigured());
        model.addAttribute("mailHost", mailService.getResolvedHost());
        model.addAttribute("mailPort", mailService.getResolvedPort());
        model.addAttribute("mailUsername", mailService.getResolvedUsername());
        model.addAttribute("mailFrom", mailService.getResolvedFrom());
        model.addAttribute("mailAuth", mailService.isResolvedAuth());
        model.addAttribute("mailStarttls", mailService.isResolvedStarttls());
        model.addAttribute("mailHasPassword", mailService.isPasswordConfigured());
        model.addAttribute("mailLastError", mailService.getLastError());

        if (!model.containsAttribute("emailConfigForm")) {
            EmailConfigDto emailDto = new EmailConfigDto();
            emailDto.setHost(mailService.getResolvedHost().isBlank() ? "smtp.gmail.com" : mailService.getResolvedHost());
            emailDto.setPort(mailService.getResolvedPort());
            emailDto.setUsername(mailService.getResolvedUsername());
            emailDto.setFrom(mailService.getResolvedFrom());
            emailDto.setAuth(mailService.isResolvedAuth());
            emailDto.setStarttls(mailService.isResolvedStarttls());
            model.addAttribute("emailConfigForm", emailDto);
        }

        if (!model.containsAttribute("profileForm")) model.addAttribute("profileForm", accounts.profile(account));
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

    @PostMapping("/admin/account/email-config")
    public String emailConfig(@ModelAttribute("emailConfigForm") EmailConfigDto form,
                              Principal principal, RedirectAttributes flash) {
        if (principal == null) return "redirect:/login";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!isAdmin(auth)) {
            throw new AccessDeniedException("Chỉ quản trị viên mới được cấu hình email.");
        }
        var result = mailService.updateConfig(form);
        if (result.success()) {
            flash.addFlashAttribute("success", result.message());
        } else {
            flash.addFlashAttribute("error", result.message());
        }
        return "redirect:/admin/account";
    }

    @PostMapping("/admin/account/test-email")
    public String testEmail(@RequestParam(value = "recipientEmail", required = false) String recipientEmail,
                            Principal principal, RedirectAttributes flash) {
        if (principal == null) return "redirect:/login";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!isAdmin(auth)) {
            throw new AccessDeniedException("Chỉ quản trị viên mới được gửi email thử.");
        }
        String targetEmail = recipientEmail;
        if (targetEmail == null || targetEmail.isBlank()) {
            var user = accounts.user(principal.getName());
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
