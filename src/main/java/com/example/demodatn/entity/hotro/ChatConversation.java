package com.example.demodatn.entity.hotro;

import com.example.demodatn.entity.khachhang.Customer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@org.hibernate.annotations.DynamicUpdate
@Table(name = "chat_hoi_thoai")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true, length = 100)
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    @Column(name="assigned_user_id") private Integer assignedUserId;
    @Column(name="legacy_ticket_id") private Integer legacyTicketId;
    @Column(name="last_customer_seen_at") private LocalDateTime lastCustomerSeenAt;
    @Column(name="last_mail_at") private LocalDateTime lastMailAt;
    @Column(name="session_token_hash",length=72) private String tokenHash;
    @Column(name="khach_id",insertable=false,updatable=false) private Integer legacyCustomerId;
    @Column(name="ma",length=20,nullable=false,insertable=false,updatable=false)
    @org.hibernate.annotations.Generated(event=org.hibernate.generator.EventType.INSERT)
    private String code;
    @org.hibernate.annotations.Nationalized @Column(name="ho_ten",length=100) private String name;
    @org.hibernate.annotations.Nationalized @Column(name="lien_he",length=100) private String contact;
    @org.hibernate.annotations.Nationalized @Column(name="trang_thai",length=20,nullable=false) private String status="MOI";
    @Column(name="nhan_vien_phu_trach_id") private Integer employeeId;
    @Column(name="last_message_at") private LocalDateTime lastMessageAt;
    @Column(name="chua_doc_khach",nullable=false) private int customerUnread;
    @Column(name="chua_doc_nhan_vien",nullable=false) private int staffUnread;
    @Column(name="da_co_nhan_vien",nullable=false) private boolean human;
    @Column(name="cho_nhan_vien",nullable=false) private boolean waiting;
}
