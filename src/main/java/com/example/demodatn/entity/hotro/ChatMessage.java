package com.example.demodatn.entity.hotro;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_tin_nhan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hoi_thoai_id", nullable = false)
    private ChatConversation conversation;

    @Column(nullable = false, length = 20)
    private String sender; // USER or BOT

    @org.hibernate.annotations.Nationalized
    @Column(name = "noi_dung", nullable = false, length = 2000)
    private String noiDung;

    @org.hibernate.annotations.Nationalized
    @Column(name = "payload_json", columnDefinition = "nvarchar(max)")
    private String payloadJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    @org.hibernate.annotations.Nationalized @Column(name="nguoi_gui_loai",length=20,nullable=false) private String senderType="HE_THONG";
    @Column(name="nguoi_gui_id") private Integer senderId;
    @org.hibernate.annotations.Nationalized @Column(name="loai",length=20,nullable=false) private String type="VAN_BAN";
    @org.hibernate.annotations.Nationalized @Column(name="tep_dinh_kem",length=255) private String attachment;
    @Column(name="da_xem_luc") private LocalDateTime seenAt;
    @Column(name="client_id",length=64) private String clientId;
    @Column(name="ip_address",length=45) private String ipAddress;
}
