package com.example.demodatn.controller.user;
import com.example.demodatn.dto.donhang.CartItemDto;
import com.example.demodatn.dto.donhang.CheckoutRequest;
import com.example.demodatn.entity.donhang.Order;
import com.example.demodatn.entity.khachhang.Customer;
import com.example.demodatn.repository.donhang.OrderRepository;
import com.example.demodatn.repository.khachhang.CustomerRepository;
import com.example.demodatn.service.donhang.OrderService;
import com.example.demodatn.service.donhang.OrderStatusService;
import com.example.demodatn.service.donhang.PricingService;
import com.example.demodatn.service.khuyenmai.VoucherService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class OrderController {

    private final OrderService orderService;
    private final CustomerRepository customerRepository;
    private final VoucherService voucherService;
    private final OrderRepository orderRepository;
    private final PricingService pricingService;
    @org.springframework.beans.factory.annotation.Autowired private com.example.demodatn.service.khachhang.ShippingAddressService shippingAddresses;
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.demodatn.repository.sanpham.ProductVariantRepository productVariants;

    @Value("${shop.shipping.fee:30000}")
    private BigDecimal shippingFee;

    @Value("${shop.shipping.free-threshold:500000}")
    private BigDecimal freeShippingThreshold;
    
    @Value("${shop.bank.name:Vietcombank}")
    private String bankName;
    @Value("${shop.bank.account:123456789}")
    private String bankAccount;
    @Value("${shop.bank.owner:SHOP GIAY}")
    private String bankOwner;

    public OrderController(OrderService orderService, CustomerRepository customerRepository, VoucherService voucherService, OrderRepository orderRepository, PricingService pricingService) {
        this.orderService = orderService;
        this.customerRepository = customerRepository;
        this.voucherService = voucherService;
        this.orderRepository = orderRepository;
        this.pricingService=pricingService;
    }

    private Customer getCustomer(Principal principal) {
        if (principal == null) return null;
        return customerRepository.findByUser_Username(principal.getName()).orElse(null);
    }

    @GetMapping("/checkout")
    public String checkoutPage(Model model, HttpSession session, Principal principal, RedirectAttributes redirectAttributes) {
        List<CartItemDto> cart = (List<CartItemDto>) session.getAttribute("cart");
        if (cart == null || cart.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Giỏ hàng rỗng.");
            return "redirect:/cart";
        }
        
        BigDecimal subTotal;
        try { subTotal=pricingService.refreshCart(cart); }
        catch(IllegalArgumentException ex) { redirectAttributes.addFlashAttribute("error",ex.getMessage());return "redirect:/cart"; }

        BigDecimal fee = subTotal.compareTo(freeShippingThreshold) >= 0 ? BigDecimal.ZERO : shippingFee;
        
        Customer customer = getCustomer(principal);
        var addressBook=customer==null ? java.util.List.<com.example.demodatn.entity.khachhang.ShippingAddress>of() : shippingAddresses.list(principal.getName());
        model.addAttribute("shippingAddresses",addressBook);
        if (!model.containsAttribute("checkoutRequest")) {
            CheckoutRequest req = new CheckoutRequest();
            if (customer != null) {
                req.setReceiverName(customer.getFullName());
                req.setPhone(customer.getPhone());
                req.setEmail(customer.getEmail());
                req.setShippingAddress(customer.getAddress());
                addressBook.stream().filter(com.example.demodatn.entity.khachhang.ShippingAddress::isDefaultAddress).findFirst().ifPresent(a -> {
                    req.setShippingAddressId(a.getId());req.setReceiverName(a.getReceiverName());req.setPhone(a.getPhone());req.setShippingAddress(a.getAddress());
                });
            }
            model.addAttribute("checkoutRequest", req);
        }
        
        model.addAttribute("cartItems", cart);
        Map<Integer,String> variantImages=new HashMap<>();
        Map<Integer,String> variantCodes=new HashMap<>();
        for (CartItemDto item:cart) productVariants.findById(item.getVariantId()).ifPresent(v->{
            variantImages.put(v.getId(),v.getProduct().getDisplayImageUrl());
            variantCodes.put(v.getId(),v.getProduct().getCode());
        });
        model.addAttribute("variantImages",variantImages);
        model.addAttribute("variantCodes",variantCodes);
        model.addAttribute("variantPrices",cart.stream().collect(java.util.stream.Collectors.toMap(CartItemDto::getVariantId,i->pricingService.priceForVariant(i.getVariantId()),(a,b)->a)));
        model.addAttribute("subTotal", subTotal);
        model.addAttribute("shippingFee", fee);
        model.addAttribute("total", subTotal.add(fee));
        
        model.addAttribute("bankName", bankName);
        model.addAttribute("bankAccount", bankAccount);
        model.addAttribute("bankOwner", bankOwner);

        return "user/thanh-toan";
    }
    
    @PostMapping("/checkout/validate-voucher-ajax")
    @ResponseBody
    public ResponseEntity<?> validateVoucher(@RequestParam String code, HttpSession session, Principal principal) {
        List<CartItemDto> cart = (List<CartItemDto>) session.getAttribute("cart");
        if (cart == null || cart.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "Giỏ hàng rỗng."));
        
        BigDecimal subTotal;
        try { subTotal=pricingService.refreshCart(cart); }
        catch(IllegalArgumentException ex) { return ResponseEntity.badRequest().body(Map.of("valid",false,"message",ex.getMessage())); }
        
        Customer customer = getCustomer(principal);
        VoucherService.VoucherResult res = voucherService.validateAndCalculate(code, customer, subTotal);
        
        Map<String, Object> response = new HashMap<>();
        response.put("valid", res.valid);
        response.put("message", res.message);
        BigDecimal fee=subTotal.compareTo(freeShippingThreshold)>=0 ? BigDecimal.ZERO : shippingFee;
        response.put("subtotal",subTotal);response.put("shippingFee",fee);
        response.put("total",subTotal.subtract(res.discountAmount).add(fee));
        if (res.valid) {
            response.put("discountAmount", res.discountAmount);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/checkout/place")
    public String placeOrder(@Valid @ModelAttribute("checkoutRequest") CheckoutRequest request, BindingResult result, 
                           HttpSession session, Principal principal, RedirectAttributes redirectAttributes, Model model) {
        
        List<CartItemDto> cart = (List<CartItemDto>) session.getAttribute("cart");
        if (cart == null || cart.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Giỏ hàng rỗng.");
            return "redirect:/cart";
        }
        
        BigDecimal subTotal;
        try { subTotal=pricingService.refreshCart(cart); }
        catch(IllegalArgumentException ex) { redirectAttributes.addFlashAttribute("error",ex.getMessage());return "redirect:/cart"; }

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.checkoutRequest", result);
            redirectAttributes.addFlashAttribute("checkoutRequest", request);
            return "redirect:/checkout";
        }

        BigDecimal fee = subTotal.compareTo(freeShippingThreshold) >= 0 ? BigDecimal.ZERO : shippingFee;
        Customer customer = getCustomer(principal);
        if(request.getShippingAddressId()!=null)shippingAddresses.owned(principal.getName(),request.getShippingAddressId());

        try {
            Order order = orderService.placeOrder(request, cart, customer, fee);
            session.removeAttribute("cart");
            return "redirect:/orders/success/" + order.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("checkoutRequest", request);
            return "redirect:/checkout";
        }
    }

    @org.springframework.beans.factory.annotation.Autowired private com.example.demodatn.util.Pagination pagination;

    @GetMapping("/orders")
    public String history(Principal principal,@RequestParam(defaultValue="0") String page,Model model) {
        Customer customer = getCustomer(principal);
        var result=customer==null ? new org.springframework.data.domain.PageImpl<Order>(List.of(),pagination.request(page,0,org.springframework.data.domain.Sort.unsorted()),0) : pagination.load(page,org.springframework.data.domain.Sort.unsorted(),request->orderRepository.findByCustomer_IdAndOrderTypeOrderByCreatedAtDescIdDesc(customer.getId(),"ONLINE",request));
        model.addAttribute("statusLabels",OrderStatusService.LABELS);
        model.addAttribute("orders",result.getContent());model.addAttribute("orderPage",result);
        return "user/don-hang";
    }

    @org.springframework.beans.factory.annotation.Autowired private OrderStatusService statuses;

    @PostMapping("/orders/{id}/cancel")
    public String cancel(@PathVariable Integer id,Principal principal,RedirectAttributes flash) {
        try { statuses.cancelByCustomer(id,principal.getName()); flash.addFlashAttribute("message","Đã hủy đơn hàng. Hàng đã trừ được hoàn kho một lần."); }
        catch(IllegalArgumentException ex) { flash.addFlashAttribute("error",ex.getMessage()); }
        return "redirect:/orders/"+id;
    }

    @GetMapping("/orders/{id}")
    public String detail(@PathVariable Integer id, Principal principal, Model model) {
        Order order = customerOrder(id,principal);
        model.addAttribute("order",order);model.addAttribute("orderDetails",order.getOrderDetails());
        model.addAttribute("statusLabels",OrderStatusService.LABELS);
        model.addAttribute("canCancel",statuses.allowedStatuses(order).contains("CANCELLED"));
        return "user/chi-tiet-don-hang";
    }

    private Order customerOrder(Integer id, Principal principal) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        Customer customer = getCustomer(principal);
        if(customer==null || order.getCustomer()==null || !customer.getId().equals(order.getCustomer().getId()))
            throw new org.springframework.security.access.AccessDeniedException("Không có quyền truy cập");
        return order;
    }

    @GetMapping("/orders/success/{id}")
    public String orderSuccess(@PathVariable Integer id, Principal principal, Model model) {
        Order order = customerOrder(id,principal);

        model.addAttribute("order", order);
        model.addAttribute("bankName", bankName);
        model.addAttribute("bankAccount", bankAccount);
        model.addAttribute("bankOwner", bankOwner);
        return "user/dat-hang-thanh-cong";
    }
}
