package com.example.demodatn.service.hotro;

import com.example.demodatn.entity.hotro.SupportTicket;
import com.example.demodatn.entity.nhanvien.Employee;
import com.example.demodatn.repository.hotro.SupportTicketRepository;
import com.example.demodatn.service.nhanvien.AccountMailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final AccountMailService accountMailService;
    private final ObjectProvider<JavaMailSender> senderProvider;

    public long countNewTickets() {
        return ticketRepository.countByTrangThai("Mới");
    }

    public Page<SupportTicket> searchTickets(String keyword, String trangThai, LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable) {
        return ticketRepository.searchTickets(keyword, trangThai, fromDate, toDate, pageable);
    }

    public SupportTicket getTicket(Integer id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu hỗ trợ ID " + id));
    }

    @Transactional
    public void replyTicket(Integer id, String traLoi, String trangThai, Employee employee) {
        SupportTicket ticket = getTicket(id);

        if (traLoi == null || traLoi.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập nội dung phản hồi.");
        }

        ticket.setTraLoi(traLoi.trim());
        ticket.setTraLoiLuc(LocalDateTime.now());
        ticket.setEmployee(employee);
        if (trangThai != null && !trangThai.isBlank()) {
            ticket.setTrangThai(trangThai);
        } else {
            ticket.setTrangThai("Đã xong");
        }
        ticketRepository.save(ticket);

        // Kiểm tra xem có email để gửi không
        String email = extractEmail(ticket.getLienHe());
        if (email == null && ticket.getCustomer() != null && ticket.getCustomer().getEmail() != null) {
            email = ticket.getCustomer().getEmail().trim();
        }

        if (email != null && !email.isBlank()) {
            sendReplyEmail(email, ticket.getHoTen(), ticket.getMa(), ticket.getNoiDung(), traLoi);
        } else {
            log.info("Phiếu hỗ trợ {} không có email liên hệ (chỉ có SĐT), nhân viên cần liên hệ qua điện thoại.", ticket.getMa());
        }
    }

    @Transactional
    public void updateStatus(Integer id, String trangThai, Employee employee) {
        SupportTicket ticket = getTicket(id);
        ticket.setTrangThai(trangThai);
        ticket.setEmployee(employee);
        ticketRepository.save(ticket);
    }

    private String extractEmail(String contact) {
        if (contact == null) return null;
        contact = contact.trim();
        if (contact.contains("@") && contact.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            return contact;
        }
        return null;
    }

    private void sendReplyEmail(String toEmail, String customerName, String ticketCode, String question, String replyContent) {
        if (!accountMailService.isConfigured()) {
            log.warn("Chưa cấu hình mail, bỏ qua gửi email phản hồi phiếu hỗ trợ {}.", ticketCode);
            return;
        }

        try {
            JavaMailSender mailSender = senderProvider.getIfAvailable();
            if (mailSender == null) return;

            String fromAddress = accountMailService.getResolvedFrom();
            String subject = "[F6S Store] Phản hồi yêu cầu hỗ trợ #" + ticketCode;

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);

            StringBuilder html = new StringBuilder();
            html.append("<div style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;\">");
            html.append("<h2 style=\"color: #1a73e8; border-bottom: 2px solid #eaeaea; padding-bottom: 10px;\">F6S STORE - PHẢN HỒI HỖ TRỢ</h2>");
            html.append("<p>Xin chào <strong>").append(HtmlUtils.htmlEscape(customerName)).append("</strong>,</p>");
            html.append("<p>Bộ phận Chăm sóc khách hàng F6S Store xin gửi phản hồi về yêu cầu hỗ trợ <strong>#").append(ticketCode).append("</strong> của bạn:</p>");
            html.append("<div style=\"background: #f8f9fa; border-left: 4px solid #ddd; padding: 10px 14px; margin: 15px 0;\">");
            html.append("<p style=\"margin: 0; font-size: 13px; color: #666;\"><strong>Yêu cầu của bạn:</strong></p>");
            html.append("<p style=\"margin: 4px 0 0;\">").append(HtmlUtils.htmlEscape(question)).append("</p>");
            html.append("</div>");
            html.append("<div style=\"background: #e8f0fe; border-left: 4px solid #1a73e8; padding: 12px 16px; margin: 15px 0;\">");
            html.append("<p style=\"margin: 0; font-size: 13px; color: #1a73e8;\"><strong>Nội dung phản hồi:</strong></p>");
            html.append("<p style=\"margin: 6px 0 0; white-space: pre-line;\">").append(HtmlUtils.htmlEscape(replyContent)).append("</p>");
            html.append("</div>");
            html.append("<p style=\"margin-top: 25px; font-size: 12px; color: #777; border-top: 1px solid #eaeaea; padding-top: 10px;\">Cảm ơn bạn đã liên hệ F6S Store. Nếu cần thêm hỗ trợ, bạn có thể nhắn tin trực tiếp qua website!</p>");
            html.append("</div>");

            helper.setText(html.toString(), true);
            mailSender.send(message);
            log.info("Đã gửi email phản hồi phiếu hỗ trợ {} tới {}", ticketCode, toEmail);
        } catch (Exception ex) {
            log.error("Lỗi gửi email phản hồi phiếu hỗ trợ {}: {}", ticketCode, ex.getMessage());
        }
    }
}
