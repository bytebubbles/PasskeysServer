package com.example.passkeys.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置类
 * 配置静态资源访问路径
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    /**
     * 配置静态资源处理器
     * 使 .well-known 目录可以在根路径访问
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置 .well-known 路径
        registry.addResourceHandler("/.well-known/**")
                .addResourceLocations("classpath:/static/.well-known/")
                .setCachePeriod(0);  // 开发环境不缓存
        
        // 保留默认的静态资源配置
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(0);
    }
}

