package com.example.demodatn.controller.admin.donhang;
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
@RequestMapping("/admin/pos")
@RequiredArgsConstructor
public class AdminPosController {
    private final ProductVariantRepository variants;
    private final PricingService pricing;
    private final VoucherService vouchers;
    private final CustomerRepository customers;
    private final EmployeeRepository employees;
    private final OrderService orders;
    private final InvoiceRepository invoices;
    private final PosHoldService holds;
    private final PosCatalogQuery catalogQuery;
    private final com.example.demodatn.repository.sanpham.CategoryRepository categories;
    private final MaterialRepository materials;
    private final SizeRepository sizes;

    @GetMapping("/customers")
    public String customerSearch(@RequestParam(defaultValue="") String customerQuery,@RequestParam(defaultValue="0") String customerPage,Model model){model.addAttribute("customerPage",catalogQuery.customers(customerQuery,customerPage));return "admin/ban-tai-quay/khach-hang :: results";}


    private void clearCart(HttpSession session) {
        for(String key:List.of("posCart","posCheckoutToken","posLoadedHold","posHeldRequest","posHoldReview"))session.removeAttribute(key);
    }

    @SuppressWarnings("unchecked")
    private Map<Integer,BigDecimal> prices(HttpSession session) {
        var snapshots=(Map<Long,Map<Integer,BigDecimal>>)session.getAttribute("posHoldPrices");
        if(snapshots==null){snapshots=new HashMap<>();session.setAttribute("posHoldPrices",snapshots);}
        return snapshots.computeIfAbsent((Long)session.getAttribute("posLoadedHold"),id->new HashMap<>());
    }

    @SuppressWarnings("unchecked")
    private List<CartItemDto> cart(HttpSession session) {
        var cart = (List<CartItemDto>) session.getAttribute("posCart");
        if (cart == null) { cart = new ArrayList<>(); session.setAttribute("posCart", cart); }
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
            Long loadedId=(Long)session.getAttribute("posLoadedHold");
            if(loadedId!=null) {
                // Revalidate expiry/ownership on every displayed held basket as well as checkout.
                try {holds.open(principal.getName(),loadedId);} catch(org.springframework.web.server.ResponseStatusException ex) {
                    clearCart(session);cart=cart(session);model.addAttribute("posCart",cart);
                    model.addAttribute("error","Hóa đơn chờ đã hết hạn hoặc không còn tồn tại.");loadedId=null;
                }
            }
            if(loadedId!=null) {
                var baseline=prices(session);
                for(var item:cart) {
                    var v=variants.findById(item.getVariantId()).orElse(null);
                    if(v==null || !Boolean.TRUE.equals(v.getStatus()) || !Boolean.TRUE.equals(v.getProduct().getStatus()))
                        lineWarnings.put(item.getVariantId(),"Sản phẩm đã ngừng bán. Xóa dòng này để tiếp tục.");
                    else {
                        var current=pricing.calculatePrice(v).getFinalPrice();
                        var previous=baseline.get(item.getVariantId());
                        if(previous!=null && previous.compareTo(current)!=0){session.setAttribute("posHoldReview",true);lineWarnings.put(item.getVariantId(),"Giá đã thay đổi. Xác nhận giá mới trước khi thanh toán.");}
                        item.setPrice(current);
                        if(v.getQuantity()<item.getQuantity())lineWarnings.put(item.getVariantId(),v.getQuantity()==0 ? "Hết hàng. Xóa dòng hoặc chờ nhập hàng." : "Không đủ tồn kho. Giảm số lượng và lưu lại.");
                    }
                }
            }
            try { pricing.refreshCart(cart); } catch (IllegalArgumentException ex) { valid=false; model.addAttribute("error",ex.getMessage()); }
            if(Boolean.TRUE.equals(session.getAttribute("posHoldReview")))valid=false;
            model.addAttribute("lineWarnings",lineWarnings);
            model.addAttribute("holdNeedsReview",Boolean.TRUE.equals(session.getAttribute("posHoldReview")));
            model.addAttribute("loadedHoldId",loadedId);
            model.addAttribute("heldOrders",holds.list(principal.getName()));
            model.addAttribute("cartValid", valid);
            model.addAttribute("total", cart.stream().map(CartItemDto::getTotalPrice).reduce(BigDecimal.ZERO,BigDecimal::add));
            if(session.getAttribute("posCheckoutToken")==null) session.setAttribute("posCheckoutToken", UUID.randomUUID().toString());
            if(!model.containsAttribute("posCheckoutRequest")) {
                var request = (PosCheckoutRequest)session.getAttribute("posHeldRequest");
                if(request==null){request=new PosCheckoutRequest();request.setPaymentMethod("CASH");}
                model.addAttribute("posCheckoutRequest", request);
            }
            model.addAttribute("checkoutToken", session.getAttribute("posCheckoutToken"));
            model.addAttribute("customerPage",catalogQuery.customers(customerQuery,customerPage));model.addAttribute("customerQuery",customerQuery);
        }
        return "admin/ban-tai-quay/ban-tai-quay";
    }

    @PostMapping("/add")
    public String add(@RequestParam Integer variantId, @RequestParam Integer quantity, HttpSession session, RedirectAttributes flash) {
        synchronized(session) {
            try {
                var v = variants.findById(variantId).orElseThrow(()->new IllegalArgumentException("Sản phẩm không tồn tại."));
                if(quantity<=0 || !Boolean.TRUE.equals(v.getStatus()) || !Boolean.TRUE.equals(v.getProduct().getStatus()))
                    throw new IllegalArgumentException("Sản phẩm hoặc số lượng không hợp lệ.");
                var cart = cart(session);
                var item = cart.stream().filter(i->i.getVariantId().equals(variantId)).findFirst().orElse(null);
                long totalQty = quantity + (item == null ? 0L : item.getQuantity());
                if(totalQty > v.getQuantity()) throw new IllegalArgumentException("Số lượng vượt quá tồn kho.");
                if(item == null) cart.add(new CartItemDto(v.getProduct().getId(),v.getId(),v.getProduct().getName(),v.getSize().getSizeValue(),v.getMaterial().getName(),pricing.calculatePrice(v).getFinalPrice(),quantity));
                else item.setQuantity((int)totalQty);
                session.removeAttribute("posCheckoutToken");
            } catch(IllegalArgumentException ex) { flash.addFlashAttribute("error",ex.getMessage()); }
        }
        return "redirect:/admin/pos";
    }

    @PostMapping("/quantity")
    public String quantity(@RequestParam Integer variantId,@RequestParam Integer quantity,HttpSession session,RedirectAttributes flash) {
        synchronized(session) {
            try {
                var v=variants.findById(variantId).orElseThrow(()->new IllegalArgumentException("Sản phẩm không tồn tại."));
                if(quantity<=0 || quantity>v.getQuantity()) throw new IllegalArgumentException("Số lượng phải từ 1 đến tồn kho hiện tại.");
                cart(session).stream().filter(i->i.getVariantId().equals(variantId)).findFirst()
                        .orElseThrow(()->new IllegalArgumentException("Dòng không còn trong giỏ.")).setQuantity(quantity);
                session.removeAttribute("posCheckoutToken");
            } catch(IllegalArgumentException ex) { flash.addFlashAttribute("error",ex.getMessage()); }
        }
        return "redirect:/admin/pos";
    }

    @PostMapping("/remove")
    public String remove(@RequestParam Integer variantId,HttpSession session) {
        synchronized(session) { cart(session).removeIf(i->i.getVariantId().equals(variantId)); session.removeAttribute("posCheckoutToken"); }
        return "redirect:/admin/pos";
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

    @PostMapping("/checkout")
    public String checkout(@Valid @ModelAttribute("posCheckoutRequest") PosCheckoutRequest request,BindingResult errors,
            HttpSession session,Principal principal,RedirectAttributes flash) {
        // A session cart is serialized so repeated checkout cannot sell the same basket twice.
        synchronized(session) {
            try {
                if(errors.hasErrors()) throw new IllegalArgumentException("Kiểm tra phương thức thanh toán và tiền khách đưa.");
                if(request.getCheckoutToken()==null || !request.getCheckoutToken().equals(session.getAttribute("posCheckoutToken")))
                    throw new IllegalArgumentException("Phiên thanh toán đã thay đổi. Kiểm tra giỏ rồi thanh toán lại.");
                if(cart(session).isEmpty()) throw new IllegalArgumentException("Giỏ hàng trống.");
                var employee=employees.findByUser_Username(principal.getName()).orElseThrow(()->new IllegalArgumentException("Tài khoản chưa gắn với nhân viên."));
                var customer=request.getCustomerId()==null ? null : customers.findById(request.getCustomerId()).orElseThrow(()->new IllegalArgumentException("Khách hàng không tồn tại."));
                if(customer!=null && !Boolean.TRUE.equals(customer.getStatus())) throw new IllegalArgumentException("Khách hàng đã bị vô hiệu hóa.");
                Long heldId=(Long)session.getAttribute("posLoadedHold");
                if(heldId!=null && Boolean.TRUE.equals(session.getAttribute("posHoldReview")))throw new IllegalArgumentException("Xác nhận giá đã tính lại trước khi thanh toán.");
                var order=heldId==null ? orders.createPosOrder(cart(session),customer,employee,request.getVoucherCode(),request.getPaymentMethod(),"Tại quầy",request.getCashReceived())
                        : holds.checkout(principal.getName(),heldId,cart(session),request);
                clearCart(session);
                return "redirect:/admin/pos/success/"+order.getId();
            } catch(org.springframework.web.server.ResponseStatusException ex) {throw ex;}
            catch(IllegalArgumentException ex) { flash.addFlashAttribute("error",ex.getMessage()); }
            catch(Exception ex) { flash.addFlashAttribute("error","Không thể tạo đơn. Giỏ được giữ lại; kiểm tra tồn kho và thử lại."); }
            flash.addFlashAttribute("posCheckoutRequest",request);
            if(errors.hasErrors())flash.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX+"posCheckoutRequest",errors);
            return "redirect:/admin/pos";
        }
    }

    @PostMapping("/holds")
    public String hold(@ModelAttribute PosCheckoutRequest request,HttpSession session,Principal principal,RedirectAttributes flash) {
        synchronized(session) {
            try {
                Long id=holds.save(principal.getName(),cart(session),request,(Long)session.getAttribute("posLoadedHold"));
                session.setAttribute("posLoadedHold",id);var baseline=prices(session);baseline.clear();
                cart(session).forEach(i->baseline.put(i.getVariantId(),i.getPrice()));
                clearCart(session);flash.addFlashAttribute("success","Đã lưu hóa đơn chờ. Giỏ hiện tại đã trống.");
            } catch(IllegalArgumentException ex) {flash.addFlashAttribute("error",ex.getMessage());}
        }
        return "redirect:/admin/pos";
    }

    @PostMapping("/holds/{id}/open")
    public String openHold(@PathVariable Long id,@RequestParam(defaultValue="CANCEL") String action,
            @ModelAttribute PosCheckoutRequest request,HttpSession session,Principal principal,RedirectAttributes flash) {
        synchronized(session) {
            if(!cart(session).isEmpty() && !Set.of("SAVE","DISCARD").contains(action)) {
                flash.addFlashAttribute("error","Chọn lưu hoặc bỏ giỏ hiện tại trước khi mở hóa đơn chờ.");return "redirect:/admin/pos";
            }
            try {
                var loaded=holds.switchTo(principal.getName(),id,cart(session),request,(Long)session.getAttribute("posLoadedHold"),"SAVE".equals(action));
                clearCart(session);session.setAttribute("posCart",loaded.cart());session.setAttribute("posLoadedHold",id);
                session.setAttribute("posHeldRequest",loaded.request());
                var baseline=prices(session);
                // Prices are never persisted in a held order. A browser/session snapshot can identify a change;
                // after restart the old price is unknown, so require explicit review of recalculated prices.
                if(baseline.isEmpty()) {loaded.cart().forEach(i->baseline.put(i.getVariantId(),i.getPrice()));session.setAttribute("posHoldReview",true);}
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
        return "redirect:/admin/pos";
    }

    @PostMapping("/holds/{id}/delete")
    public String deleteHold(@PathVariable Long id,HttpSession session,Principal principal,RedirectAttributes flash) {
        synchronized(session){holds.delete(principal.getName(),id);if(id.equals(session.getAttribute("posLoadedHold")))clearCart(session);}
        flash.addFlashAttribute("success","Đã xóa hóa đơn chờ.");return "redirect:/admin/pos";
    }

    @PostMapping("/holds/review")
    public String review(HttpSession session,Principal principal,RedirectAttributes flash) {
        synchronized(session) {
            Long id=(Long)session.getAttribute("posLoadedHold");
            if(id==null)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
            holds.open(principal.getName(),id);
            try {pricing.refreshCart(cart(session));var baseline=prices(session);baseline.clear();cart(session).forEach(i->baseline.put(i.getVariantId(),i.getPrice()));
                session.removeAttribute("posHoldReview");flash.addFlashAttribute("success","Đã xác nhận giá và tồn kho hiện tại.");
            } catch(IllegalArgumentException ex){flash.addFlashAttribute("error",ex.getMessage());}
        }
        return "redirect:/admin/pos";
    }

    @PostMapping("/new")
    public String newOrder(HttpSession session) {synchronized(session){clearCart(session);}return "redirect:/admin/pos";}

    @GetMapping("/success/{id}")
    public String success(@PathVariable Integer id,Model model) {
        var invoice=invoices.findByOrder_Id(id).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        if(!"POS".equals(invoice.getOrder().getOrderType())) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
        model.addAttribute("invoice",invoice);
        return "admin/ban-tai-quay/thanh-toan-thanh-cong";
    }
}
