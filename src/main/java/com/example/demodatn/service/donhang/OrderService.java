package com.example.demodatn.service.donhang;
import com.example.demodatn.dto.donhang.CartItemDto;
import com.example.demodatn.dto.donhang.CheckoutRequest;
import com.example.demodatn.entity.donhang.Invoice;
import com.example.demodatn.entity.donhang.Order;
import com.example.demodatn.entity.donhang.OrderDetail;
import com.example.demodatn.entity.khachhang.Customer;
import com.example.demodatn.entity.nhanvien.Employee;
import com.example.demodatn.entity.sanpham.ProductVariant;
import com.example.demodatn.repository.donhang.InvoiceRepository;
import com.example.demodatn.service.khuyenmai.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {
    @org.springframework.beans.factory.annotation.Value("${shop.shipping.fee:30000}") private BigDecimal configuredShippingFee;
    @org.springframework.beans.factory.annotation.Value("${shop.shipping.free-threshold:500000}") private BigDecimal freeShippingThreshold;
    private final VoucherService voucherService;
    private final PricingService pricingService;
    private final com.example.demodatn.service.sanpham.InventoryService inventoryService;

    private final InvoiceRepository invoices;
    private final OrderStatusService statuses;
    private final DeliveryService delivery;
    private final com.example.demodatn.repository.donhang.OrderRepository orders;

    @Transactional(rollbackFor = Exception.class)
    public Order placeOrder(CheckoutRequest request, List<CartItemDto> cartItems, Customer customer, BigDecimal ignoredShippingFee) {
        Order order = new Order();
        order.setCustomer(customer);
        order.setOrderType("ONLINE");

        order.setPaymentMethod(request.getPaymentMethod());

        order.setReceiverName(request.getReceiverName());
        order.setPhoneNumber(request.getPhone());
        order.setEmail(request.getEmail());
        order.setShippingAddress(request.getShippingAddress());
        var province=delivery.province(request.getProvinceId());order.setProvince(province);order.setProvinceName(province.getName());
        return saveSale(order, cartItems, request.getVoucherCode(), null);
    }

    @Transactional(rollbackFor = Exception.class)
    public Order createPosOrder(List<CartItemDto> items, Customer customer, Employee employee,
            String voucherCode, String paymentMethod, String address, BigDecimal cashReceived) {
        return createPosOrder(items,customer,employee,voucherCode,paymentMethod,address,cashReceived,null);
    }
    @Transactional(rollbackFor = Exception.class)
    public Order createPosOrder(List<CartItemDto> items, Customer customer, Employee employee,
            String voucherCode, String paymentMethod, String address, BigDecimal cashReceived,String reference) {
        if (employee == null || !Boolean.TRUE.equals(employee.getStatus()) || employee.getUser() == null || !Boolean.TRUE.equals(employee.getUser().getStatus()))
            throw new IllegalArgumentException("Tài khoản chưa gắn với nhân viên đang hoạt động.");
        if (!java.util.Set.of("CASH", "TRANSFER").contains(paymentMethod))
            throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ.");
        if (customer != null && !Boolean.TRUE.equals(customer.getStatus()))
            throw new IllegalArgumentException("Khách hàng đã bị vô hiệu hóa.");
        Order order = new Order();
        order.setCustomer(customer);
        order.setEmployee(employee);
        order.setOrderType("POS");
        String ref=reference;
        if(ref==null){do {ref=VietQrService.newReference();}while(orders.existsByPaymentReference(ref));}
        if(!ref.matches("F6S[A-Z0-9]{6}") || orders.existsByPaymentReference(ref))throw new IllegalArgumentException("Mã đối soát đã được sử dụng. Tạo giỏ mới rồi thử lại.");
        order.setPaymentReference(ref);

        order.setPaymentMethod(paymentMethod);

        order.setShippingAddress(address);
        return saveSale(order, items, voucherCode, cashReceived);
    }

    private Order saveSale(Order order, List<CartItemDto> items, String voucherCode, BigDecimal cashReceived) {
        var sorted = sortedItems(items);
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItemDto item : sorted) {
            ProductVariant variant = inventoryService.lock(item.getVariantId());
            BigDecimal price = pricingService.calculatePrice(variant).getFinalPrice();
            OrderDetail detail = new OrderDetail();
            detail.setProductVariant(variant);
            detail.setProductName(variant.getProduct().getName());
            detail.setSizeName(variant.getSize().getSizeValue());
            detail.setMaterialName(variant.getMaterial().getName());
            detail.setColorName(variant.getColor().getName());
            detail.setSoleName(variant.getSole().getName());
            detail.setPrice(price);
            detail.setQuantity(item.getQuantity());
            detail.setTotalPrice(price.multiply(BigDecimal.valueOf(item.getQuantity())));
            order.addDetail(detail);
            subtotal = subtotal.add(detail.getTotalPrice());
        }
        order.setTotalAmount(subtotal);
        order.setShippingFee("POS".equals(order.getOrderType()) || subtotal.compareTo(freeShippingThreshold) >= 0
                ? BigDecimal.ZERO : configuredShippingFee);
        order.setDiscountAmount(BigDecimal.ZERO);
        if (voucherCode != null && !voucherCode.isBlank()) {
            var result = voucherService.consume(voucherCode, order.getCustomer(), subtotal);
            order.setVoucher(result.voucher);
            order.setDiscountAmount(result.discountAmount);
        }
        order.setFinalAmount(subtotal.subtract(order.getDiscountAmount()).add(order.getShippingFee()));
        if ("POS".equals(order.getOrderType()) && "CASH".equals(order.getPaymentMethod())
                && (cashReceived == null || cashReceived.compareTo(order.getFinalAmount()) < 0))
            throw new IllegalArgumentException("Tiền khách đưa phải đủ thanh toán tổng đơn hàng.");
        order.setOrderCode("DH-" + UUID.randomUUID());
        statuses.place(order);
        Invoice invoice = new Invoice();
        invoice.setOrder(order);
        invoice.setInvoiceCode(String.format("HD%06d", order.getId()));
        invoice.setIssuedAt(order.getCreatedAt());
        invoice.setCustomerName(order.getCustomer() != null ? order.getCustomer().getFullName()
                : order.getReceiverName() != null ? order.getReceiverName() : "Khách lẻ");
        invoice.setCustomerPhone(order.getCustomer() != null ? order.getCustomer().getPhone() : order.getPhoneNumber());
        invoice.setEmployeeName(order.getEmployee() == null ? "Đơn trực tuyến" : order.getEmployee().getFullName());
        if ("POS".equals(order.getOrderType()) && "CASH".equals(order.getPaymentMethod())) {
            invoice.setCashReceived(cashReceived);
            invoice.setChangeAmount(cashReceived.subtract(order.getFinalAmount()));
        }
        invoices.save(invoice);
        return order;
    }

    public List<String> allowedStatuses(Order order) { return statuses.allowedStatuses(order); }
    public void confirmPayment(Integer id) { statuses.confirmPayment(id); }
    public void updateStatus(Integer id,String status) { statuses.updateStatus(id,status); }

    private List<CartItemDto> sortedItems(List<CartItemDto> items) {
        if (items == null || items.isEmpty() || items.stream().anyMatch(i -> i == null || i.getVariantId() == null || i.getQuantity() == null || i.getQuantity() <= 0))
            throw new IllegalArgumentException("Giỏ hàng hoặc số lượng không hợp lệ.");
        return items.stream().sorted(java.util.Comparator.comparing(CartItemDto::getVariantId)).toList();
    }

}
