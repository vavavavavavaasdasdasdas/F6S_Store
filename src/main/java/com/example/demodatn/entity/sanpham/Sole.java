package com.example.demodatn.entity.sanpham;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="de_giay")
@Getter @Setter @NoArgsConstructor
public class Sole {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;
    @org.hibernate.annotations.Nationalized
    @Column(name="ten",nullable=false,unique=true,length=100)
    private String name;
    @Column(name="trang_thai",nullable=false)
    private Boolean status=true;
}
