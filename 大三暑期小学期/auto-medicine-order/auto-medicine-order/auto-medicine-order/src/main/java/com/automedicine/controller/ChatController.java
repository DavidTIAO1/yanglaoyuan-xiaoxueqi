package com.automedicine.controller;

import com.automedicine.ai.dto.ChatResponse;
import com.automedicine.dto.request.ChatMessageRequest;
import com.automedicine.dto.response.ApiResponse;
import com.automedicine.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 对话控制器
 * <p>
 * 提供用户与 AI 对话的接口，用于自然语言购药。
 * </p>
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 发送消息，返回 AI 识别结果
     */
    @PostMapping("/send")
    public ApiResponse<ChatResponse> sendMessage(
            Authentication authentication,
            @Valid @RequestBody ChatMessageRequest request) {

        ChatResponse response = chatService.sendMessage(
                authentication.getName(), request.getMessage());

        return ApiResponse.success(response);
    }

    /**
     * 获取对话历史
     */
    @GetMapping("/history")
    public ApiResponse<List<Map<String, Object>>> getChatHistory(Authentication authentication) {
        List<Map<String, Object>> history = chatService.getChatHistory(authentication.getName());
        return ApiResponse.success(history);
    }

    /**
     * 清空对话历史（开始新对话）
     */
    @DeleteMapping("/history")
    public ApiResponse<Void> clearHistory(Authentication authentication) {
        chatService.clearHistory(authentication.getName());
        return ApiResponse.success();
    }
}
