package com.automedicine.dto.response;

import com.automedicine.entity.Merchant;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商家信息响应 DTO
 */
@Data
@Builder
public class MerchantResponse {

    private Long id;
    private String shopName;
    private String licenseNo;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String phone;
    private Merchant.MerchantStatus status;
    private Integer deliveryRadius;
}
