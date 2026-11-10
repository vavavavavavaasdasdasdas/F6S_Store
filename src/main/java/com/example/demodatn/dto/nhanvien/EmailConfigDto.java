package com.example.demodatn.dto.nhanvien;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailConfigDto {
    private String host;
    private Integer port;
    private String username;
    private String password;
    private String from;
    private Boolean auth;
    private Boolean starttls;
}
