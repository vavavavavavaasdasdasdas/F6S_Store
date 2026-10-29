package com.example.demodatn.controller.user;

import com.example.demodatn.dto.nhanvien.AccountPasswordForm;
import com.example.demodatn.security.CustomUserDetails;
import com.example.demodatn.security.UserDetailsServiceImpl;
import com.example.demodatn.service.nhanvien.AdminAccountService;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller @RequiredArgsConstructor
public class RequiredPasswordController {
    private final AdminAccountService accounts;
    private final UserDetailsServiceImpl users;
    @GetMapping("/account/change-required")
    public String page(Authentication auth,Model model) {
        if(!Boolean.TRUE.equals(accounts.user(auth.getName()).getMustChangePassword()))return "redirect:/";
        model.addAttribute("passwordForm",new AccountPasswordForm());return "auth/doi-mat-khau-bat-buoc";
    }
    @PostMapping("/account/change-required")
    public String save(@Valid @ModelAttribute("passwordForm") AccountPasswordForm form,BindingResult errors,
                       Authentication auth,HttpServletRequest request,HttpServletResponse response) {
        if(!Boolean.TRUE.equals(accounts.user(auth.getName()).getMustChangePassword()))return "redirect:/";
        if(!errors.hasErrors())accounts.password(auth.getName(),form,errors);
        form.setCurrentPassword(null);form.setNewPassword(null);form.setConfirmPassword(null);
        if(errors.hasErrors())return "auth/doi-mat-khau-bat-buoc";
        var fresh=(CustomUserDetails)users.loadUserByUsername(auth.getName());
        var context=SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(fresh,null,fresh.getAuthorities()));
        SecurityContextHolder.setContext(context);request.changeSessionId();
        new HttpSessionSecurityContextRepository().saveContext(context,request,response);
        new HttpSessionRequestCache().removeRequest(request,response);
        return fresh.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN"))?"redirect:/admin":"redirect:/";
    }
}
