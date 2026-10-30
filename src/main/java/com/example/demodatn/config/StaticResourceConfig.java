package com.example.demodatn.config;

import java.io.IOException;
import java.util.Map;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/** Keeps existing public asset URLs working after file organization. */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
    private static final Map<String, String> ALIASES = Map.ofEntries(
        Map.entry("admin.css", "quan-tri.css"), Map.entry("design-system.css", "he-thong-thiet-ke.css"),
        Map.entry("admin-mark.svg", "logo.jpg"), Map.entry("placeholder.svg", "anh-thay-the.svg"),
        Map.entry("product-placeholder.svg", "anh-san-pham-thay-the.svg"),
        Map.entry("admin.js", "quan-tri.js"), Map.entry("admin-catalog.js", "quan-tri-danh-muc.js"),
        Map.entry("admin-dashboard.js", "bang-dieu-khien.js"), Map.entry("admin-discounts.js", "quan-tri-giam-gia.js"),
        Map.entry("admin-invoice.js", "in-hoa-don.js"), Map.entry("admin-pos.js", "ban-tai-quay.js"),
        Map.entry("admin-statistics.js", "thong-ke.js"), Map.entry("cart.js", "gio-hang.js"),
        Map.entry("checkout.js", "thanh-toan.js"), Map.entry("product-detail.js", "chi-tiet-san-pham.js"),
        Map.entry("quick-add.js", "them-nhanh.js"), Map.entry("store-ui.js", "giao-dien-cua-hang.js"),
        Map.entry("voucher-preview.js", "xem-truoc-voucher.js")
    );

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        register(registry, "css", "/css/**");
        register(registry, "js", "/js/**");
        register(registry, "images", "/images/**");
        register(registry, "anh", "/anh/**");
    }

    private void register(ResourceHandlerRegistry registry, String folder, String... patterns) {
        var handler=registry.addResourceHandler(patterns).addResourceLocations("classpath:/static/" + folder + "/");
        if(folder.equals("images"))handler.addResourceLocations("classpath:/static/anh/");
        handler.resourceChain(true).addResolver(new PathResourceResolver() {
                @Override
                protected Resource getResource(String path, Resource location) throws IOException {
                    return super.getResource(ALIASES.getOrDefault(path, path), location);
                }
            });
    }
}
