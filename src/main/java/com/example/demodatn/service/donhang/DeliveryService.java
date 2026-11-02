package com.example.demodatn.service.donhang;
import com.example.demodatn.entity.donhang.Province;
import com.example.demodatn.repository.donhang.ProvinceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.ZoneId;

@Service @RequiredArgsConstructor
public class DeliveryService {
    public static final ZoneId SHOP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final ProvinceRepository provinces;

    public Province province(Integer id) {
        if (id == null) throw new IllegalArgumentException("Vui lòng chọn Tỉnh/Thành phố.");
        return provinces.findById(id).orElseThrow(() -> new IllegalArgumentException("Vui lòng chọn Tỉnh/Thành phố hợp lệ trong danh sách."));
    }
}
