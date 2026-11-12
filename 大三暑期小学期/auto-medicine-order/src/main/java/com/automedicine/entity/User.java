package com.automedicine.entity;

import com.automedicine.enums.UserRole;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.security.SecureRandom;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(name = "family_id", unique = true, length = 10)
    private String familyId;

    @Column(unique = true, length = 50)
    private String username;

    @Column(length = 255, nullable = false)
    private String password;

    @Column(name = "real_name", length = 50)
    private String realName;

    @Column(unique = true, length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(length = 255)
    private String address;

    @Column(precision = 10, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 6)
    private BigDecimal longitude;

    @Column
    private Integer status = 1;

    /**
     * 注册时自动生成 10 位随机数字作为亲友标识
     */
    @PrePersist
    public void generateFamilyId() {
        if (this.familyId == null) {
            SecureRandom random = new SecureRandom();
            StringBuilder sb = new StringBuilder(10);
            for (int i = 0; i < 10; i++) {
                sb.append(random.nextInt(10));
            }
            this.familyId = sb.toString();
        }
    }
}
