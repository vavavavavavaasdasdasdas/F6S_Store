package com.example.demodatn.repository.hotro;

import com.example.demodatn.entity.hotro.AdminChatAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AdminChatAuditRepository extends JpaRepository<AdminChatAudit, Long> {

    Page<AdminChatAudit> findByUsernameOrderByThoiGianDesc(String username, Pageable pageable);

    @Modifying
    @Query("DELETE FROM AdminChatAudit a WHERE a.thoiGian < :cutoff")
    int deleteOldAudits(@Param("cutoff") LocalDateTime cutoff);
}
