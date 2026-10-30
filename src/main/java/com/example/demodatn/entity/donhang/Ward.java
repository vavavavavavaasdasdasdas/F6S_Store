package com.example.demodatn.entity.donhang;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name="phuong_xa") @Getter @Setter
public class Ward {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
    @org.hibernate.annotations.Nationalized @Column(name="ma",nullable=false,length=5) private String code;
    @org.hibernate.annotations.Nationalized @Column(name="ten",nullable=false,length=100) private String name;
    @ManyToOne(fetch=FetchType.EAGER,optional=false) @JoinColumn(name="tinh_thanh_id",nullable=false) private Province province;
    @Column(precision=10,scale=7) private BigDecimal latitude;
    @Column(precision=10,scale=7) private BigDecimal longitude;
    @Column(name="geocoded_at") private LocalDateTime geocodedAt;
    @Column(name="geocode_retry_after") private LocalDateTime geocodeRetryAfter;
}
