package com.example.demodatn.repository.hotro;

import com.example.demodatn.entity.hotro.ChatbotFaq;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatbotFaqRepository extends JpaRepository<ChatbotFaq, Integer> {

    Optional<ChatbotFaq> findByMa(String ma);

    List<ChatbotFaq> findByHienThiTrue();

    @Query("SELECT f FROM ChatbotFaq f WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(f.cauHoi) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(f.tuKhoa) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(f.ma) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:hienThi IS NULL OR f.hienThi = :hienThi)")
    Page<ChatbotFaq> searchFaq(@Param("keyword") String keyword,
                               @Param("hienThi") Boolean hienThi,
                               Pageable pageable);

    @Query("SELECT MAX(CAST(SUBSTRING(f.ma, 3, LEN(f.ma) - 2) AS int)) FROM ChatbotFaq f WHERE f.ma LIKE 'KB%'")
    Integer findMaxCodeNumber();
}
