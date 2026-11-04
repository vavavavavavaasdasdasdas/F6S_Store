package com.example.demodatn.dto.hotro;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotMessageResponse {
    private String text;
    @Builder.Default
    private List<ChatbotProductCardDto> products = new ArrayList<>();
    @Builder.Default
    private List<String> quickReplies = new ArrayList<>();
    private boolean suggestSupport;
    private boolean requireLogin;
}
