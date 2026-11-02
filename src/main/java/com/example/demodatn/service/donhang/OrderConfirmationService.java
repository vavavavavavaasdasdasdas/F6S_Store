package com.example.demodatn.service.donhang;
import com.example.demodatn.entity.donhang.Order;
import com.example.demodatn.util.MoneyFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class OrderConfirmationService {
    private final MoneyFormat money;
    public String text(Order o){
        StringBuilder s = new StringBuilder("Xác nhận đơn ").append(o.getOrderCode())
            .append("\nNgười nhận: ").append(o.getReceiverName()).append(" - ").append(o.getPhoneNumber())
            .append("\nĐịa chỉ: ").append(o.getShippingAddress());
        if (o.getShippingFee() != null && o.getShippingFee().compareTo(java.math.BigDecimal.ZERO) > 0) {
            s.append("\nPhí vận chuyển: ").append(money.format(o.getShippingFee())).append(" đ");
        } else {
            s.append("\nPhí vận chuyển do đơn vị vận chuyển tính và thu riêng.");
        }
        s.append("\nTổng thanh toán: ").append(money.format(o.getFinalAmount())).append(" đ")
         .append("\nPhương thức: ").append(payment(o.getPaymentMethod()));
        return s.toString();
    }
    private String payment(String method){return "BANK_TRANSFER".equals(method)||"TRANSFER".equals(method)?"Chuyển khoản ngân hàng":"Thanh toán khi nhận hàng (COD)";}
}
