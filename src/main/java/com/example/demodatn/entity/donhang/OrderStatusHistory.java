package com.example.demodatn.entity.donhang;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity @Table(name="order_status_history") @Getter @Setter
public class OrderStatusHistory {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="order_id",nullable=false) private Order order;
 @org.hibernate.annotations.Nationalized @Column(nullable=false) private String status;
 @Column(name="changed_at",nullable=false) private LocalDateTime changedAt;
 @org.hibernate.annotations.Nationalized @Column(nullable=false,length=100) private String actor;
 @org.hibernate.annotations.Nationalized @Column(length=1000) private String note;
 @org.hibernate.annotations.Nationalized @Column(length=100) private String carrier;
 @org.hibernate.annotations.Nationalized @Column(name="tracking_code",length=100) private String trackingCode;
}
