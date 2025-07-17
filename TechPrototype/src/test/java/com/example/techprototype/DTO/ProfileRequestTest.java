package com.example.techprototype.DTO;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProfileRequestTest {

    @Test
    void getRealName() {
        ProfileRequest request = new ProfileRequest();
        String realName = "张三";
        request.setRealName(realName);
        assertEquals(realName, request.getRealName());
    }

    @Test
    void getEmail() {
        ProfileRequest request = new ProfileRequest();
        String email = "zhangsan@example.com";
        request.setEmail(email);
        assertEquals(email, request.getEmail());
    }

    @Test
    void getPhoneNumber() {
        ProfileRequest request = new ProfileRequest();
        String phoneNumber = "13812345678";
        request.setPhoneNumber(phoneNumber);
        assertEquals(phoneNumber, request.getPhoneNumber());
    }

    @Test
    void setRealName() {
        ProfileRequest request = new ProfileRequest();
        String realName = "张三";
        request.setRealName(realName);
        assertEquals(realName, request.getRealName());

        // 测试修改值
        String newRealName = "李四";
        request.setRealName(newRealName);
        assertEquals(newRealName, request.getRealName());
    }

    @Test
    void setEmail() {
        ProfileRequest request = new ProfileRequest();
        String email = "zhangsan@example.com";
        request.setEmail(email);
        assertEquals(email, request.getEmail());

        // 测试修改值
        String newEmail = "lisi@example.com";
        request.setEmail(newEmail);
        assertEquals(newEmail, request.getEmail());
    }

    @Test
    void setPhoneNumber() {
        ProfileRequest request = new ProfileRequest();
        String phoneNumber = "13812345678";
        request.setPhoneNumber(phoneNumber);
        assertEquals(phoneNumber, request.getPhoneNumber());

        // 测试修改值
        String newPhoneNumber = "13987654321";
        request.setPhoneNumber(newPhoneNumber);
        assertEquals(newPhoneNumber, request.getPhoneNumber());
    }

    @Test
    void testEquals() {
        ProfileRequest request1 = new ProfileRequest("张三", "zhangsan@example.com", "13812345678");
        ProfileRequest request2 = new ProfileRequest("张三", "zhangsan@example.com", "13812345678");
        ProfileRequest request3 = new ProfileRequest("李四", "lisi@example.com", "13987654321");

        // 测试相等性
        assertTrue(request1.equals(request2));
        assertTrue(request2.equals(request1));
        assertFalse(request1.equals(request3));
        assertFalse(request1.equals(null));
        assertTrue(request1.equals(request1));
    }

    @Test
    void canEqual() {
        ProfileRequest request1 = new ProfileRequest();
        ProfileRequest request2 = new ProfileRequest();
        Object otherObject = new Object();

        assertTrue(request1.canEqual(request2));
        assertFalse(request1.canEqual(otherObject));
    }

    @Test
    void testHashCode() {
        ProfileRequest request1 = new ProfileRequest("张三", "zhangsan@example.com", "13812345678");
        ProfileRequest request2 = new ProfileRequest("张三", "zhangsan@example.com", "13812345678");
        ProfileRequest request3 = new ProfileRequest("李四", "lisi@example.com", "13987654321");

        assertEquals(request1.hashCode(), request2.hashCode());
        assertNotEquals(request1.hashCode(), request3.hashCode());
    }

    @Test
    void testToString() {
        ProfileRequest request = new ProfileRequest("张三", "zhangsan@example.com", "13812345678");
        String toString = request.toString();

        assertTrue(toString.contains("realName=张三"));
        assertTrue(toString.contains("email=zhangsan@example.com"));
        assertTrue(toString.contains("phoneNumber=13812345678"));
    }

    @Test
    void testNoArgsConstructor() {
        ProfileRequest request = new ProfileRequest();

        assertNull(request.getRealName());
        assertNull(request.getEmail());
        assertNull(request.getPhoneNumber());
    }

    @Test
    void testAllArgsConstructor() {
        String realName = "张三";
        String email = "zhangsan@example.com";
        String phoneNumber = "13812345678";

        ProfileRequest request = new ProfileRequest(realName, email, phoneNumber);

        assertEquals(realName, request.getRealName());
        assertEquals(email, request.getEmail());
        assertEquals(phoneNumber, request.getPhoneNumber());
    }
}