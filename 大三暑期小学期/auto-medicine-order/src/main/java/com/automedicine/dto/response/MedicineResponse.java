package com.automedicine.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 药品信息响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicineResponse {

    private Long id;
    private Long merchantId;
    private String name;
    private String genericName;
    private String category;
    private BigDecimal price;
    private Integer stock;
    private String description;
    private Boolean requiresPrescription;
    private Integer status;
}
