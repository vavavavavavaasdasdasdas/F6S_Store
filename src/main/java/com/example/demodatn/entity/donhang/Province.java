package com.example.demodatn.entity.donhang;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="tinh_thanh") @Getter @Setter
public class Province {
 @org.hibernate.annotations.Nationalized @Column(name="ma_hanh_chinh",length=2) private String administrativeCode;
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
 @org.hibernate.annotations.Nationalized @Column(name="ten",nullable=false,length=100) private String name;
 @ManyToOne(fetch=FetchType.EAGER,optional=false) @JoinColumn(name="vung",nullable=false) private ShippingRegion region;
}
