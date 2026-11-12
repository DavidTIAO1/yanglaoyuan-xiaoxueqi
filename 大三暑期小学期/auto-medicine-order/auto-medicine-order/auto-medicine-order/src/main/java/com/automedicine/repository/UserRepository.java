package com.automedicine.repository;

import com.automedicine.entity.User;
import com.automedicine.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问层
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** 根据用户名查找用户 */
    Optional<User> findByUsername(String username);

    /** 根据手机号查找用户 */
    Optional<User> findByPhone(String phone);

    /** 根据亲友标识查找用户 */
    Optional<User> findByFamilyId(String familyId);

    /** 根据角色查找用户 */
    List<User> findByRole(UserRole role);

    /** 检查用户名是否已存在 */
    boolean existsByUsername(String username);

    /** 检查手机号是否已存在 */
    boolean existsByPhone(String phone);
}
