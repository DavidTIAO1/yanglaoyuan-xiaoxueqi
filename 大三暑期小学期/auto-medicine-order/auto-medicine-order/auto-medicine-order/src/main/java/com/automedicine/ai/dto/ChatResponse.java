package com.automedicine.ai.dto;

import com.automedicine.ai.dto.MedicineIntent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI 对话响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /** AI 自然语言回复 */
    private String reply;

    /** 在药品库中匹配到的药品详情 */
    private List<MatchedMedicine> matchedMedicines;

    /** AI 是否需要追问用户 */
    private Boolean needsClarification;

    /** AI 原始识别意图（未匹配库存） */
    private List<MedicineIntent> intents;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchedMedicine {

        private Long medicineId;

        private String name;

        private BigDecimal price;

        private Integer quantity;

        private Boolean inStock;
    }
}
