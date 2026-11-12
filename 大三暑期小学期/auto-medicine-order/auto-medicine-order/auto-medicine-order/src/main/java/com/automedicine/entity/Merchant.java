package com.automedicine.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商家实体 — 与 User 共享主键（@MapsId）
 */
@Data
@Entity
@Table(name = "merchants")
@EntityListeners(AuditingEntityListener.class)
public class Merchant {

    @Id
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id")
    private User user;

    @Column(name = "shop_name", length = 100)
    private String shopName;

    @Column(name = "license_no", length = 100)
    private String licenseNo;

    @Column(length = 255)
    private String address;

    @Column(precision = 10, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 6)
    private BigDecimal longitude;

    @Column(length = 20)
    private String phone;

    /**
     * 商家审核状态
     */
    public enum MerchantStatus {
        /** 待审核 — 商家刚注册，尚未通过平台审核 */
        PENDING,
        /** 已通过 — 审核通过，可正常营业和接单 */
        APPROVED,
        /** 已拒绝 — 审核未通过 */
        REJECTED
    }

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MerchantStatus status = MerchantStatus.PENDING;

    @Column(name = "delivery_radius")
    private Integer deliveryRadius = 3;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
