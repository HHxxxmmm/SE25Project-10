package com.example.techprototype.DTO;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ChangePasswordResponseTest {

    @Test
    void getStatus() {
        ChangePasswordResponse response = new ChangePasswordResponse();
        response.setStatus("SUCCESS");
        assertEquals("SUCCESS", response.getStatus());
    }

    @Test
    void getMessage() {
        ChangePasswordResponse response = new ChangePasswordResponse();
        response.setMessage("密码修改成功");
        assertEquals("密码修改成功", response.getMessage());
    }

    @Test
    void getTimestamp() {
        LocalDateTime now = LocalDateTime.now();
        ChangePasswordResponse response = new ChangePasswordResponse();
        response.setTimestamp(now);
        assertEquals(now, response.getTimestamp());
    }

    @Test
    void setStatus() {
        ChangePasswordResponse response = new ChangePasswordResponse();
        response.setStatus("FAILURE");
        assertEquals("FAILURE", response.getStatus());
    }

    @Test
    void setMessage() {
        ChangePasswordResponse response = new ChangePasswordResponse();
        response.setMessage("密码错误");
        assertEquals("密码错误", response.getMessage());
    }

    @Test
    void setTimestamp() {
        LocalDateTime now = LocalDateTime.now();
        ChangePasswordResponse response = new ChangePasswordResponse();
        response.setTimestamp(now);
        assertEquals(now, response.getTimestamp());
    }

    @Test
    void testEquals() {
        LocalDateTime now = LocalDateTime.now();
        ChangePasswordResponse response1 = new ChangePasswordResponse("SUCCESS", "密码修改成功", now);
        ChangePasswordResponse response2 = new ChangePasswordResponse("SUCCESS", "密码修改成功", now);
        ChangePasswordResponse response3 = new ChangePasswordResponse("FAILURE", "密码错误", now);

        // 测试相等性
        assertTrue(response1.equals(response2));
        assertTrue(response2.equals(response1));
        assertFalse(response1.equals(response3));
        assertFalse(response1.equals(null));
        assertTrue(response1.equals(response1));
    }

    @Test
    void canEqual() {
        ChangePasswordResponse response1 = new ChangePasswordResponse();
        ChangePasswordResponse response2 = new ChangePasswordResponse();
        Object otherObject = new Object();

        assertTrue(response1.canEqual(response2));
        assertFalse(response1.canEqual(otherObject));
    }

    @Test
    void testHashCode() {
        LocalDateTime now = LocalDateTime.now();
        ChangePasswordResponse response1 = new ChangePasswordResponse("SUCCESS", "密码修改成功", now);
        ChangePasswordResponse response2 = new ChangePasswordResponse("SUCCESS", "密码修改成功", now);
        ChangePasswordResponse response3 = new ChangePasswordResponse("FAILURE", "密码错误", now);

        assertEquals(response1.hashCode(), response2.hashCode());
        assertNotEquals(response1.hashCode(), response3.hashCode());
    }

    @Test
    void testToString() {
        LocalDateTime now = LocalDateTime.now();
        ChangePasswordResponse response = new ChangePasswordResponse("SUCCESS", "密码修改成功", now);
        String toString = response.toString();

        assertTrue(toString.contains("status=SUCCESS"));
        assertTrue(toString.contains("message=密码修改成功"));
        assertTrue(toString.contains("timestamp=" + now));
    }

    @Test
    void testSuccessFactoryMethod() {
        ChangePasswordResponse response = ChangePasswordResponse.success();

        assertEquals("SUCCESS", response.getStatus());
        assertEquals("密码修改成功", response.getMessage());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void testFailureFactoryMethod() {
        String errorMessage = "原密码错误";
        ChangePasswordResponse response = ChangePasswordResponse.failure(errorMessage);

        assertEquals("FAILURE", response.getStatus());
        assertEquals(errorMessage, response.getMessage());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void testNoArgsConstructor() {
        ChangePasswordResponse response = new ChangePasswordResponse();

        assertNull(response.getStatus());
        assertNull(response.getMessage());
        assertNull(response.getTimestamp());
    }

    @Test
    void testAllArgsConstructor() {
        String status = "SUCCESS";
        String message = "密码修改成功";
        LocalDateTime timestamp = LocalDateTime.now();

        ChangePasswordResponse response = new ChangePasswordResponse(status, message, timestamp);

        assertEquals(status, response.getStatus());
        assertEquals(message, response.getMessage());
        assertEquals(timestamp, response.getTimestamp());
    }
}