package com.example.techprototype.Config;

import com.example.techprototype.Interceptor.AuthInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebConfigTest {

    @Test
    @DisplayName("测试拦截器配置")
    void testAddInterceptors() {
        // 创建模拟对象
        InterceptorRegistry registry = mock(InterceptorRegistry.class);
        AuthInterceptor authInterceptor = mock(AuthInterceptor.class);
        InterceptorRegistration registration = mock(InterceptorRegistration.class);

        // 设置模拟返回值
        when(registry.addInterceptor(authInterceptor)).thenReturn(registration);
        when(registration.addPathPatterns(anyString())).thenReturn(registration);
        when(registration.excludePathPatterns(anyString(), anyString(), anyString(), anyString())).thenReturn(registration);

        // 创建配置对象并注入模拟依赖
        WebConfig webConfig = new WebConfig();
        // 使用反射设置authInterceptor
        try {
            java.lang.reflect.Field field = WebConfig.class.getDeclaredField("authInterceptor");
            field.setAccessible(true);
            field.set(webConfig, authInterceptor);
        } catch (Exception e) {
            fail("无法设置authInterceptor字段: " + e.getMessage());
        }

        // 执行测试方法
        webConfig.addInterceptors(registry);

        // 验证交互
        verify(registry).addInterceptor(authInterceptor);
        
        // 验证路径匹配模式
        verify(registration).addPathPatterns("/api/**");
        
        // 验证排除路径
        verify(registration).excludePathPatterns(
            eq("/api/auth/login"), 
            eq("/api/auth/register"), 
            eq("/api/auth/logout"),
            eq("/api/auth/currentUser")
        );
    }
}
