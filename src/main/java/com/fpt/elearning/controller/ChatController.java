package com.fpt.elearning.controller;

import com.fpt.elearning.dto.ChatRequest;
import com.fpt.elearning.dto.ChatResponse;
import com.fpt.elearning.service.ChatRagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoint cho chat AI (RAG). Gọi từ JS fetch ở widget chat.
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatRagService chatRagService;

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String q = request.question() == null ? "" : request.question().trim();
        if (q.isEmpty()) {
            return new ChatResponse("Bạn hãy nhập câu hỏi nhé.");
        }
        return new ChatResponse(chatRagService.answer(q));
    }
}
