package com.example.demodatn.entity.sanpham;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity @Table(name="yeu_thich") @IdClass(Favorite.Key.class) @Getter @Setter
public class Favorite {
 @Data @NoArgsConstructor @AllArgsConstructor public static class Key implements java.io.Serializable {private Integer customerId;private Integer productId;}
 @Id @Column(name="customer_id",nullable=false) private Integer customerId;
 @Id @Column(name="product_id",nullable=false) private Integer productId;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="product_id",insertable=false,updatable=false) private Product product;
 @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
}
