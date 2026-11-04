package com.example.demodatn.repository.hotro;

import com.example.demodatn.entity.hotro.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    Optional<ChatConversation> findBySessionId(String sessionId);

    @Modifying
    @Query("DELETE FROM ChatConversation c WHERE c.createdAt < :cutoff")
    int deleteOldConversations(@Param("cutoff") LocalDateTime cutoff);
}
