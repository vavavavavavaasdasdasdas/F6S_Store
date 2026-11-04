package com.example.demodatn.controller.admin.hotro;

import com.example.demodatn.entity.hotro.SupportTicket;
import com.example.demodatn.entity.nhanvien.Employee;
import com.example.demodatn.repository.nhanvien.EmployeeRepository;
import com.example.demodatn.service.hotro.SupportTicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Slf4j
@Controller
@RequestMapping("/admin/support-tickets")
@RequiredArgsConstructor
public class AdminSupportTicketController {

    private final SupportTicketService ticketService;
    private final EmployeeRepository employeeRepository;

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "") String trangThai,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {

        LocalDateTime start = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime end = toDate != null ? toDate.atTime(LocalTime.MAX) : null;

        Page<SupportTicket> pageResult = ticketService.searchTickets(
                keyword, trangThai, start, end,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        model.addAttribute("pageResult", pageResult);
        model.addAttribute("keyword", keyword);
        model.addAttribute("trangThai", trangThai);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("newTicketsCount", ticketService.countNewTickets());

        return "admin/ho-tro/danh-sach";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        SupportTicket ticket = ticketService.getTicket(id);
        model.addAttribute("ticket", ticket);
        return "admin/ho-tro/chi-tiet";
    }

    @PostMapping("/{id}/reply")
    public String reply(@PathVariable Integer id,
                        @RequestParam String traLoi,
                        @RequestParam(defaultValue = "Đã xong") String trangThai,
                        Authentication authentication,
                        RedirectAttributes redirectAttributes) {
        try {
            Employee employee = null;
            if (authentication != null) {
                employee = employeeRepository.findByUser_Username(authentication.getName()).orElse(null);
            }
            ticketService.replyTicket(id, traLoi, trangThai, employee);
            redirectAttributes.addFlashAttribute("message", "Đã gửi phản hồi và cập nhật trạng thái phiếu hỗ trợ thành công!");
        } catch (Exception ex) {
            log.error("Lỗi phản hồi phiếu hỗ trợ {}: {}", id, ex.getMessage());
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + ex.getMessage());
        }
        return "redirect:/admin/support-tickets/" + id;
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Integer id,
                               @RequestParam String trangThai,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        Employee employee = null;
        if (authentication != null) {
            employee = employeeRepository.findByUser_Username(authentication.getName()).orElse(null);
        }
        ticketService.updateStatus(id, trangThai, employee);
        redirectAttributes.addFlashAttribute("message", "Đã cập nhật trạng thái phiếu hỗ trợ!");
        return "redirect:/admin/support-tickets/" + id;
    }
}
