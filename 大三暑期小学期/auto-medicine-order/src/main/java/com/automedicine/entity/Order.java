package com.automedicine.entity;

import com.automedicine.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    @Column(name = "order_no", unique = true, length = 32)
    private String orderNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id")
    private User payer;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "delivery_address", length = 255)
    private String deliveryAddress;

    @Column(columnDefinition = "TEXT")
    private String remark;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
}
