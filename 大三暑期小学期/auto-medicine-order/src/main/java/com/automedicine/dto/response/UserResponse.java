package com.automedicine.dto.response;

import com.automedicine.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 用户信息响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String familyId;
    private String username;
    private String realName;
    private String phone;
    private UserRole role;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
