package com.automedicine.dto.response;

import com.automedicine.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单信息响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private String orderNo;
    private Long userId;
    private String userRealName;
    private Long merchantId;
    private Long payerId;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private String deliveryAddress;
    private String remark;
    private LocalDateTime paidAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime createdAt;

    private List<OrderItemResponse> items;

    /**
     * 订单明细响应
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        private Long medicineId;
        private String medicineName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
}
