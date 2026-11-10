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
    private final com.example.demodatn.security.AdminAccess access;
    private final com.example.demodatn.service.khachhang.ShippingAddressService addressService;
    private final com.example.demodatn.repository.donhang.InvoiceRepository invoices;
    private final com.example.demodatn.service.nhanvien.AccountProvisioningService accounts;
    private final com.example.demodatn.repository.khachhang.ShippingAddressRepository shippingAddresses;
    private final com.example.demodatn.repository.donhang.OrderRepository orders;
    private final com.example.demodatn.service.nhanvien.AccountMailService mailService;

    private final com.example.demodatn.util.Pagination pagination;

    @GetMapping
    public String list(@RequestParam(defaultValue="") String keyword,
                       @RequestParam(required=false) Boolean status,
                       @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       @RequestParam(defaultValue="0") String page,
                       java.security.Principal principal,
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
        model.addAttribute("currentUsername", principal != null ? principal.getName() : "");
        return "admin/khach-hang/danh-sach";
    }
    @GetMapping("/create")
    public String create(Model model) {
        var dto = new CustomerDto();
        dto.setCustomerCode(people.nextCustomerCode());
        dto.getAddresses().add(new CustomerDto.AddressBlock());
        dto.setDefaultAddressIndex(0);
        model.addAttribute("customer", dto);
        return "admin/khach-hang/bieu-mau";
    }
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Integer id,Model model) {
        var item=people.customer(id);var form=new CustomerDto();BeanUtils.copyProperties(item,form);
        var authentication=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if(!access.customerAccount(authentication,item.getUser()))throw new org.springframework.security.access.AccessDeniedException("Không được sửa tài khoản có quyền nhân sự qua hồ sơ khách hàng.");
        if(!access.isAdmin(authentication)){form.setSoCccd(null);model.addAttribute("maskedDocument",item.getMaskedSoCccd());}
        form.setUsername(item.getUser()==null?null:item.getUser().getUsername());
        var savedAddresses = shippingAddresses.findByCustomer_IdOrderBySlotAsc(id);
        int defaultIdx = 0;
        for (int i = 0; i < savedAddresses.size(); i++) {
            var a = savedAddresses.get(i);
            var block = new CustomerDto.AddressBlock();
            block.setId(a.getId());
            block.setProvinceId(a.getProvince() == null ? null : a.getProvince().getId());
            block.setWardId(a.getWard() == null ? null : a.getWard().getId());
            block.setStreetAddress(a.getDisplayStreetAddress());
            block.setLatitude(a.getLatitude());
            block.setLongitude(a.getLongitude());
            block.setCoordinateSource(a.getCoordinateSource());
            form.getAddresses().add(block);
            if (a.isDefaultAddress()) {
                defaultIdx = i;
                form.setProvinceId(block.getProvinceId());
                form.setWardId(block.getWardId());
                form.setStreetAddress(block.getStreetAddress());
                form.setLatitude(block.getLatitude());
                form.setLongitude(block.getLongitude());
                form.setCoordinateSource(block.getCoordinateSource());
            }
        }
        if (form.getAddresses().isEmpty()) {
            var block = new CustomerDto.AddressBlock();
            block.setStreetAddress(item.getAddress());
            form.getAddresses().add(block);
        }
        form.setDefaultAddressIndex(defaultIdx);
        model.addAttribute("customer",form);return "admin/khach-hang/bieu-mau";
    }
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Integer id,@RequestParam(defaultValue="0") String page,Model model) {
        var item=people.customer(id);model.addAttribute("person",item);model.addAttribute("self",people.isSelf(item.getUser()));
        var orderPage=pagination.load(page,Sort.unsorted(),request->orders.findByCustomer_IdOrderByCreatedAtDescIdDesc(id,request));
        model.addAttribute("orderPage",orderPage);
        var invoiceIds=new java.util.HashMap<Integer,Integer>();
        if(!orderPage.isEmpty())invoices.orderInvoiceIds(orderPage.getContent().stream().map(com.example.demodatn.entity.donhang.Order::getId).toList()).forEach(row->invoiceIds.put(((Number)row[0]).intValue(),((Number)row[1]).intValue()));
        model.addAttribute("invoiceIds",invoiceIds);
        model.addAttribute("shippingAddresses",shippingAddresses.findByCustomer_IdOrderBySlotAsc(id));
        return "admin/khach-hang/chi-tiet";
    }
    @GetMapping("/{customerId}/addresses/create")
    public String createAddress(@PathVariable("customerId") Integer id,Model model,RedirectAttributes flash) {
        var person=people.customer(id);
        if(shippingAddresses.countByCustomer_Id(id)>=3){flash.addFlashAttribute("warning",com.example.demodatn.service.khachhang.ShippingAddressService.LIMIT_MESSAGE);return "redirect:/admin/customers/detail/"+id;}
        var form=new com.example.demodatn.dto.khachhang.ShippingAddressForm();form.setReceiverName(person.getFullName());form.setPhone(person.getPhone());
        model.addAttribute("addressForm",form);return addressForm(id,model);
    }
    @GetMapping("/{customerId}/addresses/{addressId}/edit")
    public String editAddress(@PathVariable("customerId") Integer id,@PathVariable Integer addressId,Model model) {
        var address=addressService.ownedForCustomer(id,addressId);var form=new com.example.demodatn.dto.khachhang.ShippingAddressForm();
        form.setId(addressId);form.setReceiverName(address.getReceiverName());form.setPhone(address.getPhone());form.setAddress(address.getAddress());form.setStreetAddress(address.getDisplayStreetAddress());form.setProvinceId(address.getProvince()==null?null:address.getProvince().getId());form.setWardId(address.getWard()==null?null:address.getWard().getId());form.setLatitude(address.getLatitude());form.setLongitude(address.getLongitude());form.setCoordinateSource(address.getCoordinateSource());form.setMakeDefault(address.isDefaultAddress());
        model.addAttribute("addressForm",form);return addressForm(id,model);
    }
    private String addressForm(Integer id,Model model){model.addAttribute("person",people.customer(id));return "admin/khach-hang/dia-chi";}
    @PostMapping("/{customerId}/addresses/save")
    public String saveAddress(@PathVariable("customerId") Integer id,@Valid @ModelAttribute("addressForm") com.example.demodatn.dto.khachhang.ShippingAddressForm form,BindingResult errors,Model model,RedirectAttributes flash) {
        if(!errors.hasErrors())try{addressService.saveForCustomer(id,form);flash.addFlashAttribute("message","Đã lưu địa chỉ nhận hàng.");return "redirect:/admin/customers/detail/"+id;}
        catch(PeopleValidationException ex){errors.rejectValue(ex.getField(),"invalid",ex.getMessage());}
        catch(DataIntegrityViolationException ex){errors.rejectValue("address","changed","Địa chỉ vừa thay đổi hoặc đã đủ 3 địa chỉ. Tải lại rồi thử lại.");}
        return addressForm(id,model);
    }
    @PostMapping("/{customerId}/addresses/{addressId}/default")
    public String defaultAddress(@PathVariable("customerId") Integer id,@PathVariable Integer addressId,RedirectAttributes flash){addressService.makeDefaultForCustomer(id,addressId);flash.addFlashAttribute("message","Đã đặt địa chỉ mặc định.");return "redirect:/admin/customers/detail/"+id;}
    @PostMapping("/{customerId}/addresses/{addressId}/delete")
    public String deleteAddress(@PathVariable("customerId") Integer id,@PathVariable Integer addressId,RedirectAttributes flash){addressService.deleteForCustomer(id,addressId);flash.addFlashAttribute("message","Đã xóa địa chỉ. Địa chỉ mặc định đã được cập nhật nếu cần.");return "redirect:/admin/customers/detail/"+id;}
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("customer") CustomerDto form,BindingResult errors,Model model,RedirectAttributes flash) {
        if (!form.hasAddressBlocks()) {
            var block = new CustomerDto.AddressBlock();
            block.setProvinceId(form.getProvinceId());
            block.setWardId(form.getWardId());
            block.setStreetAddress(form.getStreetAddress());
            block.setLatitude(form.getLatitude());
            block.setLongitude(form.getLongitude());
            block.setCoordinateSource(form.getCoordinateSource());
            form.getAddresses().add(block);
            if (!errors.hasFieldErrors("provinceId") && block.getProvinceId() == null) {
                errors.rejectValue("addresses[0].provinceId", "NotNull", "Vui lòng chọn Tỉnh/Thành phố.");
            }
            if (!errors.hasFieldErrors("wardId") && block.getWardId() == null) {
                errors.rejectValue("addresses[0].wardId", "NotNull", "Vui lòng chọn Phường/Xã.");
            }
            if (!errors.hasFieldErrors("streetAddress") && (block.getStreetAddress() == null || block.getStreetAddress().isBlank())) {
                errors.rejectValue("addresses[0].streetAddress", "NotBlank", "Vui lòng nhập số nhà, tên đường.");
            }
        }
        if (form.getAddresses().size() > 3) {
            errors.rejectValue("address", "limit", com.example.demodatn.service.khachhang.ShippingAddressService.LIMIT_MESSAGE);
        }
        if(!errors.hasErrors()) {
            try {
                var saved=accounts.saveCustomer(form);flash.addFlashAttribute(saved.issued()&&!saved.sent()?"warning":"message",saved.message());
                return "redirect:/admin/customers/detail/"+saved.personId();
            } catch(PeopleValidationException ex) { errors.rejectValue(ex.getField(),"invalid",ex.getMessage()); }
            catch(DataIntegrityViolationException ex) { errors.reject("duplicate","Dữ liệu mã/thư điện tử/SĐT/căn cước/tài khoản vừa được sử dụng. Kiểm tra lại trước khi lưu."); }
        }
        if (form.getAddresses().isEmpty()) {
            form.getAddresses().add(new CustomerDto.AddressBlock());
        }
        return "admin/khach-hang/bieu-mau";
    }
    @PostMapping("/status/{id}")
    public Object status(@PathVariable Integer id,@RequestParam boolean enabled,
                         jakarta.servlet.http.HttpServletRequest request,RedirectAttributes flash) {
        try {
            people.customerStatus(id,enabled);
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
        return "redirect:/admin/customers/detail/"+id;
    }
    @PostMapping("/delete/{id}")
    public void delete(@PathVariable Integer id) {
        throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED,
                "Chức năng xóa đã chuyển thành tắt hoạt động.");
    }
    @PostMapping("/{id}/send-account")
    public String sendAccount(@PathVariable Integer id, @RequestParam(defaultValue = "false") boolean confirmed, RedirectAttributes flash) {
        try {
            var result = accounts.resend("customers", id, confirmed);
            if (result.sent()) {
                flash.addFlashAttribute("message", "Đã gửi email thông tin tài khoản thành công tới khách hàng.");
            } else {
                String err = mailService.getLastError();
                String msg = "Đã lưu tài khoản nhưng chưa gửi được email" + (err != null && !err.isBlank() ? " (" + err + ")" : "") + ". Hãy kiểm tra Cấu hình Email và thử lại.";
                flash.addFlashAttribute("warning", msg);
            }
        } catch (PeopleValidationException ex) {
            flash.addFlashAttribute("warning", ex.getMessage());
        }
        return "redirect:/admin/customers/detail/" + id;
    }
}
