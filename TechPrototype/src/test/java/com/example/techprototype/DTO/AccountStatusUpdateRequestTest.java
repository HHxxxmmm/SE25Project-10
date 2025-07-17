package com.example.techprototype.DTO;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccountStatusUpdateRequestTest {

    @Test
    void testGetterAndSetter() {
        AccountStatusUpdateRequest request = new AccountStatusUpdateRequest();

        // 测试 userId 的 getter 和 setter
        Long userId = 1L;
        request.setUserId(userId);
        assertEquals(userId, request.getUserId());

        // 测试 accountStatus 的 getter 和 setter
        Byte status = 1;
        request.setAccountStatus(status);
        assertEquals(status, request.getAccountStatus());
    }

    @Test
    void testConstructor() {
        // 测试全参构造函数
        Long userId = 1L;
        Byte status = 1;
        AccountStatusUpdateRequest request = new AccountStatusUpdateRequest(userId, status);

        assertEquals(userId, request.getUserId());
        assertEquals(status, request.getAccountStatus());
    }

    @Test
    void testEquals() {
        // 创建两个相同内容的对象
        AccountStatusUpdateRequest request1 = new AccountStatusUpdateRequest(1L, (byte) 1);
        AccountStatusUpdateRequest request2 = new AccountStatusUpdateRequest(1L, (byte) 1);
        AccountStatusUpdateRequest request3 = new AccountStatusUpdateRequest(2L, (byte) 0);

        // 测试相等性
        assertTrue(request1.equals(request2));
        assertTrue(request2.equals(request1));
        assertFalse(request1.equals(request3));
        assertFalse(request1.equals(null));
        assertTrue(request1.equals(request1));
    }

    @Test
    void testHashCode() {
        // 创建两个相同内容的对象，它们的 hashCode 应该相等
        AccountStatusUpdateRequest request1 = new AccountStatusUpdateRequest(1L, (byte) 1);
        AccountStatusUpdateRequest request2 = new AccountStatusUpdateRequest(1L, (byte) 1);
        AccountStatusUpdateRequest request3 = new AccountStatusUpdateRequest(2L, (byte) 0);

        assertEquals(request1.hashCode(), request2.hashCode());
        assertNotEquals(request1.hashCode(), request3.hashCode());
    }

    @Test
    void testToString() {
        AccountStatusUpdateRequest request = new AccountStatusUpdateRequest(1L, (byte) 1);
        String toString = request.toString();

        // 验证 toString 包含所有字段的值
        assertTrue(toString.contains("userId=1"));
        assertTrue(toString.contains("accountStatus=1"));
    }
}