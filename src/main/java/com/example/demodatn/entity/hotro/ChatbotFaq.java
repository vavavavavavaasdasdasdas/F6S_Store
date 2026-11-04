package com.example.demodatn.entity.hotro;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chatbot_faq")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotFaq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String ma;

    @org.hibernate.annotations.Nationalized
    @Column(name = "cau_hoi", nullable = false, length = 300)
    private String cauHoi;

    @org.hibernate.annotations.Nationalized
    @Column(name = "tu_khoa", length = 300)
    private String tuKhoa;

    @org.hibernate.annotations.Nationalized
    @Column(name = "tra_loi", nullable = false, length = 1500)
    private String traLoi;

    @Column(name = "hien_thi", nullable = false)
    private Boolean hienThi = true;

    @Column(name = "doi_tuong", nullable = false, length = 20)
    private String doiTuong = "KHACH";
}
