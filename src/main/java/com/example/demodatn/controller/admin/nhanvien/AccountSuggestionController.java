package com.example.demodatn.controller.admin.nhanvien;

import com.example.demodatn.service.nhanvien.AccountProvisioningService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequiredArgsConstructor
public class AccountSuggestionController {
    private final AccountProvisioningService accounts;
    @PostMapping("/admin/accounts/username-suggestion")
    public Map<String,String> suggest(@RequestParam String email) {
        return Map.of("username",accounts.suggestUsername(email.length()>100?email.substring(0,100):email));
    }
}
