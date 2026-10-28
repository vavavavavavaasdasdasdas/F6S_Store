package com.example.demodatn.repository.sanpham;
import com.example.demodatn.entity.sanpham.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);
    Page<Category> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
