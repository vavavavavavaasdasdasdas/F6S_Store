package com.example.demodatn.service.nhanvien;
import com.example.demodatn.util.PasswordPolicy;
import com.example.demodatn.dto.nhanvien.AccountPasswordForm;
import com.example.demodatn.dto.nhanvien.AccountProfileForm;
import com.example.demodatn.entity.nhanvien.User;
import com.example.demodatn.repository.nhanvien.EmployeeRepository;
import com.example.demodatn.repository.nhanvien.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

@Service @RequiredArgsConstructor
public class AdminAccountService {
    private final UserRepository users;
    private final EmployeeRepository employees;
    private final PasswordEncoder encoder;

    public User user(String username) {
        return users.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("Tài khoản không tồn tại."));
    }

    public AccountProfileForm profile(User user) {
        var employee=employees.findByUser_Id(user.getId()).orElse(null);
        var form=new AccountProfileForm();
        form.setFullName(employee==null ? user.getFullName() : employee.getFullName());
        form.setEmail(employee==null ? user.getEmail() : employee.getEmail());
        form.setPhone(employee==null ? user.getPhone() : employee.getPhone());
        return form;
    }

    @Transactional
    public void update(String username, AccountProfileForm form, BindingResult errors) {
        var user=users.lockByUsername(username).orElseThrow();
        String email=form.getEmail()==null ? "" : form.getEmail().trim();
        if(users.otherAccountsWithEmail(email,user.getId())>0)
            errors.rejectValue("email","duplicate","Thư điện tử đã được tài khoản khác sử dụng.");
        var employee=employees.findByUser_Id(user.getId()).orElse(null);
        if(employee!=null && employees.existsByPhoneAndIdNot(form.getPhone(),employee.getId()))
            errors.rejectValue("phone","duplicate","SĐT đã được nhân viên khác sử dụng.");
        if(errors.hasErrors()) return;
        user.setFullName(form.getFullName().trim());user.setEmail(email);user.setPhone(form.getPhone().trim());
        if(employee!=null) {
            employee.setFullName(user.getFullName());employee.setEmail(email);employee.setPhone(user.getPhone());
            employees.save(employee);
        }
        users.save(user);
    }

    @Transactional
    public void password(String username, AccountPasswordForm form, BindingResult errors) {
        var user=users.lockByUsername(username).orElseThrow();
        boolean currentTooLong=form.getCurrentPassword()!=null && form.getCurrentPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72;
        boolean newTooLong=form.getNewPassword()!=null && form.getNewPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72;
        if(currentTooLong || !encoder.matches(form.getCurrentPassword(),user.getPassword()))
            errors.rejectValue("currentPassword","incorrect","Mật khẩu hiện tại không đúng.");
        if(!java.util.Objects.equals(form.getNewPassword(),form.getConfirmPassword()))
            errors.rejectValue("confirmPassword","mismatch","Xác nhận mật khẩu không khớp.");
        if(!PasswordPolicy.isValid(form.getNewPassword()))errors.rejectValue("newPassword","policy",PasswordPolicy.MESSAGE);
        if(!newTooLong && encoder.matches(form.getNewPassword(),user.getPassword()))
            errors.rejectValue("newPassword","same","Mật khẩu mới phải khác mật khẩu hiện tại.");
        if(!errors.hasErrors()) {user.setPassword(encoder.encode(form.getNewPassword()));user.setMustChangePassword(false);users.save(user);}
    }
}
