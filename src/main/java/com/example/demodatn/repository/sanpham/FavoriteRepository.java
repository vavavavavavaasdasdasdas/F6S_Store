package com.example.demodatn.repository.sanpham;
import com.example.demodatn.entity.sanpham.Favorite;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import java.util.*;
public interface FavoriteRepository extends JpaRepository<Favorite,Favorite.Key>,JpaSpecificationExecutor<Favorite> {
 @Query("select f.productId from Favorite f where f.customerId=:customerId") Set<Integer> productIds(Integer customerId);
 long countByProductId(Integer productId);
 @Override @EntityGraph(attributePaths={"product","product.category"}) Page<Favorite> findAll(Specification<Favorite> spec,Pageable page);
}
