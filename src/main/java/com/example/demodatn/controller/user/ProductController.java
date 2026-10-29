package com.example.demodatn.controller.user;
import com.example.demodatn.service.sanpham.ProductSalePolicy;
import com.example.demodatn.dto.donhang.PriceInfo;
import com.example.demodatn.dto.sanpham.ProductDto;
import com.example.demodatn.entity.sanpham.Material;
import com.example.demodatn.entity.sanpham.Product;
import com.example.demodatn.entity.sanpham.ProductVariant;
import com.example.demodatn.repository.sanpham.CategoryRepository;
import com.example.demodatn.repository.sanpham.MaterialRepository;
import com.example.demodatn.repository.sanpham.SizeRepository;
import com.example.demodatn.service.donhang.PricingService;
import com.example.demodatn.service.sanpham.ProductFilterService;
import com.example.demodatn.service.sanpham.ProductGalleryService;
import com.example.demodatn.service.sanpham.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class ProductController {

    private final com.example.demodatn.util.Pagination pagination;

    private final ProductService productService;
    private final PricingService pricingService;
    private final ProductFilterService productFilterService;
    private final CategoryRepository categoryRepository;
    private final SizeRepository sizeRepository;
    private final MaterialRepository materialRepository;
    private final ProductGalleryService gallery;
    private final com.example.demodatn.repository.sanpham.SoleRepository soleRepository;

    @GetMapping({"/products", "/search"})
    public String list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer sizeId,
            @RequestParam(required = false) Integer materialId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean promotion,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") String page, @RequestParam(required=false) Integer soleId, Model model) {
        
        Page<ProductDto> productPage = productFilterService.filterProducts(keyword, categoryId, sizeId, minPrice, maxPrice, promotion, sort, pagination.index(page), pagination.getSize(), materialId,soleId);
        model.addAttribute("soles",soleRepository.findAll());model.addAttribute("soleId",soleId);
        
        model.addAttribute("productPage", productPage);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("sizes", sizeRepository.findAll());
        model.addAttribute("materials",materialRepository.findByStatusTrueOrderByNameAscIdAsc());
        model.addAttribute("materialId",materialId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("sizeId", sizeId);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("promotion", promotion);
        model.addAttribute("sort", sort);
        int activeFilterCount = 0;
        if (keyword != null && !keyword.trim().isEmpty()) activeFilterCount++;
        if (categoryId != null) activeFilterCount++;
        if (soleId != null) activeFilterCount++;
        if (materialId != null) activeFilterCount++;
        if (sizeId != null) activeFilterCount++;
        if (minPrice != null) activeFilterCount++;
        if (maxPrice != null) activeFilterCount++;
        if (Boolean.TRUE.equals(promotion)) activeFilterCount++;
        model.addAttribute("activeFilterCount", activeFilterCount);
        
        return "user/san-pham";
    }
    
    @GetMapping("/category/{id}")
    public String categoryList(@PathVariable Integer id,@RequestParam(required=false) String keyword,@RequestParam(required=false) Integer sizeId,@RequestParam(required=false) Integer materialId,@RequestParam(required=false) BigDecimal minPrice,@RequestParam(required=false) BigDecimal maxPrice,@RequestParam(required=false) Boolean promotion,@RequestParam(defaultValue="newest") String sort,@RequestParam(defaultValue="0") String page,Model model,jakarta.servlet.http.HttpServletResponse response) {
        if(!categoryRepository.existsById(id)){response.setStatus(404);return "loi/404";}
        return list(keyword,id,sizeId,materialId,minPrice,maxPrice,promotion,sort,page,null,model);
    }

    @GetMapping("/products/{id}")
    public String detail(@PathVariable Integer id, Model model, jakarta.servlet.http.HttpServletResponse response) {
        Product product = productService.getProductById(id);
        if (!ProductSalePolicy.isSelling(product)) {
            response.setStatus(404);
            return "loi/404";
        }
        
        List<ProductVariant> variants = productService.getVariantsByProductId(id);
        
        Map<Integer, PriceInfo> variantPrices = variants.stream()
            .collect(Collectors.toMap(ProductVariant::getId, pricingService::calculatePrice));

        List<Product> relatedProducts = productService.getAllProducts().stream()
            .filter(p -> p.getCategory() != null && product.getCategory() != null 
                    && p.getCategory().getId().equals(product.getCategory().getId())
                    && !p.getId().equals(product.getId()))
            .limit(4)
            .collect(Collectors.toList());
            
        model.addAttribute("product", product);
        model.addAttribute("productImages", gallery.list(id));
        model.addAttribute("variants", variants);
        model.addAttribute("variantPrices", variantPrices);
        model.addAttribute("productMaterials", variants.stream().map(ProductVariant::getMaterial).collect(Collectors.toMap(Material::getId,m -> m,(a,b) -> a,java.util.LinkedHashMap::new)).values());
        model.addAttribute("relatedProducts", relatedProducts);
        
        return "user/chi-tiet-san-pham";
    }
}
