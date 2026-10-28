package com.example.demodatn.repository.sanpham;
import com.example.demodatn.entity.sanpham.Material;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MaterialRepository extends JpaRepository<Material, Integer>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Material> {
    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);
    boolean existsByName(String name);
    List<Material> findByStatusTrueOrderByNameAscIdAsc();
    Page<Material> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
