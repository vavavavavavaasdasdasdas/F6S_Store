package com.example.demodatn.entity.donhang;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_mail_log",
       uniqueConstraints = @UniqueConstraint(name = "uq_order_mail_log_order_status", columnNames = {"order_id", "status"}),
       indexes = @Index(name = "ix_order_mail_log_order_id", columnList = "order_id"))
@Getter
@Setter
public class OrderMailLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, length = 50)
    private String status;

    @org.hibernate.annotations.Nationalized
    @Column(length = 255)
    private String email;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "thanh_cong", nullable = false)
    private boolean thanhCong;

    @org.hibernate.annotations.Nationalized
    @Column(name = "loi", columnDefinition = "nvarchar(max)")
    private String loi;
}
