package com.automedicine.repository;

import com.automedicine.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 通知数据访问层
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** 查询某用户的通知列表（按创建时间倒序） */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** 统计某用户的未读通知数 */
    long countByUserIdAndIsRead(Long userId, Boolean isRead);
}
