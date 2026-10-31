package com.example.demodatn.entity.sanpham;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    @Column(name="ma",length=20,nullable=false,insertable=false,updatable=false)
    @org.hibernate.annotations.Generated(event=org.hibernate.generator.EventType.INSERT)
    private String code;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    @org.hibernate.annotations.Nationalized private String name;

    private Boolean status = true;
}
