package com.automedicine.controller;

import com.automedicine.dto.request.FamilyBindRequest;
import com.automedicine.dto.response.ApiResponse;
import com.automedicine.dto.response.FamilyMemberResponse;
import com.automedicine.dto.response.UserResponse;
import com.automedicine.service.FamilyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 亲友控制器
 * <p>
 * 搜索用户、发起绑定、接受/拒绝绑定、查询亲友列表。
 * </p>
 */
@RestController
@RequiredArgsConstructor
public class FamilyController {

    private final FamilyService familyService;

    /**
     * 通过 familyId 搜索用户
     */
    @GetMapping("/api/users/search")
    public ApiResponse<UserResponse> searchByFamilyId(@RequestParam String familyId) {
        return ApiResponse.success(familyService.searchByFamilyId(familyId));
    }

    /**
     * 发起亲友绑定请求
     */
    @PostMapping("/api/family/bind")
    public ApiResponse<Void> sendBindRequest(
            Authentication authentication,
            @Valid @RequestBody FamilyBindRequest request) {
        familyService.sendBindRequest(authentication.getName(), request);
        return ApiResponse.success();
    }

    /**
     * 接受绑定邀请
     */
    @PostMapping("/api/family/{id}/accept")
    public ApiResponse<Void> acceptBind(
            @PathVariable Long id,
            Authentication authentication) {
        familyService.acceptBind(id, authentication.getName());
        return ApiResponse.success();
    }

    /**
     * 拒绝绑定邀请
     */
    @PostMapping("/api/family/{id}/reject")
    public ApiResponse<Void> rejectBind(
            @PathVariable Long id,
            Authentication authentication) {
        familyService.rejectBind(id, authentication.getName());
        return ApiResponse.success();
    }

    /**
     * 我的亲友列表
     */
    @GetMapping("/api/family/members")
    public ApiResponse<List<FamilyMemberResponse>> getFamilyMembers(
            Authentication authentication) {
        return ApiResponse.success(familyService.getFamilyMembers(authentication.getName()));
    }
}
