package com.automedicine.controller;

import com.automedicine.dto.response.ApiResponse;
import com.automedicine.dto.response.NotificationResponse;
import com.automedicine.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知控制器
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 获取通知列表
     */
    @GetMapping
    public ApiResponse<List<NotificationResponse>> getNotifications(
            Authentication authentication) {
        return ApiResponse.success(notificationService.getNotifications(authentication.getName()));
    }

    /**
     * 获取未读通知数
     */
    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadCount(Authentication authentication) {
        return ApiResponse.success(notificationService.getUnreadCount(authentication.getName()));
    }

    /**
     * 标记通知为已读
     */
    @PutMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {
        notificationService.markAsRead(id, authentication.getName());
        return ApiResponse.success();
    }
}
