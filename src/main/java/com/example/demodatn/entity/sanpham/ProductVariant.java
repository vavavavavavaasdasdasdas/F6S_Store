package com.example.demodatn.entity.sanpham;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "product_variants")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "size_id")
    private Size size;

    @ManyToOne(optional = false)
    @JoinColumn(name = "chat_lieu_id", nullable = false)
    private Material material;

    @ManyToOne(optional = false)
    @JoinColumn(name = "mau_sac_id", nullable = false)
    private Color color;

    @ManyToOne(optional = false)
    @JoinColumn(name = "de_giay_id", nullable = false)
    private Sole sole;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer quantity = 0;

    private Boolean status = true;
}
