package com.example.demodatn.repository.khachhang;
import com.example.demodatn.entity.khachhang.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Integer>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Customer> {
    java.util.Optional<Customer> findByUser_Id(Integer userId);
    java.util.Optional<Customer> findByUser_Username(String username);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from Customer c join c.user u where u.username=:username")
    java.util.Optional<Customer> findForUpdateByUsername(@org.springframework.data.repository.query.Param("username") String username);
    boolean existsByEmailIgnoreCaseAndIdNot(String email,Integer id);
    boolean existsByPhoneAndIdNot(String phone,Integer id);
    boolean existsByCustomerCodeIgnoreCaseAndIdNot(String code,Integer id);
    org.springframework.data.domain.Page<Customer> findByFullNameContainingIgnoreCaseOrPhoneContainingOrEmailContainingIgnoreCase(
            String name,String phone,String email,org.springframework.data.domain.Pageable pageable);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Customer p where p.id=:id")
    java.util.Optional<Customer> findForUpdate(@org.springframework.data.repository.query.Param("id") Integer id);
    java.util.Optional<Customer> findByEmail(String email);
    java.util.Optional<Customer> findByPhone(String phone);
    java.util.List<Customer> findByPhoneInOrderByIdAsc(java.util.Collection<String> phones);
}
