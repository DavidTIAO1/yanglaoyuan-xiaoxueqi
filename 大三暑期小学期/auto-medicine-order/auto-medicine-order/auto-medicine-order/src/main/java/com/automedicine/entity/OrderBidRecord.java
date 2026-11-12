package com.automedicine.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 抢单记录实体 — 仅有 createdAt，无 updatedAt
 */
@Data
@Entity
@Table(name = "order_bid_records")
@EntityListeners(AuditingEntityListener.class)
public class OrderBidRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    /**
     * 抢单操作类型
     */
    public enum BidAction {
        /** 接单 — 商家接受了该订单 */
        ACCEPTED,
        /** 放弃 — 商家放弃了已接的订单，订单回退为 PENDING */
        ABANDONED
    }

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private BidAction action;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
