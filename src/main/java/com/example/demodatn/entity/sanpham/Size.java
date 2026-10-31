package com.example.demodatn.entity.sanpham;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sizes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Size {
    @Column(name="ma",length=20,nullable=false,insertable=false,updatable=false)
    @org.hibernate.annotations.Generated(event=org.hibernate.generator.EventType.INSERT)
    private String code;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "size_value", nullable = false, unique = true)
    @org.hibernate.annotations.Nationalized private String sizeValue;
}
