package com.automedicine.repository;

import com.automedicine.entity.FamilyRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 亲友关系数据访问层
 */
@Repository
public interface FamilyRelationRepository extends JpaRepository<FamilyRelation, Long> {

    /**
     * 查询用户所有已接受的亲友关系（双向查询：作为发起方或被邀请方）
     */
    @Query("""
            SELECT fr FROM FamilyRelation fr
            WHERE fr.status = 'ACCEPTED'
              AND (fr.requester.id = :userId OR fr.target.id = :userId)
            """)
    List<FamilyRelation> findAcceptedRelationsByUserId(@Param("userId") Long userId);

    /**
     * 查询发给某用户的待处理绑定请求
     */
    @Query("""
            SELECT fr FROM FamilyRelation fr
            WHERE fr.target.id = :userId AND fr.status = 'PENDING'
            """)
    List<FamilyRelation> findPendingRequestsByTargetId(@Param("userId") Long userId);

    /**
     * 检查两个用户之间是否已存在亲友关系（双向查询，防止重复绑定）
     */
    @Query("""
            SELECT CASE WHEN COUNT(fr) > 0 THEN true ELSE false END
            FROM FamilyRelation fr
            WHERE (fr.requester.id = :userId1 AND fr.target.id = :userId2)
               OR (fr.requester.id = :userId2 AND fr.target.id = :userId1)
            """)
    boolean existsRelationBetween(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}
