package com.example.demodatn.repository.nhanvien;
import com.example.demodatn.entity.nhanvien.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);
    boolean existsByUsernameIgnoreCaseAndIdNot(String username,Integer id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from User u where u.username=:username")
    Optional<User> lockByUsername(@org.springframework.data.repository.query.Param("username") String username);

    @org.springframework.data.jpa.repository.Query("select count(u) from User u where u.id<>:id and (lower(u.email)=lower(:email) or u.id in (select e.user.id from Employee e where lower(e.email)=lower(:email)) or u.id in (select c.user.id from Customer c where lower(c.email)=lower(:email)))")
    long otherAccountsWithEmail(@org.springframework.data.repository.query.Param("email") String email,
            @org.springframework.data.repository.query.Param("id") Integer id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from User u where u.id=:id")
    Optional<User> lockById(@org.springframework.data.repository.query.Param("id") Integer id);
}
