package com.example.demodatn.repository.sanpham;

import com.example.demodatn.entity.sanpham.Color;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ColorRepository extends JpaRepository<Color,Integer>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Color> {
    List<Color> findByStatusTrueOrderByNameAscIdAsc();
    boolean existsByNameIgnoreCaseAndIdNot(String name,Integer id);
    org.springframework.data.domain.Page<Color> findByNameContainingIgnoreCase(String name,org.springframework.data.domain.Pageable page);
}
