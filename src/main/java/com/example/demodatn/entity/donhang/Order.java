package com.example.demodatn.entity.donhang;
import com.example.demodatn.entity.khachhang.Customer;
import com.example.demodatn.entity.khuyenmai.Voucher;
import com.example.demodatn.entity.nhanvien.Employee;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "order_code", unique = true, nullable = false)
    private String orderCode;

    @org.hibernate.annotations.Nationalized
    @Column(name="payment_reference",length=40) private String paymentReference;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(name = "order_type", nullable = false) // ONLINE, IN_STORE
    private String orderType;

    @Column(nullable = false) // PENDING, CONFIRMED, etc.
    private String status;

    @Column(name = "da_tru_kho", nullable = false)
    private Boolean daTruKho = false;

    @Column(name = "total_amount", nullable = false) // Subtotal
    private BigDecimal totalAmount;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

    @Column(name = "shipping_fee")
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "final_amount", nullable = false) // Total after discount + shipping
    private BigDecimal finalAmount;

    @Column(name = "payment_method") // COD, BANK_TRANSFER
    private String paymentMethod;

    @Column(name = "payment_status") // UNPAID, PAID
    private String paymentStatus = "UNPAID";

    @Column(name = "receiver_name")
    @org.hibernate.annotations.Nationalized private String receiverName;

    @Column(name = "shipping_address")
    @org.hibernate.annotations.Nationalized private String shippingAddress;

    @Column(name = "phone_number")
    @org.hibernate.annotations.Nationalized private String phoneNumber;

    @Column(name = "email")
    @org.hibernate.annotations.Nationalized private String email;

    @ManyToOne
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderDetail> orderDetails = new ArrayList<>();

    public void addDetail(OrderDetail detail) {
        orderDetails.add(detail);
        detail.setOrder(this);
    }
}
