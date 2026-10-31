package com.example.demodatn.entity.khuyenmai;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Promotion {
    @Column(name="ma",length=20,nullable=false,insertable=false,updatable=false)
    @org.hibernate.annotations.Generated(event=org.hibernate.generator.EventType.INSERT)
    private String code;

    public Promotion(Integer id,String name,String discountType,BigDecimal discountValue,LocalDateTime startDate,LocalDateTime endDate,Boolean status){this.id=id;this.name=name;this.discountType=discountType;this.discountValue=discountValue;this.startDate=startDate;this.endDate=endDate;this.status=status;}
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    @org.hibernate.annotations.Nationalized private String name;

    @Column(name = "discount_type", nullable = false)
    private String discountType;

    @Column(name = "discount_value", nullable = false)
    private BigDecimal discountValue;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    private Boolean status = true;
}
