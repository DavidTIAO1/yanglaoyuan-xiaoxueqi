package com.automedicine.config;

import com.automedicine.websocket.JwtHandshakeInterceptor;
import com.automedicine.websocket.NotificationWebSocketHandler;
import com.automedicine.websocket.OrderWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置
 * <p>
 * 注册两个 WebSocket 端点：
 * /ws/orders — 订单推送（新订单广播、状态变更通知）
 * /ws/notifications — 通用通知推送
 * 连接时需携带 JWT 参数：ws://localhost:8080/ws/orders?token=xxx
 * </p>
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final OrderWebSocketHandler orderWebSocketHandler;
    private final NotificationWebSocketHandler notificationWebSocketHandler;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(orderWebSocketHandler, "/ws/orders")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOriginPatterns("*");

        registry.addHandler(notificationWebSocketHandler, "/ws/notifications")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
