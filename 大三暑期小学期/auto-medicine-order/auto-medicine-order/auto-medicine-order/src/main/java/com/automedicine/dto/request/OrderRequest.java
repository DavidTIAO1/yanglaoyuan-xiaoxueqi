package com.automedicine.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 创建订单请求 DTO
 */
@Data
public class OrderRequest {

    @NotEmpty(message = "订单项不能为空")
    @Valid
    private List<OrderItemRequest> items;

    @NotBlank(message = "配送地址不能为空")
    private String deliveryAddress;

    private String remark;

    /**
     * 订单明细请求
     */
    @Data
    public static class OrderItemRequest {

        @NotNull(message = "药品ID不能为空")
        private Long medicineId;

        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量至少为1")
        private Integer quantity;
    }
}
