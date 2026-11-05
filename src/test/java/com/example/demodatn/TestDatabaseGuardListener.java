package com.example.demodatn;

import org.springframework.core.Ordered;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

public class TestDatabaseGuardListener implements TestExecutionListener, Ordered {
    public static final String PRODUCTION_DB_NAME = "shopbangiay";

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void prepareTestInstance(TestContext testContext) throws Exception {
        verify(testContext);
    }

    @Override
    public void beforeTestClass(TestContext testContext) throws Exception {
        verify(testContext);
    }

    @Override
    public void beforeTestMethod(TestContext testContext) throws Exception {
        verify(testContext);
    }

    private void verify(TestContext testContext) throws Exception {
        var env = testContext.getApplicationContext().getEnvironment();
        String url = env.getProperty("spring.datasource.url");
        if (url != null) {
            String lower = url.toLowerCase();
            if (lower.contains("databasename=" + PRODUCTION_DB_NAME + ";") 
                    || lower.contains("databasename=" + PRODUCTION_DB_NAME + "&")
                    || lower.endsWith("databasename=" + PRODUCTION_DB_NAME)) {
                throw new IllegalStateException("CH?T CH?N B?O V?: B? test t? ch?i ch?y tr?n database ch?nh th?c '" 
                    + PRODUCTION_DB_NAME + "'! H?y c?u h?nh database t?m ?? ki?m th?.");
            }
        }
        var context = testContext.getApplicationContext();
        if (context.containsBean("dataSource")) {
            DataSource ds = context.getBean(DataSource.class);
            try (Connection conn = ds.getConnection()) {
                String dbName = conn.getCatalog();
                if (dbName == null || dbName.isBlank()) {
                    try (var stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT DB_NAME()")) {
                        if (rs.next()) dbName = rs.getString(1);
                    }
                }
                if (dbName != null && dbName.trim().equalsIgnoreCase(PRODUCTION_DB_NAME)) {
                    throw new IllegalStateException("CH?T CH?N B?O V?: B? test t? ch?i ch?y tr?n database ch?nh th?c '" 
                        + PRODUCTION_DB_NAME + "'! H?y c?u h?nh database t?m ?? ki?m th?.");
                }
            }
        }
    }
}
