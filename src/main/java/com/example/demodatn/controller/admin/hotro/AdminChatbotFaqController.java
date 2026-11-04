package com.example.demodatn.controller.admin.hotro;

import com.example.demodatn.entity.hotro.ChatbotFaq;
import com.example.demodatn.repository.hotro.ChatbotFaqRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/admin/chatbot-faq")
@RequiredArgsConstructor
public class AdminChatbotFaqController {

    private final ChatbotFaqRepository faqRepository;

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(required = false) Boolean hienThi,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        Page<ChatbotFaq> pageResult = faqRepository.searchFaq(
                keyword, hienThi, PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "ma"))
        );

        model.addAttribute("pageResult", pageResult);
        model.addAttribute("keyword", keyword);
        model.addAttribute("hienThi", hienThi);
        return "admin/chatbot-faq/danh-sach";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        Integer maxNum = faqRepository.findMaxCodeNumber();
        int nextNum = (maxNum != null ? maxNum : 0) + 1;
        String autoCode = String.format("KB%03d", nextNum);

        ChatbotFaq faq = new ChatbotFaq();
        faq.setMa(autoCode);
        faq.setHienThi(true);

        model.addAttribute("faq", faq);
        model.addAttribute("isNew", true);
        return "admin/chatbot-faq/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Integer id, Model model) {
        ChatbotFaq faq = faqRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kiến thức FAQ ID " + id));

        model.addAttribute("faq", faq);
        model.addAttribute("isNew", false);
        return "admin/chatbot-faq/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("faq") ChatbotFaq faq,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isNew", faq.getId() == null);
            return "admin/chatbot-faq/form";
        }

        if (faq.getId() == null) {
            Integer maxNum = faqRepository.findMaxCodeNumber();
            int nextNum = (maxNum != null ? maxNum : 0) + 1;
            faq.setMa(String.format("KB%03d", nextNum));
        } else {
            // Đảm bảo không đổi mã
            ChatbotFaq existing = faqRepository.findById(faq.getId()).orElse(null);
            if (existing != null) {
                faq.setMa(existing.getMa());
            }
        }

        faqRepository.save(faq);
        redirectAttributes.addFlashAttribute("message", "Đã lưu kiến thức chatbot thành công!");
        return "redirect:/admin/chatbot-faq";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        ChatbotFaq faq = faqRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy FAQ ID " + id));

        faq.setHienThi(!Boolean.TRUE.equals(faq.getHienThi()));
        faqRepository.save(faq);

        redirectAttributes.addFlashAttribute("message", "Đã cập nhật trạng thái hiển thị cho mã " + faq.getMa());
        return "redirect:/admin/chatbot-faq";
    }
}
