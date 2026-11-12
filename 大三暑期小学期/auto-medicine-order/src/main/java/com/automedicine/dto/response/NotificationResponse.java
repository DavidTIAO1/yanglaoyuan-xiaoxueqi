package com.automedicine.dto.response;

import com.automedicine.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通知响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;

    private String title;

    private String content;

    private NotificationType type;

    private Long relatedOrderId;

    private Long relatedFamilyRelationId;

    /**
     * 亲友关系当前状态（PENDING / ACCEPTED / REJECTED），
     * 仅当 relatedFamilyRelationId 不为空时有值，前端据此展示已处理状态。
     */
    private String familyRelationStatus;

    private Boolean isRead;

    private LocalDateTime createdAt;
}
