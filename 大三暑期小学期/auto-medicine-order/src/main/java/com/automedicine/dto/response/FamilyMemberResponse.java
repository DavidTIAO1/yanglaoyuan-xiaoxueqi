package com.automedicine.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 亲友成员响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyMemberResponse {

    private Long userId;

    private String username;

    private String realName;

    private String phone;

    private String familyId;

    /** 与当前用户的关系描述 */
    private String relationship;

    /** 当前用户在该关系中的角色（对方是发起方还是接收方） */
    private String relationRole;
}
