package com.automedicine.controller;

import com.automedicine.dto.request.MedicineRequest;
import com.automedicine.dto.response.ApiResponse;
import com.automedicine.dto.response.MedicineResponse;
import com.automedicine.service.MedicineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 药品管理接口（仅限 MERCHANT 角色）
 * <p>
 * - POST /api/medicines — 新增药品
 * - GET /api/medicines — 查询当前商家的药品列表
 * - PUT /api/medicines/{id} — 修改药品
 * - DELETE /api/medicines/{id} — 下架药品
 * </p>
 */
@RestController
@RequestMapping("/api/medicines")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MERCHANT')")
public class MedicineController {

    private final MedicineService medicineService;

    /**
     * 新增药品
     */
    @PostMapping
    public ApiResponse<MedicineResponse> addMedicine(Authentication authentication,
                                                      @Valid @RequestBody MedicineRequest request) {
        return ApiResponse.success("药品添加成功", medicineService.addMedicine(authentication.getName(), request));
    }

    /**
     * 查询当前商家的药品列表
     */
    @GetMapping
    public ApiResponse<List<MedicineResponse>> getMyMedicines(Authentication authentication) {
        return ApiResponse.success(medicineService.getMyMedicines(authentication.getName()));
    }

    /**
     * 修改药品信息
     */
    @PutMapping("/{id}")
    public ApiResponse<MedicineResponse> updateMedicine(Authentication authentication,
                                                         @PathVariable Long id,
                                                         @Valid @RequestBody MedicineRequest request) {
        return ApiResponse.success("药品更新成功", medicineService.updateMedicine(authentication.getName(), id, request));
    }

    /**
     * 下架药品
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteMedicine(Authentication authentication, @PathVariable Long id) {
        medicineService.deleteMedicine(authentication.getName(), id);
        return ApiResponse.success("药品已下架", null);
    }
}
