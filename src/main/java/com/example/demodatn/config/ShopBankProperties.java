package com.example.demodatn.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data @Component @ConfigurationProperties(prefix="shop.bank")
public class ShopBankProperties {
    private String bin="";
    private String account="";
    private String accountName="";
    private String name="";
}
