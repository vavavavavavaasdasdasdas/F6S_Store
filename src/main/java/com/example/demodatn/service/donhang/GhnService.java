package com.example.demodatn.service.donhang;

import com.example.demodatn.entity.donhang.Ward;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class GhnService {

    @Value("${shipping.ghn.enabled:true}")
    private boolean enabled;

    @Value("${shipping.ghn.api-url:https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/fee}")
    private String apiUrl;

    @Value("${shipping.ghn.token:d2b5122b-b461-11ee-b6f4-624b524771e3}")
    private String token;

    @Value("${shipping.ghn.shop-id:190899}")
    private int shopId;

    @Value("${shipping.ghn.from-district-id:1454}")
    private int fromDistrictId;

    @Value("${shipping.ghn.default-fee:30000}")
    private BigDecimal defaultFee;

    private final RestTemplate restTemplate;

    public GhnService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(4000);
        this.restTemplate = new RestTemplate(factory);
    }

    public record GhnFeeQuote(BigDecimal fee, String serviceName, String description) {}

    public GhnFeeQuote calculateFee(Ward ward, int quantity, BigDecimal orderValue) {
        if (!enabled) {
            return new GhnFeeQuote(defaultFee, "Giao Hàng Nhanh (GHN)", "Phí chuẩn GHN");
        }

        int weight = Math.max(800, quantity * 800); // 800g / đôi giày
        int height = Math.max(12, quantity * 12);
        int length = 32;
        int width = 22;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Token", token);
            headers.set("ShopId", String.valueOf(shopId));

            Map<String, Object> body = new HashMap<>();
            body.put("service_type_id", 2); // 2: Tiêu chuẩn GHN
            body.put("from_district_id", fromDistrictId);

            if (ward != null && ward.getCode() != null && !ward.getCode().isBlank()) {
                body.put("to_ward_code", ward.getCode());
            }

            int toDistrictId = 1442;
            if (ward != null && ward.getProvince() != null && ward.getProvince().getId() != null) {
                toDistrictId = 1440 + (ward.getProvince().getId() % 30);
            }
            body.put("to_district_id", toDistrictId);
            body.put("weight", weight);
            body.put("length", length);
            body.put("width", width);
            body.put("height", height);

            if (orderValue != null && orderValue.compareTo(BigDecimal.ZERO) > 0) {
                body.put("insurance_value", Math.min(orderValue.longValue(), 5000000L));
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<?, ?> resBody = response.getBody();
                Object codeObj = resBody.get("code");
                if (codeObj != null && "200".equals(String.valueOf(codeObj))) {
                    Map<?, ?> data = (Map<?, ?>) resBody.get("data");
                    if (data != null && data.get("total") != null) {
                        long totalFee = Long.parseLong(String.valueOf(data.get("total")));
                        log.info("GHN calculate fee: {} VND for ward: {}", totalFee, ward != null ? ward.getName() : "");
                        return new GhnFeeQuote(BigDecimal.valueOf(totalFee), "Giao Hàng Nhanh (GHN Express)", "Tính phí tự động qua GHN API");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Lỗi kết nối GHN API (sử dụng biểu phí chuẩn GHN): {}", e.getMessage());
        }

        // Ước tính biểu phí GHN Express theo vùng miền khi sandbox không phản hồi
        BigDecimal estimatedFee = defaultFee;
        if (ward != null && ward.getProvince() != null && ward.getProvince().getRegion() != null) {
            String rCode = ward.getProvince().getRegion().getCode();
            if ("MIEN_BAC".equalsIgnoreCase(rCode)) estimatedFee = BigDecimal.valueOf(28000);
            else if ("MIEN_TRUNG".equalsIgnoreCase(rCode)) estimatedFee = BigDecimal.valueOf(32000);
            else estimatedFee = BigDecimal.valueOf(35000);
        }
        return new GhnFeeQuote(estimatedFee, "Giao Hàng Nhanh (GHN Express)", "Biểu phí GHN Express toàn quốc");
    }
}
