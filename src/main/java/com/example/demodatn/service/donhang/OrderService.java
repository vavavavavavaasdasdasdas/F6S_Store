package com.example.demodatn.service.donhang;

import com.example.demodatn.dto.donhang.CartItemDto;
import com.example.demodatn.dto.donhang.CheckoutRequest;
import com.example.demodatn.dto.donhang.DeliveryAddressForm;
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
    private final ShippingFeeService shipping;
    private final VoucherService voucherService;
    private final PricingService pricingService;
    private final com.example.demodatn.service.sanpham.InventoryService inventoryService;

    private final InvoiceRepository invoices;
    private final OrderStatusService statuses;
    private final com.example.demodatn.repository.donhang.OrderRepository orders;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Transactional(rollbackFor = Exception.class)
    public Order placeOrder(CheckoutRequest request, List<CartItemDto> items, Customer customer, BigDecimal ignoredShippingFee) {
        return saveSale(online(request, items, customer), items, request.getVoucherCode(), null);
    }

    private Order online(CheckoutRequest request, List<CartItemDto> items, Customer customer) {
        var order = new Order();
        order.setCustomer(customer);
        order.setOrderType("ONLINE");
        order.setPaymentMethod(request.getPaymentMethod());
        order.setReceiverName(request.getReceiverName());
        order.setPhoneNumber(request.getPhone());
        order.setEmail(request.getEmail());
        shipping.apply(order, request, ShippingFeeService.quantity(items));
        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    public Order createAssistedOnline(CheckoutRequest request, List<CartItemDto> items, Customer customer, Employee employee, String source, String note) {
        if (employee == null || !Boolean.TRUE.equals(employee.getStatus()) || employee.getUser() == null || !Boolean.TRUE.equals(employee.getUser().getStatus()))
            throw new IllegalArgumentException("Tài khoản chưa gắn với nhân viên đang hoạt động.");
        if (!Order.SOURCE_LABELS.containsKey(source)) throw new IllegalArgumentException("Nguồn đơn không hợp lệ.");
        if (note != null && note.length() > 1000) throw new IllegalArgumentException("Ghi chú tối đa 1000 ký tự.");
        var order = online(request, items, customer);
        order.setEmployee(employee);
        order.setOrderSource(source);
        order.setSalesNote(note);
        return saveSale(order, items, request.getVoucherCode(), null);
    }

    @Transactional(rollbackFor = Exception.class)
    public Order createPosOrder(List<CartItemDto> items, Customer customer, Employee employee,
                                String voucherCode, String paymentMethod, String address, BigDecimal cashReceived) {
        return createPosOrder(items, customer, employee, voucherCode, paymentMethod, address, cashReceived, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public Order createPosOrder(List<CartItemDto> items, Customer customer, Employee employee,
                                String voucherCode, String paymentMethod, String address, BigDecimal cashReceived, String reference) {
        return createPosOrder(items, customer, employee, voucherCode, paymentMethod, address, cashReceived, null, reference, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public Order createPosOrder(List<CartItemDto> items, Customer customer, Employee employee,
                                String voucherCode, String paymentMethod, String address, BigDecimal cashReceived, BigDecimal transferAmount, String reference, String paymentNote) {
        if (employee == null || !Boolean.TRUE.equals(employee.getStatus()) || employee.getUser() == null || !Boolean.TRUE.equals(employee.getUser().getStatus()))
            throw new IllegalArgumentException("Tài khoản chưa gắn với nhân viên đang hoạt động.");
        if (!java.util.Set.of("CASH", "TRANSFER", "COMBINED", "VNPAY").contains(paymentMethod))
            throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ.");
        if (customer != null && !Boolean.TRUE.equals(customer.getStatus()))
            throw new IllegalArgumentException("Khách hàng đã bị vô hiệu hóa.");
        Order order = new Order();
        order.setCustomer(customer);
        order.setEmployee(employee);
        order.setOrderType("POS");
        String ref = reference;
        if (ref == null) {
            do {
                ref = VietQrService.newReference();
            } while (orders.existsByPaymentReference(ref));
        }
        if (!ref.matches("F6S[A-Z0-9]{6}") || orders.existsByPaymentReference(ref))
            throw new IllegalArgumentException("Mã đối soát đã được sử dụng. Tạo giỏ mới rồi thử lại.");
        order.setPaymentReference(ref);
        order.setPaymentMethod(paymentMethod);
        order.setShippingAddress(address);
        order.setPaymentNote(paymentNote);
        return saveSale(order, items, voucherCode, cashReceived, transferAmount);
    }

    @Transactional(rollbackFor = Exception.class)
    public Order createPosDeliveryOrder(List<CartItemDto> items, Customer customer, Employee employee,
                                        String voucherCode, String paymentMethod,
                                        String receiverName, String phone, String email,
                                        Integer provinceId, Integer wardId, String streetAddress,
                                        BigDecimal latitude, BigDecimal longitude, String coordinateSource,
                                        boolean transferReceived, boolean confirmNow, String note) {
        if (employee == null || !Boolean.TRUE.equals(employee.getStatus()) || employee.getUser() == null || !Boolean.TRUE.equals(employee.getUser().getStatus()))
            throw new IllegalArgumentException("Tài khoản chưa gắn với nhân viên đang hoạt động.");
        if (customer != null && !Boolean.TRUE.equals(customer.getStatus()))
            throw new IllegalArgumentException("Khách hàng đã bị vô hiệu hóa.");
        if ("CASH".equalsIgnoreCase(paymentMethod)) {
            paymentMethod = "COD";
        }
        if (!java.util.Set.of("COD", "TRANSFER", "VNPAY").contains(paymentMethod))
            throw new IllegalArgumentException("Đơn giao hàng chỉ hỗ trợ Tiền mặt, Chuyển khoản hoặc VNPay.");
        if (receiverName == null || receiverName.isBlank())
            throw new IllegalArgumentException("Vui lòng nhập họ tên người nhận.");
        if (phone == null || phone.isBlank())
            throw new IllegalArgumentException("Vui lòng nhập số điện thoại người nhận.");
        if (provinceId == null)
            throw new IllegalArgumentException("Vui lòng chọn Tỉnh/Thành phố giao hàng.");
        if (wardId == null)
            throw new IllegalArgumentException("Vui lòng chọn Phường/Xã giao hàng.");
        if (streetAddress == null || streetAddress.isBlank())
            throw new IllegalArgumentException("Vui lòng nhập địa chỉ chi tiết (số nhà, tên đường).");
        if (note != null && note.length() > 1000)
            throw new IllegalArgumentException("Ghi chú tối đa 1000 ký tự.");

        Order order = new Order();
        order.setCustomer(customer);
        order.setEmployee(employee);
        order.setOrderType("ONLINE");
        order.setPaymentMethod(paymentMethod);
        order.setReceiverName(receiverName.trim());
        order.setPhoneNumber(phone.trim());
        order.setEmail(email != null && !email.isBlank() ? email.trim() : null);
        order.setPaymentNote(note);
        order.setSalesNote(note);

        var addressForm = new DeliveryAddressForm();
        addressForm.setProvinceId(provinceId);
        addressForm.setWardId(wardId);
        addressForm.setStreetAddress(streetAddress.trim());
        addressForm.setLatitude(latitude);
        addressForm.setLongitude(longitude);
        addressForm.setCoordinateSource(coordinateSource);
        shipping.apply(order, addressForm, ShippingFeeService.quantity(items));

        if ("TRANSFER".equals(paymentMethod) || "VNPAY".equals(paymentMethod)) {
            String ref;
            do {
                ref = VietQrService.newReference();
            } while (orders.existsByPaymentReference(ref));
            order.setPaymentReference(ref);
        }

        saveSale(order, items, voucherCode, null, null);

        if ("TRANSFER".equals(paymentMethod) && transferReceived) {
            statuses.confirmPayment(order.getId());
        }
        if (confirmNow) {
            statuses.updateStatus(order.getId(), "CONFIRMED");
        }
        return order;
    }

    private Order saveSale(Order order, List<CartItemDto> items, String voucherCode, BigDecimal cashReceived) {
        return saveSale(order, items, voucherCode, cashReceived, null);
    }

    private Order saveSale(Order order, List<CartItemDto> items, String voucherCode, BigDecimal cashReceived, BigDecimal transferAmount) {
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
        if ("POS".equals(order.getOrderType())) {
            order.setShippingFee(BigDecimal.ZERO);
            order.setFreeShip(false);
            order.setShippingPolicyVersion(1);
        }
        order.setDiscountAmount(BigDecimal.ZERO);
        if (voucherCode != null && !voucherCode.isBlank()) {
            var result = voucherService.consume(voucherCode, order.getCustomer(), subtotal);
            order.setVoucher(result.voucher);
            order.setDiscountAmount(result.discountAmount);
        }
        order.setFinalAmount(subtotal.subtract(order.getDiscountAmount()).add(order.getShippingFee()));
        if ("POS".equals(order.getOrderType())) {
            if ("CASH".equals(order.getPaymentMethod())) {
                if (cashReceived == null || cashReceived.compareTo(order.getFinalAmount()) < 0)
                    throw new IllegalArgumentException("Tiền khách đưa phải đủ thanh toán tổng đơn hàng.");
                order.setCashAmount(order.getFinalAmount().longValue());
                order.setTransferAmount(0L);
            } else if ("TRANSFER".equals(order.getPaymentMethod()) || "VNPAY".equals(order.getPaymentMethod())) {
                order.setCashAmount(0L);
                order.setTransferAmount(order.getFinalAmount().longValue());
            } else if ("COMBINED".equals(order.getPaymentMethod())) {
                if (cashReceived == null || cashReceived.compareTo(BigDecimal.ZERO) <= 0)
                    throw new IllegalArgumentException("Tiền mặt phải lớn hơn 0 khi thanh toán kết hợp.");
                if (transferAmount == null || transferAmount.compareTo(BigDecimal.ZERO) <= 0)
                    throw new IllegalArgumentException("Tiền chuyển khoản phải lớn hơn 0 khi thanh toán kết hợp.");
                if (cashReceived.add(transferAmount).compareTo(order.getFinalAmount()) != 0)
                    throw new IllegalArgumentException("Tổng tiền mặt và tiền chuyển khoản phải bằng tổng thanh toán.");
                order.setCashAmount(cashReceived.longValue());
                order.setTransferAmount(transferAmount.longValue());
            }
        } else {
            if ("CASH".equals(order.getPaymentMethod()) || "COD".equals(order.getPaymentMethod())) {
                order.setCashAmount(order.getFinalAmount().longValue());
                order.setTransferAmount(0L);
            } else {
                order.setCashAmount(0L);
                order.setTransferAmount(order.getFinalAmount().longValue());
            }
        }
        order.setOrderCode("DH-" + UUID.randomUUID());
        statuses.place(order);
        Invoice invoice = new Invoice();
        invoice.setOrder(order);
        java.time.LocalDateTime issuedAt = order.getCreatedAt() != null ? order.getCreatedAt() : java.time.LocalDateTime.now();
        // Định dạng ngắn gọn theo thứ tự: giờ (HHmm), ngày (dd), tháng (MM), năm (yyyy) - không thêm giây
        String baseCode = "HD" + issuedAt.format(java.time.format.DateTimeFormatter.ofPattern("HHmmddMMyyyy"));
        String invoiceCode = baseCode;
        int seq = 1;
        while (invoices.existsByInvoiceCode(invoiceCode)) {
            invoiceCode = baseCode + "_" + seq++;
        }
        invoice.setInvoiceCode(invoiceCode);
        invoice.setIssuedAt(issuedAt);
        invoice.setCustomerName(order.getCustomer() != null ? order.getCustomer().getFullName()
                : order.getReceiverName() != null ? order.getReceiverName() : "Khách lẻ");
        invoice.setCustomerPhone(order.getCustomer() != null ? order.getCustomer().getPhone() : order.getPhoneNumber());
        invoice.setEmployeeName(order.getEmployee() == null ? "Đơn trực tuyến" : order.getEmployee().getFullName());
        if ("POS".equals(order.getOrderType())) {
            if ("CASH".equals(order.getPaymentMethod())) {
                invoice.setCashReceived(cashReceived);
                invoice.setChangeAmount(cashReceived.subtract(order.getFinalAmount()));
            } else if ("COMBINED".equals(order.getPaymentMethod())) {
                invoice.setCashReceived(cashReceived);
                invoice.setChangeAmount(BigDecimal.ZERO);
            }
        }
        invoices.save(invoice);
        eventPublisher.publishEvent(new OrderPlacedEvent(order.getId()));
        return order;
    }

    public List<String> allowedStatuses(Order order) { return statuses.allowedStatuses(order); }
    public void confirmPayment(Integer id) { statuses.confirmPayment(id); }
    public void updateStatus(Integer id, String status) { statuses.updateStatus(id, status); }

    private List<CartItemDto> sortedItems(List<CartItemDto> items) {
        if (items == null || items.isEmpty() || items.stream().anyMatch(i -> i == null || i.getVariantId() == null || i.getQuantity() == null || i.getQuantity() <= 0))
            throw new IllegalArgumentException("Giỏ hàng hoặc số lượng không hợp lệ.");
        return items.stream().sorted(java.util.Comparator.comparing(CartItemDto::getVariantId)).toList();
    }
}
