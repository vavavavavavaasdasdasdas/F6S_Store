package com.example.demodatn.controller.admin.khachhang;
import com.example.demodatn.dto.khachhang.CustomerDto;
import com.example.demodatn.entity.khachhang.Customer;
import com.example.demodatn.exception.PeopleValidationException;
import com.example.demodatn.repository.khachhang.CustomerRepository;
import com.example.demodatn.service.PeopleService;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/admin/customers")
@RequiredArgsConstructor
public class AdminCustomerController {
    private final CustomerRepository repository;
    private final PeopleService people;
    private final com.example.demodatn.service.nhanvien.AccountProvisioningService accounts;
    private final com.example.demodatn.repository.khachhang.ShippingAddressRepository shippingAddresses;
    private final com.example.demodatn.repository.donhang.OrderRepository orders;

    private final com.example.demodatn.util.Pagination pagination;

    @GetMapping
    public String list(@RequestParam(defaultValue="") String keyword,
                       @RequestParam(required=false) Boolean status,
                       @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       @RequestParam(defaultValue="0") String page,
                       Model model) {
        String query=keyword==null?"":keyword.trim();
        Specification<Customer> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!query.isEmpty()) {
                var userJoin = root.join("user", JoinType.LEFT);
                String pattern = "%" + query.toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(root.get("customerCode"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("fullName"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("phone"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("email"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(userJoin.get("username"), "")), pattern)
                ));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        var result=pagination.load(page,Sort.by(Sort.Direction.DESC,"id"),request->repository.findAll(spec,request));
        model.addAttribute("peoplePage",result);
        model.addAttribute("keyword",keyword);
        model.addAttribute("status",status);
        model.addAttribute("from",from);
        model.addAttribute("to",to);
        int activeFilterCount = 0;
        if (!query.isEmpty()) activeFilterCount++;
        if (status != null) activeFilterCount++;
        if (from != null) activeFilterCount++;
        if (to != null) activeFilterCount++;
        model.addAttribute("activeFilterCount", activeFilterCount);
        return "admin/khach-hang/danh-sach";
    }
    @GetMapping("/create")
    public String create(Model model) { model.addAttribute("customer",new CustomerDto());return "admin/khach-hang/bieu-mau"; }
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Integer id,Model model) {
        var item=people.customer(id);var form=new CustomerDto();BeanUtils.copyProperties(item,form);
        form.setUsername(item.getUser()==null?null:item.getUser().getUsername());
        model.addAttribute("customer",form);return "admin/khach-hang/bieu-mau";
    }
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Integer id,@RequestParam(defaultValue="0") String page,Model model) {
        var item=people.customer(id);model.addAttribute("person",item);model.addAttribute("self",people.isSelf(item.getUser()));
        model.addAttribute("orderPage",pagination.load(page,Sort.unsorted(),request->orders.findByCustomer_IdOrderByCreatedAtDescIdDesc(id,request)));
        model.addAttribute("shippingAddresses",shippingAddresses.findByCustomer_IdOrderBySlotAsc(id));
        return "admin/khach-hang/chi-tiet";
    }
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("customer") CustomerDto form,BindingResult errors,Model model,RedirectAttributes flash) {
        if(!errors.hasErrors()) {
            try {
                var saved=accounts.saveCustomer(form);flash.addFlashAttribute(saved.issued()&&!saved.sent()?"warning":"message",saved.message());
                return "redirect:/admin/customers/detail/"+saved.personId();
            } catch(PeopleValidationException ex) { errors.rejectValue(ex.getField(),"invalid",ex.getMessage()); }
            catch(DataIntegrityViolationException ex) { errors.reject("duplicate","Dữ liệu mã/thư điện tử/SĐT/căn cước/tài khoản vừa được sử dụng. Kiểm tra lại trước khi lưu."); }
        }
        return "admin/khach-hang/bieu-mau";
    }
    @PostMapping("/status/{id}")
    public String status(@PathVariable Integer id,@RequestParam boolean enabled,RedirectAttributes flash) {
        try { people.customerStatus(id,enabled);flash.addFlashAttribute("message",enabled?"Đã kích hoạt. Tài khoản chỉ hoạt động khi mọi hồ sơ liên kết đều hoạt động.":"Đã vô hiệu hóa hồ sơ và tài khoản liên kết. Lịch sử vẫn được giữ nguyên."); }
        catch(IllegalArgumentException ex) { flash.addFlashAttribute("error",ex.getMessage()); }
        return "redirect:/admin/customers/detail/"+id;
    }
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Integer id,RedirectAttributes flash) {
        try { flash.addFlashAttribute("message",people.deleteCustomer(id)); }
        catch(IllegalArgumentException ex) { flash.addFlashAttribute("error",ex.getMessage()); }
        catch(DataIntegrityViolationException ex) { flash.addFlashAttribute("error","Dữ liệu vừa phát sinh tham chiếu. Không xóa; hãy vô hiệu hóa hồ sơ."); }
        return "redirect:/admin/customers";
    }
    @PostMapping("/{id}/send-account")
    public String sendAccount(@PathVariable Integer id,@RequestParam(defaultValue="false") boolean confirmed,RedirectAttributes flash) {
        try {var result=accounts.resend("customers",id,confirmed);flash.addFlashAttribute(result.sent()?"message":"warning",result.message());}
        catch(PeopleValidationException ex){flash.addFlashAttribute("warning",ex.getMessage());}
        return "redirect:/admin/customers/detail/"+id;
    }
}
