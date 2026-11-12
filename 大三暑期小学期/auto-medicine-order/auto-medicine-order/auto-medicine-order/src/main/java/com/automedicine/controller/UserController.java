package com.automedicine.controller;

import com.automedicine.dto.response.ApiResponse;
import com.automedicine.dto.response.UserResponse;
import com.automedicine.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 用户接口
 * <p>
 * 提供获取和更新个人资料两个端点，均需登录访问。
 * 通过 SecurityContext 中的 Authentication 获取当前用户名，
 * 确保用户只能查看和修改自己的资料。
 * </p>
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取个人资料
     */
    @GetMapping("/profile")
    public ApiResponse<UserResponse> getProfile(Authentication authentication) {
        return ApiResponse.success(userService.getProfile(authentication.getName()));
    }

    /**
     * 更新个人资料
     * <p>
     * 接受 JSON 请求体，仅更新传入的非 null 字段。
     * 可更新字段：realName、phone、address、latitude、longitude。
     * </p>
     */
    @PutMapping("/profile")
    public ApiResponse<UserResponse> updateProfile(Authentication authentication,
                                                    @RequestBody Map<String, Object> body) {
        String realName = (String) body.get("realName");
        String phone = (String) body.get("phone");
        String address = (String) body.get("address");
        BigDecimal latitude = body.get("latitude") != null
                ? new BigDecimal(body.get("latitude").toString()) : null;
        BigDecimal longitude = body.get("longitude") != null
                ? new BigDecimal(body.get("longitude").toString()) : null;

        UserResponse updated = userService.updateProfile(
                authentication.getName(), realName, phone, address, latitude, longitude);
        return ApiResponse.success("更新成功", updated);
    }
}
