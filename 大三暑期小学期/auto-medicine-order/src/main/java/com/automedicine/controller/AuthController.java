package com.automedicine.controller;

import com.automedicine.dto.request.LoginRequest;
import com.automedicine.dto.request.RegisterRequest;
import com.automedicine.dto.response.ApiResponse;
import com.automedicine.dto.response.UserResponse;
import com.automedicine.exception.BusinessException;
import com.automedicine.security.JwtTokenProvider;
import com.automedicine.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口
 * <p>
 * 提供注册、登录、刷新令牌三个端点，均在 /api/auth/** 下，不受 Security 拦截。
 * </p>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("注册成功", authService.register(request));
    }

    /**
     * 用户登录，返回 JWT
     */
    @PostMapping("/login")
    public ApiResponse<String> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request.getUsername(), request.getPassword());
        return ApiResponse.success("登录成功", token);
    }

    /**
     * 刷新令牌
     * <p>
     * 需要携带当前有效的 JWT，返回一个新 token。
     * 由于 /api/auth/** 放行，JwtAuthenticationFilter 不会自动设置 SecurityContext，
     * 因此手动从请求头提取并校验 token。
     * </p>
     */
    @PostMapping("/refresh")
    public ApiResponse<String> refresh(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new BusinessException("令牌无效或已过期", 401);
        }

        String username = jwtTokenProvider.getUsernameFromToken(token);
        // 构造 Authentication 供 JwtTokenProvider 生成新 token
        Authentication authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                new org.springframework.security.core.userdetails.User(username, "", java.util.Collections.emptyList()),
                null,
                java.util.Collections.emptyList());

        String newToken = authService.refreshToken(authentication);
        return ApiResponse.success("刷新成功", newToken);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
