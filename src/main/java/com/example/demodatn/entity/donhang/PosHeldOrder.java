package com.example.demodatn.entity.donhang;
import com.example.demodatn.entity.khachhang.Customer;
import com.example.demodatn.entity.nhanvien.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name="pos_held_orders",indexes=@Index(name="ix_pos_hold_staff_expiry",columnList="staff_user_id,expires_at"))
@Getter @Setter
public class PosHeldOrder {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="order_type",nullable=false,length=10) private String orderType="POS";
    @org.hibernate.annotations.Nationalized @Column(name="delivery_data",columnDefinition="nvarchar(max)") private String deliveryData;
    @Column(nullable=false,unique=true,length=40) private String code;
    @ManyToOne(optional=false) @JoinColumn(name="staff_user_id",nullable=false) private User staffUser;
    @ManyToOne @JoinColumn(name="customer_id") private Customer customer;
    @org.hibernate.annotations.Nationalized
    @Column(name="customer_label",length=255) private String customerLabel;
    @Column(name="voucher_code",length=50) private String voucherCode;
    @Column(name="payment_method",nullable=false,length=20) private String paymentMethod;
    @org.hibernate.annotations.Nationalized
    @Column(length=500) private String note;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
    @Column(name="expires_at",nullable=false) private LocalDateTime expiresAt;
    @OneToMany(mappedBy="held",cascade=CascadeType.ALL,orphanRemoval=true)
    private List<PosHeldItem> items=new ArrayList<>();
}
