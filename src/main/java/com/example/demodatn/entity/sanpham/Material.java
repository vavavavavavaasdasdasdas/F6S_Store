package com.example.demodatn.entity.sanpham;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chat_lieu")
@Getter @Setter @NoArgsConstructor
public class Material {
    @Column(name="ma",length=20,nullable=false,insertable=false,updatable=false)
    @org.hibernate.annotations.Generated(event=org.hibernate.generator.EventType.INSERT)
    private String code;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "ten", nullable = false, unique = true, columnDefinition = "nvarchar(100)")
    @org.hibernate.annotations.Nationalized private String name;
    @Column(name = "trang_thai", nullable = false)
    private Boolean status = true;
}
