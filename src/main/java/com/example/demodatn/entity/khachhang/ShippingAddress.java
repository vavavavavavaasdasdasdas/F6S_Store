package com.example.demodatn.entity.khachhang;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name="dia_chi_nhan_hang") @Getter @Setter
public class ShippingAddress {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="customer_id",nullable=false) private Customer customer;
    @org.hibernate.annotations.Nationalized @Column(name="ten_nguoi_nhan",nullable=false,length=100) private String receiverName;
    @Column(name="so_dien_thoai",nullable=false,length=20) private String phone;
    @org.hibernate.annotations.Nationalized @Column(name="dia_chi",nullable=false,length=255) private String address;
    @Column(name="la_mac_dinh",nullable=false) private boolean defaultAddress;
    @Column(name="thu_tu",nullable=false,columnDefinition="tinyint") private Integer slot;
    @ManyToOne @JoinColumn(name="tinh_thanh_id") private com.example.demodatn.entity.donhang.Province province;

    @Column(precision=10,scale=7) private java.math.BigDecimal latitude;
    @Column(precision=10,scale=7) private java.math.BigDecimal longitude;
    @org.hibernate.annotations.Nationalized @Column(name="nguon_toa_do",length=20) private String coordinateSource;
    @org.hibernate.annotations.Nationalized @Column(name="so_nha_duong",length=255) private String streetAddress;
    @ManyToOne @JoinColumn(name="phuong_xa_id") private com.example.demodatn.entity.donhang.Ward ward;
    public String getDisplayStreetAddress(){return streetAddress==null?address:streetAddress;}
    public String getFullAddress(){return ward==null?address:getDisplayStreetAddress()+", "+ward.getName()+", "+ward.getProvince().getName();}
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
}
