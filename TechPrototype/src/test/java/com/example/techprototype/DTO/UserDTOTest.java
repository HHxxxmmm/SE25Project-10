package com.example.techprototype.DTO;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserDTOTest {

    @Test
    void testSettersAndGetters() {
        UserDTO userDTO = new UserDTO();
        
        // 设置所有字段
        userDTO.setUserId(1L);
        userDTO.setRealName("张三");
        userDTO.setPhoneNumber("13800138000");
        userDTO.setEmail("test@example.com");
        userDTO.setToken("test-token-123");
        userDTO.setPassengerId(100L);

        // 验证所有字段
        assertEquals(1L, userDTO.getUserId());
        assertEquals("张三", userDTO.getRealName());
        assertEquals("13800138000", userDTO.getPhoneNumber());
        assertEquals("test@example.com", userDTO.getEmail());
        assertEquals("test-token-123", userDTO.getToken());
        assertEquals(100L, userDTO.getPassengerId());
    }

    @Test
    void testNullValues() {
        UserDTO userDTO = new UserDTO();
        
        // 设置null值
        userDTO.setUserId(null);
        userDTO.setRealName(null);
        userDTO.setPhoneNumber(null);
        userDTO.setEmail(null);
        userDTO.setToken(null);
        userDTO.setPassengerId(null);

        // 验证null值
        assertNull(userDTO.getUserId());
        assertNull(userDTO.getRealName());
        assertNull(userDTO.getPhoneNumber());
        assertNull(userDTO.getEmail());
        assertNull(userDTO.getToken());
        assertNull(userDTO.getPassengerId());
    }

    @Test
    void testEmptyStrings() {
        UserDTO userDTO = new UserDTO();
        
        // 设置空字符串
        userDTO.setRealName("");
        userDTO.setPhoneNumber("");
        userDTO.setEmail("");
        userDTO.setToken("");

        // 验证空字符串
        assertEquals("", userDTO.getRealName());
        assertEquals("", userDTO.getPhoneNumber());
        assertEquals("", userDTO.getEmail());
        assertEquals("", userDTO.getToken());
    }

    @Test
    void testSpecialCharacters() {
        UserDTO userDTO = new UserDTO();
        
        // 设置包含特殊字符的值
        userDTO.setRealName("张三@李四");
        userDTO.setPhoneNumber("+86-138-0013-8000");
        userDTO.setEmail("test+tag@example.com");
        userDTO.setToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c");

        // 验证特殊字符
        assertEquals("张三@李四", userDTO.getRealName());
        assertEquals("+86-138-0013-8000", userDTO.getPhoneNumber());
        assertEquals("test+tag@example.com", userDTO.getEmail());
        assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c", userDTO.getToken());
    }

    @Test
    void testLongValues() {
        UserDTO userDTO = new UserDTO();
        
        // 设置长值
        userDTO.setUserId(Long.MAX_VALUE);
        userDTO.setPassengerId(Long.MAX_VALUE);
        userDTO.setRealName("这是一个很长的真实姓名，用来测试边界情况");
        userDTO.setPhoneNumber("12345678901234567890");
        userDTO.setEmail("very.long.email.address.with.many.subdomains@example.com");
        userDTO.setToken("very.long.token.with.many.characters.that.exceeds.normal.length");

        // 验证长值
        assertEquals(Long.MAX_VALUE, userDTO.getUserId());
        assertEquals(Long.MAX_VALUE, userDTO.getPassengerId());
        assertEquals("这是一个很长的真实姓名，用来测试边界情况", userDTO.getRealName());
        assertEquals("12345678901234567890", userDTO.getPhoneNumber());
        assertEquals("very.long.email.address.with.many.subdomains@example.com", userDTO.getEmail());
        assertEquals("very.long.token.with.many.characters.that.exceeds.normal.length", userDTO.getToken());
    }

    @Test
    void testZeroValues() {
        UserDTO userDTO = new UserDTO();
        
        // 设置零值
        userDTO.setUserId(0L);
        userDTO.setPassengerId(0L);

        // 验证零值
        assertEquals(0L, userDTO.getUserId());
        assertEquals(0L, userDTO.getPassengerId());
    }

    @Test
    void testNegativeValues() {
        UserDTO userDTO = new UserDTO();
        
        // 设置负值
        userDTO.setUserId(-1L);
        userDTO.setPassengerId(-100L);

        // 验证负值
        assertEquals(-1L, userDTO.getUserId());
        assertEquals(-100L, userDTO.getPassengerId());
    }

    @Test
    void testUnicodeCharacters() {
        UserDTO userDTO = new UserDTO();
        
        // 设置Unicode字符
        userDTO.setRealName("张三李四王五赵六钱七孙八周九吴十郑十一王十二");
        userDTO.setEmail("张三@example.com");
        userDTO.setToken("张三李四王五赵六钱七孙八周九吴十郑十一王十二");

        // 验证Unicode字符
        assertEquals("张三李四王五赵六钱七孙八周九吴十郑十一王十二", userDTO.getRealName());
        assertEquals("张三@example.com", userDTO.getEmail());
        assertEquals("张三李四王五赵六钱七孙八周九吴十郑十一王十二", userDTO.getToken());
    }

    @Test
    void testWhitespaceValues() {
        UserDTO userDTO = new UserDTO();
        
        // 设置包含空格的值
        userDTO.setRealName(" 张三 李四 ");
        userDTO.setPhoneNumber(" 138 0013 8000 ");
        userDTO.setEmail(" test @ example.com ");
        userDTO.setToken(" test token ");

        // 验证包含空格的值
        assertEquals(" 张三 李四 ", userDTO.getRealName());
        assertEquals(" 138 0013 8000 ", userDTO.getPhoneNumber());
        assertEquals(" test @ example.com ", userDTO.getEmail());
        assertEquals(" test token ", userDTO.getToken());
    }

    @Test
    void testMultipleUpdates() {
        UserDTO userDTO = new UserDTO();
        
        // 第一次设置
        userDTO.setUserId(1L);
        userDTO.setRealName("张三");
        userDTO.setPhoneNumber("13800138000");
        userDTO.setEmail("test1@example.com");
        userDTO.setToken("token1");
        userDTO.setPassengerId(100L);

        // 验证第一次设置
        assertEquals(1L, userDTO.getUserId());
        assertEquals("张三", userDTO.getRealName());
        assertEquals("13800138000", userDTO.getPhoneNumber());
        assertEquals("test1@example.com", userDTO.getEmail());
        assertEquals("token1", userDTO.getToken());
        assertEquals(100L, userDTO.getPassengerId());

        // 第二次更新
        userDTO.setUserId(2L);
        userDTO.setRealName("李四");
        userDTO.setPhoneNumber("13900139000");
        userDTO.setEmail("test2@example.com");
        userDTO.setToken("token2");
        userDTO.setPassengerId(200L);

        // 验证第二次更新
        assertEquals(2L, userDTO.getUserId());
        assertEquals("李四", userDTO.getRealName());
        assertEquals("13900139000", userDTO.getPhoneNumber());
        assertEquals("test2@example.com", userDTO.getEmail());
        assertEquals("token2", userDTO.getToken());
        assertEquals(200L, userDTO.getPassengerId());
    }
} 