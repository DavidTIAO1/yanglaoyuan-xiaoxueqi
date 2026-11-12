package com.automedicine.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 亲友绑定请求 DTO
 */
@Data
public class FamilyBindRequest {

    /** 对方的 familyId（10 位数字） */
    @NotBlank(message = "亲友标识不能为空")
    @Size(min = 10, max = 10, message = "亲友标识必须为10位数字")
    private String targetFamilyId;

    /** 关系描述（子女/配偶/其他） */
    @NotBlank(message = "关系描述不能为空")
    @Size(max = 20, message = "关系描述不能超过20个字符")
    private String relationship;
}
