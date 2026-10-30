package com.example.demodatn.controller.user;
import com.example.demodatn.util.PasswordPolicy;
import com.example.demodatn.entity.khachhang.Customer;
import com.example.demodatn.repository.khachhang.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final CustomerRepository customerRepository;
    private final com.example.demodatn.repository.nhanvien.UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.example.demodatn.service.PeopleService people;
    private final com.example.demodatn.service.khachhang.ShippingAddressService shippingAddresses;

    @GetMapping
    public String viewProfile(Authentication auth,@RequestParam(required=false) Integer addressId,
            @RequestParam(defaultValue="false") boolean newAddress,Model model) {
        if(auth==null || !auth.isAuthenticated())return "redirect:/login";
        var form=new com.example.demodatn.dto.khachhang.ShippingAddressForm();
        var rows=shippingAddresses.list(auth.getName());
        if(!newAddress && !rows.isEmpty()) {
            var selected=addressId==null ? rows.stream().filter(com.example.demodatn.entity.khachhang.ShippingAddress::isDefaultAddress).findFirst().orElse(rows.get(0)) : shippingAddresses.owned(auth.getName(),addressId);
            form.setId(selected.getId());form.setReceiverName(selected.getReceiverName());form.setPhone(selected.getPhone());form.setAddress(selected.getAddress());
            form.setMakeDefault(selected.isDefaultAddress());form.setProvinceId(selected.getProvince()==null?null:selected.getProvince().getId());
        } else if(addressId!=null) shippingAddresses.owned(auth.getName(),addressId);
        model.addAttribute("shippingAddress",form);
        return profile(auth,model);
    }
    private String profile(Authentication auth,Model model) {
        var customer=customerRepository.findByUser_Username(auth.getName()).orElse(null);
        model.addAttribute("customer",customer);
        var rows=shippingAddresses.list(auth.getName());model.addAttribute("shippingAddresses",rows);
        model.addAttribute("addressLimit",com.example.demodatn.service.khachhang.ShippingAddressService.LIMIT_MESSAGE);
        return "user/ho-so";
    }
    @PostMapping("/update")
    public String updateProfile(@RequestParam String fullName,@RequestParam String phone,@RequestParam String email,
            Authentication auth,RedirectAttributes ra) {
        try {people.updateProfile(auth.getName(),fullName,phone,email);ra.addFlashAttribute("message","Cập nhật thông tin thành công!");}
        catch(IllegalArgumentException ex){ra.addFlashAttribute("error",ex.getMessage());}
        catch(org.springframework.dao.DataIntegrityViolationException ex){ra.addFlashAttribute("error","Thư điện tử hoặc số điện thoại vừa được sử dụng. Vui lòng thử lại.");}
        return "redirect:/profile";
    }
    @PostMapping("/addresses/save")
    public String saveAddress(@jakarta.validation.Valid @org.springframework.web.bind.annotation.ModelAttribute("shippingAddress") com.example.demodatn.dto.khachhang.ShippingAddressForm form,
            org.springframework.validation.BindingResult errors,Authentication auth,Model model,RedirectAttributes flash) {
        if(form.getId()!=null)shippingAddresses.owned(auth.getName(),form.getId());
        if(!errors.hasErrors()) {
            try {
                var saved=shippingAddresses.save(auth.getName(),form);flash.addFlashAttribute("message","Đã lưu địa chỉ nhận hàng.");
                return "redirect:/profile?addressId="+saved.getId();
            } catch(com.example.demodatn.exception.PeopleValidationException ex){errors.rejectValue(ex.getField(),"invalid",ex.getMessage());}
              catch(org.springframework.dao.DataIntegrityViolationException ex){errors.reject("conflict","Sổ địa chỉ vừa thay đổi. Vui lòng tải lại trang.");}
        }
        return profile(auth,model);
    }
    @PostMapping("/addresses/{id}/delete")
    public String deleteAddress(@org.springframework.web.bind.annotation.PathVariable Integer id,Authentication auth,RedirectAttributes flash) {
        shippingAddresses.delete(auth.getName(),id);flash.addFlashAttribute("message","Đã xóa địa chỉ nhận hàng.");return "redirect:/profile";
    }
    @PostMapping("/addresses/{id}/default")
    public String defaultAddress(@org.springframework.web.bind.annotation.PathVariable Integer id,Authentication auth,RedirectAttributes flash) {
        shippingAddresses.makeDefault(auth.getName(),id);flash.addFlashAttribute("message","Đã đặt địa chỉ mặc định.");return "redirect:/profile?addressId="+id;
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Authentication auth,
                                 RedirectAttributes ra) {
        String username = auth.getName();
        var user = userRepository.findByUsername(username).orElse(null);
        if (user != null) {
            if (oldPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72 || !passwordEncoder.matches(oldPassword, user.getPassword())) {
                ra.addFlashAttribute("error", "Mật khẩu cũ không chính xác!");
                return "redirect:/profile";
            }
            if (!newPassword.equals(confirmPassword)) {
                ra.addFlashAttribute("error", "Mật khẩu xác nhận không khớp!");
                return "redirect:/profile";
            }
            if(!PasswordPolicy.isValid(newPassword)){ra.addFlashAttribute("error",PasswordPolicy.MESSAGE);return "redirect:/profile";}
            user.setMustChangePassword(false);user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            ra.addFlashAttribute("message", "Đổi mật khẩu thành công!");
        }
        return "redirect:/profile";
    }
}
