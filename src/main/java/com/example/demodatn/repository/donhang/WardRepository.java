package com.example.demodatn.repository.donhang;
import com.example.demodatn.entity.donhang.Ward;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
public interface WardRepository extends JpaRepository<Ward,Integer> {
    @EntityGraph(attributePaths={"province","province.region"}) List<Ward> findByProvince_IdOrderByNameAsc(Integer provinceId);
    @Modifying @Transactional @Query("update Ward w set w.latitude=:lat,w.longitude=:lng,w.geocodedAt=:at,w.geocodeRetryAfter=null where w.id=:id and w.latitude is null")
    int storeCoordinates(@Param("id") Integer id,@Param("lat") BigDecimal lat,@Param("lng") BigDecimal lng,@Param("at") LocalDateTime at);
    @Modifying @Transactional @Query("update Ward w set w.geocodeRetryAfter=:after where w.id=:id and w.latitude is null")
    int retryAfter(@Param("id") Integer id,@Param("after") LocalDateTime after);
}
