package com.example.demodatn.service.donhang;
import com.example.demodatn.dto.donhang.*;
import com.example.demodatn.entity.donhang.Order;
import com.example.demodatn.entity.khachhang.Customer;
import com.example.demodatn.repository.khachhang.CustomerRepository;
import com.example.demodatn.repository.nhanvien.EmployeeRepository;
import com.example.demodatn.service.PeopleService;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service @RequiredArgsConstructor
public class PosOnlineOrderService {
    private final CustomerRepository customers;private final EmployeeRepository employees;private final JdbcTemplate jdbc;private final Validator validator;private final OrderService orders;
    public List<Customer> byPhone(String raw){
        if(raw==null || !raw.matches("(?:0[0-9]{9}|[+]84[0-9]{9})"))return List.of();String phone=PeopleService.phone(raw);
        return customers.findByPhoneInOrderByIdAsc(List.of(phone,"+84"+phone.substring(1)));
    }
    @Transactional(rollbackFor=Exception.class)
    public Order create(String username,PosOnlineOrderForm form,List<CartItemDto> cart){
        var errors=validator.validate(form);if(!errors.isEmpty())throw new IllegalArgumentException(errors.iterator().next().getMessage());
        var employee=employees.findByUser_Username(username).orElseThrow(()->new IllegalArgumentException("Tài khoản chưa gắn với nhân viên."));
        String phone=PeopleService.phone(form.getPhone());
        Integer locked=jdbc.queryForObject("DECLARE @r INT; EXEC @r=sys.sp_getapplock @Resource=?,@LockMode='Exclusive',@LockOwner='Transaction',@LockTimeout=5000;SELECT @r",Integer.class,"customer-phone:"+phone);
        if(locked==null || locked<0)throw new IllegalArgumentException("Số điện thoại đang được xử lý, vui lòng thử lại.");
        var matches=byPhone(phone);Customer customer;
        if(matches.isEmpty()){customer=new Customer();customer.setCustomerCode("MXH-"+UUID.randomUUID().toString().replace("-","").substring(0,12));customer.setFullName(form.getReceiverName().trim());customer.setPhone(phone);customer.setAddress(form.getStreetAddress().trim());customer.setStatus(true);customer=customers.saveAndFlush(customer);}
        else {customer=matches.get(0);if(!Boolean.TRUE.equals(customer.getStatus()))throw new IllegalArgumentException("Khách hàng đã bị vô hiệu hóa.");}
        var request=new CheckoutRequest();org.springframework.beans.BeanUtils.copyProperties(form,request);request.setPhone(phone);
        // No account provisioning: guest checkout uses the same order/stock/state service as the website.
        return orders.createAssistedOnline(request,cart,customer,employee,form.getOrderSource(),form.getNote());
    }
}
