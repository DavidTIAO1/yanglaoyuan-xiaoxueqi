package com.automedicine.config;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * DeepSeek / Spring AI 配置
 * <p>
 * 基于 application.yml 中配置的 OpenAI 兼容参数（base-url 指向 DeepSeek），
 * 创建 ChatClient Bean 供 AI 服务层调用。
 * </p>
 */
@Configuration
public class DeepSeekConfig {

    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
