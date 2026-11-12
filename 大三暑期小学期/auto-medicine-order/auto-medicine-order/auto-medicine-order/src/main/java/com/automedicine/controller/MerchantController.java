package com.automedicine.controller;

import com.automedicine.dto.response.ApiResponse;
import com.automedicine.dto.response.MedicineResponse;
import com.automedicine.dto.response.MerchantResponse;
import com.automedicine.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 商家接口
 * <p>
 * - GET /api/merchants/profile — 获取商家资料（需商家角色）
 * - PUT /api/merchants/profile — 更新商家资料（首次更新自动创建记录）
 * - GET /api/merchants/nearby — 按坐标查附近商家
 * - GET /api/merchants/{id}/medicines — 查看某商家的药品列表
 * </p>
 */
@RestController
@RequestMapping("/api/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    /**
     * 获取商家资料
     */
    @GetMapping("/profile")
    public ApiResponse<MerchantResponse> getProfile(Authentication authentication) {
        return ApiResponse.success(merchantService.getProfile(authentication.getName()));
    }

    /**
     * 更新商家资料
     */
    @PutMapping("/profile")
    public ApiResponse<MerchantResponse> updateProfile(Authentication authentication,
                                                        @RequestBody Map<String, Object> body) {
        String shopName = (String) body.get("shopName");
        String licenseNo = (String) body.get("licenseNo");
        String address = (String) body.get("address");
        BigDecimal latitude = body.get("latitude") != null
                ? new BigDecimal(body.get("latitude").toString()) : null;
        BigDecimal longitude = body.get("longitude") != null
                ? new BigDecimal(body.get("longitude").toString()) : null;
        String phone = (String) body.get("phone");
        Integer deliveryRadius = body.get("deliveryRadius") != null
                ? Integer.valueOf(body.get("deliveryRadius").toString()) : null;

        MerchantResponse updated = merchantService.updateProfile(
                authentication.getName(), shopName, licenseNo, address,
                latitude, longitude, phone, deliveryRadius);
        return ApiResponse.success("更新成功", updated);
    }

    /**
     * 查询附近商家
     */
    @GetMapping("/nearby")
    public ApiResponse<List<MerchantResponse>> findNearby(
            @RequestParam BigDecimal lat,
            @RequestParam BigDecimal lng,
            @RequestParam(defaultValue = "5") int radius) {
        return ApiResponse.success(merchantService.findNearby(lat, lng, radius));
    }

    /**
     * 获取指定商家的药品列表
     */
    @GetMapping("/{id}/medicines")
    public ApiResponse<List<MedicineResponse>> getMedicines(@PathVariable Long id) {
        return ApiResponse.success(merchantService.getMedicines(id));
    }
}
