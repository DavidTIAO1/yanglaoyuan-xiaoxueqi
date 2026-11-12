package com.automedicine.repository;

import com.automedicine.entity.Order;
import com.automedicine.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 订单数据访问层
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /** 根据订单编号查找 */
    Optional<Order> findByOrderNo(String orderNo);

    /** 查询某用户的所有订单（按创建时间倒序） */
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** 查询某商家在指定状态下的订单列表 */
    List<Order> findByMerchantIdAndStatusIn(Long merchantId, List<OrderStatus> statuses);

    /** 查询某用户作为支付人的待支付订单（亲友代付场景） */
    List<Order> findByPayerIdAndStatus(Long payerId, OrderStatus status);

    /** 查询所有待接单订单 */
    List<Order> findByStatus(OrderStatus status);

    /** 查询无商家归属且状态匹配的订单（用于商家发现可抢单的已支付订单） */
    List<Order> findByMerchantIdIsNullAndStatusIn(List<OrderStatus> statuses);

    /** 查询多个用户在指定状态下的订单（亲属代付场景：查询亲属的待支付订单） */
    List<Order> findByUserIdInAndStatus(List<Long> userIds, OrderStatus status);
}
