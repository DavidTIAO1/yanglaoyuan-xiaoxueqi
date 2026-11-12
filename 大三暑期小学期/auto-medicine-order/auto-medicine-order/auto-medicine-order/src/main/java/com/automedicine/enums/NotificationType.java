package com.automedicine.enums;

/**
 * 通知类型枚举 — 定义系统中各类通知的分类
 */
public enum NotificationType {
    /** 新订单通知 — 有新订单推送给附近商家 */
    NEW_ORDER,
    /** 订单状态变更通知 — 订单状态发生变化时通知用户 */
    ORDER_STATUS,
    /** 支付通知 — 支付相关通知（如亲友代付请求） */
    PAYMENT,
    /** 亲友通知 — 亲友绑定请求等家庭关系相关通知 */
    FAMILY
}
