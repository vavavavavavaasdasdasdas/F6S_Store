package com.example.demodatn.service.donhang;
import com.example.demodatn.entity.donhang.Order;
import com.example.demodatn.util.MoneyFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
@Service @RequiredArgsConstructor
public class OrderConfirmationService {
    private final DeliveryService delivery;private final MoneyFormat money;
    public String text(Order o){
        var s=new StringBuilder("Xin chào ").append(o.getReceiverName()==null?"quý khách":o.getReceiverName()).append("!\nCửa hàng xác nhận đã tiếp nhận đơn ").append(o.getOrderCode()).append(".\nSản phẩm:\n");
        for(var d:o.getOrderDetails())s.append("- ").append(d.getProductName()).append(" / ").append(d.getSizeName()).append(" / ").append(d.getDisplayColorName()).append(" / ").append(d.getDisplayMaterialName()).append(" / ").append(d.getDisplaySoleName()).append(" × ").append(d.getQuantity()).append(" — ").append(money.format(d.getTotalPrice())).append(" đ\n");
        s.append("Tiền hàng: ").append(money.format(o.getTotalAmount())).append(" đ\nGiảm giá: ").append(money.format(o.getDiscountAmount())).append(" đ\nPhí vận chuyển: ").append(money.format(o.getShippingFee())).append(" đ\nTổng thanh toán: ").append(money.format(o.getFinalAmount())).append(" đ\nĐịa chỉ: ").append(o.getShippingAddress());
        String estimate=delivery.estimateText(o);if(estimate!=null)s.append("\n").append(estimate);return s.toString();
    }
}
