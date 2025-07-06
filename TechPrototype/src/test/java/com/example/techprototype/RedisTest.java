package com.example.techprototype;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
public class RedisTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    public void testRedisOperations() {
        // 测试写入
        redisTemplate.opsForValue().set("testKey", "Hello Redis");

        // 测试读取
        String value = redisTemplate.opsForValue().get("testKey");
        assertEquals("Hello Redis", value);

        // 测试删除
        redisTemplate.delete("testKey");
        assertNull(redisTemplate.opsForValue().get("testKey"));
    }
}