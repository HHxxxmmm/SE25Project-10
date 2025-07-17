package com.example.techprototype.DTO;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BasicResponseTest {

    @Test
    void getStatus() {
        BasicResponse response = new BasicResponse();
        response.setStatus("SUCCESS");
        assertEquals("SUCCESS", response.getStatus());
    }

    @Test
    void getMessage() {
        BasicResponse response = new BasicResponse();
        response.setMessage("操作成功");
        assertEquals("操作成功", response.getMessage());
    }

    @Test
    void getTimestamp() {
        LocalDateTime now = LocalDateTime.now();
        BasicResponse response = new BasicResponse();
        response.setTimestamp(now);
        assertEquals(now, response.getTimestamp());
    }

    @Test
    void setStatus() {
        BasicResponse response = new BasicResponse();
        response.setStatus("FAILURE");
        assertEquals("FAILURE", response.getStatus());
    }

    @Test
    void setMessage() {
        BasicResponse response = new BasicResponse();
        response.setMessage("操作失败");
        assertEquals("操作失败", response.getMessage());
    }

    @Test
    void setTimestamp() {
        LocalDateTime now = LocalDateTime.now();
        BasicResponse response = new BasicResponse();
        response.setTimestamp(now);
        assertEquals(now, response.getTimestamp());
    }

    @Test
    void testEquals() {
        LocalDateTime now = LocalDateTime.now();
        BasicResponse response1 = new BasicResponse("SUCCESS", "测试消息", now);
        BasicResponse response2 = new BasicResponse("SUCCESS", "测试消息", now);
        BasicResponse response3 = new BasicResponse("FAILURE", "不同消息", now);

        // 测试相等性
        assertTrue(response1.equals(response2));
        assertTrue(response2.equals(response1));
        assertFalse(response1.equals(response3));
        assertFalse(response1.equals(null));
        assertTrue(response1.equals(response1));
    }

    @Test
    void canEqual() {
        BasicResponse response1 = new BasicResponse();
        BasicResponse response2 = new BasicResponse();

        assertTrue(response1.canEqual(response2));
        assertFalse(response1.canEqual(new Object()));
    }

    @Test
    void testHashCode() {
        LocalDateTime now = LocalDateTime.now();
        BasicResponse response1 = new BasicResponse("SUCCESS", "测试消息", now);
        BasicResponse response2 = new BasicResponse("SUCCESS", "测试消息", now);
        BasicResponse response3 = new BasicResponse("FAILURE", "不同消息", now);

        // 测试哈希码
        assertEquals(response1.hashCode(), response2.hashCode());
        assertNotEquals(response1.hashCode(), response3.hashCode());
    }

    @Test
    void testToString() {
        LocalDateTime now = LocalDateTime.now();
        BasicResponse response = new BasicResponse("SUCCESS", "测试消息", now);
        String toString = response.toString();

        assertTrue(toString.contains("status=SUCCESS"));
        assertTrue(toString.contains("message=测试消息"));
        assertTrue(toString.contains("timestamp=" + now));
    }

    @Test
    void testSuccessFactoryMethod() {
        String message = "操作成功";
        BasicResponse response = BasicResponse.success(message);

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(message, response.getMessage());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void testFailureFactoryMethod() {
        String message = "操作失败";
        BasicResponse response = BasicResponse.failure(message);

        assertEquals("FAILURE", response.getStatus());
        assertEquals(message, response.getMessage());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void testNoArgsConstructor() {
        BasicResponse response = new BasicResponse();
        assertNull(response.getStatus());
        assertNull(response.getMessage());
        assertNull(response.getTimestamp());
    }

    @Test
    void testAllArgsConstructor() {
        String status = "SUCCESS";
        String message = "测试消息";
        LocalDateTime timestamp = LocalDateTime.now();

        BasicResponse response = new BasicResponse(status, message, timestamp);

        assertEquals(status, response.getStatus());
        assertEquals(message, response.getMessage());
        assertEquals(timestamp, response.getTimestamp());
    }
}