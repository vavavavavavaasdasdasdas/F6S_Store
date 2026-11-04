package com.example.demodatn.service.donhang;

import com.example.demodatn.dto.donhang.CartItemDto;
import com.example.demodatn.dto.donhang.CheckoutRequest;
import com.example.demodatn.dto.khachhang.ShippingAddressForm;
import com.example.demodatn.entity.donhang.Order;
import com.example.demodatn.entity.khachhang.Customer;
import com.example.demodatn.service.khachhang.ShippingAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckoutService {
    private final OrderService orders;
    private final ShippingAddressService addresses;

    @Transactional
    public Order place(CheckoutRequest request, List<CartItemDto> cart, Customer customer, String username) {
        if (request.isSaveAddress() && request.getShippingAddressId() == null) {
            var address = new ShippingAddressForm();
            org.springframework.beans.BeanUtils.copyProperties(request, address);
            address.setAddress(request.getStreetAddress());
            addresses.save(username, address);
        }
        return orders.placeOrder(request, cart, customer, BigDecimal.ZERO);
    }
}
