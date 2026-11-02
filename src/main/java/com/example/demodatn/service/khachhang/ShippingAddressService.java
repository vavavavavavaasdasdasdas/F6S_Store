package com.example.demodatn.service.khachhang;

import com.example.demodatn.dto.khachhang.CustomerDto;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class ShippingAddressService {
    public static final String LIMIT_MESSAGE="Mỗi khách chỉ lưu tối đa 3 địa chỉ";
    private final CustomerRepository customers;
    private final ShippingAddressRepository addresses;
    private final Validator validator;
    private final Clock clock;
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
    @Transactional
    public void syncCustomerAddresses(Integer customerId, List<CustomerDto.AddressBlock> blocks, Integer defaultIndex, boolean replaceAll) {
        var customer = customers.findForUpdate(customerId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (blocks == null || blocks.isEmpty()) {
            throw new PeopleValidationException("address", "Vui lòng nhập số nhà, tên đường.");
        }
        if (blocks.size() > 3) {
            throw new PeopleValidationException("address", LIMIT_MESSAGE);
        }
        int defIdx = (defaultIndex != null && defaultIndex >= 0 && defaultIndex < blocks.size()) ? defaultIndex : 0;
        if (!replaceAll) {
            var b = blocks.get(0);
            var address = toForm(customer, b, true);
            addresses.findByCustomer_IdOrderBySlotAsc(customerId).stream()
                    .filter(ShippingAddress::isDefaultAddress)
                    .findFirst()
                    .ifPresent(a -> address.setId(a.getId()));
            saveForCustomer(customerId, address);
            return;
        }
        List<ShippingAddressForm> forms = new ArrayList<>(blocks.size());
        for (int i = 0; i < blocks.size(); i++) {
            var sf = toForm(customer, blocks.get(i), false);
            var errors = validator.validate(sf);
            if (!errors.isEmpty()) {
                var e = errors.iterator().next();
                throw new PeopleValidationException(e.getPropertyPath().toString(), e.getMessage());
            }
            try {
                shipping.validateAddress(sf);
            } catch (IllegalArgumentException ex) {
                throw new PeopleValidationException(ex.getMessage().contains("Phường") ? "wardId" : "streetAddress", ex.getMessage());
            }
            forms.add(sf);
        }
        var existing = addresses.findByCustomer_IdOrderBySlotAsc(customerId);
        var keepIds = blocks.stream().map(CustomerDto.AddressBlock::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        for (var old : new ArrayList<>(existing)) {
            if (!keepIds.contains(old.getId())) {
                existing.remove(old);
                addresses.delete(old);
            }
        }
        addresses.flush();
        if (!existing.isEmpty()) {
            existing.forEach(a -> a.setDefaultAddress(false));
            addresses.saveAllAndFlush(existing);
        }
        ShippingAddress defaultSaved = null;
        for (int i = 0; i < forms.size(); i++) {
            ShippingAddress saved = persist(customer, forms.get(i));
            if (i == defIdx) {
                defaultSaved = saved;
            }
        }
        var updatedRows = addresses.findByCustomer_IdOrderBySlotAsc(customerId);
        if (defaultSaved == null && !updatedRows.isEmpty()) {
            defaultSaved = updatedRows.get(0);
        }
        if (defaultSaved != null) {
            chooseDefault(customer, defaultSaved, updatedRows);
        }
    }
    private ShippingAddressForm toForm(Customer customer, CustomerDto.AddressBlock b, boolean makeDefault) {
        var sf = new ShippingAddressForm();
        sf.setId(b.getId());
        sf.setReceiverName(customer.getFullName());
        sf.setPhone(customer.getPhone());
        sf.setProvinceId(b.getProvinceId());
        sf.setWardId(b.getWardId());
        sf.setStreetAddress(b.getStreetAddress());
        sf.setAddress(b.getStreetAddress());
        sf.setLatitude(b.getLatitude());
        sf.setLongitude(b.getLongitude());
        sf.setCoordinateSource(b.getCoordinateSource());
        sf.setMakeDefault(makeDefault);
        return sf;
    }
    private ShippingAddress persist(Customer customer,ShippingAddressForm form) {
        final com.example.demodatn.entity.donhang.Province province;
        final com.example.demodatn.entity.donhang.Ward ward;
        try {ward=shipping.validateAddress(form);province=ward.getProvince();}
        catch(IllegalArgumentException ex){throw new PeopleValidationException(ex.getMessage().contains("Phường")?"wardId":"streetAddress",ex.getMessage());}
        var rows=addresses.findByCustomer_IdOrderBySlotAsc(customer.getId());
        ShippingAddress address=null;
        if(form.getId()!=null) {
            address=rows.stream().filter(a -> a.getId().equals(form.getId())).findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy địa chỉ."));
        }
        if(address==null) {
            if(rows.size()>=3) throw new PeopleValidationException("address",LIMIT_MESSAGE);
            address=new ShippingAddress();address.setCustomer(customer);address.setCreatedAt(LocalDateTime.now(clock));
            int slot=1;
            while(containsSlot(rows,slot))slot++;
            address.setSlot(slot);
        }
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
    @Transactional(readOnly=true)
    public ShippingAddress ownedForCustomer(Integer customerId,Integer id) {
        return addresses.findByIdAndCustomer_Id(id,customerId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy địa chỉ."));
    }
    @Transactional
    public void makeDefaultForCustomer(Integer customerId,Integer id) {
        makeDefault(customers.findForUpdate(customerId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND)),id);
    }
    @Transactional
    public void deleteForCustomer(Integer customerId,Integer id) {
        remove(customers.findForUpdate(customerId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND)),id);
    }
    @Transactional
    public void makeDefault(String username,Integer id) {makeDefault(owner(username,true),id);}
    private void makeDefault(Customer customer,Integer id) {
        var rows=addresses.findByCustomer_IdOrderBySlotAsc(customer.getId());
        var selected=rows.stream().filter(a -> a.getId().equals(id)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy địa chỉ."));
        chooseDefault(customer,selected,rows);
    }
    @Transactional
    public void delete(String username,Integer id) {remove(owner(username,true),id);}
    private void remove(Customer customer,Integer id) {
        var rows=addresses.findByCustomer_IdOrderBySlotAsc(customer.getId());
        var selected=rows.stream().filter(a -> a.getId().equals(id)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy địa chỉ."));
        boolean wasDefault=selected.isDefaultAddress();rows.remove(selected);addresses.delete(selected);addresses.flush();
        if(wasDefault) {
            if(rows.isEmpty()){customer.setAddress(null);customers.saveAndFlush(customer);}
            else chooseDefault(customer,rows.get(0),rows);
        }
    }
}
