package com.example.demodatn.entity.sanpham;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="mau_sac")
@Getter @Setter @NoArgsConstructor
public class Color {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;
    @org.hibernate.annotations.Nationalized
    @Column(name="ten",nullable=false,unique=true,length=100)
    private String name;
    @Column(name="ma_mau",length=7) private String hexCode;
    @Column(name="trang_thai",nullable=false)
    private Boolean status=true;
}
