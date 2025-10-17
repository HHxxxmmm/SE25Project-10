package com.example.techprototype.Config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    // 从 application.properties 注入配置值
    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private String redisPort;

    @Value("${spring.data.redis.password}") // 使用配置中的密码
    private String redisPassword;

    @Value("${spring.data.redis.database:0}") // 默认使用DB 0
    private int redisDatabase;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();

        config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort) // 使用注入的主机和端口
//                .setPassword(redisPassword) // 使用注入的密码
                .setDatabase(redisDatabase)
                .setConnectionPoolSize(20)
                .setConnectionMinimumIdleSize(5)
                .setIdleConnectionTimeout(10000)
                .setConnectTimeout(10000)
                .setTimeout(3000);

        return Redisson.create(config);
    }
}
