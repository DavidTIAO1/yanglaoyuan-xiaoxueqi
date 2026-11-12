package com.automedicine.enums;

/**
 * 订单状态枚举 — 定义订单完整生命周期中的各个状态
 */
public enum OrderStatus {
    /** 待接单 — 订单已创建，等待附近商家抢单 */
    PENDING,
    /** 待支付 — 订单已被接单或由亲友代付场景下等待支付 */
    WAITING_PAYMENT,
    /** 已支付 — 支付完成，等待商家开始配送 */
    PAID,
    /** 已接单 — 商家已接受订单（抢单模式下被商家锁定） */
    ACCEPTED,
    /** 配送中 — 商家已出发配送 */
    DELIVERING,
    /** 已送达 — 药品已送达用户 */
    DELIVERED,
    /** 已完成 — 订单全流程结束 */
    COMPLETED
}
