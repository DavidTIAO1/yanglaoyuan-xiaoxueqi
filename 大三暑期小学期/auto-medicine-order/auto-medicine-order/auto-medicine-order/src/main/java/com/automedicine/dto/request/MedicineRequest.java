package com.automedicine.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 药品新增/修改请求 DTO
 */
@Data
public class MedicineRequest {

    @NotBlank(message = "药品名称不能为空")
    @Size(max = 100, message = "药品名称最多100个字符")
    private String name;

    @Size(max = 100, message = "通用名最多100个字符")
    private String genericName;

    @Size(max = 50, message = "分类最多50个字符")
    private String category;

    @NotNull(message = "价格不能为空")
    @Min(value = 0, message = "价格不能为负数")
    private BigDecimal price;

    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;

    private String description;

    private Boolean requiresPrescription;
}
