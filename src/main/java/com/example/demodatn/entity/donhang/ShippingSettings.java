package com.example.demodatn.entity.donhang;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
@Entity @Table(name="cau_hinh_phi_ship") @Getter @Setter
public class ShippingSettings {
    @Id private Integer id;
    @org.hibernate.annotations.Nationalized @Column(name="dia_chi_kho",nullable=false,length=255) private String originAddress;
    @Column(name="latitude_kho",nullable=false,precision=10,scale=7) private BigDecimal originLatitude;
    @Column(name="longitude_kho",nullable=false,precision=10,scale=7) private BigDecimal originLongitude;
    @Column(name="moc_1_km",nullable=false,precision=10,scale=2) private BigDecimal firstLimitKm;
    @Column(name="moc_2_km",nullable=false,precision=10,scale=2) private BigDecimal secondLimitKm;
    @Column(name="phi_1",nullable=false,precision=18,scale=2) private BigDecimal firstFee;
    @Column(name="phi_2",nullable=false,precision=18,scale=2) private BigDecimal secondFee;
    @Column(name="phi_xa",nullable=false,precision=18,scale=2) private BigDecimal fallbackFee;
    @Column(name="so_luong_mien_phi",nullable=false) private int freeQuantity;
}
