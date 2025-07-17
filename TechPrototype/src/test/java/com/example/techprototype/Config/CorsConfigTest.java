package com.example.techprototype.Config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CorsConfigTest {

    @Test
    @DisplayName("测试CORS过滤器配置")
    void testCorsFilter() throws Exception {
        // 创建配置对象
        CorsConfig corsConfig = new CorsConfig();
        
        // 获取过滤器
        CorsFilter filter = corsConfig.corsFilter();
        assertNotNull(filter, "CORS过滤器不应为空");
        
        // 获取配置源
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) 
            getPrivateField(filter, "configSource");
        assertNotNull(source, "配置源不应为空");
        
        // 获取CORS配置
        CorsConfiguration config = source.getCorsConfigurations().get("/**");
        assertNotNull(config, "CORS配置不应为空");
        
        // 验证允许的来源
        List<String> expectedOrigins = Arrays.asList(
            "http://localhost:3000", 
            "http://127.0.0.1:3000", 
            "http://localhost:3001", 
            "http://127.0.0.1:3001"
        );
        assertTrue(config.getAllowedOrigins().containsAll(expectedOrigins), 
            "应允许所有预期的来源");
        
        // 验证允许的头信息
        assertEquals("*", config.getAllowedHeaders().get(0), "应允许所有头信息");
        
        // 验证允许的方法
        assertEquals("*", config.getAllowedMethods().get(0), "应允许所有HTTP方法");
        
        // 验证允许的凭证
        assertTrue(config.getAllowCredentials(), "应允许凭证");
        
        // 验证暴露的响应头
        List<String> expectedExposedHeaders = Arrays.asList("Authorization", "Set-Cookie");
        assertTrue(config.getExposedHeaders().containsAll(expectedExposedHeaders), 
            "应暴露预期的响应头");
        
        // 验证预检请求的有效期
        assertEquals(3600L, config.getMaxAge(), "预检请求有效期应为3600秒");
    }
    
    // 辅助方法：通过反射获取私有字段的值
    private Object getPrivateField(Object obj, String fieldName) throws Exception {
        java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }
}
