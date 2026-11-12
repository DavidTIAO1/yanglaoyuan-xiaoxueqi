package com.automedicine.service;

import com.automedicine.ai.IntentRecognitionService;
import com.automedicine.ai.MedicineChatService;
import com.automedicine.ai.dto.ChatResponse;
import com.automedicine.exception.BusinessException;
import com.automedicine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 对话编排服务
 * <p>
 * 管理对话历史 → 调用 AI → 解析意图 → 匹配药品库 → 返回确认信息。
 * 对话历史存储在 Redis 中，过期时间 1 天。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final MedicineChatService medicineChatService;
    private final IntentRecognitionService intentRecognitionService;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CHAT_HISTORY_PREFIX = "chat:history:";
    private static final String CHAT_LOG_PREFIX = "chat:log:";
    private static final int MAX_HISTORY_MESSAGES = 20;  // 最多保留 10 轮（20 条消息）
    private static final long EXPIRE_DAYS = 1;

    /**
     * 发送消息并获取 AI 响应
     */
    public ChatResponse sendMessage(String username, String message) {
        userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", 404));

        // 从 Redis 获取历史上下文
        List<String> history = getConversationHistory(username);

        // 调用 AI
        ChatResponse aiResponse = medicineChatService.chat(history, message);

        // 匹配药品库
        List<ChatResponse.MatchedMedicine> matched = List.of();
        if (aiResponse.getIntents() != null && !aiResponse.getIntents().isEmpty()) {
            matched = intentRecognitionService.matchMedicines(aiResponse.getIntents());
        }

        // 构建最终响应
        ChatResponse response = ChatResponse.builder()
                .reply(aiResponse.getReply())
                .matchedMedicines(matched)
                .needsClarification(aiResponse.getNeedsClarification())
                .intents(aiResponse.getIntents())
                .build();

        // 更新 Redis 对话历史
        String historyKey = CHAT_HISTORY_PREFIX + username;
        redisTemplate.opsForList().rightPush(historyKey, message);
        redisTemplate.opsForList().rightPush(historyKey, response.getReply());

        // 超过 20 条时裁剪，保留最近 20 条
        Long size = redisTemplate.opsForList().size(historyKey);
        if (size != null && size > MAX_HISTORY_MESSAGES) {
            redisTemplate.opsForList().trim(historyKey, size - MAX_HISTORY_MESSAGES, -1);
        }
        redisTemplate.expire(historyKey, EXPIRE_DAYS, TimeUnit.DAYS);

        // 记录展示用的对话日志到 Redis
        String logKey = CHAT_LOG_PREFIX + username;
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("userMessage", message);
        entry.put("aiReply", response.getReply());
        entry.put("matchedMedicines", matched);
        entry.put("needsClarification", response.getNeedsClarification());
        redisTemplate.opsForList().rightPush(logKey, entry);
        redisTemplate.expire(logKey, EXPIRE_DAYS, TimeUnit.DAYS);

        return response;
    }

    /**
     * 获取对话历史（供前端展示）
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getChatHistory(String username) {
        userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", 404));

        String logKey = CHAT_LOG_PREFIX + username;
        List<Object> raw = redisTemplate.opsForList().range(logKey, 0, -1);
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        return raw.stream()
                .map(o -> (Map<String, Object>) o)
                .toList();
    }

    /**
     * 清空对话历史
     */
    public void clearHistory(String username) {
        redisTemplate.delete(CHAT_HISTORY_PREFIX + username);
        redisTemplate.delete(CHAT_LOG_PREFIX + username);
    }

    /**
     * 从 Redis 读取原始对话历史（用于 AI 上下文）
     */
    private List<String> getConversationHistory(String username) {
        String key = CHAT_HISTORY_PREFIX + username;
        List<Object> raw = redisTemplate.opsForList().range(key, 0, -1);
        if (raw == null) {
            return new ArrayList<>();
        }
        return raw.stream()
                .map(Object::toString)
                .toList();
    }
}
