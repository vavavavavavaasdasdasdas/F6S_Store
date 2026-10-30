package com.example.demodatn.controller.admin.donhang;
import com.example.demodatn.repository.donhang.ShippingRegionRepository;
import com.example.demodatn.service.donhang.DeliveryService;
import com.example.demodatn.util.Pagination;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.*;
@Controller @RequestMapping("/admin/shipping-times") @RequiredArgsConstructor
public class AdminShippingRegionController {
 private final ShippingRegionRepository regions;private final DeliveryService delivery;private final Pagination pagination;
 @GetMapping public String list(@RequestParam(defaultValue="") String region,@RequestParam(defaultValue="0") String page,Model model){
  var rows=regions.findAll(Sort.by("code")).stream().filter(r->region.isBlank() || r.getCode().equals(region)).toList();
  var request=pagination.request(page,rows.size(),Sort.by("code"));int start=(int)request.getOffset();
  model.addAttribute("regionPage",new PageImpl<>(rows.subList(start,Math.min(start+request.getPageSize(),rows.size())),request,rows.size()));model.addAttribute("region",region);model.addAttribute("regions",regions.findAll(Sort.by("code")));return "admin/giao-hang/thoi-gian";
 }
 private int days(String raw){try{return Integer.parseInt(raw);}catch(NumberFormatException ex){throw new IllegalArgumentException("Số ngày phải là số nguyên từ 1 đến 30.");}}
 @PostMapping("/{code}") public String save(@PathVariable String code,@RequestParam(defaultValue="") String minDays,@RequestParam(defaultValue="") String maxDays,RedirectAttributes flash){
  try{delivery.updateRegion(code,days(minDays),days(maxDays));flash.addFlashAttribute("message","Đã cập nhật thời gian giao hàng. Đơn đã xác nhận giữ dự kiến đã lưu.");}
  catch(IllegalArgumentException ex){flash.addFlashAttribute("error",ex.getMessage());}return "redirect:/admin/shipping-times";
 }
}
