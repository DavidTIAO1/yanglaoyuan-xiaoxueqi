package com.automedicine.ai;

import com.automedicine.ai.dto.ChatResponse;
import com.automedicine.ai.dto.MedicineIntent;
import com.automedicine.ai.prompt.SystemPromptTemplate;
import com.automedicine.entity.Medicine;
import com.automedicine.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DeepSeek 对话服务
 * <p>
 * 负责调用 DeepSeek API，传入系统提示词 + 对话历史 + 用户消息，获取结构化 JSON 响应。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MedicineChatService {

    private final ChatClient chatClient;
    private final MedicineRepository medicineRepository;

    private static final Pattern JSON_PATTERN = Pattern.compile("\\{[\\s\\S]*\\}");

    /**
     * 调用 DeepSeek 获取 AI 响应
     *
     * @param conversationHistory 对话历史，偶数位为用户消息，奇数位为 AI 回复
     * @param userMessage         当前用户消息
     * @return 解析后的 ChatResponse
     */
    public ChatResponse chat(List<String> conversationHistory, String userMessage) {
        String medicineCatalog = buildMedicineCatalog();
        String systemPrompt = SystemPromptTemplate.getSystemPrompt(medicineCatalog);

        try {
            // 构建消息列表：系统提示词 + 历史对话 + 当前用户消息
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(systemPrompt));

            // 添加历史对话上下文（最多保留最近 10 条，即 5 轮）
            int start = Math.max(0, conversationHistory.size() - 10);
            for (int i = start; i < conversationHistory.size(); i++) {
                if (i % 2 == 0) {
                    messages.add(new UserMessage(conversationHistory.get(i)));
                } else {
                    messages.add(new AssistantMessage(conversationHistory.get(i)));
                }
            }

            // 当前用户消息
            messages.add(new UserMessage(userMessage));

            String aiResponse = chatClient.prompt(new Prompt(messages))
                    .call()
                    .content();

            log.info("DeepSeek raw response: {}", aiResponse);

            return parseAIResponse(aiResponse);

        } catch (Exception e) {
            log.error("DeepSeek API 调用失败", e);
            return ChatResponse.builder()
                    .reply("抱歉，AI 服务暂时不可用，请稍后再试。您也可以直接联系药房咨询。")
                    .matchedMedicines(List.of())
                    .needsClarification(false)
                    .intents(List.of())
                    .build();
        }
    }

    /**
     * 构建药品目录文本，供系统提示词使用
     */
    private String buildMedicineCatalog() {
        List<Medicine> medicines = medicineRepository.findByStatus(1);
        if (medicines.isEmpty()) {
            return "（当前无可用药品）";
        }

        StringBuilder sb = new StringBuilder();
        for (Medicine m : medicines) {
            sb.append("- ")
              .append(m.getName())
              .append("（¥").append(m.getPrice()).append("，库存")
              .append(m.getStock() != null ? m.getStock() : 0).append("）");
            if (m.getCategory() != null && !m.getCategory().isBlank()) {
                sb.append(" [").append(m.getCategory()).append("]");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * 解析 AI 返回的文本为 ChatResponse
     */
    private ChatResponse parseAIResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return emptyResponse("抱歉，未能获取回复，请重试。");
        }

        try {
            String json = extractJson(rawResponse);

            // 使用 Jackson 手动解析（避免依赖额外的 ObjectMapper 注入）
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(json);

            // 解析 medicines 数组
            List<MedicineIntent> intents = new ArrayList<>();
            if (node.has("medicines") && node.get("medicines").isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode m : node.get("medicines")) {
                    intents.add(MedicineIntent.builder()
                            .name(m.has("name") ? m.get("name").asText() : "")
                            .quantity(m.has("quantity") ? m.get("quantity").asInt(1) : 1)
                            .confidence(m.has("confidence") ? m.get("confidence").asDouble(0.5) : 0.5)
                            .build());
                }
            }

            boolean needsClarification = node.has("needsClarification")
                    && node.get("needsClarification").asBoolean(false);
            String reply = node.has("reply") ? node.get("reply").asText("") : "";

            return ChatResponse.builder()
                    .reply(reply)
                    .matchedMedicines(List.of())
                    .needsClarification(needsClarification)
                    .intents(intents)
                    .build();

        } catch (Exception e) {
            log.info("AI 响应为非 JSON 格式，将作为纯文本回复: {}", rawResponse);
            // 解析失败时，将原始回复作为纯文本返回给用户
            return ChatResponse.builder()
                    .reply(rawResponse)
                    .matchedMedicines(List.of())
                    .needsClarification(true)
                    .intents(List.of())
                    .build();
        }
    }

    /**
     * 从可能包含 markdown 代码块或多余文字的响应中提取 JSON
     */
    private String extractJson(String text) {
        // 尝试去掉 ```json ... ``` 包裹
        String cleaned = text.trim();
        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            if (firstNewline > 0) {
                cleaned = cleaned.substring(firstNewline + 1);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
            cleaned = cleaned.trim();
        }

        // 尝试提取第一个 { ... } 块
        Matcher matcher = JSON_PATTERN.matcher(cleaned);
        if (matcher.find()) {
            return matcher.group();
        }

        return cleaned;
    }

    private ChatResponse emptyResponse(String reply) {
        return ChatResponse.builder()
                .reply(reply)
                .matchedMedicines(List.of())
                .needsClarification(false)
                .intents(List.of())
                .build();
    }
}
