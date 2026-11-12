package com.automedicine.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "family_relations")
public class FamilyRelation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = false)
    private User target;

    @Column(length = 20)
    private String relationship;

    /**
     * 亲友绑定状态
     */
    public enum RelationStatus {
        /** 待确认 — 绑定请求已发送，等待对方确认 */
        PENDING,
        /** 已接受 — 对方已同意绑定 */
        ACCEPTED,
        /** 已拒绝 — 对方已拒绝绑定请求 */
        REJECTED
    }

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RelationStatus status = RelationStatus.PENDING;
}
