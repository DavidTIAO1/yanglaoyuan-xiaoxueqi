package com.automedicine.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置
 * <p>
 * 为 SPA 前端路由添加视图控制器，将前端路由路径转发到 index.html，
 * 由 Vue Router 接管客户端路由。
 * </p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 角色选择页
        registry.addViewController("/").setViewName("forward:/index.html");
        // 登录/注册页
        registry.addViewController("/login/{role}").setViewName("forward:/index.html");
        registry.addViewController("/register/{role}").setViewName("forward:/index.html");
        // 用户端路由
        registry.addViewController("/user/**").setViewName("forward:/index.html");
        // 商家端路由
        registry.addViewController("/merchant/**").setViewName("forward:/index.html");
        // 设置优先级高于 Spring Boot 默认的 WelcomePageHandlerMapping
        registry.setOrder(-1);
    }
}
