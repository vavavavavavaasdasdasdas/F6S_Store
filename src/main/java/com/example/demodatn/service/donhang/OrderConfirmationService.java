package com.example.demodatn.service.donhang;
import com.example.demodatn.entity.donhang.Order;
import com.example.demodatn.util.MoneyFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
@Service @RequiredArgsConstructor
public class OrderConfirmationService {
    private final MoneyFormat money;
    public String text(Order o){
        String fee=Boolean.TRUE.equals(o.getFreeShip())?"Miễn phí vận chuyển":money.format(o.getShippingFee())+" đ";
        StringBuilder s=new StringBuilder("Xác nhận đơn ").append(o.getOrderCode()).append("\nNgười nhận: ").append(o.getReceiverName()).append(" - ").append(o.getPhoneNumber()).append("\nĐịa chỉ: ").append(o.getShippingAddress()).append("\nPhí vận chuyển: ").append(fee).append("\nTổng thanh toán: ").append(money.format(o.getFinalAmount())).append(" đ\nPhương thức: ").append(payment(o.getPaymentMethod()));
        return s.toString();
    }
    private String payment(String method){return "BANK_TRANSFER".equals(method)||"TRANSFER".equals(method)?"Chuyển khoản ngân hàng":"Thanh toán khi nhận hàng (COD)";}
}
