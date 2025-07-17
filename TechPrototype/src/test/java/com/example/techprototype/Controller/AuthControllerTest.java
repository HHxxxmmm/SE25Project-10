package com.example.techprototype.Controller;

import com.example.techprototype.DTO.LoginDTO;
import com.example.techprototype.DTO.RegisterDTO;
import com.example.techprototype.DTO.UserDTO;
import com.example.techprototype.Service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.mockito.Mockito.mock;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AuthControllerTest {

    @InjectMocks
    private AuthController authController;

    @Mock
    private UserService userService;

    private HttpSession session;
    private HttpServletResponse response;
    private HttpServletRequest request;
    private UserDTO mockUserDTO;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        
        // 初始化Mock对象
        session = mock(HttpSession.class);
        response = new MockHttpServletResponse();
        request = new MockHttpServletRequest();
        
        // 创建模拟用户数据
        mockUserDTO = new UserDTO();
        mockUserDTO.setUserId(1L);
        mockUserDTO.setRealName("测试用户");
        mockUserDTO.setPhoneNumber("13800138000");
        mockUserDTO.setEmail("test@example.com");
        mockUserDTO.setToken("mock-jwt-token");
        mockUserDTO.setPassengerId(1L);
    }

    @Test
    @DisplayName("登录成功测试")
    public void testLoginSuccess() throws Exception {
        // 准备测试数据
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setPhoneNumber("13800138000");
        loginDTO.setPassword("password");
        
        // 模拟UserService返回结果
        when(userService.login(any(LoginDTO.class))).thenReturn(mockUserDTO);
        
        // 执行测试
        ResponseEntity<Object> result = authController.login(loginDTO, response, session);
        
        // 验证结果
        assertEquals(HttpStatus.OK, result.getStatusCode(), "HTTP状态码应为200 OK");
        assertTrue(result.getBody() instanceof Map, "响应体应该是Map类型");
        
        Map<String, Object> responseBody = (Map<String, Object>) result.getBody();
        assertTrue((Boolean) responseBody.get("success"), "成功标志应为true");
        assertEquals("登录成功", responseBody.get("message"), "成功消息应为'登录成功'");
        assertEquals(mockUserDTO, responseBody.get("user"), "返回的用户数据应该匹配");
        
        // 验证会话属性是否被正确设置（使用verify方法验证setAttribute调用）
        verify(session).setAttribute("userId", mockUserDTO.getUserId());
        verify(session).setAttribute("realName", mockUserDTO.getRealName());
        verify(session).setAttribute("phoneNumber", mockUserDTO.getPhoneNumber());
        verify(session).setAttribute("token", mockUserDTO.getToken());
    }

    @Test
    @DisplayName("登录失败测试")
    public void testLoginFailure() throws Exception {
        // 准备测试数据
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setPhoneNumber("13800138000");
        loginDTO.setPassword("wrong-password");
        
        // 模拟UserService抛出异常
        when(userService.login(any(LoginDTO.class))).thenThrow(new Exception("密码错误"));
        
        // 执行测试
        ResponseEntity<Object> result = authController.login(loginDTO, response, session);
        
        // 验证结果
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode(), "HTTP状态码应为401 UNAUTHORIZED");
        assertTrue(result.getBody() instanceof Map, "响应体应该是Map类型");
        
        Map<String, Object> responseBody = (Map<String, Object>) result.getBody();
        assertFalse((Boolean) responseBody.get("success"), "成功标志应为false");
        assertEquals("密码错误", responseBody.get("message"), "错误消息应匹配异常消息");
    }

    @Test
    @DisplayName("注册成功测试")
    public void testRegisterSuccess() throws Exception {
        // 准备测试数据
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setRealName("新用户");
        registerDTO.setPhoneNumber("13900139000");
        registerDTO.setPassword("password");
        registerDTO.setEmail("new@example.com");
        registerDTO.setIdCardNumber("110101199001010011");
        
        // 模拟UserService返回结果
        when(userService.register(any(RegisterDTO.class))).thenReturn(mockUserDTO);
        
        // 执行测试
        ResponseEntity<Object> result = authController.register(registerDTO, response, session);
        
        // 验证结果
        assertEquals(HttpStatus.CREATED, result.getStatusCode(), "HTTP状态码应为201 CREATED");
        assertTrue(result.getBody() instanceof Map, "响应体应该是Map类型");
        
        Map<String, Object> responseBody = (Map<String, Object>) result.getBody();
        assertTrue((Boolean) responseBody.get("success"), "成功标志应为true");
        assertEquals("注册成功", responseBody.get("message"), "成功消息应为'注册成功'");
        assertEquals(mockUserDTO, responseBody.get("user"), "返回的用户数据应该匹配");
        
        // 验证会话属性是否被正确设置（使用verify方法验证setAttribute调用）
        verify(session).setAttribute("userId", mockUserDTO.getUserId());
        verify(session).setAttribute("realName", mockUserDTO.getRealName());
        verify(session).setAttribute("phoneNumber", mockUserDTO.getPhoneNumber());
        verify(session).setAttribute("token", mockUserDTO.getToken());
    }

    @Test
    @DisplayName("注册失败测试")
    public void testRegisterFailure() throws Exception {
        // 准备测试数据
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setRealName("新用户");
        registerDTO.setPhoneNumber("13800138000");
        registerDTO.setPassword("password");
        registerDTO.setEmail("test@example.com");
        registerDTO.setIdCardNumber("110101199001010011");
        
        // 模拟UserService抛出异常
        when(userService.register(any(RegisterDTO.class))).thenThrow(new Exception("手机号已被注册"));
        
        // 执行测试
        ResponseEntity<Object> result = authController.register(registerDTO, response, session);
        
        // 验证结果
        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode(), "HTTP状态码应为400 BAD_REQUEST");
        assertTrue(result.getBody() instanceof Map, "响应体应该是Map类型");
        
        Map<String, Object> responseBody = (Map<String, Object>) result.getBody();
        assertFalse((Boolean) responseBody.get("success"), "成功标志应为false");
        assertEquals("手机号已被注册", responseBody.get("message"), "错误消息应匹配异常消息");
    }

    @Test
    @DisplayName("登出测试")
    public void testLogout() {
        // 准备测试数据 - 设置cookie
        Cookie cookie = new Cookie("mini12306_auth", "true");
        cookie.setPath("/");
        cookie.setMaxAge(24 * 60 * 60);
        
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setCookies(cookie);
        
        // 执行测试
        ResponseEntity<Object> result = authController.logout(mockRequest, response, session);
        
        // 验证结果
        assertEquals(HttpStatus.OK, result.getStatusCode(), "HTTP状态码应为200 OK");
        assertTrue(result.getBody() instanceof Map, "响应体应该是Map类型");
        
        Map<String, Object> responseBody = (Map<String, Object>) result.getBody();
        assertTrue((Boolean) responseBody.get("success"), "成功标志应为true");
        assertEquals("登出成功", responseBody.get("message"), "成功消息应为'登出成功'");
        
        // 验证会话已失效
        verify(session).invalidate();
    }

    @Test
    @DisplayName("获取当前用户信息 - 已登录")
    public void testGetCurrentUserLoggedIn() {
        // 准备测试数据 - 模拟已登录会话
        when(session.getAttribute("userId")).thenReturn(1L);
        when(session.getAttribute("realName")).thenReturn("测试用户");
        when(session.getAttribute("phoneNumber")).thenReturn("13800138000");
        when(session.getAttribute("token")).thenReturn("mock-jwt-token");
        
        // 执行测试
        ResponseEntity<Object> result = authController.getCurrentUser(session);
        
        // 验证结果
        assertEquals(HttpStatus.OK, result.getStatusCode(), "HTTP状态码应为200 OK");
        assertTrue(result.getBody() instanceof Map, "响应体应该是Map类型");
        
        Map<String, Object> responseBody = (Map<String, Object>) result.getBody();
        assertTrue((Boolean) responseBody.get("success"), "成功标志应为true");
        assertTrue(responseBody.get("user") instanceof UserDTO, "应该返回用户DTO");
        
        UserDTO userDTO = (UserDTO) responseBody.get("user");
        assertEquals(1L, userDTO.getUserId(), "用户ID应该匹配");
        assertEquals("测试用户", userDTO.getRealName(), "用户名应该匹配");
        assertEquals("13800138000", userDTO.getPhoneNumber(), "手机号应该匹配");
        assertEquals("mock-jwt-token", userDTO.getToken(), "Token应该匹配");
    }

    @Test
    @DisplayName("获取当前用户信息 - 未登录")
    public void testGetCurrentUserNotLoggedIn() {
        // 准备测试数据 - 模拟未登录会话
        when(session.getAttribute("userId")).thenReturn(null);
        
        // 执行测试
        ResponseEntity<Object> result = authController.getCurrentUser(session);
        
        // 验证结果
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode(), "HTTP状态码应为401 UNAUTHORIZED");
        assertTrue(result.getBody() instanceof Map, "响应体应该是Map类型");
        
        Map<String, Object> responseBody = (Map<String, Object>) result.getBody();
        assertFalse((Boolean) responseBody.get("success"), "成功标志应为false");
        assertEquals("未登录", responseBody.get("message"), "消息应为'未登录'");
    }
}
