package com.automedicine.ai.prompt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 系统提示词模板
 * <p>
 * 从 classpath:ai/system-prompt.txt 加载提示词模板，运行时注入药品目录。
 * 提示词文件中使用 %s 作为药品目录占位符。
 * </p>
 */
@Slf4j
public final class SystemPromptTemplate {

    private static final String TEMPLATE;

    static {
        String loaded;
        try {
            loaded = new String(
                    new ClassPathResource("ai/system-prompt.txt").getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("无法加载系统提示词文件 ai/system-prompt.txt", e);
            loaded = "你是一个社区药品配送助手，仅用户完成药品下单，不要提供任何的医学建议。";
        }
        TEMPLATE = loaded;
    }

    private SystemPromptTemplate() {
    }

    /**
     * 获取系统提示词
     *
     * @param medicineCatalog 当前药品库目录（名称、价格、库存），供 AI 参考匹配
     */
    public static String getSystemPrompt(String medicineCatalog) {
        return TEMPLATE.formatted(medicineCatalog);
    }
}
