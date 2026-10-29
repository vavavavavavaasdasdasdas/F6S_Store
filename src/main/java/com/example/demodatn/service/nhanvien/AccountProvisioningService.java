package com.example.demodatn.service.nhanvien;

import com.example.demodatn.dto.khachhang.CustomerDto;
import com.example.demodatn.dto.nhanvien.EmployeeDto;
import com.example.demodatn.entity.nhanvien.User;
import com.example.demodatn.exception.PeopleValidationException;
import com.example.demodatn.repository.khachhang.CustomerRepository;
import com.example.demodatn.repository.nhanvien.*;
import com.example.demodatn.service.PeopleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Locale;

@Service @RequiredArgsConstructor
public class AccountProvisioningService {
    private final PeopleService people;
    private final CustomerRepository customers;
    private final EmployeeRepository employees;
    private final UserRepository users;
    private final RoleRepository roles;
    private final AccountIssueRepository issues;
    private final AccountMailService mail;
    private final PasswordEncoder encoder;
    private final PlatformTransactionManager transactions;
    @Value("${shop.account-mail.max-per-hour:3}") private int maxPerHour;
    private static final SecureRandom RANDOM=new SecureRandom();

    public record Outcome(int personId,boolean issued,boolean sent) {
        public String message(){return !issued?"Đã lưu thông tin.":sent?"Đã lưu tài khoản và gửi email.":"Đã lưu tài khoản. Chưa gửi được email. Hãy dùng nút Gửi lại tài khoản.";}
    }
    // Deliberately not a record/@Data: a credential must never acquire a generated toString().
    private static final class Issued {
        final int personId,userId;final String hash,password,email,name,username;
        Issued(int personId,User user,String password){this.personId=personId;userId=user.getId();hash=user.getPassword();this.password=password;email=user.getEmail();name=user.getFullName();username=user.getUsername();}
    }
    public static String temporaryPassword() {
        String upper="ABCDEFGHJKLMNPQRSTUVWXYZ",lower="abcdefghijkmnopqrstuvwxyz",digits="23456789",symbols="!@#$%_-",pool=upper+lower+digits+symbols;
        char[] value=new char[12];String[] groups={upper,lower,digits,symbols};
        for(int i=0;i<4;i++)value[i]=groups[i].charAt(RANDOM.nextInt(groups[i].length()));
        for(int i=4;i<value.length;i++)value[i]=pool.charAt(RANDOM.nextInt(pool.length()));
        for(int i=value.length-1;i>0;i--){int j=RANDOM.nextInt(i+1);char c=value[i];value[i]=value[j];value[j]=c;}
        return new String(value);
    }
    public String suggestUsername(String email) {
        String local=email==null?"":email.split("@",2)[0];
        String base=Normalizer.normalize(local,Normalizer.Form.NFD).replaceAll("\\p{M}","").replace('đ','d').replace('Đ','D').toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]","");
        if(base.length()<3)base=(base+"user");
        base=base.substring(0,Math.min(40,base.length()));
        String candidate=base;int suffix=1;
        while(users.existsByUsernameIgnoreCaseAndIdNot(candidate,-1))candidate=base+suffix++;
        return candidate;
    }
    private String username(String requested,String email,int userId) {
        String name=requested==null||requested.isBlank()?suggestUsername(email):requested.trim().toLowerCase(Locale.ROOT);
        if(!name.matches("[A-Za-z0-9_.-]{3,50}"))throw new PeopleValidationException("username","Tên đăng nhập gồm 3–50 chữ, số, dấu chấm, gạch dưới hoặc gạch ngang.");
        if(users.existsByUsernameIgnoreCaseAndIdNot(name,userId))throw new PeopleValidationException("username","Tên đăng nhập đã tồn tại.");
        return name;
    }
    private void emailAvailable(String email,int customerId,int employeeId,int userId) {
        if(email==null || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))throw new PeopleValidationException("email","Thư điện tử không hợp lệ.");
        if(issues.emailUsed(email,customerId,employeeId,userId))throw new PeopleValidationException("email","Thư điện tử đã được một hồ sơ hoặc tài khoản khác sử dụng.");
    }
    private void beginIssue(User user,String password) {
        LocalDateTime now=LocalDateTime.now();
        if(user.getCredentialMailPendingUntil()!=null && user.getCredentialMailPendingUntil().isAfter(now))
            throw new PeopleValidationException("email","Đang gửi tài khoản. Vui lòng đợi một phút rồi thử lại.");
        user.setPassword(encoder.encode(password));user.setMustChangePassword(true);user.setCredentialMailFailed(true);
        user.setCredentialMailPendingUntil(now.plusMinutes(1));
    }
    private User customerUser(String requested,String email) {
        User user=new User();user.setUsername(username(requested,email,-1));user.setRole(roles.findByName("ROLE_USER"));
        if(user.getRole()==null)throw new IllegalStateException("Thiếu quyền khách hàng.");
        return user;
    }
    private Outcome deliver(Issued issued) {
        if(issued.password==null)return new Outcome(issued.personId,false,false);
        boolean sent=mail.send(issued.email,issued.name,issued.username,issued.password);
        new TransactionTemplate(transactions).executeWithoutResult(s->issues.delivery(issued.userId,issued.hash,sent));
        return new Outcome(issued.personId,true,sent);
    }

    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public Outcome saveCustomer(CustomerDto form) {
        Issued issued=new TransactionTemplate(transactions).execute(s->{
            var previous=form.getId()==null?null:people.customer(form.getId());
            int uid=previous==null||previous.getUser()==null?-1:previous.getUser().getId();
            String email=PeopleService.email(form.getEmail());emailAvailable(email,form.getId()==null?-1:form.getId(),-1,uid);
            var customer=people.saveCustomer(form);
            User user=customer.getUser();String password=null;
            if(user==null && form.getId()==null){user=customerUser(form.getUsername(),email);password=temporaryPassword();beginIssue(user,password);}
            if(user!=null){
                user.setUsername(username(form.getUsername()==null||form.getUsername().isBlank()?user.getUsername():form.getUsername(),email,uid));
                user.setEmail(email);user.setFullName(customer.getFullName());user.setPhone(customer.getPhone());
                customer.setUser(users.saveAndFlush(user));customers.saveAndFlush(customer);
                return new Issued(customer.getId(),user,password);
            }
            // Editing an old customer without an account does not issue credentials implicitly.
            User empty=new User();empty.setId(-1);return new Issued(customer.getId(),empty,null);
        });
        return deliver(issued);
    }

    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public Outcome saveEmployee(EmployeeDto form) {
        Issued issued=new TransactionTemplate(transactions).execute(s->{
            var previous=form.getId()==null?null:people.employee(form.getId());
            int uid=previous==null||previous.getUser()==null?-1:previous.getUser().getId();
            String email=PeopleService.email(form.getEmail());emailAvailable(email,-1,form.getId()==null?-1:form.getId(),uid);
            form.setUsername(username(form.getUsername(),email,uid));
            String password=uid<0?temporaryPassword():null;
            var employee=people.saveEmployeeGenerated(form,password==null?null:encoder.encode(password));
            User user=employee.getUser();user.setFullName(employee.getFullName());user.setPhone(employee.getPhone());user.setEmail(email);
            if(password!=null){user.setMustChangePassword(true);user.setCredentialMailFailed(true);user.setCredentialMailPendingUntil(LocalDateTime.now().plusMinutes(1));}
            users.saveAndFlush(user);return new Issued(employee.getId(),user,password);
        });
        return deliver(issued);
    }

    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public Outcome resend(String kind,int id,boolean confirmed) {
        Issued issued=new TransactionTemplate(transactions).execute(s->{
            boolean customer="customers".equals(kind);
            if(!customer && !"employees".equals(kind))throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            var c=customer?customers.findForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND)):null;
            var e=customer?null:employees.findForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
            User linked=customer?c.getUser():e.getUser();
            User user=linked==null?customerUser(null,PeopleService.email(c.getEmail())):users.lockById(linked.getId()).orElseThrow();
            if(!confirmed && (Boolean.FALSE.equals(user.getStatus()) || customer && !Boolean.TRUE.equals(c.getStatus()) || !customer && !Boolean.TRUE.equals(e.getStatus()) || linked!=null&&!Boolean.TRUE.equals(user.getMustChangePassword())))
                throw new PeopleValidationException("email","Cần xác nhận đặt lại mật khẩu của tài khoản này.");
            String email=PeopleService.email(customer?c.getEmail():e.getEmail());
            emailAvailable(email,customer?id:-1,customer?-1:id,user.getId()==null?-1:user.getId());
            LocalDateTime now=LocalDateTime.now();
            if(user.getCredentialMailWindow()==null || !user.getCredentialMailWindow().isAfter(now.minusHours(1))){user.setCredentialMailWindow(now);user.setCredentialMailCount(0);}
            if(user.getCredentialMailCount()>=Math.max(1,maxPerHour))throw new PeopleValidationException("email","Đã đạt giới hạn gửi lại tài khoản trong một giờ. Vui lòng thử lại sau.");
            user.setCredentialMailCount(user.getCredentialMailCount()+1);
            user.setEmail(email);user.setFullName(customer?c.getFullName():e.getFullName());user.setPhone(customer?c.getPhone():e.getPhone());
            if(linked==null)user.setStatus(Boolean.TRUE.equals(c.getStatus()));
            String password=temporaryPassword();beginIssue(user,password);users.saveAndFlush(user);
            if(customer){c.setUser(user);customers.saveAndFlush(c);}
            return new Issued(id,user,password);
        });
        return deliver(issued);
    }
}
