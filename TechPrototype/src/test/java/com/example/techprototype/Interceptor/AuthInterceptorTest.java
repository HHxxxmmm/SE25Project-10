package com.example.techprototype.Interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthInterceptorTest {

    private AuthInterceptor authInterceptor;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    @Mock
    private PrintWriter writer;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        authInterceptor = new AuthInterceptor();
        
        // 设置基本的模拟行为
        when(request.getSession(false)).thenReturn(session);
        when(response.getWriter()).thenReturn(writer);
    }

    @Test
    @DisplayName("测试公开路径应放行")
    void testPublicPathShouldPass() throws Exception {
        // 设置请求URI为公开路径
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        
        // 执行预处理方法
        boolean result = authInterceptor.preHandle(request, response, null);
        
        // 验证结果
        assertTrue(result, "公开路径应该放行");
        verifyNoInteractions(session); // 不应访问会话
    }

    @Test
    @DisplayName("测试已认证请求应放行")
    void testAuthenticatedRequestShouldPass() throws Exception {
        // 设置请求URI为需要认证的路径
        when(request.getRequestURI()).thenReturn("/api/tickets/search");
        
        // 模拟已登录会话
        when(session.getAttribute("userId")).thenReturn(1L);
        
        // 执行预处理方法
        boolean result = authInterceptor.preHandle(request, response, null);
        
        // 验证结果
        assertTrue(result, "已认证请求应该放行");
    }

    @Test
    @DisplayName("测试未认证请求应拦截")
    void testUnauthenticatedRequestShouldBlock() throws Exception {
        // 设置请求URI为需要认证的路径
        when(request.getRequestURI()).thenReturn("/api/tickets/search");
        
        // 模拟未登录会话
        when(session.getAttribute("userId")).thenReturn(null);
        
        // 执行预处理方法
        boolean result = authInterceptor.preHandle(request, response, null);
        
        // 验证结果
        assertFalse(result, "未认证请求应该被拦截");
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        verify(writer).write(contains("未登录或登录已过期"));
    }

    @Test
    @DisplayName("测试会话为空应拦截")
    void testNullSessionShouldBlock() throws Exception {
        // 设置请求URI为需要认证的路径
        when(request.getRequestURI()).thenReturn("/api/tickets/search");
        
        // 模拟会话为空
        when(request.getSession(false)).thenReturn(null);
        
        // 执行预处理方法
        boolean result = authInterceptor.preHandle(request, response, null);
        
        // 验证结果
        assertFalse(result, "会话为空时应该被拦截");
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    @DisplayName("测试使用真实的HttpServletRequest和HttpServletResponse")
    void testWithRealHttpObjects() throws Exception {
        // 创建真实的请求和响应对象
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse();
        MockHttpSession mockSession = new MockHttpSession();
        
        // 设置会话属性
        mockSession.setAttribute("userId", 1L);
        mockRequest.setSession(mockSession);
        
        // 设置请求URI
        mockRequest.setRequestURI("/api/tickets/search");
        
        // 执行预处理方法
        boolean result = authInterceptor.preHandle(mockRequest, mockResponse, null);
        
        // 验证结果
        assertTrue(result, "带有有效会话的请求应该放行");
    }
    
    @Test
    @DisplayName("测试多种公开路径")
    void testVariousPublicPaths() throws Exception {
        // 准备各种公开路径
        String[] publicPaths = {
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/logout",
            "/api/auth/currentUser",
            "/api/trains/list",
            "/api/trains/direct",
            "/api/trains/byTime",
            "/api/trains/transfer",
            "/api/trains/search",
            "/error"
        };
        
        for (String path : publicPaths) {
            // 设置请求URI为当前测试的公开路径
            when(request.getRequestURI()).thenReturn(path);
            
            // 执行预处理方法
            boolean result = authInterceptor.preHandle(request, response, null);
            
            // 验证结果
            assertTrue(result, "公开路径 " + path + " 应该放行");
            
            // 验证不会检查session
            verify(session, never()).getAttribute("userId");
        }
    }
    
    @Test
    @DisplayName("测试扩展路径匹配")
    void testExtendedPathMatching() throws Exception {
        // 设置请求URI为公开路径的子路径
        when(request.getRequestURI()).thenReturn("/api/auth/login/extended");
        
        // 执行预处理方法
        boolean result = authInterceptor.preHandle(request, response, null);
        
        // 验证结果
        assertTrue(result, "公开路径的子路径应该放行");
        verifyNoInteractions(session); // 不应访问会话
    }
}
