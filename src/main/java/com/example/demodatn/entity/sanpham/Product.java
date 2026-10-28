package com.example.demodatn.entity.sanpham;
import com.example.demodatn.util.ProductMedia;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true)
    private String code;

    @Column(nullable = false)
    @org.hibernate.annotations.Nationalized private String name;

    @org.hibernate.annotations.Nationalized private String description;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name="de_giay_id")
    private Sole sole;

    @Column(name = "image_url")
    @org.hibernate.annotations.Nationalized private String imageUrl;

    private Boolean status = true;
    public String getDisplayImageUrl() { return ProductMedia.source(imageUrl); }
}
