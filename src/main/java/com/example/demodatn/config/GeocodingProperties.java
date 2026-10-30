package com.example.demodatn.config;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.properties.ConfigurationProperties;
@Component @ConfigurationProperties(prefix="shop.geocoding") @Getter @Setter
public class GeocodingProperties {
    private boolean enabled;
    private String baseUrl;
    private String userAgent;
    private String tileUrl;
    private int timeoutMillis;
    private long minIntervalMillis;
    private long cacheSeconds;
    private long retrySeconds;
    private int requestsPerMinute;
}
