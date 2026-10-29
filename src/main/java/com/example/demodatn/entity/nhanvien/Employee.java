package com.example.demodatn.entity.nhanvien;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "employees")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "employee_code", unique = true)
    private String employeeCode;

    @Column(name = "full_name", nullable = false)
    @org.hibernate.annotations.Nationalized
    private String fullName;

    @Column(unique = true)
    @org.hibernate.annotations.Nationalized private String phone;

    @org.hibernate.annotations.Nationalized private String email;

    @org.hibernate.annotations.Nationalized private String address;

    @org.hibernate.annotations.Nationalized private String position;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private Boolean status = true;
    @Column(name="so_cccd",length=12) @ToString.Exclude private String soCccd;
    @Column(name="ngay_sinh") private java.time.LocalDate ngaySinh;
    @org.hibernate.annotations.Nationalized @Column(name="gioi_tinh",length=10) private String gioiTinh;
    public String getMaskedSoCccd(){return soCccd==null || soCccd.isBlank()?"Chưa có":"***"+soCccd.substring(Math.max(0,soCccd.length()-4));}
}
