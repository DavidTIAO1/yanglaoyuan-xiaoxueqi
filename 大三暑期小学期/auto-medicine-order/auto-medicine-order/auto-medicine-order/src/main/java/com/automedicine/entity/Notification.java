package com.automedicine.entity;

import com.automedicine.enums.NotificationType;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 通知实体 — 仅有 createdAt，无 updatedAt
 */
@Data
@Entity
@Table(name = "notifications")
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 100, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private NotificationType type;

    @Column(name = "related_order_id")
    private Long relatedOrderId;

    @Column(name = "related_family_relation_id")
    private Long relatedFamilyRelationId;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
