package com.example.demodatn.entity.khachhang;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "customer_code", unique = true)
    private String customerCode;

    @Column(name = "full_name", nullable = false)
    @org.hibernate.annotations.Nationalized private String fullName;

    @Column(unique = true)
    @org.hibernate.annotations.Nationalized private String phone;

    @org.hibernate.annotations.Nationalized private String email;

    @org.hibernate.annotations.Nationalized private String address;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    private Boolean status = true;
    @Column(name="danh_gia_cua_so",insertable=false,updatable=false) private LocalDateTime reviewWindow;
    @Column(name="danh_gia_so_lan",nullable=false,insertable=false,updatable=false) private Integer reviewCount=0;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private com.example.demodatn.entity.nhanvien.User user;
    @Column(name="so_cccd",length=12) @ToString.Exclude private String soCccd;
    @Column(name="ngay_sinh") private java.time.LocalDate ngaySinh;
    @org.hibernate.annotations.Nationalized @Column(name="gioi_tinh",length=10) private String gioiTinh;
    public String getMaskedSoCccd(){return soCccd==null || soCccd.isBlank()?"Chưa có":"***"+soCccd.substring(Math.max(0,soCccd.length()-4));}
}

