package com.example.demodatn.repository.sanpham;
import com.example.demodatn.entity.sanpham.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

public interface SizeRepository extends JpaRepository<Size, Integer>, JpaSpecificationExecutor<Size> {
    boolean existsBySizeValueIgnoreCaseAndIdNot(String value, Integer id);
    Page<Size> findBySizeValueContainingIgnoreCase(String sizeValue, Pageable pageable);
    List<Size> findByStatusTrueOrderByIdAsc();
    List<Size> findByStatusTrueOrderBySizeValueAsc();
}
