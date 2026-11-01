package com.example.demodatn.repository.donhang;
import com.example.demodatn.entity.donhang.PosHeldOrder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PosHeldOrderRepository extends JpaRepository<PosHeldOrder,Long> {
    long countByStaffUser_IdAndOrderTypeAndExpiresAtAfter(Integer staffId,String orderType,LocalDateTime now);
    long countByStaffUser_IdAndExpiresAtAfter(Integer staffId,LocalDateTime now);
    List<PosHeldOrder> findByStaffUser_UsernameAndExpiresAtAfterOrderByCreatedAtAsc(String username,LocalDateTime now);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from PosHeldOrder h where h.id=:id and h.staffUser.username=:username and h.expiresAt>:now")
    Optional<PosHeldOrder> lockActive(@Param("id") Long id,@Param("username") String username,@Param("now") LocalDateTime now);
    List<PosHeldOrder> findByExpiresAtLessThanEqual(LocalDateTime now);
}
