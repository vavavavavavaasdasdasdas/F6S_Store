package com.example.demodatn.service.sanpham;

import com.example.demodatn.dto.sanpham.LookupForm;
import com.example.demodatn.entity.sanpham.Sole;
import com.example.demodatn.exception.CatalogValidationException;
import com.example.demodatn.repository.sanpham.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service @RequiredArgsConstructor
public class SoleService {
    private final SoleRepository soles;
    private final ProductRepository products;

    public Sole get(Integer id) {
        return soles.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy đế giày."));
    }
    @Transactional
    public Sole save(LookupForm form) {
        String name=form.getValue()==null?"":form.getValue().trim();
        if(name.isEmpty()||name.length()>100)throw new CatalogValidationException("value","Nhập tên đế giày từ 1 đến 100 ký tự.");
        if(soles.existsByNameIgnoreCaseAndIdNot(name,form.getId()==null?-1:form.getId()))
            throw new CatalogValidationException("value","Tên đế giày đã tồn tại.");
        Sole sole=form.getId()==null?new Sole():get(form.getId());
        sole.setName(name);sole.setStatus(Boolean.TRUE.equals(form.getStatus()));
        return soles.saveAndFlush(sole);
    }
    @Transactional
    public void toggle(Integer id) {
        Sole sole=get(id);sole.setStatus(!Boolean.TRUE.equals(sole.getStatus()));soles.saveAndFlush(sole);
    }
    @Transactional
    public String delete(Integer id) {
        Sole sole=get(id);long count=products.countBySole_Id(id);
        if(count>0)return "Không thể xóa đế giày đang được "+count+" sản phẩm sử dụng.";
        soles.delete(sole);soles.flush();return "Đã xóa đế giày chưa được sử dụng.";
    }
}
