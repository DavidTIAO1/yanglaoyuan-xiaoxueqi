package com.automedicine.ai;

import com.automedicine.ai.dto.ChatResponse;
import com.automedicine.ai.dto.MedicineIntent;
import com.automedicine.entity.Medicine;
import com.automedicine.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 意图解析与药品匹配服务
 * <p>
 * 将 AI 返回的 MedicineIntent 列表与药品库进行匹配，
 * 返回包含实际药品信息（价格、库存）的 MatchedMedicine 列表。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntentRecognitionService {

    private final MedicineRepository medicineRepository;

    /**
     * 匹配 AI 识别的药品意图与药品库
     *
     * @param intents AI 识别出的药品意图列表
     * @return 匹配到的药品详情列表
     */
    public List<ChatResponse.MatchedMedicine> matchMedicines(List<MedicineIntent> intents) {
        List<ChatResponse.MatchedMedicine> matched = new ArrayList<>();

        for (MedicineIntent intent : intents) {
            List<Medicine> candidates = medicineRepository
                    .findByNameContainingAndStatus(intent.getName(), 1);

            if (!candidates.isEmpty()) {
                // 优先选库存充足的，取第一个
                Medicine best = candidates.stream()
                        .filter(m -> m.getStock() != null && m.getStock() > 0)
                        .findFirst()
                        .orElse(candidates.get(0));

                matched.add(ChatResponse.MatchedMedicine.builder()
                        .medicineId(best.getId())
                        .name(best.getName())
                        .price(best.getPrice())
                        .quantity(intent.getQuantity())
                        .inStock(best.getStock() != null && best.getStock() >= intent.getQuantity())
                        .build());
            } else {
                // 精确名称未匹配，尝试用更短的关键词搜索
                String keyword = extractKeyword(intent.getName());
                if (!keyword.equals(intent.getName())) {
                    candidates = medicineRepository
                            .findByNameContainingAndStatus(keyword, 1);
                    if (!candidates.isEmpty()) {
                        Medicine best = candidates.stream()
                                .filter(m -> m.getStock() != null && m.getStock() > 0)
                                .findFirst()
                                .orElse(candidates.get(0));

                        matched.add(ChatResponse.MatchedMedicine.builder()
                                .medicineId(best.getId())
                                .name(best.getName())
                                .price(best.getPrice())
                                .quantity(intent.getQuantity())
                                .inStock(best.getStock() != null && best.getStock() >= intent.getQuantity())
                                .build());
                        continue;
                    }
                }

                log.info("未匹配到药品: {}", intent.getName());
            }
        }

        return matched;
    }

    /**
     * 从药品名称中提取核心关键词用于模糊匹配
     * <p>
     * 例如："布洛芬缓释胶囊" → "布洛芬"；"阿莫西林胶囊" → "阿莫西林"
     * 去掉常见的剂型后缀。
     * </p>
     */
    private String extractKeyword(String name) {
        String[] suffixes = {"胶囊", "片", "颗粒", "口服液", "丸", "散", "膏", "注射液", "滴眼液", "糖浆"};
        for (String suffix : suffixes) {
            int idx = name.indexOf(suffix);
            if (idx > 0) {
                return name.substring(0, idx);
            }
        }
        return name;
    }
}
