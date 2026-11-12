package com.automedicine.repository;

import com.automedicine.entity.OrderBidRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 抢单记录数据访问层
 */
@Repository
public interface OrderBidRecordRepository extends JpaRepository<OrderBidRecord, Long> {

    /**
     * 检查某商家对某订单是否有指定操作记录
     * 用于抢单校验：若商家已有 ABANDONED 记录则不允许再次接单
     */
    boolean existsByOrderIdAndMerchantIdAndAction(Long orderId, Long merchantId, OrderBidRecord.BidAction action);

    /** 查询某订单的所有操作记录 */
    List<OrderBidRecord> findByOrderId(Long orderId);
}
