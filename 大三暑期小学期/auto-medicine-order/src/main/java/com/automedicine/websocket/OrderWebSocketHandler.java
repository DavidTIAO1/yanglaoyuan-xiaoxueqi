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
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订单 WebSocket 处理器
 * <p>
 * 维护商家连接池和用户连接池。
 * 订单创建/放弃时广播给所有商家；订单状态变更时推送给对应用户。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    /** 商家连接池：username → session */
    private final ConcurrentHashMap<String, WebSocketSession> merchantSessions = new ConcurrentHashMap<>();

    /** 用户连接池：username → session */
    private final ConcurrentHashMap<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String username = (String) session.getAttributes().get("username");
        if (username == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        // 默认加入商家连接池（MERCHANT 角色），前端可根据角色选择连接端点
        merchantSessions.put(username, session);
        log.info("商家 WebSocket 连接建立: {}", username);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String username = (String) session.getAttributes().get("username");

        // 处理客户端发送的角色声明消息
        String payload = message.getPayload();
        if (payload.contains("\"role\":\"USER\"")) {
            merchantSessions.remove(username);
            userSessions.put(username, session);
            log.info("用户 {} 切换到 USER 连接池", username);
        } else if (payload.contains("\"role\":\"MERCHANT\"")) {
            userSessions.remove(username);
            merchantSessions.put(username, session);
            log.info("用户 {} 切换到 MERCHANT 连接池", username);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String username = (String) session.getAttributes().get("username");
        if (username != null) {
            merchantSessions.remove(username);
            userSessions.remove(username);
            log.info("WebSocket 连接关闭: {} ({})", username, status);
        }
    }

    /**
     * 广播新订单给所有连接的商家
     */
    public void broadcastNewOrder(String orderNo, Long orderId,
                                  List<Map<String, Object>> items,
                                  BigDecimal totalAmount, String deliveryAddress) {
        try {
            Map<String, Object> payload = Map.of(
                    "type", "NEW_ORDER",
                    "orderNo", orderNo,
                    "orderId", orderId,
                    "items", items,
                    "totalAmount", totalAmount,
                    "deliveryAddress", deliveryAddress != null ? deliveryAddress : ""
            );
            sendToAllMerchants(payload);
        } catch (Exception e) {
            log.error("广播新订单失败", e);
        }
    }

    /**
     * 订单状态变更 → 推送给对应用户
     */
    public void sendOrderStatusToUser(String username, Long orderId, String orderNo,
                                      String newStatus, String message) {
        try {
            Map<String, Object> payload = Map.of(
                    "type", "ORDER_STATUS",
                    "orderId", orderId,
                    "orderNo", orderNo,
                    "status", newStatus,
                    "message", message != null ? message : ""
            );
            sendToUser(username, payload);
        } catch (Exception e) {
            log.error("推送订单状态给用户失败: {}", username, e);
        }
    }

    // ========== 内部工具方法 ==========

    private void sendToAllMerchants(Map<String, Object> payload) {
        String json = toJson(payload);
        TextMessage textMessage = new TextMessage(json);

        merchantSessions.forEach((username, session) -> {
            if (session.isOpen()) {
                sendMessageSafely(session, textMessage);
            }
        });
    }

    private void sendToUser(String username, Map<String, Object> payload) {
        WebSocketSession session = userSessions.get(username);
        if (session != null && session.isOpen()) {
            sendMessageSafely(session, new TextMessage(toJson(payload)));
        }
    }

    private void sendMessageSafely(WebSocketSession session, TextMessage message) {
        synchronized (session) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            } catch (IOException e) {
                log.error("WebSocket 消息发送失败, session={}", session.getId(), e);
            }
        }
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("JSON 序列化失败", e);
            return "{}";
        }
    }
}
