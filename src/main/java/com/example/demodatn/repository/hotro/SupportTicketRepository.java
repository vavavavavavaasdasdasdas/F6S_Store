package com.example.demodatn.repository.hotro;

import com.example.demodatn.entity.hotro.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Integer> {

    Optional<SupportTicket> findByMa(String ma);

    long countByTrangThai(String trangThai);

    @Query("SELECT t FROM SupportTicket t WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(t.ma) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(t.hoTen) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(t.lienHe) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(t.noiDung) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:trangThai IS NULL OR :trangThai = '' OR t.trangThai = :trangThai) AND " +
           "(:fromDate IS NULL OR t.createdAt >= :fromDate) AND " +
           "(:toDate IS NULL OR t.createdAt <= :toDate)")
    Page<SupportTicket> searchTickets(@Param("keyword") String keyword,
                                      @Param("trangThai") String trangThai,
                                      @Param("fromDate") LocalDateTime fromDate,
                                      @Param("toDate") LocalDateTime toDate,
                                      Pageable pageable);

    @Query("SELECT MAX(CAST(SUBSTRING(t.ma, 3, LEN(t.ma) - 2) AS int)) FROM SupportTicket t WHERE t.ma LIKE 'HT%'")
    Integer findMaxCodeNumber();
}
