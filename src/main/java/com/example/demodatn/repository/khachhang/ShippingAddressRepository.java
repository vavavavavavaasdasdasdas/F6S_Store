package com.example.demodatn.repository.khachhang;

import com.example.demodatn.entity.khachhang.ShippingAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ShippingAddressRepository extends JpaRepository<ShippingAddress,Integer> {
    List<ShippingAddress> findByCustomer_IdOrderBySlotAsc(Integer customerId);
    Optional<ShippingAddress> findByIdAndCustomer_Id(Integer id,Integer customerId);
    long countByCustomer_Id(Integer customerId);
}
