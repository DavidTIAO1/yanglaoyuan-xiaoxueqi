package com.automedicine.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 对话消息请求 DTO
 */
@Data
public class ChatMessageRequest {

    @NotBlank(message = "消息内容不能为空")
    private String message;
}
