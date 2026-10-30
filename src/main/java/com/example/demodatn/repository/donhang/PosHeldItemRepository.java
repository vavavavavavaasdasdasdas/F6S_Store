package com.example.demodatn.repository.donhang;

import com.example.demodatn.entity.donhang.PosHeldItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PosHeldItemRepository extends JpaRepository<PosHeldItem,Long> {
    boolean existsByVariant_Id(Integer variantId);
}
