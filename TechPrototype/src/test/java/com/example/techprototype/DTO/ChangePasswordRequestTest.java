package com.example.techprototype.DTO;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChangePasswordRequestTest {

    @Test
    void testGettersAndSetters() {
        ChangePasswordRequest request = new ChangePasswordRequest();

        // 测试 userId
        Long userId = 1L;
        request.setUserId(userId);
        assertEquals(userId, request.getUserId());

        // 测试 currentPassword
        String currentPassword = "oldPass123";
        request.setCurrentPassword(currentPassword);
        assertEquals(currentPassword, request.getCurrentPassword());

        // 测试 newPassword
        String newPassword = "newPass456";
        request.setNewPassword(newPassword);
        assertEquals(newPassword, request.getNewPassword());
    }

    @Test
    void testNoArgsConstructor() {
        ChangePasswordRequest request = new ChangePasswordRequest();

        assertNull(request.getUserId());
        assertNull(request.getCurrentPassword());
        assertNull(request.getNewPassword());
    }

    @Test
    void testAllArgsConstructor() {
        Long userId = 1L;
        String currentPassword = "oldPass123";
        String newPassword = "newPass456";

        ChangePasswordRequest request = new ChangePasswordRequest(userId, currentPassword, newPassword);

        assertEquals(userId, request.getUserId());
        assertEquals(currentPassword, request.getCurrentPassword());
        assertEquals(newPassword, request.getNewPassword());
    }

    @Test
    void testEquals() {
        ChangePasswordRequest request1 = new ChangePasswordRequest(1L, "oldPass", "newPass");
        ChangePasswordRequest request2 = new ChangePasswordRequest(1L, "oldPass", "newPass");
        ChangePasswordRequest request3 = new ChangePasswordRequest(2L, "diffPass", "diffNewPass");

        // 测试相等性
        assertTrue(request1.equals(request2));
        assertTrue(request2.equals(request1));
        assertFalse(request1.equals(request3));
        assertFalse(request1.equals(null));
        assertTrue(request1.equals(request1));
    }

    @Test
    void canEqual() {
        ChangePasswordRequest request1 = new ChangePasswordRequest();
        ChangePasswordRequest request2 = new ChangePasswordRequest();
        Object otherObject = new Object();

        assertTrue(request1.canEqual(request2));
        assertFalse(request1.canEqual(otherObject));
    }

    @Test
    void testHashCode() {
        ChangePasswordRequest request1 = new ChangePasswordRequest(1L, "oldPass", "newPass");
        ChangePasswordRequest request2 = new ChangePasswordRequest(1L, "oldPass", "newPass");
        ChangePasswordRequest request3 = new ChangePasswordRequest(2L, "diffPass", "diffNewPass");

        // 测试哈希码
        assertEquals(request1.hashCode(), request2.hashCode());
        assertNotEquals(request1.hashCode(), request3.hashCode());
    }

    @Test
    void testToString() {
        ChangePasswordRequest request = new ChangePasswordRequest(1L, "oldPass", "newPass");
        String toString = request.toString();

        // 验证 toString 包含所有字段的值
        assertTrue(toString.contains("userId=1"));
        assertTrue(toString.contains("currentPassword=oldPass"));
        assertTrue(toString.contains("newPassword=newPass"));
    }
}