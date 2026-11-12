package com.automedicine.repository;

import com.automedicine.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 商家数据访问层
 */
@Repository
public interface MerchantRepository extends JpaRepository<Merchant, Long> {

    /** 根据关联的用户ID查找商家 */
    Optional<Merchant> findByUserId(Long userId);

    /**
     * 查询附近商家 — 使用 Haversine 公式计算球面距离
     * @param latitude  用户纬度
     * @param longitude 用户经度
     * @param radiusKm  搜索半径（公里）
     * @return 半径范围内的商家列表（按距离升序）
     */
    @Query(value = """
            SELECT m.*, 
                   (6371 * ACOS(
                       COS(RADIANS(:latitude)) * COS(RADIANS(m.latitude)) 
                       * COS(RADIANS(m.longitude) - RADIANS(:longitude)) 
                       + SIN(RADIANS(:latitude)) * SIN(RADIANS(m.latitude))
                   )) AS distance
            FROM merchants m
            INNER JOIN users u ON m.id = u.id
            WHERE u.status = 1
              AND m.status = 'APPROVED'
              AND m.latitude IS NOT NULL
              AND m.longitude IS NOT NULL
            HAVING distance <= :radiusKm
            ORDER BY distance ASC
            """, nativeQuery = true)
    List<Merchant> findNearbyMerchants(
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude,
            @Param("radiusKm") int radiusKm);
}
