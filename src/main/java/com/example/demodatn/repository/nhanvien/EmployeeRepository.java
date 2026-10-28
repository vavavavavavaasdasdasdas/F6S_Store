package com.example.demodatn.repository.nhanvien;
import com.example.demodatn.entity.nhanvien.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Integer>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Employee> {
    @org.springframework.data.jpa.repository.Query("select distinct e.position from Employee e where e.position is not null and trim(e.position) <> '' order by e.position")
    java.util.List<String> distinctPositions();
    java.util.Optional<Employee> findByUser_Id(Integer userId);
    java.util.Optional<Employee> findByUser_Username(String username);
    boolean existsByEmailIgnoreCaseAndIdNot(String email,Integer id);
    boolean existsByPhoneAndIdNot(String phone,Integer id);
    boolean existsByEmployeeCodeIgnoreCaseAndIdNot(String code,Integer id);
    org.springframework.data.domain.Page<Employee> findByFullNameContainingIgnoreCaseOrPhoneContainingOrEmailContainingIgnoreCase(
            String name,String phone,String email,org.springframework.data.domain.Pageable pageable);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Employee p where p.id=:id")
    java.util.Optional<Employee> findForUpdate(@org.springframework.data.repository.query.Param("id") Integer id);
}
