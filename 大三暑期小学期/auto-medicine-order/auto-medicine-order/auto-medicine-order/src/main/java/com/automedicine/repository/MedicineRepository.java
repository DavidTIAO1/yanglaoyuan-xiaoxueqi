package com.automedicine.repository;

import com.automedicine.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 药品数据访问层
 */
@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Long> {

    /** 查询所有上架药品（AI 构建药品目录用） */
    List<Medicine> findByStatus(Integer status);

    /** 查询某商家的所有上架药品 */
    List<Medicine> findByMerchantIdAndStatus(Long merchantId, Integer status);

    /** 查询某商家的所有上架药品（按ID倒序，最新添加的在前） */
    List<Medicine> findByMerchantIdAndStatusOrderByIdDesc(Long merchantId, Integer status);

    /** 根据药品名称模糊匹配（用于 AI 识别后匹配库存） */
    List<Medicine> findByNameContainingAndStatus(String name, Integer status);

    /** 根据药品名称和商家ID查找 */
    List<Medicine> findByNameContainingAndMerchantIdAndStatus(String name, Long merchantId, Integer status);
}
