package com.automedicine.service;

import com.automedicine.dto.response.UserResponse;
import com.automedicine.entity.User;
import com.automedicine.exception.BusinessException;
import com.automedicine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 用户服务
 * <p>
 * 提供获取个人资料和更新个人资料两个核心操作。
 * 所有操作均基于 SecurityContext 中的当前用户名，确保只能操作自己的数据。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * 获取当前登录用户的个人资料
     */
    public UserResponse getProfile(String username) {
        User user = findUserByUsername(username);
        return toUserResponse(user);
    }

    /**
     * 更新当前登录用户的个人资料
     * <p>
     * 仅允许更新 realName、phone、address、latitude、longitude，
     * 不允许通过此接口修改用户名、密码、角色等敏感字段。
     * </p>
     */
    @Transactional
    public UserResponse updateProfile(String username, String realName, String phone,
                                       String address, BigDecimal latitude, BigDecimal longitude) {
        User user = findUserByUsername(username);

        if (realName != null) {
            user.setRealName(realName);
        }
        if (phone != null) {
            // 校验手机号是否被其他用户占用
            userRepository.findByPhone(phone).ifPresent(existing -> {
                if (!existing.getId().equals(user.getId())) {
                    throw new BusinessException("该手机号已被其他用户注册", 400);
                }
            });
            user.setPhone(phone);
        }
        if (address != null) {
            user.setAddress(address);
        }
        if (latitude != null) {
            user.setLatitude(latitude);
        }
        if (longitude != null) {
            user.setLongitude(longitude);
        }

        userRepository.save(user);
        return toUserResponse(user);
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", 404));
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .familyId(user.getFamilyId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .role(user.getRole())
                .address(user.getAddress())
                .latitude(user.getLatitude())
                .longitude(user.getLongitude())
                .build();
    }
}
