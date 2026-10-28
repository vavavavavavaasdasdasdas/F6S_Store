package com.example.demodatn.repository.sanpham;

import com.example.demodatn.entity.sanpham.Sole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SoleRepository extends JpaRepository<Sole,Integer>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Sole> {
    List<Sole> findByStatusTrueOrderByNameAscIdAsc();
    boolean existsByNameIgnoreCaseAndIdNot(String name,Integer id);
    org.springframework.data.domain.Page<Sole> findByNameContainingIgnoreCase(String name,org.springframework.data.domain.Pageable page);
}
