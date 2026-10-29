package com.example.demodatn.service;

import com.example.demodatn.config.IdentityLogMask;
import com.example.demodatn.service.nhanvien.AccountProvisioningService;
import com.example.demodatn.util.PasswordPolicy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AccountCredentialPrivacyTest {
    @Test void generatedCredentialsAlwaysMeetTheSharedPolicy() {
        var generated=new java.util.HashSet<String>();
        for(int i=0;i<1000;i++) {
            String secret=AccountProvisioningService.temporaryPassword();
            assertTrue(secret.length()==12&&PasswordPolicy.isValid(secret),"Generated credential violates the shared policy");
            assertTrue(generated.add(secret),"Repeated generated credential");
        }
    }
    @Test void identityNumbersAreMaskedInExceptionAndMessageText() {
        String original="Duplicate key (001234567890), cause: rejected 009876543210";
        String masked=new IdentityLogMask().transform(null,original);
        assertFalse(masked.contains("001234567890"));assertFalse(masked.contains("009876543210"));
        assertTrue(masked.contains("***7890")&&masked.contains("***3210"));
    }
}
