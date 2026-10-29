package com.example.demodatn.controller.admin.nhanvien;
import com.example.demodatn.dto.nhanvien.EmployeeDto;
import com.example.demodatn.entity.nhanvien.Employee;
import com.example.demodatn.exception.PeopleValidationException;
import com.example.demodatn.repository.nhanvien.EmployeeRepository;
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
@RequestMapping("/admin/employees")
@RequiredArgsConstructor
public class AdminEmployeeController {
    private final EmployeeRepository repository;
    private final PeopleService people;
    private final com.example.demodatn.service.nhanvien.AccountProvisioningService accounts;

    private final com.example.demodatn.util.Pagination pagination;

    @ModelAttribute("positions")
    public List<String> positions() {
        var values=new java.util.ArrayList<>(java.util.List.of("Quản lý","Nhân viên bán hàng","Thu ngân","Thủ kho","Kế toán"));
        for(String value:repository.distinctPositions())if(values.stream().noneMatch(v->com.example.demodatn.util.VietnameseText.sameFolded(v,value)))values.add(value);
        return values;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue="") String keyword,
                       @RequestParam(required=false) String position,
                       @RequestParam(required=false) Boolean status,
                       @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       @RequestParam(defaultValue="0") String page,
                       Model model) {
        String query=keyword==null?"":keyword.trim();
        String pos = (position != null && !position.trim().isEmpty()) ? position.trim() : null;
        Specification<Employee> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            var userJoin = root.join("user", JoinType.LEFT);
            if (!query.isEmpty()) {
                String pattern = "%" + query.toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(root.get("employeeCode"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("fullName"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("phone"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("email"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(userJoin.get("username"), "")), pattern)
                ));
            }
            if (pos != null) {
                predicates.add(cb.equal(cb.lower(root.get("position")), pos.toLowerCase(Locale.ROOT)));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(userJoin.get("createdAt"), from.atStartOfDay()));
            }
            if (to != null) {
                predicates.add(cb.lessThan(userJoin.get("createdAt"), to.plusDays(1).atStartOfDay()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        var result=pagination.load(page,Sort.by(Sort.Direction.DESC,"id"),request->repository.findAll(spec,request));
        model.addAttribute("peoplePage",result);
        model.addAttribute("keyword",keyword);
        model.addAttribute("position",pos);
        model.addAttribute("status",status);
        model.addAttribute("from",from);
        model.addAttribute("to",to);
        int activeFilterCount = 0;
        if (!query.isEmpty()) activeFilterCount++;
        if (pos != null) activeFilterCount++;
        if (status != null) activeFilterCount++;
        if (from != null) activeFilterCount++;
        if (to != null) activeFilterCount++;
        model.addAttribute("activeFilterCount", activeFilterCount);
        return "admin/nhan-vien/danh-sach";
    }
    @GetMapping("/create")
    public String create(Model model) { model.addAttribute("employee",new EmployeeDto());return "admin/nhan-vien/bieu-mau"; }
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Integer id,Model model) {
        var item=people.employee(id);var form=new EmployeeDto();BeanUtils.copyProperties(item,form);
        form.setUsername(item.getUser().getUsername());
        model.addAttribute("employee",form);return "admin/nhan-vien/bieu-mau";
    }
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Integer id,@RequestParam(defaultValue="0") String page,Model model) {
        var item=people.employee(id);model.addAttribute("person",item);model.addAttribute("self",people.isSelf(item.getUser()));
        return "admin/nhan-vien/chi-tiet";
    }
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("employee") EmployeeDto form,BindingResult errors,Model model,RedirectAttributes flash) {
        if(!errors.hasErrors()) {
            try {
                var saved=accounts.saveEmployee(form);flash.addFlashAttribute(saved.issued()&&!saved.sent()?"warning":"message",saved.message());
                return "redirect:/admin/employees/detail/"+saved.personId();
            } catch(PeopleValidationException ex) { errors.rejectValue(ex.getField(),"invalid",ex.getMessage()); }
            catch(DataIntegrityViolationException ex) { errors.reject("duplicate","Dữ liệu mã/thư điện tử/SĐT/căn cước/tài khoản vừa được sử dụng. Kiểm tra lại trước khi lưu."); }
        }
        form.setPassword(null);
        return "admin/nhan-vien/bieu-mau";
    }
    @PostMapping("/status/{id}")
    public String status(@PathVariable Integer id,@RequestParam boolean enabled,RedirectAttributes flash) {
        try { people.employeeStatus(id,enabled);flash.addFlashAttribute("message",enabled?"Đã kích hoạt. Tài khoản chỉ hoạt động khi mọi hồ sơ liên kết đều hoạt động.":"Đã vô hiệu hóa hồ sơ và tài khoản liên kết. Lịch sử vẫn được giữ nguyên."); }
        catch(IllegalArgumentException ex) { flash.addFlashAttribute("error",ex.getMessage()); }
        return "redirect:/admin/employees/detail/"+id;
    }
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Integer id,RedirectAttributes flash) {
        try { flash.addFlashAttribute("message",people.deleteEmployee(id)); }
        catch(IllegalArgumentException ex) { flash.addFlashAttribute("error",ex.getMessage()); }
        catch(DataIntegrityViolationException ex) { flash.addFlashAttribute("error","Dữ liệu vừa phát sinh tham chiếu. Không xóa; hãy vô hiệu hóa hồ sơ."); }
        return "redirect:/admin/employees";
    }
    @PostMapping("/{id}/send-account")
    public String sendAccount(@PathVariable Integer id,@RequestParam(defaultValue="false") boolean confirmed,RedirectAttributes flash) {
        try {var result=accounts.resend("employees",id,confirmed);flash.addFlashAttribute(result.sent()?"message":"warning",result.message());}
        catch(PeopleValidationException ex){flash.addFlashAttribute("warning",ex.getMessage());}
        return "redirect:/admin/employees/detail/"+id;
    }
}
