package com.example.demodatn.entity.hotro;

import com.example.demodatn.entity.khachhang.Customer;
import com.example.demodatn.entity.nhanvien.Employee;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "phieu_ho_tro")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String ma;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khach_id")
    private Customer customer;

    @org.hibernate.annotations.Nationalized
    @Column(name = "ho_ten", nullable = false, length = 100)
    private String hoTen;

    @org.hibernate.annotations.Nationalized
    @Column(name = "lien_he", nullable = false, length = 100)
    private String lienHe;

    @org.hibernate.annotations.Nationalized
    @Column(name = "noi_dung", nullable = false, length = 1000)
    private String noiDung;

    @org.hibernate.annotations.Nationalized
    @Column(name = "trich_doan_chat", columnDefinition = "nvarchar(max)")
    private String trichDoanChat;

    @org.hibernate.annotations.Nationalized
    @Column(name = "trang_thai", nullable = false, length = 50)
    private String trangThai = "Mới";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "xu_ly_boi")
    private Employee employee;

    @org.hibernate.annotations.Nationalized
    @Column(name = "tra_loi", columnDefinition = "nvarchar(max)")
    private String traLoi;

    @Column(name = "tra_loi_luc")
    private LocalDateTime traLoiLuc;
}
