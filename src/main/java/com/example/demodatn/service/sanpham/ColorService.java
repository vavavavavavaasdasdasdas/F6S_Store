package com.example.demodatn.service.sanpham;

import com.example.demodatn.dto.sanpham.LookupForm;
import com.example.demodatn.entity.sanpham.Color;
import com.example.demodatn.exception.CatalogValidationException;
import com.example.demodatn.repository.sanpham.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service @RequiredArgsConstructor
public class ColorService {
    private final ColorRepository colors;
    private final ProductVariantRepository variants;

    public Color get(Integer id) {
        return colors.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy màu sắc."));
    }
    @Transactional
    public Color save(LookupForm form) {
        String name=com.example.demodatn.util.VietnameseText.clean(form.getValue());
        if(name.isEmpty()||name.length()>100)throw new CatalogValidationException("value","Nhập tên màu sắc từ 1 đến 100 ký tự.");
        if(colors.findAll().stream().anyMatch(c->!java.util.Objects.equals(c.getId(),form.getId()) && com.example.demodatn.util.VietnameseText.sameFolded(c.getName(),name)))
            throw new CatalogValidationException("value","Tên màu sắc đã tồn tại.");
        Color color=form.getId()==null?new Color():get(form.getId());
        String hex=form.getHexCode()==null || form.getHexCode().isBlank()?null:form.getHexCode().trim().toUpperCase(java.util.Locale.ROOT);
        if(hex!=null && !hex.matches("#[0-9A-F]{6}"))throw new CatalogValidationException("hexCode","Mã màu phải có dạng #RRGGBB.");
        color.setHexCode(hex);
        color.setName(name);color.setStatus(Boolean.TRUE.equals(form.getStatus()));
        return colors.saveAndFlush(color);
    }
    @Transactional
    public void toggle(Integer id) {
        Color color=get(id);color.setStatus(!Boolean.TRUE.equals(color.getStatus()));colors.saveAndFlush(color);
    }
    @Transactional
    public String delete(Integer id) {
        Color color=get(id);long count=variants.colorUsage().stream().filter(r->((Number)r.get("colorId")).intValue()==id).mapToLong(r->((Number)r.get("usageCount")).longValue()).sum();
        if(count>0)return "Không thể xóa màu sắc đang được "+count+" biến thể sử dụng.";

        colors.delete(color);colors.flush();return "Đã xóa màu sắc chưa được sử dụng.";
    }
}
