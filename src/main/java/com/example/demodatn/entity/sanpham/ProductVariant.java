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
    @Column(name="ma",length=20,nullable=false,insertable=false,updatable=false)
    @org.hibernate.annotations.Generated(event=org.hibernate.generator.EventType.INSERT)
    private String code;

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
    public String getMa() { return code; }
    public String getColorName() { return color != null ? color.getName() : ""; }
    public String getColorHexCode() { return color != null ? color.getHexCode() : ""; }
    public String getSizeValue() { return size != null ? size.getSizeValue() : ""; }
    public String getMaterialName() { return material != null ? material.getName() : ""; }
    public String getSoleName() { return sole != null ? sole.getName() : ""; }
}
