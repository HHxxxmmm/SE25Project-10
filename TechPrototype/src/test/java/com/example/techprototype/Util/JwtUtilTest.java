package com.example.techprototype.Util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
    }

    @Test
    void testGenerateToken() {
        Long userId = 123L;
        String token = jwtUtil.generateToken(userId);
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
        
        // 验证可以从token中解析出用户ID
        Long extractedUserId = jwtUtil.getUserIdFromToken(token);
        assertEquals(userId, extractedUserId);
    }

    @Test
    void testGetUserIdFromToken() {
        Long userId = 456L;
        String token = jwtUtil.generateToken(userId);
        
        Long extractedUserId = jwtUtil.getUserIdFromToken(token);
        assertEquals(userId, extractedUserId);
    }

    @Test
    void testGetExpirationDateFromToken() {
        Long userId = 789L;
        String token = jwtUtil.generateToken(userId);
        
        Date expirationDate = jwtUtil.getExpirationDateFromToken(token);
        assertNotNull(expirationDate);
        
        // 验证过期时间在未来
        assertTrue(expirationDate.after(new Date()));
    }

    @Test
    void testGetClaimFromToken() {
        Long userId = 999L;
        String token = jwtUtil.generateToken(userId);
        
        // 测试获取subject声明
        String subject = jwtUtil.getClaimFromToken(token, Claims::getSubject);
        assertEquals(userId.toString(), subject);
        
        // 测试获取issuedAt声明
        Date issuedAt = jwtUtil.getClaimFromToken(token, Claims::getIssuedAt);
        assertNotNull(issuedAt);
    }

    @Test
    void testValidateToken_ValidToken() {
        Long userId = 111L;
        String token = jwtUtil.generateToken(userId);
        
        Boolean isValid = jwtUtil.validateToken(token, userId);
        assertTrue(isValid);
    }

    @Test
    void testValidateToken_InvalidUserId() {
        Long userId = 222L;
        String token = jwtUtil.generateToken(userId);
        
        Boolean isValid = jwtUtil.validateToken(token, 333L); // 不同的用户ID
        assertFalse(isValid);
    }

    @Test
    void testValidateToken_InvalidToken() {
        Long userId = 444L;
        
        // 无效token会抛出异常，应该捕获并返回false
        assertThrows(Exception.class, () -> {
            jwtUtil.validateToken("invalid.token.here", userId);
        });
    }

    @Test
    void testValidateToken_NullToken() {
        Long userId = 555L;
        
        // null token会抛出异常，应该捕获并返回false
        assertThrows(Exception.class, () -> {
            jwtUtil.validateToken(null, userId);
        });
    }

    @Test
    void testValidateToken_EmptyToken() {
        Long userId = 666L;
        
        // 空token会抛出异常，应该捕获并返回false
        assertThrows(Exception.class, () -> {
            jwtUtil.validateToken("", userId);
        });
    }

    @Test
    void testTokenUniqueness() {
        Long userId1 = 777L;
        Long userId2 = 888L;
        
        String token1 = jwtUtil.generateToken(userId1);
        String token2 = jwtUtil.generateToken(userId2);
        
        // 验证不同用户生成的token不同
        assertNotEquals(token1, token2);
        
        // 验证token内容正确
        assertEquals(userId1, jwtUtil.getUserIdFromToken(token1));
        assertEquals(userId2, jwtUtil.getUserIdFromToken(token2));
    }

    @Test
    void testMultipleTokensForSameUser() {
        Long userId = 999L;
        
        String token1 = jwtUtil.generateToken(userId);
        String token2 = jwtUtil.generateToken(userId);
        
        // 验证两个token都能正确解析用户ID
        assertEquals(userId, jwtUtil.getUserIdFromToken(token1));
        assertEquals(userId, jwtUtil.getUserIdFromToken(token2));
        
        // 验证两个token都能验证通过
        assertTrue(jwtUtil.validateToken(token1, userId));
        assertTrue(jwtUtil.validateToken(token2, userId));
        
        // 验证token格式正确（包含两个点，表示header.payload.signature）
        assertTrue(token1.contains(".") && token1.split("\\.").length == 3);
        assertTrue(token2.contains(".") && token2.split("\\.").length == 3);
    }
} 