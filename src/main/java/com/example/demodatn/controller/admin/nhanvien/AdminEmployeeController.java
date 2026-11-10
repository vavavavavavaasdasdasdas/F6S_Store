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
    private final com.example.demodatn.repository.nhanvien.UserRepository users;
    private final PeopleService people;
    private final com.example.demodatn.service.nhanvien.AccountProvisioningService accounts;
    private final com.example.demodatn.service.nhanvien.AccountMailService mailService;

    private final com.example.demodatn.util.Pagination pagination;

    @ModelAttribute("positions")
    public List<String> positions() {
        return java.util.List.of("Admin","Nhân viên");
    }

    @GetMapping
    public String list(@RequestParam(defaultValue="") String keyword,
                       @RequestParam(required=false) String position,
                       @RequestParam(required=false) Boolean status,
                       @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       @RequestParam(defaultValue="0") String page,
                       java.security.Principal principal,
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
                String roleTarget = "admin".equalsIgnoreCase(pos) ? "ROLE_ADMIN" : "ROLE_EMPLOYEE";
                var roleJoin = userJoin.join("role", JoinType.LEFT);
                predicates.add(cb.equal(roleJoin.get("name"), roleTarget));
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
        model.addAttribute("currentUsername", principal != null ? principal.getName() : "");
        model.addAttribute("activeAdminCount", users.countByRole_NameAndStatusTrue("ROLE_ADMIN"));
        return "admin/nhan-vien/danh-sach";
    }
    @GetMapping("/create")
    public String create(Model model) { var dto = new EmployeeDto(); dto.setEmployeeCode(people.nextEmployeeCode()); model.addAttribute("employee", dto); return "admin/nhan-vien/bieu-mau"; }
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Integer id,Model model,RedirectAttributes flash) {
        var item=people.employee(id);
        boolean isSelf = people.isSelf(item.getUser());
        boolean isOtherAdmin = item.getUser() != null && item.getUser().getRole() != null 
                && "ROLE_ADMIN".equals(item.getUser().getRole().getName()) && !isSelf;
        if (isOtherAdmin) {
            flash.addFlashAttribute("error", "Quản trị viên không thể chỉnh sửa thông tin của quản trị viên khác.");
            return "redirect:/admin/employees/detail/" + id;
        }
        var form=new EmployeeDto();BeanUtils.copyProperties(item,form);
        form.setUsername(item.getUser().getUsername());
        model.addAttribute("employee",form);return "admin/nhan-vien/bieu-mau";
    }
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Integer id,@RequestParam(defaultValue="0") String page,Model model) {
        var item=people.employee(id);
        boolean isSelf = people.isSelf(item.getUser());
        boolean isOtherAdmin = item.getUser() != null && item.getUser().getRole() != null 
                && "ROLE_ADMIN".equals(item.getUser().getRole().getName()) && !isSelf;
        model.addAttribute("person",item);
        model.addAttribute("self",isSelf);
        model.addAttribute("isOtherAdmin",isOtherAdmin);
        model.addAttribute("canEdit",!isOtherAdmin);
        return "admin/nhan-vien/chi-tiet";
    }
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("employee") EmployeeDto form,BindingResult errors,Model model,RedirectAttributes flash) {
        if (form.getId() != null) {
            var existing = people.employee(form.getId());
            if (existing != null && existing.getUser() != null && existing.getUser().getRole() != null 
                    && "ROLE_ADMIN".equals(existing.getUser().getRole().getName()) && !people.isSelf(existing.getUser())) {
                flash.addFlashAttribute("error", "Quản trị viên không thể chỉnh sửa thông tin của quản trị viên khác.");
                return "redirect:/admin/employees/detail/" + form.getId();
            }
        }
        if(!errors.hasErrors()) {
            try {
                var saved=accounts.saveEmployee(form);flash.addFlashAttribute(saved.issued()&&!saved.sent()?"warning":"message",saved.message());
                return "redirect:/admin/employees/detail/"+saved.personId();
            } catch(PeopleValidationException ex) { errors.rejectValue(ex.getField(),"invalid",ex.getMessage()); }
            catch(org.springframework.security.access.AccessDeniedException ex) {
                flash.addFlashAttribute("error", ex.getMessage());
                return form.getId() != null ? "redirect:/admin/employees/detail/" + form.getId() : "redirect:/admin/employees";
            }
            catch(DataIntegrityViolationException ex) { errors.reject("duplicate","Dữ liệu mã/thư điện tử/SĐT/căn cước/tài khoản vừa được sử dụng. Kiểm tra lại trước khi lưu."); }
        }
        form.setPassword(null);
        return "admin/nhan-vien/bieu-mau";
    }
    @PostMapping("/status/{id}")
    public Object status(@PathVariable Integer id,@RequestParam boolean enabled,
                         jakarta.servlet.http.HttpServletRequest request,RedirectAttributes flash) {
        try {
            people.employeeStatus(id,enabled);
            String msg = enabled ? "Đã kích hoạt. Tài khoản chỉ hoạt động khi mọi hồ sơ liên kết đều hoạt động." : "Đã vô hiệu hóa hồ sơ và tài khoản liên kết. Lịch sử vẫn được giữ nguyên.";
            String xhr = request.getHeader("X-Requested-With");
            String accept = request.getHeader("Accept");
            if ("XMLHttpRequest".equalsIgnoreCase(xhr) || (accept != null && accept.contains("application/json"))) {
                return org.springframework.http.ResponseEntity.ok(java.util.Map.of(
                        "id", id,
                        "enabled", enabled,
                        "active", enabled,
                        "stateLabel", enabled ? "Đang hoạt động" : "Đã tắt",
                        "message", msg
                ));
            }
            flash.addFlashAttribute("message", msg);
        } catch(IllegalArgumentException ex) {
            String xhr = request.getHeader("X-Requested-With");
            String accept = request.getHeader("Accept");
            if ("XMLHttpRequest".equalsIgnoreCase(xhr) || (accept != null && accept.contains("application/json"))) {
                return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("message", ex.getMessage()));
            }
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/employees/detail/"+id;
    }
    @PostMapping("/delete/{id}")
    public void delete(@PathVariable Integer id) {
        throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED,
                "Chức năng xóa đã chuyển thành tắt hoạt động.");
    }
    @PostMapping("/{id}/send-account")
    public String sendAccount(@PathVariable Integer id, @RequestParam(defaultValue = "false") boolean confirmed, RedirectAttributes flash) {
        var item = people.employee(id);
        if (item.getUser() != null && item.getUser().getRole() != null 
                && "ROLE_ADMIN".equals(item.getUser().getRole().getName()) && !people.isSelf(item.getUser())) {
            flash.addFlashAttribute("error", "Quản trị viên không thể cấp lại thông tin của quản trị viên khác.");
            return "redirect:/admin/employees/detail/" + id;
        }
        try {
            var result = accounts.resend("employees", id, confirmed);
            if (result.sent()) {
                flash.addFlashAttribute("message", "Đã gửi email thông tin tài khoản thành công tới nhân viên.");
            } else {
                String err = mailService.getLastError();
                String msg = "Đã lưu tài khoản nhưng chưa gửi được email" + (err != null && !err.isBlank() ? " (" + err + ")" : "") + ". Hãy kiểm tra Cấu hình Email và thử lại.";
                flash.addFlashAttribute("warning", msg);
            }
        } catch (PeopleValidationException ex) {
            flash.addFlashAttribute("warning", ex.getMessage());
        }
        return "redirect:/admin/employees/detail/" + id;
    }
}
