package com.automedicine.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通知 WebSocket 处理器
 * <p>
 * 维护用户连接池，新通知创建时实时推送给目标用户。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    /** 用户连接池：username → session */
    private final ConcurrentHashMap<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String username = (String) session.getAttributes().get("username");
        if (username == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        userSessions.put(username, session);
        log.info("通知 WebSocket 连接建立: {}", username);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 客户端心跳或消息，暂不处理
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String username = (String) session.getAttributes().get("username");
        if (username != null) {
            userSessions.remove(username);
            log.info("通知 WebSocket 连接关闭: {} ({})", username, status);
        }
    }

    /**
     * 推送通知给指定用户
     */
    public void pushNotification(String username, Long notificationId,
                                 String title, String content, String type) {
        try {
            Map<String, Object> payload = Map.of(
                    "type", "NOTIFICATION",
                    "notificationId", notificationId,
                    "title", title,
                    "content", content,
                    "notificationType", type
            );

            WebSocketSession session = userSessions.get(username);
            if (session != null && session.isOpen()) {
                String json = objectMapper.writeValueAsString(payload);
                synchronized (session) {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(json));
                    }
                }
                log.info("通知推送成功: {} → {}", username, title);
            } else {
                log.debug("用户 {} 未连接通知 WebSocket，跳过推送", username);
            }
        } catch (IOException e) {
            log.error("通知推送失败: {}", username, e);
        }
    }
}
