package com.example.demodatn.repository.hotro;

import com.example.demodatn.entity.hotro.AdminChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminChatMessageRepository extends JpaRepository<AdminChatMessage, Long> {

    @Query("SELECT m FROM AdminChatMessage m WHERE m.conversation.sessionId = :sessionId ORDER BY m.createdAt DESC")
    List<AdminChatMessage> findRecentBySessionId(@Param("sessionId") String sessionId, Pageable pageable);

    @Query("SELECT m FROM AdminChatMessage m WHERE m.conversation.sessionId = :sessionId ORDER BY m.createdAt ASC")
    List<AdminChatMessage> findAllBySessionId(@Param("sessionId") String sessionId);
}
