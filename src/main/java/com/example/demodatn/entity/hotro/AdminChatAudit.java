package com.example.demodatn.entity.hotro;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_chat_audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminChatAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "role", nullable = false, length = 50)
    private String role;

    @Column(name = "thoi_gian", nullable = false)
    private LocalDateTime thoiGian = LocalDateTime.now();

    @org.hibernate.annotations.Nationalized
    @Column(name = "cau_hoi_tom_tat", nullable = false, length = 500)
    private String cauHoiTomTat;

    @Column(name = "cong_cu_da_goi", nullable = false, length = 100)
    private String congCuDaGoi;

    @Column(name = "session_id", length = 100)
    private String sessionId;
}
