package com.example.demodatn.entity.sanpham;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@org.hibernate.annotations.DynamicUpdate
@Entity @Table(name="danh_gia") @Getter @Setter
public class ProductReview {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
 @Column(name="ma",length=20,nullable=false,insertable=false,updatable=false) @org.hibernate.annotations.Generated(event=org.hibernate.generator.EventType.INSERT) private String code;
 @ManyToOne(optional=false) @JoinColumn(name="product_id",nullable=false) private Product product;
 @ManyToOne(optional=false) @JoinColumn(name="customer_id",nullable=false) private com.example.demodatn.entity.khachhang.Customer customer;
 @ManyToOne(optional=false) @JoinColumn(name="order_id",nullable=false) private com.example.demodatn.entity.donhang.Order order;
 @Column(name="so_sao",nullable=false) private Integer stars;
 @org.hibernate.annotations.Nationalized @Column(name="noi_dung",nullable=false,length=1000) private String content;
 @Column(name="hien_thi",nullable=false) private boolean visible=true;
 @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
 @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
 public String getMaskedCustomerName(){String name=customer.getFullName();return name==null||name.isBlank()?"Khách hàng":name.substring(0,name.offsetByCodePoints(0,1))+"***";}
}
