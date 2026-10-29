package com.example.demodatn.repository.sanpham;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

@Repository @RequiredArgsConstructor
public class ProductCodeRepository {
    private final JdbcTemplate jdbc;
    @Transactional(propagation = Propagation.MANDATORY)
    public String next() {
        Long number = jdbc.queryForObject("SELECT NEXT VALUE FOR dbo.product_code_seq", Long.class);
        return String.format(Locale.ROOT, "SP%03d", number);
    }
}
