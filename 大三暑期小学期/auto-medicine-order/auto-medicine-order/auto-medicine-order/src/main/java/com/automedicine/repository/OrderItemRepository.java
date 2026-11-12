package com.automedicine.repository;

import com.automedicine.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 订单明细数据访问层
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /** 根据订单ID查询明细列表 */
    List<OrderItem> findByOrderId(Long orderId);
}
