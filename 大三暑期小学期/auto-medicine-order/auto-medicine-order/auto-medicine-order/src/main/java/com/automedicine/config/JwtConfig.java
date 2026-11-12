package com.automedicine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 参数配置
 * <p>
 * 从 application.yml 中读取 app.jwt 前缀的配置项。
 * </p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.jwt")
public class JwtConfig {

    /** JWT 签名密钥 */
    private String secret;

    /** JWT 过期时间（毫秒） */
    private Long expirationMs;
}
