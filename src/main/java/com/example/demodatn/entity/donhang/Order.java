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

    @ManyToOne @JoinColumn(name="tinh_thanh_id") private Province province;
    @org.hibernate.annotations.Nationalized @Column(name="ten_tinh_thanh",length=100) private String provinceName;
    @Column(name="du_kien_giao_tu") private java.time.LocalDate estimatedFrom;
    @Column(name="du_kien_giao_den") private java.time.LocalDate estimatedTo;
    @Column(name="ngay_giao_thuc_te") private java.time.LocalDate deliveredOn;


    @Column(precision=10,scale=7) private java.math.BigDecimal latitude;
    @Column(precision=10,scale=7) private java.math.BigDecimal longitude;
    @org.hibernate.annotations.Nationalized @Column(name="nguon_toa_do",length=20) private String coordinateSource;
    @org.hibernate.annotations.Nationalized @Column(name="so_nha_duong",length=255) private String streetAddress;
    @ManyToOne @JoinColumn(name="phuong_xa_id") private com.example.demodatn.entity.donhang.Ward ward;

    @org.hibernate.annotations.Nationalized @Column(name="ma_tinh_giao",length=2) private String deliveryProvinceCode;
    @org.hibernate.annotations.Nationalized @Column(name="ma_phuong_giao",length=5) private String deliveryWardCode;
    @org.hibernate.annotations.Nationalized @Column(name="ten_phuong_giao",length=100) private String deliveryWardName;
    @org.hibernate.annotations.Nationalized @Column(name="dia_chi_giao_snapshot",length=600) private String deliveryAddressSnapshot;
    @Column(name="distance_km",precision=12,scale=2) private BigDecimal distanceKm;
    @Column(name="is_free_ship") private Boolean freeShip;
    @Column(name="shipping_policy_version") private Integer shippingPolicyVersion;
    @org.hibernate.annotations.Nationalized @Column(name="shipping_note",length=255) private String shippingNote;
    @org.hibernate.annotations.Nationalized @Column(name="order_source",length=30) private String orderSource;
    @org.hibernate.annotations.Nationalized @Column(name="sales_note",length=1000) private String salesNote;
    public String getShippingAddress(){return deliveryAddressSnapshot==null?shippingAddress:deliveryAddressSnapshot;}
    public static final java.util.Map<String,String> SOURCE_LABELS=java.util.Map.of("FACEBOOK","Facebook","ZALO","Zalo","TIKTOK","TikTok","INSTAGRAM","Instagram","PHONE","Điện thoại","OTHER","Khác");
    public String getSourceLabel(){return orderSource==null?null:SOURCE_LABELS.getOrDefault(orderSource,"Khác");}
    public String getChannelLabel(){return "POS".equals(orderType)?"Tại quầy":orderSource==null?"Trực tuyến":"Online · "+getSourceLabel();}

    public void addDetail(OrderDetail detail) {
        orderDetails.add(detail);
        detail.setOrder(this);
    }
}
