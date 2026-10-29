package com.example.demodatn.repository.nhanvien;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository @RequiredArgsConstructor
public class AccountIssueRepository {
    private final JdbcTemplate jdbc;

    // Serializable scans also cover old profiles without accounts; no email normalization migration is needed.
    public boolean emailUsed(String email,int customerId,int employeeId,int userId) {
        long users=jdbc.queryForObject("SELECT COUNT_BIG(*) FROM users WITH(UPDLOCK,HOLDLOCK) WHERE LOWER(LTRIM(RTRIM(email)))=? AND id<>?",Long.class,email,userId);
        long customers=jdbc.queryForObject("SELECT COUNT_BIG(*) FROM customers WITH(UPDLOCK,HOLDLOCK) WHERE LOWER(LTRIM(RTRIM(email)))=? AND id<>? AND (user_id IS NULL OR user_id<>?)",Long.class,email,customerId,userId);
        long employees=jdbc.queryForObject("SELECT COUNT_BIG(*) FROM employees WITH(UPDLOCK,HOLDLOCK) WHERE LOWER(LTRIM(RTRIM(email)))=? AND id<>? AND (user_id IS NULL OR user_id<>?)",Long.class,email,employeeId,userId);
        return users+customers+employees>0;
    }

    public boolean documentUsed(String table,String document,int id) {
        if(!java.util.Set.of("customers","employees").contains(table))throw new IllegalArgumentException();
        return jdbc.queryForObject("SELECT COUNT_BIG(*) FROM "+table+" WITH(UPDLOCK,HOLDLOCK) WHERE so_cccd=? AND id<>?",Long.class,document,id)>0;
    }

    public void delivery(int userId,String passwordHash,boolean sent) {
        jdbc.update("UPDATE users SET credential_mail_failed=?,credential_mail_pending_until=NULL WHERE id=? AND password=?",!sent,userId,passwordHash);
    }
}
