package com.example.techprototype.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // 允许cookies跨域
        config.setAllowCredentials(true);
        
        // 允许的源（更全面的配置）
        config.addAllowedOrigin("http://localhost:3000"); // 标准前端开发端口
        config.addAllowedOrigin("http://127.0.0.1:3000"); // 使用IP时的前端地址
        
        // 允许的头信息
        config.addAllowedHeader("*");
        
        // 允许的HTTP方法
        config.addAllowedMethod("*");
        
        // 暴露的响应头
        config.addExposedHeader("Authorization");
        config.addExposedHeader("Set-Cookie");
        
        // 预检请求的有效期，单位：秒
        config.setMaxAge(3600L);
        
        // 注册配置
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
}
