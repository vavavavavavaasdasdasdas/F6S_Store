package com.example.demodatn.repository.donhang;
import com.example.demodatn.entity.donhang.Province;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ProvinceRepository extends JpaRepository<Province,Integer> { java.util.List<Province> findAllByOrderByNameAsc(); }
