package com.example.demodatn.controller.user;

import com.example.demodatn.entity.donhang.Order;
import com.example.demodatn.repository.donhang.OrderRepository;
import com.example.demodatn.service.donhang.VnPayService;
import com.example.demodatn.service.donhang.VnPayService.LocalCheckoutStatus;
import com.example.demodatn.service.donhang.VnPayService.PaymentResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class VnPayController {

    private final VnPayService vnPayService;
    private final OrderRepository orderRepository;
    private final com.example.demodatn.repository.donhang.InvoiceRepository invoiceRepository;

    @GetMapping("/payment/vnpay/create/{orderId}")
    public String createPayment(@PathVariable Integer orderId,
                                @RequestParam(value = "returnUrl", required = false) String returnUrl,
                                HttpServletRequest request, RedirectAttributes redirectAttributes) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng cần thanh toán.");
            return returnUrl != null ? "redirect:" + returnUrl : "redirect:/orders";
        }
        if (returnUrl != null && !returnUrl.isBlank()) {
            request.getSession().setAttribute("vnpay_custom_return_url", returnUrl);
        }

        if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            redirectAttributes.addFlashAttribute("message", "Đơn hàng này đã được thanh toán trước đó.");
            return resolveOrderDestination(order, request);
        }

        try {
            String clientIp = clientIp(request);
            String paymentUrl = vnPayService.createPaymentUrl(order, clientIp);
            return "redirect:" + paymentUrl;
        } catch (Exception e) {
            log.error("Lỗi khởi tạo thanh toán VNPay cho đơn hàng {}", order.getOrderCode(), e);
            redirectAttributes.addFlashAttribute("error", "Không thể kết nối cổng thanh toán VNPay: " + e.getMessage());
            return resolveOrderDestination(order, request);
        }
    }

    @GetMapping("/payment/vnpay/return")
    public String handleReturn(@RequestParam Map<String, String> params, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        PaymentResult result = vnPayService.processReturn(params);
        if (result.order() != null) {
            if (result.success()) {
                redirectAttributes.addFlashAttribute("message", "Thanh toán qua cổng VNPay thành công!");
                return resolveOrderDestination(result.order(), request);
            } else {
                redirectAttributes.addFlashAttribute("error", result.message());
                if ("POS".equalsIgnoreCase(result.order().getOrderType()) || result.order().getEmployee() != null) {
                    return "redirect:/admin/pos?paymentResult=failed&orderCode=" + encode(result.order().getOrderCode());
                }
                return resolveOrderDestination(result.order(), request);
            }
        } else {
            redirectAttributes.addFlashAttribute("error", result.message());
            String customReturnUrl = (String) request.getSession().getAttribute("vnpay_custom_return_url");
            request.getSession().removeAttribute("vnpay_custom_return_url");
            if (customReturnUrl != null && !customReturnUrl.isBlank()) {
                return "redirect:" + customReturnUrl;
            }
            return "redirect:/orders";
        }
    }

    private String resolveOrderDestination(Order order, HttpServletRequest request) {
        String customReturnUrl = (String) request.getSession().getAttribute("vnpay_custom_return_url");
        if (customReturnUrl != null && !customReturnUrl.isBlank()) {
            request.getSession().removeAttribute("vnpay_custom_return_url");
            return "redirect:" + customReturnUrl;
        }
        if ("POS".equalsIgnoreCase(order.getOrderType()) || order.getEmployee() != null) {
            return "redirect:/admin/pos?paymentResult=success&orderCode=" + encode(order.getOrderCode());
        }
        return "redirect:/orders/success/" + order.getId();
    }

    @GetMapping("/api/payment/vnpay/ipn")
    @ResponseBody
    public Map<String, String> handleIpn(@RequestParam Map<String, String> params) {
        return vnPayService.handleIpn(params);
    }

    /* =========================================================================
     *  Local VNPay Checkout Simulator (Phục vụ kiểm thử trực tiếp trên localhost)
     * ========================================================================= */

    @GetMapping(
            value = {
                    "/vnpay-demo",
                    "/vnpay-demo.html",
                    "/api/shop/payments/vnpay/checkout"
            },
            produces = MediaType.TEXT_HTML_VALUE
    )
    @ResponseBody
    public ResponseEntity<byte[]> vnpayMethodSelectionPage() throws IOException {
        return htmlPage("templates/Vnpaymethodselection.html");
    }

    @GetMapping(
            value = "/api/shop/payments/vnpay/checkout/card",
            produces = MediaType.TEXT_HTML_VALUE
    )
    @ResponseBody
    public ResponseEntity<byte[]> vnpayCardCheckoutPage() throws IOException {
        return htmlPage("templates/VnPayCheckout.html");
    }

    @GetMapping("/api/shop/payments/vnpay/checkout/status")
    @ResponseBody
    public ResponseEntity<LocalCheckoutStatus> vnpayCheckoutStatus(@RequestParam Map<String, String> params) {
        LocalCheckoutStatus status = vnPayService.localCheckoutStatus(params);
        return status.valid() ? ResponseEntity.ok(status) : ResponseEntity.badRequest().body(status);
    }

    @GetMapping("/api/shop/payments/vnpay/checkout/scan-confirm")
    public ResponseEntity<Void> confirmQrScan(@RequestParam Map<String, String> params, HttpServletRequest request) {
        PaymentResult result = vnPayService.completeLocalCheckout(params, true, "LOCAL-QR-");
        return localRedirect(result, request);
    }

    @GetMapping(
            value = "/api/shop/payments/vnpay/checkout/scan-result",
            produces = MediaType.TEXT_HTML_VALUE
    )
    @ResponseBody
    public ResponseEntity<byte[]> scanResultPage() throws IOException {
        return htmlPage("templates/VnPayScanResult.html");
    }

    @PostMapping(
            value = "/api/shop/payments/vnpay/checkout/complete",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public ResponseEntity<Void> completeCardPayment(@RequestParam Map<String, String> form, HttpServletRequest request) {
        Map<String, String> signedParams = new LinkedHashMap<>(form);
        String action = signedParams.remove("outcome");
        signedParams.remove("testCardNumber");
        signedParams.remove("testCardDate");
        signedParams.remove("testCvv");
        signedParams.remove("testOtp");

        boolean approved = !"cancel".equalsIgnoreCase(action);
        PaymentResult result = vnPayService.completeLocalCheckout(signedParams, approved, "LOCAL-CARD-");
        return localRedirect(result, request);
    }

    @PostMapping(
            value = "/api/shop/payments/vnpay/checkout/wallet/complete",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public ResponseEntity<Void> completeWalletPayment(@RequestParam Map<String, String> form, HttpServletRequest request) {
        Map<String, String> signedParams = new LinkedHashMap<>(form);
        String action = signedParams.remove("outcome");
        String channel = signedParams.remove("paymentChannel");

        boolean approved = !"cancel".equalsIgnoreCase(action);
        PaymentResult result = vnPayService.completeLocalCheckout(signedParams, approved, "LOCAL-WALLET-" + (channel != null ? channel : "") + "-");
        return localRedirect(result, request);
    }

    private ResponseEntity<Void> localRedirect(PaymentResult result, HttpServletRequest request) {
        String customReturnUrl = request != null ? (String) request.getSession().getAttribute("vnpay_custom_return_url") : null;
        if (request != null) {
            request.getSession().removeAttribute("vnpay_custom_return_url");
        }
        if (customReturnUrl != null && !customReturnUrl.isBlank()) {
            return ResponseEntity.status(302).location(URI.create(customReturnUrl)).build();
        }
        if (result.order() != null) {
            if ("POS".equalsIgnoreCase(result.order().getOrderType()) || result.order().getEmployee() != null) {
                String target = "/admin/pos?paymentResult=" + (result.success() ? "success" : "failed")
                        + "&orderCode=" + encode(result.order().getOrderCode());
                return ResponseEntity.status(302).location(URI.create(target)).build();
            }
            String target = "/orders/success/" + result.order().getId();
            return ResponseEntity.status(302).location(URI.create(target)).build();
        }
        return ResponseEntity.status(302).location(URI.create("/orders")).build();
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private ResponseEntity<byte[]> htmlPage(String resourcePath) throws IOException {
        byte[] html;
        try (InputStream input = new ClassPathResource(resourcePath).getInputStream()) {
            html = input.readAllBytes();
        }
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .body(html);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
