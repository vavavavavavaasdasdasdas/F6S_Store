package com.example.demodatn.controller.admin.donhang;
import com.example.demodatn.service.sanpham.ProductSalePolicy;
import com.example.demodatn.dto.donhang.CartItemDto;
import com.example.demodatn.dto.donhang.PosCheckoutRequest;
import com.example.demodatn.repository.donhang.InvoiceRepository;
import com.example.demodatn.repository.khachhang.CustomerRepository;
import com.example.demodatn.repository.nhanvien.EmployeeRepository;
import com.example.demodatn.repository.sanpham.MaterialRepository;
import com.example.demodatn.repository.sanpham.ProductVariantRepository;
import com.example.demodatn.repository.sanpham.SizeRepository;
import com.example.demodatn.service.donhang.OrderService;
import com.example.demodatn.service.donhang.PosCatalogQuery;
import com.example.demodatn.service.donhang.PosHoldService;
import com.example.demodatn.service.donhang.PricingService;
import com.example.demodatn.service.khuyenmai.VoucherService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.*;

@Controller
@RequestMapping({"/admin/pos","/admin/ban-online"})
@RequiredArgsConstructor
public class AdminPosController {
    private final jakarta.servlet.http.HttpServletRequest http;
    private String mode(){return http.getRequestURI().startsWith(http.getContextPath()+"/admin/ban-online")?"ONLINE":"POS";}
    private String base(){return "ONLINE".equals(mode())?"/admin/ban-online":"/admin/pos";}
    private String key(String value){return "ONLINE".equals(mode())?"online-"+value:value;}
    @ModelAttribute public void workspace(Model model){model.addAttribute("posBase",base());model.addAttribute("onlineMode","ONLINE".equals(mode()));}
    private final ProductVariantRepository variants;
    private final PricingService pricing;
    private final com.example.demodatn.service.donhang.VietQrService vietQr;
    private final VoucherService vouchers;
    private final CustomerRepository customers;
    private final com.example.demodatn.repository.khachhang.ShippingAddressRepository addressBook;
    private final EmployeeRepository employees;
    private final OrderService orders;
    private final com.example.demodatn.service.donhang.PosOnlineOrderService onlineOrders;
    private final com.example.demodatn.service.donhang.ShippingFeeService shipping;
    private final InvoiceRepository invoices;
    private final PosHoldService holds;
    private final PosCatalogQuery catalogQuery;
    private final com.example.demodatn.repository.sanpham.CategoryRepository categories;
    private final MaterialRepository materials;
    private final SizeRepository sizes;
    private final com.example.demodatn.service.nhanvien.AccountProvisioningService accounts;
    private final com.example.demodatn.service.PeopleService people;

    @GetMapping("/customers")
    public String customerSearch(@RequestParam(defaultValue="") String customerQuery,@RequestParam(defaultValue="0") String customerPage,Model model){model.addAttribute("customerPage",catalogQuery.customers(customerQuery,customerPage));model.addAttribute("customerQuery",customerQuery.trim());return "admin/ban-tai-quay/khach-hang :: results";}


    @GetMapping("/customers/{id}/addresses") @ResponseBody
    public Object customerAddresses(@PathVariable Integer id){
        var c=customers.findById(id).filter(x->Boolean.TRUE.equals(x.getStatus())).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        return java.util.Map.of("name",c.getFullName(),"phone",java.util.Objects.toString(c.getPhone(),""),"addresses",addressBook.findByCustomer_IdOrderBySlotAsc(id).stream().map(a->{var m=new java.util.LinkedHashMap<String,Object>();m.put("id",a.getId());m.put("label",a.getFullAddress());m.put("defaultAddress",a.isDefaultAddress());m.put("receiverName",a.getReceiverName());m.put("phone",a.getPhone());m.put("provinceId",a.getProvince()==null?null:a.getProvince().getId());m.put("wardId",a.getWard()==null?null:a.getWard().getId());m.put("streetAddress",a.getDisplayStreetAddress());m.put("latitude",a.getLatitude());m.put("longitude",a.getLongitude());m.put("coordinateSource",a.getCoordinateSource());return m;}).toList());
    }

    @PostMapping("/customers/quick-create")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> quickCreateCustomer(
            @RequestParam String fullName,
            @RequestParam String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Integer provinceId,
            @RequestParam(required = false) Integer wardId,
            @RequestParam(required = false) String streetAddress) {
        try {
            com.example.demodatn.dto.khachhang.CustomerDto form = new com.example.demodatn.dto.khachhang.CustomerDto();
            form.setFullName(fullName != null ? fullName.trim() : "");
            form.setPhone(phone != null ? phone.trim() : "");
            form.setEmail(email != null && !email.isBlank() ? email.trim() : null);
            String code = "KH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
            form.setCustomerCode(code);
            form.setProvinceId(provinceId != null ? provinceId : 1);
            form.setWardId(wardId != null ? wardId : 1);
            form.setStreetAddress(streetAddress != null && !streetAddress.isBlank() ? streetAddress.trim() : "Tại quầy");
            form.setAddress(form.getStreetAddress());

            if (form.getEmail() != null && !form.getEmail().isBlank()) {
                var outcome = accounts.saveCustomer(form);
                var c = customers.findById(outcome.personId()).orElseThrow();
                return org.springframework.http.ResponseEntity.ok(Map.of(
                        "id", c.getId(),
                        "customerCode", c.getCustomerCode(),
                        "fullName", c.getFullName(),
                        "phone", c.getPhone(),
                        "email", c.getEmail() != null ? c.getEmail() : "",
                        "accountIssued", outcome.issued(),
                        "accountSent", outcome.sent(),
                        "message", outcome.message()
                ));
            } else {
                var c = people.saveCustomer(form);
                return org.springframework.http.ResponseEntity.ok(Map.of(
                        "id", c.getId(),
                        "customerCode", c.getCustomerCode(),
                        "fullName", c.getFullName(),
                        "phone", c.getPhone(),
                        "email", "",
                        "accountIssued", false,
                        "accountSent", false,
                        "message", "Đã lưu thông tin khách hàng."
                ));
            }
        } catch (com.example.demodatn.exception.PeopleValidationException ex) {
            return org.springframework.http.ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            return org.springframework.http.ResponseEntity.badRequest().body(Map.of("message", "Lỗi tạo khách hàng: " + ex.getMessage()));
        }
    }

    private void clearCart(HttpSession session) {
        for(String key:List.of("posCart","posCheckoutToken","posLoadedHold","posHeldRequest","posHoldReview","posPaymentReference","posOnlineRequest"))session.removeAttribute(key(key));
    }

    @SuppressWarnings("unchecked")
    private Map<Integer,BigDecimal> prices(HttpSession session) {
        var snapshots=(Map<Long,Map<Integer,BigDecimal>>)session.getAttribute(key("posHoldPrices"));
        if(snapshots==null){snapshots=new HashMap<>();session.setAttribute(key("posHoldPrices"),snapshots);}
        return snapshots.computeIfAbsent((Long)session.getAttribute(key("posLoadedHold")),id->new HashMap<>());
    }

    @SuppressWarnings("unchecked")
    private List<CartItemDto> cart(HttpSession session) {
        var cart = (List<CartItemDto>) session.getAttribute(key("posCart"));
        if (cart == null) { cart = new ArrayList<>(); session.setAttribute(key("posCart"), cart); }
        return cart;
    }

    @GetMapping
    public String page(@RequestParam(defaultValue="") String q,
                       @RequestParam(required=false) Integer category,
                       @RequestParam(required=false) Integer materialId,
                       @RequestParam(required=false) Integer sizeId,
                       @RequestParam(defaultValue="0") String page,
                       @RequestParam(defaultValue="") String customerQuery,
                       @RequestParam(defaultValue="0") String customerPage,
                       HttpSession session, Principal principal, Model model) {
        synchronized(session) {
            var cart = cart(session);
            var productPage=catalogQuery.products(q,category,materialId,sizeId,page);
            var available=productPage.isEmpty()?List.<com.example.demodatn.entity.sanpham.ProductVariant>of():variants.findByProduct_IdInAndStatusTrueAndProduct_StatusTrue(productPage.getContent().stream().map(p->p.getId()).toList());
            model.addAttribute("categories",categories.findAll(org.springframework.data.domain.Sort.by("name","id")));
            model.addAttribute("materials",materials.findAll(org.springframework.data.domain.Sort.by("name","id")));
            model.addAttribute("sizes",sizes.findAll(org.springframework.data.domain.Sort.by("id")));
            model.addAttribute("products",productPage.getContent());model.addAttribute("productPage",productPage);
            model.addAttribute("variants",available.stream().filter(v->v.getQuantity()>0
                    && (materialId == null || v.getMaterial().getId().equals(materialId))
                    && (sizeId == null || v.getSize().getId().equals(sizeId))).toList());
            var prices=pricing.calculatePrices(available);
            model.addAttribute("variantPrices", prices);
            model.addAttribute("productPrices", available.stream().collect(java.util.stream.Collectors.toMap(v->v.getProduct().getId(),v->prices.get(v.getId()).getFinalPrice(),BigDecimal::min)));
            model.addAttribute("q", q);
            model.addAttribute("category", category);
            model.addAttribute("materialId", materialId);
            model.addAttribute("sizeId", sizeId);
            int activeFilterCount = 0;
            if (q != null && !q.trim().isEmpty()) activeFilterCount++;
            if (category != null) activeFilterCount++;
            if (materialId != null) activeFilterCount++;
            if (sizeId != null) activeFilterCount++;
            model.addAttribute("activeFilterCount", activeFilterCount);
            model.addAttribute("posCart", cart);
            var productCodes=new HashMap<Integer,String>();
            for(var v:variants.findAllById(cart.stream().map(CartItemDto::getVariantId).toList()))
                productCodes.put(v.getId(),v.getProduct().getCode());
            model.addAttribute("productCodes",productCodes);
            boolean valid = true;
            var lineWarnings=new HashMap<Integer,String>();
            Long loadedId=(Long)session.getAttribute(key("posLoadedHold"));
            if(loadedId!=null) {
                // Revalidate expiry/ownership on every displayed held basket as well as checkout.
                try {holds.open(principal.getName(),loadedId,mode());} catch(org.springframework.web.server.ResponseStatusException ex) {
                    clearCart(session);cart=cart(session);model.addAttribute("posCart",cart);
                    model.addAttribute("error","Hóa đơn chờ đã hết hạn hoặc không còn tồn tại.");loadedId=null;
                }
            }
            if(loadedId!=null) {
                var baseline=prices(session);
                for(var item:cart) {
                    var v=variants.findById(item.getVariantId()).orElse(null);
                    if(!ProductSalePolicy.isSelling(v))
                        lineWarnings.put(item.getVariantId(),"Sản phẩm đã ngừng bán. Xóa dòng này để tiếp tục.");
                    else {
                        var current=pricing.calculatePrice(v).getFinalPrice();
                        var previous=baseline.get(item.getVariantId());
                        if(previous!=null && previous.compareTo(current)!=0){session.setAttribute(key("posHoldReview"),true);lineWarnings.put(item.getVariantId(),"Giá đã thay đổi. Xác nhận giá mới trước khi thanh toán.");}
                        item.setPrice(current);
                        if(v.getQuantity()<item.getQuantity())lineWarnings.put(item.getVariantId(),v.getQuantity()==0 ? "Hết hàng. Xóa dòng hoặc chờ nhập hàng." : "Không đủ tồn kho. Giảm số lượng và lưu lại.");
                    }
                }
            }
            try { pricing.refreshCart(cart); } catch (IllegalArgumentException ex) { valid=false; model.addAttribute("error",ex.getMessage()); }
            if(Boolean.TRUE.equals(session.getAttribute(key("posHoldReview"))))valid=false;
            model.addAttribute("lineWarnings",lineWarnings);
            model.addAttribute("holdNeedsReview",Boolean.TRUE.equals(session.getAttribute(key("posHoldReview"))));
            model.addAttribute("loadedHoldId",loadedId);
            model.addAttribute("heldOrders",holds.list(principal.getName(),mode()));
            model.addAttribute("cartValid", valid);
            model.addAttribute("total", cart.stream().map(CartItemDto::getTotalPrice).reduce(BigDecimal.ZERO,BigDecimal::add));
            if(session.getAttribute(key("posCheckoutToken"))==null) session.setAttribute(key("posCheckoutToken"), UUID.randomUUID().toString());
            if(!model.containsAttribute("posCheckoutRequest")) {
                var request = (PosCheckoutRequest)session.getAttribute(key("posHeldRequest"));
                if(request==null){request=new PosCheckoutRequest();request.setPaymentMethod("CASH");}
                model.addAttribute("posCheckoutRequest", request);
            }
            model.addAttribute("checkoutToken", session.getAttribute(key("posCheckoutToken")));
            model.addAttribute("customerPage",org.springframework.data.domain.Page.empty(org.springframework.data.domain.PageRequest.of(0,12)));
            model.addAttribute("customerQuery","");
            if(session.getAttribute(key("posPaymentReference"))==null)session.setAttribute(key("posPaymentReference"),com.example.demodatn.service.donhang.VietQrService.newReference());
            model.addAttribute("paymentReference",session.getAttribute(key("posPaymentReference")));
        }
        if(!model.containsAttribute("posOnlineRequest"))model.addAttribute("posOnlineRequest",(session.getAttribute(key("posOnlineRequest")) instanceof com.example.demodatn.dto.donhang.PosOnlineOrderForm saved?saved:new com.example.demodatn.dto.donhang.PosOnlineOrderForm()));
        model.addAttribute("orderSources",com.example.demodatn.entity.donhang.Order.SOURCE_LABELS);
        return "admin/ban-tai-quay/ban-tai-quay";
    }

    @PostMapping("/add")
    public String add(@RequestParam Integer variantId, @RequestParam Integer quantity, HttpSession session, RedirectAttributes flash) {
        synchronized(session) {
            try {
                var v = variants.findById(variantId).orElseThrow(()->new IllegalArgumentException("Sản phẩm không tồn tại."));
                if(quantity<=0 || !ProductSalePolicy.isSelling(v))
                    throw new IllegalArgumentException("Sản phẩm hoặc số lượng không hợp lệ.");
                var cart = cart(session);
                var item = cart.stream().filter(i->i.getVariantId().equals(variantId)).findFirst().orElse(null);
                long totalQty = quantity + (item == null ? 0L : item.getQuantity());
                if(totalQty > v.getQuantity()) throw new IllegalArgumentException("Số lượng vượt quá tồn kho.");
                if(item == null) cart.add(new CartItemDto(v.getProduct().getId(),v.getId(),v.getProduct().getName(),v.getSize().getSizeValue(),v.getMaterial().getName(),v.getColor().getName(),v.getSole().getName(),pricing.calculatePrice(v).getFinalPrice(),quantity));
                else item.setQuantity((int)totalQty);
                session.removeAttribute(key("posCheckoutToken"));
            } catch(IllegalArgumentException ex) { flash.addFlashAttribute("error",ex.getMessage()); }
        }
        return "redirect:"+base();
    }

    @PostMapping("/quantity")
    public String quantity(@RequestParam Integer variantId,@RequestParam Integer quantity,HttpSession session,RedirectAttributes flash) {
        synchronized(session) {
            try {
                var v=variants.findById(variantId).orElseThrow(()->new IllegalArgumentException("Sản phẩm không tồn tại."));
                if(quantity<=0 || quantity>v.getQuantity()) throw new IllegalArgumentException("Số lượng phải từ 1 đến tồn kho hiện tại.");
                cart(session).stream().filter(i->i.getVariantId().equals(variantId)).findFirst()
                        .orElseThrow(()->new IllegalArgumentException("Dòng không còn trong giỏ.")).setQuantity(quantity);
                session.removeAttribute(key("posCheckoutToken"));
            } catch(IllegalArgumentException ex) { flash.addFlashAttribute("error",ex.getMessage()); }
        }
        return "redirect:"+base();
    }

    @PostMapping("/remove")
    public String remove(@RequestParam Integer variantId,HttpSession session) {
        synchronized(session) { cart(session).removeIf(i->i.getVariantId().equals(variantId)); session.removeAttribute(key("posCheckoutToken")); }
        return "redirect:"+base();
    }

    @PostMapping("/validate-voucher") @ResponseBody
    public org.springframework.http.ResponseEntity<?> voucher(@RequestParam String code,@RequestParam(required=false) Integer customerId,HttpSession session) {
        synchronized(session) {
            try {
                if(cart(session).isEmpty()) throw new IllegalArgumentException("Giỏ hàng trống.");
                var customer=customerId==null ? null : customers.findById(customerId).orElseThrow(()->new IllegalArgumentException("Khách hàng không tồn tại."));
                var subtotal=pricing.refreshCart(cart(session));
                var result=vouchers.validateAndCalculate(code,customer,subtotal);
                return org.springframework.http.ResponseEntity.ok(Map.of("valid",result.valid,"message",result.message,"discountAmount",result.discountAmount,"subtotal",subtotal,"shippingFee",BigDecimal.ZERO,"total",subtotal.subtract(result.discountAmount)));
            } catch(IllegalArgumentException ex) { return org.springframework.http.ResponseEntity.badRequest().body(Map.of("valid",false,"message",ex.getMessage())); }
        }
    }

    @PostMapping("/payment-qr") @ResponseBody
    public com.example.demodatn.service.donhang.VietQrService.PaymentQr paymentQr(@ModelAttribute PosCheckoutRequest request,HttpSession session) {
        synchronized(session) {
            try {
                var total=transferTotal(request,session);
                if(session.getAttribute(key("posPaymentReference"))==null)session.setAttribute(key("posPaymentReference"),com.example.demodatn.service.donhang.VietQrService.newReference());
                return vietQr.create(total,(String)session.getAttribute(key("posPaymentReference")));
            } catch(IllegalArgumentException ex){return new com.example.demodatn.service.donhang.VietQrService.PaymentQr(false,ex.getMessage(),null,null,null,null,null,null);}
        }
    }
    private BigDecimal transferTotal(PosCheckoutRequest request,HttpSession session) {
        var cart=cart(session);if(cart.isEmpty())throw new IllegalArgumentException("Giỏ hàng trống.");
        if(Boolean.TRUE.equals(session.getAttribute(key("posHoldReview"))))throw new IllegalArgumentException("Xác nhận lại giá hóa đơn chờ trước khi chuyển khoản.");
        var loaded=variants.findAllById(cart.stream().map(CartItemDto::getVariantId).toList());
        var byId=loaded.stream().collect(java.util.stream.Collectors.toMap(v->v.getId(),v->v));
        var priceMap=pricing.calculatePrices(loaded);BigDecimal subtotal=BigDecimal.ZERO;
        for(var item:cart) {
            var variant=byId.get(item.getVariantId());
            if(!ProductSalePolicy.isSelling(variant) || item.getQuantity()==null || item.getQuantity()<=0 || variant.getQuantity()<item.getQuantity())throw new IllegalArgumentException("Sản phẩm đã ngừng bán hoặc không đủ tồn kho.");
            var price=priceMap.get(variant.getId()).getFinalPrice();
            if(session.getAttribute(key("posLoadedHold"))!=null && price.compareTo(item.getPrice())!=0)throw new IllegalArgumentException("Giá đã thay đổi. Xác nhận lại hóa đơn chờ trước khi chuyển khoản.");
            subtotal=subtotal.add(price.multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        var customer=request.getCustomerId()==null?null:customers.findById(request.getCustomerId()).filter(c->Boolean.TRUE.equals(c.getStatus())).orElseThrow(()->new IllegalArgumentException("Khách hàng không còn hoạt động."));
        if(request.getVoucherCode()!=null && !request.getVoucherCode().isBlank()) {
            var result=vouchers.validateAndCalculate(request.getVoucherCode(),customer,subtotal);
            if(!result.valid)throw new IllegalArgumentException(result.message);
            subtotal=subtotal.subtract(result.discountAmount);
        }
        return subtotal;
    }

    @PostMapping("/checkout")
    public String checkout(@Valid @ModelAttribute("posCheckoutRequest") PosCheckoutRequest request,BindingResult errors,
            HttpSession session,Principal principal,RedirectAttributes flash) {
        if("ONLINE".equals(mode()))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
        // A session cart is serialized so repeated checkout cannot sell the same basket twice.
        synchronized(session) {
            try {
                if(errors.hasErrors()) throw new IllegalArgumentException("Kiểm tra phương thức thanh toán và tiền khách đưa.");
                if(request.getCheckoutToken()==null || !request.getCheckoutToken().equals(session.getAttribute(key("posCheckoutToken"))))
                    throw new IllegalArgumentException("Phiên thanh toán đã thay đổi. Kiểm tra giỏ rồi thanh toán lại.");
                if(cart(session).isEmpty()) throw new IllegalArgumentException("Giỏ hàng trống.");
                var employee=employees.findByUser_Username(principal.getName()).orElseThrow(()->new IllegalArgumentException("Tài khoản chưa gắn với nhân viên."));
                var customer=request.getCustomerId()==null ? null : customers.findById(request.getCustomerId()).orElseThrow(()->new IllegalArgumentException("Khách hàng không tồn tại."));
                if(customer!=null && !Boolean.TRUE.equals(customer.getStatus())) throw new IllegalArgumentException("Khách hàng đã bị vô hiệu hóa.");
                if("TRANSFER".equals(request.getPaymentMethod())) {
                    if(!request.isTransferReceived())throw new IllegalArgumentException("Cần xác nhận đã nhận đủ số tiền chuyển khoản.");
                    var expected=transferTotal(request,session);
                    if(request.getTransferAmount()==null || expected.compareTo(request.getTransferAmount())!=0)
                        throw new IllegalArgumentException("Tổng tiền đã thay đổi. Kiểm tra lại QR và xác nhận số tiền mới.");
                }
                request.setPaymentReference((String)session.getAttribute(key("posPaymentReference")));
                Long heldId=(Long)session.getAttribute(key("posLoadedHold"));
                if(heldId!=null && Boolean.TRUE.equals(session.getAttribute(key("posHoldReview"))))throw new IllegalArgumentException("Xác nhận giá đã tính lại trước khi thanh toán.");
                var order=heldId==null ? orders.createPosOrder(cart(session),customer,employee,request.getVoucherCode(),request.getPaymentMethod(),"Tại quầy",request.getCashReceived(),request.getPaymentReference())
                        : holds.checkout(principal.getName(),heldId,cart(session),request);
                clearCart(session);
                return "redirect:"+base()+"/success/"+order.getId();
            } catch(org.springframework.web.server.ResponseStatusException ex) {throw ex;}
            catch(IllegalArgumentException ex) { flash.addFlashAttribute("error",ex.getMessage()); }
            catch(Exception ex) { flash.addFlashAttribute("error","Không thể tạo đơn. Giỏ được giữ lại; kiểm tra tồn kho và thử lại."); }
            flash.addFlashAttribute("posCheckoutRequest",request);
            if(errors.hasErrors())flash.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX+"posCheckoutRequest",errors);
            return "redirect:"+base();
        }
    }

    @PostMapping("/online-orders")
    public String onlineOrder(@Valid @ModelAttribute("posOnlineRequest") com.example.demodatn.dto.donhang.PosOnlineOrderForm form,BindingResult errors,HttpSession session,Principal principal,RedirectAttributes flash){
        if(!"ONLINE".equals(mode()))return "redirect:/admin/ban-online";
        synchronized(session){try{
            if(errors.hasErrors())throw new IllegalArgumentException("Kiểm tra thông tin người nhận và địa chỉ giao hàng.");
            if(form.getCheckoutToken()==null || !form.getCheckoutToken().equals(session.getAttribute(key("posCheckoutToken"))))throw new IllegalArgumentException("Phiên thanh toán đã thay đổi. Kiểm tra giỏ rồi tạo đơn lại.");
            if(cart(session).isEmpty())throw new IllegalArgumentException("Giỏ hàng trống.");
            if(Boolean.TRUE.equals(session.getAttribute(key("posHoldReview"))))throw new IllegalArgumentException("Xác nhận lại giá hóa đơn chờ trước khi tạo đơn.");
            shipping.prepare(form,principal.getName());Long heldId=(Long)session.getAttribute(key("posLoadedHold"));
            var order=heldId==null?onlineOrders.create(principal.getName(),form,cart(session)):holds.checkoutOnline(principal.getName(),heldId,cart(session),form);
            clearCart(session);flash.addFlashAttribute("message","Đã tạo đơn giao hàng online. Đơn đang chờ xác nhận.");return "redirect:/admin/invoices/"+invoices.findByOrder_Id(order.getId()).orElseThrow().getId();
        }catch(IllegalArgumentException ex){flash.addFlashAttribute("error",ex.getMessage());}
        catch(org.springframework.web.server.ResponseStatusException ex){throw ex;}
        catch(Exception ex){flash.addFlashAttribute("error","Không thể tạo đơn. Giỏ được giữ lại; kiểm tra tồn kho và thử lại.");}
        flash.addFlashAttribute("posOnlineRequest",form);flash.addFlashAttribute("deliveryMode",true);if(errors.hasErrors())flash.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX+"posOnlineRequest",errors);return "redirect:"+base();}
    }

    @PostMapping("/holds")
    public String hold(@ModelAttribute PosCheckoutRequest request,@ModelAttribute com.example.demodatn.dto.donhang.PosOnlineOrderForm online,HttpSession session,Principal principal,RedirectAttributes flash) {
        synchronized(session) {
            try {
                Long id=holds.save(principal.getName(),cart(session),request,(Long)session.getAttribute(key("posLoadedHold")),mode(),"ONLINE".equals(mode())?online:null);
                session.setAttribute(key("posLoadedHold"),id);var baseline=prices(session);baseline.clear();
                cart(session).forEach(i->baseline.put(i.getVariantId(),i.getPrice()));
                clearCart(session);flash.addFlashAttribute("success","Đã lưu hóa đơn chờ. Giỏ hiện tại đã trống.");
            } catch(IllegalArgumentException ex) {flash.addFlashAttribute("error",ex.getMessage());}
        }
        return "redirect:"+base();
    }

    @PostMapping("/holds/{id}/open")
    public String openHold(@PathVariable Long id,@RequestParam(defaultValue="CANCEL") String action,
            @ModelAttribute PosCheckoutRequest request,@ModelAttribute com.example.demodatn.dto.donhang.PosOnlineOrderForm online,HttpSession session,Principal principal,RedirectAttributes flash) {
        synchronized(session) {
            if(!cart(session).isEmpty() && !Set.of("SAVE","DISCARD").contains(action)) {
                flash.addFlashAttribute("error","Chọn lưu hoặc bỏ giỏ hiện tại trước khi mở hóa đơn chờ.");return "redirect:"+base();
            }
            try {
                var loaded=holds.switchTo(principal.getName(),id,cart(session),request,(Long)session.getAttribute(key("posLoadedHold")),"SAVE".equals(action),mode(),"ONLINE".equals(mode())?online:null);
                clearCart(session);session.setAttribute(key("posCart"),loaded.cart());session.setAttribute(key("posLoadedHold"),id);
                session.setAttribute(key("posHeldRequest"),loaded.request());session.setAttribute(key("posOnlineRequest"),loaded.online());
                var baseline=prices(session);
                // Prices are never persisted in a held order. A browser/session snapshot can identify a change;
                // after restart the old price is unknown, so require explicit review of recalculated prices.
                if(baseline.isEmpty()) {loaded.cart().forEach(i->baseline.put(i.getVariantId(),i.getPrice()));session.setAttribute(key("posHoldReview"),true);}
                var checkout=loaded.request();
                if(checkout.getCustomerId()!=null && customers.findById(checkout.getCustomerId()).filter(c->Boolean.TRUE.equals(c.getStatus())).isEmpty()) {
                    checkout.setCustomerId(null);flash.addFlashAttribute("error","Khách đã bị vô hiệu hóa. Chuyển về Khách lẻ.");
                }
                if(checkout.getVoucherCode()!=null && !checkout.getVoucherCode().isBlank()) {
                    var customer=checkout.getCustomerId()==null ? null : customers.findById(checkout.getCustomerId()).orElse(null);
                    var subtotal=loaded.cart().stream().map(CartItemDto::getTotalPrice).reduce(BigDecimal.ZERO,BigDecimal::add);
                    var result=vouchers.validateAndCalculate(checkout.getVoucherCode(),customer,subtotal);
                    if(!result.valid){checkout.setVoucherCode(null);flash.addFlashAttribute("error","Đã bỏ phiếu giảm giá: "+result.message);}
                }
                flash.addFlashAttribute("success","Đã mở "+loaded.code()+". Giá và tồn kho đã được tính lại.");
            } catch(IllegalArgumentException ex) {flash.addFlashAttribute("error",ex.getMessage());}
        }
        return "redirect:"+base();
    }

    @PostMapping("/holds/{id}/delete")
    public String deleteHold(@PathVariable Long id,HttpSession session,Principal principal,RedirectAttributes flash) {
        synchronized(session){holds.delete(principal.getName(),id,mode());if(id.equals(session.getAttribute(key("posLoadedHold"))))clearCart(session);}
        flash.addFlashAttribute("success","Đã xóa hóa đơn chờ.");return "redirect:"+base();
    }

    @PostMapping("/holds/review")
    public String review(HttpSession session,Principal principal,RedirectAttributes flash) {
        synchronized(session) {
            Long id=(Long)session.getAttribute(key("posLoadedHold"));
            if(id==null)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
            holds.open(principal.getName(),id,mode());
            try {pricing.refreshCart(cart(session));var baseline=prices(session);baseline.clear();cart(session).forEach(i->baseline.put(i.getVariantId(),i.getPrice()));
                session.removeAttribute(key("posHoldReview"));flash.addFlashAttribute("success","Đã xác nhận giá và tồn kho hiện tại.");
            } catch(IllegalArgumentException ex){flash.addFlashAttribute("error",ex.getMessage());}
        }
        return "redirect:"+base();
    }

    @PostMapping("/new")
    public String newOrder(HttpSession session) {synchronized(session){clearCart(session);}return "redirect:"+base();}

    @GetMapping("/success/{id}")
    public String success(@PathVariable Integer id,Model model) {
        var invoice=invoices.findByOrder_Id(id).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        if(!"POS".equals(invoice.getOrder().getOrderType())) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
        model.addAttribute("invoice",invoice);
        return "admin/ban-tai-quay/thanh-toan-thanh-cong";
    }
}
