package com.automedicine.service;

import com.automedicine.dto.response.NotificationResponse;
import com.automedicine.entity.FamilyRelation;
import com.automedicine.entity.Notification;
import com.automedicine.entity.User;
import com.automedicine.enums.NotificationType;
import com.automedicine.exception.BusinessException;
import com.automedicine.repository.FamilyRelationRepository;
import com.automedicine.repository.NotificationRepository;
import com.automedicine.repository.UserRepository;
import com.automedicine.websocket.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 通知服务
 * <p>
 * 创建通知 → WebSocket 实时推送 → 查询列表 → 未读计数 → 标记已读。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final FamilyRelationRepository familyRelationRepository;
    private final NotificationWebSocketHandler notificationWebSocketHandler;

    /**
     * 创建通知并通过 WebSocket 实时推送
     */
    public void createNotification(Long userId, String title, String content,
                                   NotificationType type, Long relatedOrderId) {
        createNotification(userId, title, content, type, relatedOrderId, null);
    }

    /**
     * 创建通知（含亲友关系ID）并通过 WebSocket 实时推送
     */
    public void createNotification(Long userId, String title, String content,
                                   NotificationType type, Long relatedOrderId,
                                   Long relatedFamilyRelationId) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在", 404));

        Notification notification = new Notification();
        notification.setUser(targetUser);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setRelatedOrderId(relatedOrderId);
        notification.setRelatedFamilyRelationId(relatedFamilyRelationId);
        notification.setIsRead(false);
        notificationRepository.save(notification);

        // WebSocket 实时推送给目标用户
        notificationWebSocketHandler.pushNotification(
                targetUser.getUsername(), notification.getId(),
                title, content, type.name());
    }

    /**
     * 查询用户的通知列表（按时间倒序）
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(String username) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", 404));

        List<Notification> notifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId());

        // Batch-load family relation statuses to avoid N+1
        List<Long> relationIds = notifications.stream()
                .map(Notification::getRelatedFamilyRelationId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, String> relationStatusMap = relationIds.isEmpty()
                ? Map.of()
                : familyRelationRepository.findAllById(relationIds).stream()
                    .collect(Collectors.toMap(
                            FamilyRelation::getId,
                            fr -> fr.getStatus().name()));

        return notifications.stream()
                .map(n -> toResponse(n, relationStatusMap))
                .toList();
    }

    /**
     * 查询未读通知数
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(String username) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", 404));

        return notificationRepository.countByUserIdAndIsRead(user.getId(), false);
    }

    /**
     * 标记通知为已读
     */
    @Transactional
    public void markAsRead(Long notificationId, String username) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", 404));

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException("通知不存在", 404));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new BusinessException("您无权操作此通知", 403);
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    private NotificationResponse toResponse(Notification n, Map<Long, String> relationStatusMap) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .content(n.getContent())
                .type(n.getType())
                .relatedOrderId(n.getRelatedOrderId())
                .relatedFamilyRelationId(n.getRelatedFamilyRelationId())
                .familyRelationStatus(
                        n.getRelatedFamilyRelationId() != null
                                ? relationStatusMap.get(n.getRelatedFamilyRelationId())
                                : null)
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
