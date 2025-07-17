package com.example.techprototype.DTO;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class LastLoginUpdateRequestTest {

    @Test
    void testGettersAndSetters() {
        LastLoginUpdateRequest request = new LastLoginUpdateRequest();

        // 测试 userId 的 getter 和 setter
        Long userId = 1L;
        request.setUserId(userId);
        assertEquals(userId, request.getUserId());

        // 测试 loginTime 的 getter 和 setter
        LocalDateTime loginTime = LocalDateTime.now();
        request.setLoginTime(loginTime);
        assertEquals(loginTime, request.getLoginTime());
    }

    @Test
    void getUserId() {
        LastLoginUpdateRequest request = new LastLoginUpdateRequest();
        Long userId = 1L;
        request.setUserId(userId);
        assertEquals(userId, request.getUserId());
    }

    @Test
    void getLoginTime() {
        LastLoginUpdateRequest request = new LastLoginUpdateRequest();
        LocalDateTime loginTime = LocalDateTime.now();
        request.setLoginTime(loginTime);
        assertEquals(loginTime, request.getLoginTime());
    }

    @Test
    void setUserId() {
        LastLoginUpdateRequest request = new LastLoginUpdateRequest();
        Long userId = 1L;
        request.setUserId(userId);
        assertEquals(userId, request.getUserId());

        // 测试设置不同的值
        Long newUserId = 2L;
        request.setUserId(newUserId);
        assertEquals(newUserId, request.getUserId());
    }

    @Test
    void setLoginTime() {
        LastLoginUpdateRequest request = new LastLoginUpdateRequest();
        LocalDateTime loginTime = LocalDateTime.now();
        request.setLoginTime(loginTime);
        assertEquals(loginTime, request.getLoginTime());

        // 测试设置不同的时间
        LocalDateTime newLoginTime = LocalDateTime.now().plusHours(1);
        request.setLoginTime(newLoginTime);
        assertEquals(newLoginTime, request.getLoginTime());
    }

    @Test
    void testEquals() {
        LocalDateTime now = LocalDateTime.now();
        LastLoginUpdateRequest request1 = new LastLoginUpdateRequest(1L, now);
        LastLoginUpdateRequest request2 = new LastLoginUpdateRequest(1L, now);
        LastLoginUpdateRequest request3 = new LastLoginUpdateRequest(2L, now.plusHours(1));

        // 测试相等性
        assertTrue(request1.equals(request2));
        assertTrue(request2.equals(request1));
        assertFalse(request1.equals(request3));
        assertFalse(request1.equals(null));
        assertTrue(request1.equals(request1));
    }

    @Test
    void canEqual() {
        LastLoginUpdateRequest request1 = new LastLoginUpdateRequest();
        LastLoginUpdateRequest request2 = new LastLoginUpdateRequest();
        Object otherObject = new Object();

        assertTrue(request1.canEqual(request2));
        assertFalse(request1.canEqual(otherObject));
    }

    @Test
    void testHashCode() {
        LocalDateTime now = LocalDateTime.now();
        LastLoginUpdateRequest request1 = new LastLoginUpdateRequest(1L, now);
        LastLoginUpdateRequest request2 = new LastLoginUpdateRequest(1L, now);
        LastLoginUpdateRequest request3 = new LastLoginUpdateRequest(2L, now.plusHours(1));

        assertEquals(request1.hashCode(), request2.hashCode());
        assertNotEquals(request1.hashCode(), request3.hashCode());
    }

    @Test
    void testToString() {
        Long userId = 1L;
        LocalDateTime loginTime = LocalDateTime.of(2025, 7, 18, 10, 30);
        LastLoginUpdateRequest request = new LastLoginUpdateRequest(userId, loginTime);

        String toString = request.toString();
        assertTrue(toString.contains("userId=" + userId));
        assertTrue(toString.contains("loginTime=" + loginTime));
    }

    @Test
    void testNoArgsConstructor() {
        LastLoginUpdateRequest request = new LastLoginUpdateRequest();

        assertNull(request.getUserId());
        assertNull(request.getLoginTime());
    }

    @Test
    void testAllArgsConstructor() {
        Long userId = 1L;
        LocalDateTime loginTime = LocalDateTime.now();

        LastLoginUpdateRequest request = new LastLoginUpdateRequest(userId, loginTime);

        assertEquals(userId, request.getUserId());
        assertEquals(loginTime, request.getLoginTime());
    }
}