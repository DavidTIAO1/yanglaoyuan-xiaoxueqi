package com.automedicine.controller;

import com.automedicine.dto.request.OrderRequest;
import com.automedicine.dto.response.ApiResponse;
import com.automedicine.dto.response.OrderResponse;
import com.automedicine.enums.OrderStatus;
import com.automedicine.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单接口
 * <p>
 * - POST /api/orders — 创建订单
 * - GET /api/orders — 查询我的订单（用户查自己的，商家查分配给自己的）
 * - GET /api/orders/proxy-payable — 查询亲属待代付订单
 * - GET /api/orders/{id} — 订单详情
 * - PUT /api/orders/{id}/status — 更新订单状态（含状态机校验）
 * - POST /api/orders/{id}/pay — 支付（亲友代付入口）
 * - POST /api/orders/{id}/accept — 商家接单
 * - POST /api/orders/{id}/abandon — 商家放弃
 * </p>
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 创建订单
     */
    @PostMapping
    public ApiResponse<OrderResponse> createOrder(Authentication authentication,
                                                   @Valid @RequestBody OrderRequest request) {
        return ApiResponse.success("订单创建成功", orderService.createOrder(authentication.getName(), request));
    }

    /**
     * 查询我的订单
     * <p>
     * role=USER 查用户自己的订单；role=MERCHANT 查分配给商家的订单。
     * </p>
     */
    @GetMapping
    public ApiResponse<List<OrderResponse>> getOrders(
            Authentication authentication,
            @RequestParam(required = false) List<OrderStatus> statuses) {

        String username = authentication.getName();
        boolean isMerchant = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MERCHANT"));

        if (isMerchant) {
            List<OrderStatus> queryStatuses = (statuses != null && !statuses.isEmpty())
                    ? statuses
                    : List.of(OrderStatus.PAID, OrderStatus.ACCEPTED, OrderStatus.DELIVERING,
                              OrderStatus.DELIVERED, OrderStatus.COMPLETED);
            return ApiResponse.success(orderService.getMerchantOrders(username, queryStatuses));
        } else {
            return ApiResponse.success(orderService.getUserOrders(username));
        }
    }

    /**
     * 查询亲属待代付订单
     * <p>
     * 返回当前用户的所有 ACCEPTED 亲属的 PENDING 状态订单。
     * </p>
     */
    @GetMapping("/proxy-payable")
    public ApiResponse<List<OrderResponse>> getProxyPayableOrders(Authentication authentication) {
        return ApiResponse.success(orderService.getProxyPayableOrders(authentication.getName()));
    }

    /**
     * 订单详情
     */
    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getOrderDetail(@PathVariable Long id) {
        return ApiResponse.success(orderService.getOrderDetail(id));
    }

    /**
     * 更新订单状态
     */
    @PutMapping("/{id}/status")
    public ApiResponse<OrderResponse> updateStatus(@PathVariable Long id,
                                                    @RequestParam OrderStatus status) {
        return ApiResponse.success("状态更新成功", orderService.updateStatus(id, status));
    }

    /**
     * 支付（亲友代付入口）
     */
    @PostMapping("/{id}/pay")
    public ApiResponse<OrderResponse> pay(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.success("支付成功", orderService.pay(id, authentication.getName()));
    }

    /**
     * 商家接单
     */
    @PostMapping("/{id}/accept")
    public ApiResponse<OrderResponse> acceptOrder(@PathVariable Long id,
                                                   Authentication authentication) {
        return ApiResponse.success("接单成功", orderService.acceptOrder(id, authentication.getName()));
    }

    /**
     * 商家放弃订单
     */
    @PostMapping("/{id}/abandon")
    public ApiResponse<OrderResponse> abandonOrder(@PathVariable Long id,
                                                    Authentication authentication) {
        return ApiResponse.success("已放弃订单", orderService.abandonOrder(id, authentication.getName()));
    }
}
