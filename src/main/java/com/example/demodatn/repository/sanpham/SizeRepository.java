package com.example.demodatn.repository.sanpham;
import com.example.demodatn.entity.sanpham.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SizeRepository extends JpaRepository<Size, Integer> {
    boolean existsBySizeValueIgnoreCaseAndIdNot(String value, Integer id);
    Page<Size> findBySizeValueContainingIgnoreCase(String sizeValue, Pageable pageable);
}
