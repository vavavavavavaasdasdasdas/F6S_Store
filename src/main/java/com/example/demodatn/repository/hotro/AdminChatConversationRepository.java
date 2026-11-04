package com.example.demodatn.repository.hotro;

import com.example.demodatn.entity.hotro.AdminChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AdminChatConversationRepository extends JpaRepository<AdminChatConversation, Long> {

    Optional<AdminChatConversation> findBySessionId(String sessionId);

    @Modifying
    @Query("DELETE FROM AdminChatConversation c WHERE c.createdAt < :cutoff")
    int deleteOldConversations(@Param("cutoff") LocalDateTime cutoff);
}
