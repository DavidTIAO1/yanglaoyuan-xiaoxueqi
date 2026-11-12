package com.automedicine.service;

import com.automedicine.dto.request.RegisterRequest;
import com.automedicine.dto.response.UserResponse;
import com.automedicine.entity.Merchant;
import com.automedicine.entity.User;
import com.automedicine.enums.UserRole;
import com.automedicine.exception.BusinessException;
import com.automedicine.repository.MerchantRepository;
import com.automedicine.repository.UserRepository;
import com.automedicine.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务
 * <p>
 * 提供注册、登录、刷新令牌三个核心认证操作。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final MerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 用户注册
     * <p>
     * 校验用户名/手机号唯一 → BCrypt 加密密码 → 保存用户 → 返回含 familyId 的 UserResponse。
     * familyId 由 User 实体的 @PrePersist 自动生成 10 位随机数字。
     * </p>
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("用户名已存在", 400);
        }
        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException("手机号已被注册", 400);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setRole(request.getRole());
        user.setAddress(request.getAddress());

        userRepository.save(user);

        // 商家角色自动创建 Merchant 记录，避免后续 API 调用因缺少 Merchant 而 404
        if (user.getRole() == UserRole.MERCHANT) {
            Merchant merchant = new Merchant();
            merchant.setUser(user);
            merchant.setShopName(user.getRealName() != null ? user.getRealName() + "的店铺" : "新店铺");
            merchant.setStatus(Merchant.MerchantStatus.APPROVED);
            merchantRepository.save(merchant);
        }

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

    /**
     * 用户登录
     * <p>
     * AuthenticationManager 校验用户名密码 → JwtTokenProvider 生成 token → 返回 token 字符串。
     * 认证失败时由 GlobalExceptionHandler 捕获 BadCredentialsException 并返回 401。
     * </p>
     */
    public String login(String username, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
        return jwtTokenProvider.generateToken(authentication);
    }

    /**
     * 刷新令牌
     * <p>
     * 根据当前有效的 Authentication 重新生成一个新 token。
     * </p>
     */
    public String refreshToken(Authentication authentication) {
        return jwtTokenProvider.generateToken(authentication);
    }
}
