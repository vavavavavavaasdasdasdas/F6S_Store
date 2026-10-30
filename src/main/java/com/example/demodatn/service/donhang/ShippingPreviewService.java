package com.example.demodatn.service.donhang;
import com.example.demodatn.dto.donhang.*;
import com.example.demodatn.entity.khachhang.Customer;
import com.example.demodatn.service.khuyenmai.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
@Service @RequiredArgsConstructor
public class ShippingPreviewService {
    private final PricingService prices;private final VoucherService vouchers;private final ShippingFeeService shipping;
    public record Preview(boolean valid,String message,BigDecimal discountAmount,BigDecimal subtotal,BigDecimal shippingFee,BigDecimal total,ShippingFeeService.Quote shipping) {}
    public Preview preview(List<CartItemDto> cart,DeliveryAddressForm address,Customer customer,String code,boolean geocode,String client){
        var subtotal=prices.refreshCart(cart);var quote=shipping.quote(address,ShippingFeeService.quantity(cart),geocode,client);
        BigDecimal discount=BigDecimal.ZERO;boolean valid=true;String message="";
        if(code!=null && !code.isBlank()){var result=vouchers.validateAndCalculate(code,customer,subtotal);valid=result.valid;message=result.message;if(valid)discount=result.discountAmount;}
        return new Preview(valid,message,discount,subtotal,quote.shippingFee(),subtotal.subtract(discount).add(quote.shippingFee()),quote);
    }
}
