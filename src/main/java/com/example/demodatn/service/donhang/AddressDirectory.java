package com.example.demodatn.service.donhang;
import com.example.demodatn.entity.donhang.*;
import com.example.demodatn.repository.donhang.*;
import com.example.demodatn.util.VietnameseText;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;
@Service @RequiredArgsConstructor
public class AddressDirectory {
    private final ProvinceRepository provinces;
    private final WardRepository wards;
    private final JdbcTemplate jdbc;
    public List<Ward> wards(Integer provinceId){return provinceId==null?List.of():wards.findByProvince_IdOrderByNameAsc(provinceId);}
    public Ward requireWard(Integer provinceId,Integer wardId){
        if(provinceId==null)throw new IllegalArgumentException("Vui lòng chọn Tỉnh/Thành phố.");
        if(wardId==null)throw new IllegalArgumentException("Vui lòng chọn Phường/Xã.");
        var ward=wards.findById(wardId).orElseThrow(()->new IllegalArgumentException("Phường/Xã không hợp lệ."));
        if(!ward.getProvince().getId().equals(provinceId))throw new IllegalArgumentException("Phường/Xã không thuộc Tỉnh/Thành phố đã chọn.");
        return ward;
    }
    public static String normalizedName(String raw){return VietnameseText.fold(raw).replaceFirst("^(thanh pho|tinh|phuong|xa|dac khu|tp)\\s+","");}
    public Province matchProvince(String raw){
        String key=normalizedName(raw);if(key.isBlank())return null;
        var names=jdbc.queryForList("SELECT tinh_thanh_id,ten FROM ten_tinh_thanh_cu");
        var ids=names.stream().filter(row->normalizedName(String.valueOf(row.get("ten"))).equals(key)).map(row->((Number)row.get("tinh_thanh_id")).intValue()).distinct().toList();
        return ids.size()==1?provinces.findById(ids.get(0)).orElse(null):null;
    }
    public Ward matchWard(Province province,String raw){
        if(province==null)return null;String key=normalizedName(raw);
        var matches=wards(province.getId()).stream().filter(w->normalizedName(w.getName()).equals(key)).toList();
        return matches.size()==1?matches.get(0):null;
    }
}
