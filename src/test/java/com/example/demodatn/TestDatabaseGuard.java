package com.example.demodatn;

import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class TestDatabaseGuard {
    public static final String PRODUCTION_DB_NAME = "shopbangiay";

    public static void check(JdbcTemplate jdbc) {
        String dbName = jdbc.queryForObject("SELECT DB_NAME()", String.class);
        assertNotNull(dbName, "T?n database kh?ng ???c null");
        if (PRODUCTION_DB_NAME.equalsIgnoreCase(dbName.trim())) {
            throw new IllegalStateException("CH?T CH?N B?O V?: B? test t? ch?i ch?y tr?n database ch?nh th?c '" 
                + PRODUCTION_DB_NAME + "'! H?y c?u h?nh database t?m ?? ki?m th?.");
        }
    }
}
