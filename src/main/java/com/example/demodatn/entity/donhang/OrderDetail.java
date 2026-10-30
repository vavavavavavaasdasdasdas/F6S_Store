package com.example.demodatn.entity.donhang;
import com.example.demodatn.entity.sanpham.ProductVariant;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    // Snapshot fields
    @Column(name = "product_name")
    @org.hibernate.annotations.Nationalized private String productName;

    @Column(name = "size_name")
    @org.hibernate.annotations.Nationalized private String sizeName;

    @Column(name = "ten_chat_lieu", columnDefinition = "nvarchar(100)")
    @org.hibernate.annotations.Nationalized private String materialName;

    @Column(name="ten_mau_sac",columnDefinition="nvarchar(100)")
    @org.hibernate.annotations.Nationalized private String colorName;
    @Column(name="ten_de_giay",columnDefinition="nvarchar(100)")
    @org.hibernate.annotations.Nationalized private String soleName;
    public String getDisplayColorName() { return colorName == null || colorName.isBlank() ? "—" : colorName; }
    public String getDisplaySoleName() { return soleName == null || soleName.isBlank() ? "—" : soleName; }

    public String getDisplayMaterialName() { return materialName == null || materialName.isBlank() ? "—" : materialName; }

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice;
}
