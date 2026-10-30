package com.example.demodatn.controller;
import com.example.demodatn.repository.donhang.ProvinceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@ControllerAdvice(assignableTypes={com.example.demodatn.controller.user.ProfileController.class,com.example.demodatn.controller.user.OrderController.class,com.example.demodatn.controller.admin.khachhang.AdminCustomerController.class,com.example.demodatn.controller.admin.donhang.AdminOrderController.class,com.example.demodatn.controller.admin.donhang.AdminPosController.class}) @RequiredArgsConstructor
public class ProvinceAdvice {
 private final ProvinceRepository provinces;
 @ModelAttribute("provinces") public java.util.List<com.example.demodatn.entity.donhang.Province> provinces(){return provinces.findAllByOrderByNameAsc();}
}
