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
}
