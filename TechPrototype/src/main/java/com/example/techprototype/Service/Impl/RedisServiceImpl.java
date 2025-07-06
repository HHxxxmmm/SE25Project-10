package com.example.techprototype.Service.Impl;

import com.example.techprototype.Service.RedisService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import com.example.techprototype.Entity.Order;
import com.example.techprototype.Entity.Ticket;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class RedisServiceImpl implements RedisService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private RedissonClient redissonClient;
    
    private final Snowflake snowflake = IdUtil.getSnowflake(1, 1);
    
    // Lua脚本：原子减库存
    private static final String DECR_STOCK_SCRIPT = 
        "local key = KEYS[1] " +
        "local quantity_str = ARGV[1] " +
        "local quantity = tonumber(quantity_str) " +
        "if quantity == nil then " +
        "    return -2 " +
        "end " +
        "local current = redis.call('get', key) " +
        "if current == nil then " +
        "    return -1 " +
        "end " +
        "current = tonumber(current) " +
        "if current == nil then " +
        "    return -3 " +
        "end " +
        "if current >= quantity then " +
        "    redis.call('decrby', key, quantity) " +
        "    return 1 " +
        "else " +
        "    return 0 " +
        "end";
    
    // Lua脚本：原子加库存
    private static final String INCR_STOCK_SCRIPT = 
        "local key = KEYS[1] " +
        "local quantity_str = ARGV[1] " +
        "local quantity = tonumber(quantity_str) " +
        "if quantity == nil then " +
        "    return -1 " +
        "end " +
        "redis.call('incrby', key, quantity) " +
        "return 1";
    
    @Override
    public boolean decrStock(Integer trainId, Long departureStopId, Long arrivalStopId, LocalDate travelDate, Integer carriageTypeId, int quantity) {
        String key = buildStockKey(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId);
        System.out.println("尝试减库存，key: " + key + ", quantity: " + quantity);
        
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(DECR_STOCK_SCRIPT);
        script.setResultType(Long.class);
        
        Long result = redisTemplate.execute(script, Arrays.asList(key), String.valueOf(quantity));
        System.out.println("减库存结果: " + result);
        
        if (result == null) {
            System.err.println("Lua脚本执行失败，key: " + key);
            return false;
        }
        
        switch (result.intValue()) {
            case 1: return true;  // 成功
            case 0: return false; // 库存不足
            case -1: 
                System.err.println("库存key不存在: " + key);
                return false;
            case -2: 
                System.err.println("数量参数无效: " + quantity);
                return false;
            case -3: 
                System.err.println("库存值格式错误: " + key);
                return false;
            default:
                System.err.println("未知错误码: " + result);
                return false;
        }
    }
    
    @Override
    public boolean incrStock(Integer trainId, Long departureStopId, Long arrivalStopId, LocalDate travelDate, Integer carriageTypeId, int quantity) {
        String key = buildStockKey(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId);
        System.out.println("尝试加库存，key: " + key + ", quantity: " + quantity);
        
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(INCR_STOCK_SCRIPT);
        script.setResultType(Long.class);
        
        Long result = redisTemplate.execute(script, Arrays.asList(key), String.valueOf(quantity));
        System.out.println("加库存结果: " + result);
        
        return result != null && result == 1;
    }
    
    @Override
    public Optional<Integer> getStock(Integer trainId, Long departureStopId, Long arrivalStopId, LocalDate travelDate, Integer carriageTypeId) {
        String key = buildStockKey(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId);
        Object value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            return Optional.of(Integer.valueOf(value.toString()));
        }
        return Optional.empty();
    }
    
    @Override
    public void setStock(Integer trainId, Long departureStopId, Long arrivalStopId, LocalDate travelDate, Integer carriageTypeId, int quantity) {
        String key = buildStockKey(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId);
        System.out.println("设置库存，key: " + key + ", quantity: " + quantity);
        redisTemplate.opsForValue().set(key, String.valueOf(quantity));
    }
    
    @Override
    public boolean tryLock(String lockKey, long waitTime, long leaseTime) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            return lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    @Override
    public void unlock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
    
    @Override
    public String generateOrderNumber() {
        return String.valueOf(snowflake.nextId());
    }
    
    @Override
    public void setChangeMapping(String mappingKey, String mappingValue) {
        redisTemplate.opsForValue().set(mappingKey, mappingValue);
        System.out.println("设置改签配对: " + mappingKey + " -> " + mappingValue);
    }
    
    @Override
    public Optional<String> getChangeMapping(String mappingKey) {
        Object value = redisTemplate.opsForValue().get(mappingKey);
        if (value != null) {
            return Optional.of(value.toString());
        }
        return Optional.empty();
    }
    
    @Override
    public void deleteChangeMapping(String mappingKey) {
        redisTemplate.delete(mappingKey);
        System.out.println("删除改签配对: " + mappingKey);
    }
    
    private String buildStockKey(Integer trainId, Long departureStopId, Long arrivalStopId, LocalDate travelDate, Integer carriageTypeId) {
        return String.format("stock:%d:%d:%d:%s:%d", trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId);
    }
    
    @Override
    public void cacheOrder(Order order) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String orderJson = mapper.writeValueAsString(order);
            String key = "order:" + order.getOrderNumber();
            redisTemplate.opsForValue().set(key, orderJson, 30, TimeUnit.MINUTES); // 30分钟过期
            System.out.println("订单已缓存到Redis: " + order.getOrderNumber());
        } catch (JsonProcessingException e) {
            System.err.println("缓存订单失败: " + e.getMessage());
        }
    }
    
    @Override
    public Optional<Order> getCachedOrder(String orderNumber) {
        try {
            String key = "order:" + orderNumber;
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                ObjectMapper mapper = new ObjectMapper();
                Order order = mapper.readValue(value.toString(), Order.class);
                return Optional.of(order);
            }
        } catch (Exception e) {
            System.err.println("从Redis获取订单失败: " + e.getMessage());
        }
        return Optional.empty();
    }
    
    @Override
    public void cacheTicket(Ticket ticket) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String ticketJson = mapper.writeValueAsString(ticket);
            String key = "ticket:" + ticket.getTicketId();
            redisTemplate.opsForValue().set(key, ticketJson, 30, TimeUnit.MINUTES); // 30分钟过期
            
            // 同时缓存到订单车票列表
            String orderTicketsKey = "order_tickets:" + ticket.getOrderId();
            redisTemplate.opsForList().rightPush(orderTicketsKey, ticket.getTicketId());
            redisTemplate.expire(orderTicketsKey, 30, TimeUnit.MINUTES);
            
            System.out.println("车票已缓存到Redis: " + ticket.getTicketId());
        } catch (JsonProcessingException e) {
            System.err.println("缓存车票失败: " + e.getMessage());
        }
    }
    
    @Override
    public List<Ticket> getCachedTickets(Long orderId) {
        try {
            String orderTicketsKey = "order_tickets:" + orderId;
            List<Object> ticketIds = redisTemplate.opsForList().range(orderTicketsKey, 0, -1);
            if (ticketIds == null || ticketIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<Ticket> tickets = new ArrayList<>();
            ObjectMapper mapper = new ObjectMapper();
            
            for (Object ticketIdObj : ticketIds) {
                String ticketKey = "ticket:" + ticketIdObj;
                Object ticketValue = redisTemplate.opsForValue().get(ticketKey);
                if (ticketValue != null) {
                    Ticket ticket = mapper.readValue(ticketValue.toString(), Ticket.class);
                    tickets.add(ticket);
                }
            }
            
            return tickets;
        } catch (Exception e) {
            System.err.println("从Redis获取车票失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    @Override
    public void deleteCachedOrder(String orderNumber) {
        String key = "order:" + orderNumber;
        redisTemplate.delete(key);
        System.out.println("已删除订单缓存: " + orderNumber);
    }
    
    @Override
    public void deleteCachedTicket(Long ticketId) {
        String key = "ticket:" + ticketId;
        redisTemplate.delete(key);
        System.out.println("已删除车票缓存: " + ticketId);
    }
} 