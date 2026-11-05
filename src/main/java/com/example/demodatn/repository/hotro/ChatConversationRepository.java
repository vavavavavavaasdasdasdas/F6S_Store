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
    Optional<ChatConversation> findByCustomer_Id(Integer customerId);
    Optional<ChatConversation> findByTokenHash(String hash);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ChatConversation c where c.id=:id")
    Optional<ChatConversation> lock(@Param("id") Long id);
    @Modifying
    @Query("delete from ChatConversation c where (c.human=true or c.employeeId is not null or c.waiting=true) and coalesce(c.lastMessageAt,c.updatedAt)<:cutoff")
    int deleteHumanBefore(@Param("cutoff") LocalDateTime cutoff);
    @Modifying
    @Query("delete from ChatConversation c where c.human=false and c.employeeId is null and c.waiting=false and coalesce(c.lastMessageAt,c.updatedAt)<:cutoff")
    int deleteAutomaticBefore(@Param("cutoff") LocalDateTime cutoff);
}
