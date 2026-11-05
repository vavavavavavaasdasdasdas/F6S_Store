package com.example.demodatn.repository.hotro;

import com.example.demodatn.entity.hotro.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT m FROM ChatMessage m WHERE m.conversation.sessionId = :sessionId ORDER BY m.createdAt DESC")
    List<ChatMessage> findRecentBySessionId(@Param("sessionId") String sessionId, Pageable pageable);

    @Query("SELECT m FROM ChatMessage m WHERE m.conversation.sessionId = :sessionId ORDER BY m.createdAt ASC")
    List<ChatMessage> findAllBySessionId(@Param("sessionId") String sessionId);
    @Query("select m from ChatMessage m where m.conversation.id=:id and (:before is null or m.id<:before) order by m.id desc")
    List<ChatMessage> history(@Param("id") Long id,@Param("before") Long before,Pageable page);
    java.util.Optional<ChatMessage> findByConversation_IdAndSenderTypeAndClientId(Long id,String senderType,String clientId);
    @Query("select m from ChatMessage m join m.conversation c left join c.customer owner where (c.id=:id or owner.id=:owner or c.legacyCustomerId=:owner) and (:before is null or m.id<:before) order by m.id desc")
    List<ChatMessage> historyForCustomer(@Param("id") Long id,@Param("owner") Integer owner,@Param("before") Long before,Pageable page);
}
