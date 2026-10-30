package com.example.demodatn.service;
import com.example.demodatn.util.PasswordPolicy;
import com.example.demodatn.util.VietnameseText;
import com.example.demodatn.dto.khachhang.CustomerDto;
import com.example.demodatn.dto.nhanvien.EmployeeDto;
import com.example.demodatn.entity.khachhang.Customer;
import com.example.demodatn.entity.nhanvien.Employee;
import com.example.demodatn.entity.nhanvien.Role;
import com.example.demodatn.entity.nhanvien.User;
import com.example.demodatn.exception.PeopleValidationException;
import com.example.demodatn.repository.donhang.OrderRepository;
import com.example.demodatn.repository.khachhang.CustomerRepository;
import com.example.demodatn.repository.khuyenmai.VoucherRepository;
import com.example.demodatn.repository.nhanvien.EmployeeRepository;
import com.example.demodatn.repository.nhanvien.RoleRepository;
import com.example.demodatn.repository.nhanvien.UserRepository;
import com.example.demodatn.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PeopleService {
    private final CustomerRepository customers;
    private final com.example.demodatn.repository.khachhang.ShippingAddressRepository shippingAddresses;
    private final EmployeeRepository employees;
    private final UserRepository users;
    private final RoleRepository roles;
    private final OrderRepository orders;
    private final VoucherRepository vouchers;
    private final PasswordEncoder encoder;
    private final com.example.demodatn.service.khachhang.ShippingAddressService addressService;
    private final com.example.demodatn.repository.nhanvien.AccountIssueRepository accountIssues;
    @Value("${shop.employee-role:ROLE_ADMIN}") private String employeeRole;

    public Customer customer(Integer id) {
        return customers.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    public Employee employee(Integer id) {
        return employees.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    public static String phone(String input) {
        String value=input.trim();
        return value.startsWith("+84") ? "0"+value.substring(3) : value;
    }
    public static String email(String input) { return input.trim().toLowerCase(Locale.ROOT); }
    public boolean isSelf(User user) {
        if(user==null)return false;
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth==null)return false;
        if(auth.getPrincipal() instanceof CustomUserDetails details) return user.getId().equals(details.getUserId());
        return user.getUsername().equalsIgnoreCase(auth.getName());
    }
    private void protectSelf(User user) {
        if(isSelf(user))throw new IllegalArgumentException("Không thể vô hiệu hóa hoặc xóa chính tài khoản đang đăng nhập.");
    }
    private void syncAccount(User user) {
        if(user==null)return;
        boolean customerEnabled=customers.findByUser_Id(user.getId()).map(c->Boolean.TRUE.equals(c.getStatus())).orElse(true);
        boolean employeeEnabled=employees.findByUser_Id(user.getId()).map(e->Boolean.TRUE.equals(e.getStatus())).orElse(true);
        user.setStatus(customerEnabled && employeeEnabled);
        users.save(user);
    }
    @Transactional
    public void updateProfile(String username,String fullName,String phone,String email) {
        Customer c=customers.findForUpdateByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String normalizedPhone=phone(phone),normalizedEmail=email(email);
        if(customers.existsByEmailIgnoreCaseAndIdNot(normalizedEmail,c.getId()) || customers.existsByPhoneAndIdNot(normalizedPhone,c.getId()))
            throw new IllegalArgumentException("Thư điện tử hoặc số điện thoại đã tồn tại.");
        c.setFullName(fullName);c.setPhone(normalizedPhone);c.setEmail(normalizedEmail);customers.saveAndFlush(c);
    }
    @Transactional
    public Customer saveCustomer(CustomerDto form) {
        int id=form.getId()==null ? -1 : form.getId();
        if(customers.existsByCustomerCodeIgnoreCaseAndIdNot(form.getCustomerCode().trim(),id))throw new PeopleValidationException("customerCode","Mã khách hàng đã tồn tại.");
        if(customers.existsByEmailIgnoreCaseAndIdNot(email(form.getEmail()),id))throw new PeopleValidationException("email","Thư điện tử đã tồn tại.");
        if(customers.existsByPhoneAndIdNot(phone(form.getPhone()),id))throw new PeopleValidationException("phone","Số điện thoại đã tồn tại.");
        Customer c=form.getId()==null ? new Customer() : customers.findForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        String document=form.getSoCccd()==null||form.getSoCccd().isBlank()?null:form.getSoCccd().trim();
        if(document!=null && !document.matches("[0-9]{12}"))
            throw new PeopleValidationException("soCccd","Số căn cước phải gồm đúng 12 chữ số.");
        if(document!=null && accountIssues.documentUsed("customers",document,id))
            throw new PeopleValidationException("soCccd","Số căn cước đã tồn tại.");
        c.setSoCccd(document);c.setNgaySinh(form.getNgaySinh());c.setGioiTinh(form.getGioiTinh()==null||form.getGioiTinh().isBlank()?null:form.getGioiTinh());
        c.setCustomerCode(form.getCustomerCode().trim());c.setFullName(form.getFullName().trim());
        c.setPhone(phone(form.getPhone()));c.setEmail(email(form.getEmail()));
        // Customer editing uses the same address service and keeps other saved addresses intact.
        customers.saveAndFlush(c);
        var address=new com.example.demodatn.dto.khachhang.ShippingAddressForm();
        shippingAddresses.findByCustomer_IdOrderBySlotAsc(c.getId()).stream().filter(com.example.demodatn.entity.khachhang.ShippingAddress::isDefaultAddress).findFirst().ifPresent(a->address.setId(a.getId()));
        address.setReceiverName(c.getFullName());address.setPhone(c.getPhone());address.setAddress(form.getStreetAddress());address.setProvinceId(form.getProvinceId());address.setWardId(form.getWardId());address.setStreetAddress(form.getStreetAddress());address.setLatitude(form.getLatitude());address.setLongitude(form.getLongitude());address.setCoordinateSource(form.getCoordinateSource());address.setMakeDefault(true);
        addressService.saveForCustomer(c.getId(),address);
        // Status changes have a separate confirmed POST and cannot be smuggled through this form.
        return customers.saveAndFlush(c);
    }
    public String resolvePosition(String raw) {
        String cleaned = VietnameseText.clean(raw);
        if (cleaned.isEmpty()) return null;
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream().noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            throw new AccessDeniedException("Chỉ quản trị viên mới được tạo hoặc gán chức vụ.");
        }
        if (cleaned.length() > 100) {
            throw new PeopleValidationException("position", "Chức vụ tối đa 100 ký tự.");
        }
        for (String existing : employees.distinctPositions()) {
            if (VietnameseText.sameFolded(existing, cleaned)) {
                return existing.trim();
            }
        }
        return cleaned;
    }
    @Transactional
    public Employee saveEmployee(EmployeeDto form) { return persistEmployee(form,null,false); }
    @Transactional
    public Employee saveEmployeeGenerated(EmployeeDto form,String temporaryHash) { return persistEmployee(form,temporaryHash,true); }
    private Employee persistEmployee(EmployeeDto form,String temporaryHash,boolean generated) {
        int id=form.getId()==null ? -1 : form.getId();
        if(employees.existsByEmployeeCodeIgnoreCaseAndIdNot(form.getEmployeeCode().trim(),id))throw new PeopleValidationException("employeeCode","Mã nhân viên đã tồn tại.");
        if(employees.existsByEmailIgnoreCaseAndIdNot(email(form.getEmail()),id))throw new PeopleValidationException("email","Thư điện tử đã tồn tại.");
        if(employees.existsByPhoneAndIdNot(phone(form.getPhone()),id))throw new PeopleValidationException("phone","Số điện thoại đã tồn tại.");
        String rawPos = (form.getNewPosition() != null && !form.getNewPosition().isBlank()) ? form.getNewPosition() : form.getPosition();
        String resolvedPosition = resolvePosition(rawPos);
        Employee e=form.getId()==null ? new Employee() : employees.findForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        User u=e.getUser()==null ? new User() : e.getUser();
        String username=form.getUsername().trim().toLowerCase(Locale.ROOT);
        if(users.existsByUsernameIgnoreCaseAndIdNot(username,u.getId()==null ? -1 : u.getId()))throw new PeopleValidationException("username","Tên đăng nhập đã tồn tại.");
        String password=generated?null:form.getPassword();
        if(temporaryHash!=null)u.setPassword(temporaryHash);
        if(u.getId()==null && temporaryHash==null && (password==null || password.isBlank()))throw new PeopleValidationException("password","Nhập mật khẩu cho tài khoản mới.");
        if(password!=null && !password.isEmpty()) {
            if(!PasswordPolicy.isValid(password))
                throw new PeopleValidationException("password",PasswordPolicy.MESSAGE);
            u.setPassword(encoder.encode(password));
        }
        if(u.getId()==null) {
            Role role=roles.findByName(employeeRole);
            if(role==null || "ROLE_USER".equals(employeeRole))throw new IllegalStateException("Cấu hình role nhân viên không hợp lệ.");
            u.setRole(role);u.setStatus(true);
        }
        u.setUsername(username);
        e.setUser(users.saveAndFlush(u));
        String document=form.getSoCccd()==null||form.getSoCccd().isBlank()?null:form.getSoCccd().trim();
        if(document!=null && !document.matches("[0-9]{12}"))
            throw new PeopleValidationException("soCccd","Số căn cước phải gồm đúng 12 chữ số.");
        if(document!=null && accountIssues.documentUsed("employees",document,id))
            throw new PeopleValidationException("soCccd","Số căn cước đã tồn tại.");
        e.setSoCccd(document);e.setNgaySinh(form.getNgaySinh());e.setGioiTinh(form.getGioiTinh()==null||form.getGioiTinh().isBlank()?null:form.getGioiTinh());
        e.setEmployeeCode(form.getEmployeeCode().trim());e.setFullName(form.getFullName().trim());
        e.setPhone(phone(form.getPhone()));e.setEmail(email(form.getEmail()));e.setAddress(form.getAddress());e.setPosition(resolvedPosition);
        form.setPosition(resolvedPosition);
        return employees.saveAndFlush(e);
    }
    @Transactional
    public void customerStatus(Integer id,boolean enabled) {
        Customer c=customers.findForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(!enabled)protectSelf(c.getUser());
        c.setStatus(enabled);customers.saveAndFlush(c);syncAccount(c.getUser());
    }
    @Transactional
    public void employeeStatus(Integer id,boolean enabled) {
        Employee e=employees.findForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(!enabled)protectSelf(e.getUser());
        e.setStatus(enabled);employees.saveAndFlush(e);syncAccount(e.getUser());
    }
    @Transactional
    public String deleteCustomer(Integer id) {
        Customer c=customers.findForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        User u=c.getUser();protectSelf(u);
        boolean linkedEmployee=u!=null && employees.findByUser_Id(u.getId()).isPresent();
        if(orders.existsByCustomer_Id(id) || vouchers.existsByCustomer_Id(id) || linkedEmployee) {
            c.setStatus(false);customers.saveAndFlush(c);syncAccount(u);
            return "Khách hàng có dữ liệu liên quan: đã vô hiệu hóa, giữ nguyên đơn hàng và lịch sử.";
        }
        customers.delete(c);customers.flush();
        if(u!=null)users.delete(u);
        users.flush();
        return "Đã xóa khách hàng chưa có dữ liệu liên quan và tài khoản đi kèm (nếu có).";
    }
    @Transactional
    public String deleteEmployee(Integer id) {
        Employee e=employees.findForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        User u=e.getUser();protectSelf(u);
        if(orders.existsByEmployee_Id(id) || customers.findByUser_Id(u.getId()).isPresent()) {
            e.setStatus(false);employees.saveAndFlush(e);syncAccount(u);
            return "Nhân viên có dữ liệu liên quan: đã vô hiệu hóa nhân viên và tài khoản, giữ nguyên lịch sử.";
        }
        employees.delete(e);employees.flush();users.delete(u);users.flush();
        return "Đã xóa nhân viên và tài khoản trong cùng giao dịch.";
    }
}
