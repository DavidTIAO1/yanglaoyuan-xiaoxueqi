package com.automedicine.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 识别出的药品意图
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicineIntent {

    /** 药品名称 */
    private String name;

    /** 数量 */
    private Integer quantity;

    /** 置信度 0~1 */
    private Double confidence;
}
