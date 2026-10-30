package com.example.demodatn.service.khachhang;

import com.example.demodatn.dto.khachhang.ShippingAddressForm;
import com.example.demodatn.entity.khachhang.Customer;
import com.example.demodatn.entity.khachhang.ShippingAddress;
import com.example.demodatn.exception.PeopleValidationException;
import com.example.demodatn.repository.khachhang.CustomerRepository;
import com.example.demodatn.repository.khachhang.ShippingAddressRepository;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service @RequiredArgsConstructor
public class ShippingAddressService {
    public static final String LIMIT_MESSAGE="Mỗi khách chỉ lưu tối đa 3 địa chỉ";
    private final CustomerRepository customers;
    private final ShippingAddressRepository addresses;
    private final Validator validator;
    private final Clock clock;
    private final com.example.demodatn.service.donhang.DeliveryService delivery;
    private final com.example.demodatn.service.donhang.ShippingFeeService shipping;

    private Customer owner(String username,boolean lock) {
        return (lock ? customers.findForUpdateByUsername(username) : customers.findByUser_Username(username))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy hồ sơ khách hàng."));
    }
    @Transactional(readOnly=true)
    public List<ShippingAddress> list(String username) {
        return addresses.findByCustomer_IdOrderBySlotAsc(owner(username,false).getId());
    }
    @Transactional(readOnly=true)
    public ShippingAddress owned(String username,Integer id) {
        return addresses.findByIdAndCustomer_Id(id,owner(username,false).getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy địa chỉ."));
    }
    @Transactional
    public ShippingAddress save(String username,ShippingAddressForm form) {
        var violations=validator.validate(form);
        if(!violations.isEmpty()) {
            var v=violations.stream().sorted(java.util.Comparator.comparing(x -> x.getPropertyPath().toString())).findFirst().orElseThrow();
            throw new PeopleValidationException(v.getPropertyPath().toString(),v.getMessage());
        }
        return persist(owner(username,true),form);
    }
    @Transactional
    public ShippingAddress saveForCustomer(Integer customerId,ShippingAddressForm form) {
        var customer=customers.findForUpdate(customerId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        var errors=validator.validate(form);
        if(!errors.isEmpty()){var e=errors.iterator().next();throw new PeopleValidationException(e.getPropertyPath().toString(),e.getMessage());}
        return persist(customer,form);
    }
    private ShippingAddress persist(Customer customer,ShippingAddressForm form) {
        final com.example.demodatn.entity.donhang.Province province;
        final com.example.demodatn.entity.donhang.Ward ward;
        try {ward=shipping.validateAddress(form);province=ward.getProvince();}
        catch(IllegalArgumentException ex){throw new PeopleValidationException(ex.getMessage().contains("Phường")?"wardId":"streetAddress",ex.getMessage());}
        var rows=addresses.findByCustomer_IdOrderBySlotAsc(customer.getId());
        ShippingAddress address;
        if(form.getId()==null) {
            if(rows.size()>=3) throw new PeopleValidationException("address",LIMIT_MESSAGE);
            address=new ShippingAddress();address.setCustomer(customer);address.setCreatedAt(LocalDateTime.now(clock));
            int slot=1;
            while(containsSlot(rows,slot))slot++;
            address.setSlot(slot);
        } else address=rows.stream().filter(a -> a.getId().equals(form.getId())).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy địa chỉ."));
        address.setProvince(province);address.setWard(ward);address.setStreetAddress(form.getStreetAddress().trim());
        var point=shipping.quote(form,0,false,"address-book");address.setLatitude(point.latitude());address.setLongitude(point.longitude());address.setCoordinateSource(point.coordinateSource());
        address.setReceiverName(form.getReceiverName().trim());address.setPhone(form.getPhone().trim());address.setAddress(form.getStreetAddress().trim());
        boolean chooseDefault=rows.isEmpty() || address.isDefaultAddress() || form.isMakeDefault();
        addresses.saveAndFlush(address);
        if(!rows.contains(address))rows.add(address);
        if(chooseDefault)chooseDefault(customer,address,rows);
        return address;
    }
    private boolean containsSlot(List<ShippingAddress> rows,int slot) { return rows.stream().anyMatch(a -> a.getSlot()==slot); }
    // Flush the old flag before setting the new one, respecting SQL Server's filtered unique index.
    private void chooseDefault(Customer customer,ShippingAddress selected,List<ShippingAddress> rows) {
        rows.forEach(a -> a.setDefaultAddress(false));addresses.saveAllAndFlush(rows);
        selected.setDefaultAddress(true);addresses.saveAndFlush(selected);
        customer.setAddress(selected.getAddress());customers.saveAndFlush(customer);
    }
    @Transactional
    public void makeDefault(String username,Integer id) {
        Customer customer=owner(username,true);var rows=addresses.findByCustomer_IdOrderBySlotAsc(customer.getId());
        var selected=rows.stream().filter(a -> a.getId().equals(id)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy địa chỉ."));
        chooseDefault(customer,selected,rows);
    }
    @Transactional
    public void delete(String username,Integer id) {
        Customer customer=owner(username,true);var rows=addresses.findByCustomer_IdOrderBySlotAsc(customer.getId());
        var selected=rows.stream().filter(a -> a.getId().equals(id)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy địa chỉ."));
        boolean wasDefault=selected.isDefaultAddress();rows.remove(selected);addresses.delete(selected);addresses.flush();
        if(wasDefault) {
            if(rows.isEmpty()){customer.setAddress(null);customers.saveAndFlush(customer);}
            else chooseDefault(customer,rows.get(0),rows);
        }
    }
}
