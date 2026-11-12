package com.automedicine.service;

import com.automedicine.dto.request.OrderRequest;
import com.automedicine.dto.response.OrderResponse;
import com.automedicine.entity.*;
import com.automedicine.enums.OrderStatus;
import com.automedicine.enums.NotificationType;
import com.automedicine.exception.BusinessException;
import com.automedicine.repository.*;
import com.automedicine.websocket.OrderWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 订单服务
 * <p>
 * 覆盖订单全生命周期：创建、查询、状态流转、支付、商家接单/放弃。
 * WebSocket 广播新订单给商家、状态变更推送给用户。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderBidRecordRepository bidRecordRepository;
    private final UserRepository userRepository;
    private final MerchantRepository merchantRepository;
    private final MedicineRepository medicineRepository;
    private final FamilyRelationRepository familyRelationRepository;
    private final NotificationService notificationService;
    private final OrderWebSocketHandler orderWebSocketHandler;

    /**
     * 创建订单
     * <p>
     * 生成唯一 orderNo → 校验药品有效性 → 保存 Order + OrderItem → 计算总金额。
     * 创建完成后 WebSocket 广播给所有连接的商家。
     * </p>
     */
    @Transactional
    public OrderResponse createOrder(String username, OrderRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", 404));

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUser(user);
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setRemark(request.getRemark());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(BigDecimal.ZERO);

        orderRepository.save(order);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();

        for (OrderRequest.OrderItemRequest itemReq : request.getItems()) {
            Medicine medicine = medicineRepository.findById(itemReq.getMedicineId())
                    .orElseThrow(() -> new BusinessException("药品不存在: " + itemReq.getMedicineId(), 404));

            if (medicine.getStatus() != 1) {
                throw new BusinessException("药品已下架: " + medicine.getName(), 400);
            }
            if (medicine.getStock() < itemReq.getQuantity()) {
                throw new BusinessException("库存不足: " + medicine.getName(), 400);
            }

            BigDecimal subtotal = medicine.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setMedicine(medicine);
            item.setMedicineName(medicine.getName());
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(medicine.getPrice());
            item.setSubtotal(subtotal);

            items.add(item);
            totalAmount = totalAmount.add(subtotal);
        }

        orderItemRepository.saveAll(items);

        order.setTotalAmount(totalAmount);
        orderRepository.save(order);

        // WebSocket 广播新订单给所有商家
        List<Map<String, Object>> itemsSummary = items.stream()
                .map(i -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", i.getMedicineName());
                    m.put("quantity", i.getQuantity());
                    return m;
                }).toList();
        orderWebSocketHandler.broadcastNewOrder(
                order.getOrderNo(), order.getId(), itemsSummary,
                totalAmount, order.getDeliveryAddress());

        // 通知所有亲属（ACCEPTED 关系的家庭成员）有新订单待代付
        List<FamilyRelation> familyRelations = familyRelationRepository
                .findAcceptedRelationsByUserId(user.getId());
        String userDisplayName = user.getRealName() != null ? user.getRealName() : user.getUsername();
        for (FamilyRelation relation : familyRelations) {
            // 获取亲属的用户 ID（关系是双向的，取另一端）
            Long familyMemberId = relation.getRequester().getId().equals(user.getId())
                    ? relation.getTarget().getId()
                    : relation.getRequester().getId();
            notificationService.createNotification(
                    familyMemberId,
                    "亲属订单待支付",
                    userDisplayName + " 提交了新订单，金额 ¥" + totalAmount + "，请帮忙代付",
                    NotificationType.PAYMENT,
                    order.getId());
        }

        return toOrderResponse(order, items);
    }

    /**
     * 查询当前用户的订单列表
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", 404));

        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(o -> toOrderResponse(o, orderItemRepository.findByOrderId(o.getId())))
                .toList();
    }

    /**
     * 查询亲属待代付订单
     * <p>
     * 查找当前用户的所有 ACCEPTED 亲属关系，获取他们的 PENDING 状态订单。
     * </p>
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getProxyPayableOrders(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", 404));

        List<FamilyRelation> relations = familyRelationRepository
                .findAcceptedRelationsByUserId(user.getId());

        if (relations.isEmpty()) {
            return List.of();
        }

        // 收集所有亲属的用户 ID
        List<Long> familyMemberIds = relations.stream()
                .map(r -> r.getRequester().getId().equals(user.getId())
                        ? r.getTarget().getId()
                        : r.getRequester().getId())
                .toList();

        // 查询亲属的 PENDING 订单
        return orderRepository.findByUserIdInAndStatus(familyMemberIds, OrderStatus.PENDING).stream()
                .map(o -> toOrderResponse(o, orderItemRepository.findByOrderId(o.getId())))
                .toList();
    }

    /**
     * 查询商家的订单列表（按状态过滤）
     * <p>
     * 包含两部分数据：
     * 1. 已分配给该商家的订单（merchant_id = 当前商家）
     * 2. 可抢单的已支付订单（merchant_id = null, status = PAID）
     * </p>
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getMerchantOrders(String username, List<OrderStatus> statuses) {
        Merchant merchant = findMerchantByUsername(username);

        // 已分配给该商家的订单
        List<Order> assignedOrders = orderRepository.findByMerchantIdAndStatusIn(merchant.getId(), statuses);

        // 可抢单的已支付订单（无商家归属，仅 PAID）
        List<Order> availableOrders = orderRepository.findByMerchantIdIsNullAndStatusIn(
                List.of(OrderStatus.PAID));

        // 合并去重 + 按创建时间倒序
        List<Order> allOrders = new ArrayList<>(assignedOrders);
        Set<Long> assignedIds = assignedOrders.stream().map(Order::getId).collect(java.util.stream.Collectors.toSet());
        for (Order o : availableOrders) {
            if (!assignedIds.contains(o.getId())) {
                allOrders.add(o);
            }
        }
        allOrders.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        return allOrders.stream()
                .map(o -> toOrderResponse(o, orderItemRepository.findByOrderId(o.getId())))
                .toList();
    }

    /**
     * 查询订单详情
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderDetail(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("订单不存在", 404));
        return toOrderResponse(order, orderItemRepository.findByOrderId(order.getId()));
    }

    /**
     * 通用状态更新（含状态机校验）
     */
    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("订单不存在", 404));

        validateTransition(order.getStatus(), newStatus);
        order.setStatus(newStatus);

        if (newStatus == OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
        }

        orderRepository.save(order);

        // WebSocket 推送状态变更给用户
        orderWebSocketHandler.sendOrderStatusToUser(
                order.getUser().getUsername(), order.getId(), order.getOrderNo(),
                newStatus.name(), "订单状态已更新为：" + getStatusLabel(newStatus));

        return toOrderResponse(order, orderItemRepository.findByOrderId(order.getId()));
    }

    /**
     * 支付（支持普通支付和亲友代付）
     * <p>
     * PENDING 状态可直接支付，WAITING_PAYMENT 状态也可支付（亲友代付场景）。
     * 支付后订单变为 PAID，等待商家接单。
     * </p>
     */
    @Transactional
    public OrderResponse pay(Long orderId, String payerUsername) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("订单不存在", 404));

        if (order.getStatus() != OrderStatus.PENDING
                && order.getStatus() != OrderStatus.WAITING_PAYMENT) {
            throw new BusinessException("订单当前状态不允许支付", 400);
        }

        if (payerUsername != null) {
            User payer = userRepository.findByUsername(payerUsername)
                    .orElseThrow(() -> new BusinessException("支付人不存在", 404));
            order.setPayer(payer);

            // 通知订单所有者：亲属已代付
            if (!payer.getId().equals(order.getUser().getId())) {
                String payerName = payer.getRealName() != null ? payer.getRealName() : payer.getUsername();
                notificationService.createNotification(
                        order.getUser().getId(),
                        "订单已代付",
                        payerName + " 已为您代付订单 " + order.getOrderNo() + "，金额 ¥" + order.getTotalAmount(),
                        NotificationType.PAYMENT,
                        order.getId());
            }
        }

        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);

        // WebSocket 通知用户支付成功
        orderWebSocketHandler.sendOrderStatusToUser(
                order.getUser().getUsername(), order.getId(), order.getOrderNo(),
                OrderStatus.PAID.name(), "支付成功，等待商家接单");

        // WebSocket 广播给所有商家：已支付订单可抢单
        List<OrderItem> paidItems = orderItemRepository.findByOrderId(order.getId());
        List<Map<String, Object>> itemsSummary = paidItems.stream()
                .map(i -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", i.getMedicineName());
                    m.put("quantity", i.getQuantity());
                    return m;
                }).toList();
        orderWebSocketHandler.broadcastNewOrder(
                order.getOrderNo(), order.getId(), itemsSummary,
                order.getTotalAmount(), order.getDeliveryAddress());

        return toOrderResponse(order, orderItemRepository.findByOrderId(order.getId()));
    }

    /**
     * 商家接单（抢单）
     * <p>
     * 仅接受 PAID 状态的订单，确保商家接单时订单已付款。
     * </p>
     */
    @Transactional
    public OrderResponse acceptOrder(Long orderId, String merchantUsername) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("订单不存在", 404));
        Merchant merchant = findMerchantByUsername(merchantUsername);

        if (order.getStatus() != OrderStatus.PAID) {
            throw new BusinessException("订单未支付，暂不可接单", 400);
        }

        // 校验是否已放弃过
        if (bidRecordRepository.existsByOrderIdAndMerchantIdAndAction(
                orderId, merchant.getId(), OrderBidRecord.BidAction.ABANDONED)) {
            throw new BusinessException("您已放弃过此订单，无法再次接单", 400);
        }

        order.setMerchant(merchant);
        order.setStatus(OrderStatus.ACCEPTED);
        order.setAcceptedAt(LocalDateTime.now());
        orderRepository.save(order);

        OrderBidRecord record = new OrderBidRecord();
        record.setOrder(order);
        record.setMerchant(merchant);
        record.setAction(OrderBidRecord.BidAction.ACCEPTED);
        bidRecordRepository.save(record);

        // WebSocket 通知用户商家已接单
        orderWebSocketHandler.sendOrderStatusToUser(
                order.getUser().getUsername(), order.getId(), order.getOrderNo(),
                OrderStatus.ACCEPTED.name(), "商家已接单，正在准备配送");

        return toOrderResponse(order, orderItemRepository.findByOrderId(order.getId()));
    }

    /**
     * 商家放弃订单
     * <p>
     * 状态回退为 PAID（保留支付状态）→ 写入 ABANDONED 记录 → 重新广播给商家。
     * </p>
     */
    @Transactional
    public OrderResponse abandonOrder(Long orderId, String merchantUsername) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("订单不存在", 404));
        Merchant merchant = findMerchantByUsername(merchantUsername);

        if (order.getMerchant() == null || !order.getMerchant().getId().equals(merchant.getId())) {
            throw new BusinessException("您不是该订单的接单商家", 400);
        }
        if (order.getStatus() != OrderStatus.ACCEPTED) {
            throw new BusinessException("仅已接单的订单可以放弃", 400);
        }

        order.setMerchant(null);
        order.setStatus(OrderStatus.PAID);
        order.setAcceptedAt(null);
        orderRepository.save(order);

        OrderBidRecord record = new OrderBidRecord();
        record.setOrder(order);
        record.setMerchant(merchant);
        record.setAction(OrderBidRecord.BidAction.ABANDONED);
        bidRecordRepository.save(record);

        // WebSocket 通知用户商家已放弃
        orderWebSocketHandler.sendOrderStatusToUser(
                order.getUser().getUsername(), order.getId(), order.getOrderNo(),
                OrderStatus.PAID.name(), "商家已放弃订单，正在重新寻找商家");

        // WebSocket 广播给所有商家（订单重新可接单）
        List<OrderItem> abandonedItems = orderItemRepository.findByOrderId(order.getId());
        List<Map<String, Object>> itemsSummary = abandonedItems.stream()
                .map(i -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", i.getMedicineName());
                    m.put("quantity", i.getQuantity());
                    return m;
                }).toList();
        orderWebSocketHandler.broadcastNewOrder(
                order.getOrderNo(), order.getId(), itemsSummary,
                order.getTotalAmount(), order.getDeliveryAddress());

        return toOrderResponse(order, abandonedItems);
    }

    // ========== 状态机校验 ==========

    private static final Set<OrderStatus> FROM_PENDING = Set.of(
            OrderStatus.WAITING_PAYMENT, OrderStatus.PAID);
    private static final Set<OrderStatus> FROM_WAITING = Set.of(OrderStatus.PAID);
    private static final Set<OrderStatus> FROM_PAID = Set.of(OrderStatus.ACCEPTED);
    private static final Set<OrderStatus> FROM_ACCEPTED = Set.of(
            OrderStatus.DELIVERING, OrderStatus.PAID);
    private static final Set<OrderStatus> FROM_DELIVERING = Set.of(OrderStatus.DELIVERED);
    private static final Set<OrderStatus> FROM_DELIVERED = Set.of(OrderStatus.COMPLETED);

    private void validateTransition(OrderStatus from, OrderStatus to) {
        Set<OrderStatus> allowed = switch (from) {
            case PENDING -> FROM_PENDING;
            case WAITING_PAYMENT -> FROM_WAITING;
            case PAID -> FROM_PAID;
            case ACCEPTED -> FROM_ACCEPTED;
            case DELIVERING -> FROM_DELIVERING;
            case DELIVERED -> FROM_DELIVERED;
            case COMPLETED -> Set.of();
        };
        if (!allowed.contains(to)) {
            throw new BusinessException(
                    "不允许从 " + from + " 转换到 " + to, 400);
        }
    }

    // ========== 工具方法 ==========

    private String getStatusLabel(OrderStatus status) {
        return switch (status) {
            case PENDING -> "待接单";
            case WAITING_PAYMENT -> "待支付";
            case PAID -> "已支付";
            case ACCEPTED -> "已接单";
            case DELIVERING -> "配送中";
            case DELIVERED -> "已送达";
            case COMPLETED -> "已完成";
        };
    }

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(100000, 999999);
        return timestamp + random;
    }

    private Merchant findMerchantByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", 404));
        return merchantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException("商家资料不存在", 404));
    }

    private OrderResponse toOrderResponse(Order order, List<OrderItem> items) {
        List<OrderResponse.OrderItemResponse> itemResponses = items.stream()
                .map(i -> OrderResponse.OrderItemResponse.builder()
                        .medicineId(i.getMedicine().getId())
                        .medicineName(i.getMedicineName())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .subtotal(i.getSubtotal())
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUser().getId())
                .userRealName(order.getUser().getRealName())
                .merchantId(order.getMerchant() != null ? order.getMerchant().getId() : null)
                .payerId(order.getPayer() != null ? order.getPayer().getId() : null)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .deliveryAddress(order.getDeliveryAddress())
                .remark(order.getRemark())
                .paidAt(order.getPaidAt())
                .acceptedAt(order.getAcceptedAt())
                .deliveredAt(order.getDeliveredAt())
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .build();
    }
}
