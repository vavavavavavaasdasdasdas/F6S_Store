package com.example.demodatn.entity.donhang;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="vung_giao_hang") @Getter @Setter
public class ShippingRegion {
 @Id @org.hibernate.annotations.Nationalized @Column(name="ma",length=10) private String code;
 @org.hibernate.annotations.Nationalized @Column(name="ten",nullable=false,length=100) private String name;
 @Column(name="ngay_min",nullable=false) private int minDays;
 @Column(name="ngay_max",nullable=false) private int maxDays;
}
