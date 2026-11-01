package com.example.demodatn.controller.user;
import com.example.demodatn.service.sanpham.ProductFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import java.security.Principal;
@ControllerAdvice(basePackages="com.example.demodatn.controller.user") @RequiredArgsConstructor
public class ProductFeedbackAdvice {
 private final ProductFeedbackService feedback;
 @ModelAttribute public void feedback(Model model,Principal principal){model.addAttribute("feedbackSignedIn",principal!=null);model.addAttribute("reviewSummaries",feedback.summaries());model.addAttribute("favoriteIds",principal==null?java.util.Set.of():feedback.favoriteIds(principal.getName()));}
}
