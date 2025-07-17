package com.example.techprototype.Util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class JwtUtilTest {
    
    private JwtUtil jwtUtil;
    private static final Long USER_ID = 123L;
    
    @BeforeEach
    public void setup() {
        jwtUtil = new JwtUtil();
    }
    
    @Test
    @DisplayName("生成JWT令牌测试")
    public void testGenerateToken() {
        // 生成令牌
        String token = jwtUtil.generateToken(USER_ID);
        
        // 验证令牌不为空
        assertNotNull(token, "令牌不应该为空");
        assertTrue(token.length() > 0, "令牌应该有内容");
    }
    
    @Test
    @DisplayName("从令牌解析用户ID测试")
    public void testGetUserIdFromToken() {
        // 生成令牌
        String token = jwtUtil.generateToken(USER_ID);
        
        // 从令牌解析用户ID
        Long extractedUserId = jwtUtil.getUserIdFromToken(token);
        
        // 验证解析出的用户ID与原始ID一致
        assertEquals(USER_ID, extractedUserId, "解析出的用户ID应该与原始ID一致");
    }
    
    @Test
    @DisplayName("从令牌解析过期时间测试")
    public void testGetExpirationDateFromToken() {
        // 生成令牌
        String token = jwtUtil.generateToken(USER_ID);
        
        // 从令牌解析过期时间
        Date expirationDate = jwtUtil.getExpirationDateFromToken(token);
        
        // 验证过期时间在当前时间之后
        assertTrue(expirationDate.after(new Date()), "令牌过期时间应该在当前时间之后");
    }
    
    @Test
    @DisplayName("验证有效令牌测试")
    public void testValidateToken() {
        // 生成令牌
        String token = jwtUtil.generateToken(USER_ID);
        
        // 验证令牌对应的用户ID是否有效
        Boolean isValid = jwtUtil.validateToken(token, USER_ID);
        
        // 验证结果为有效
        assertTrue(isValid, "对于有效用户ID的有效令牌，验证结果应为true");
    }
    
    @Test
    @DisplayName("验证无效用户ID的令牌测试")
    public void testValidateTokenWithInvalidUserId() {
        // 生成令牌
        String token = jwtUtil.generateToken(USER_ID);
        
        // 使用错误的用户ID验证令牌
        Boolean isValid = jwtUtil.validateToken(token, 456L);
        
        // 验证结果为无效
        assertFalse(isValid, "对于不匹配用户ID的令牌，验证结果应为false");
    }
    
    @Test
    @DisplayName("从令牌解析所有声明测试")
    public void testGetClaimFromToken() {
        // 生成令牌
        String token = jwtUtil.generateToken(USER_ID);
        
        // 从令牌解析主题
        String subject = jwtUtil.getClaimFromToken(token, Claims::getSubject);
        
        // 验证主题与用户ID一致
        assertEquals(USER_ID.toString(), subject, "解析出的主题应该与用户ID字符串一致");
        
        // 从令牌解析发布时间
        Date issuedAt = jwtUtil.getClaimFromToken(token, Claims::getIssuedAt);
        
        // 验证发布时间不为空
        assertNotNull(issuedAt, "发布时间不应该为空");
        
        // 验证发布时间在当前时间或之前
        assertTrue(issuedAt.before(new Date()) || issuedAt.equals(new Date()), "发布时间应该在当前时间或之前");
    }
}
