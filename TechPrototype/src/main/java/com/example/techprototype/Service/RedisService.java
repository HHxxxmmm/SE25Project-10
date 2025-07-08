package com.example.techprototype.Service;

import java.time.LocalDate;
import java.util.Optional;
import com.example.techprototype.Entity.Order;
import com.example.techprototype.Entity.Ticket;

public interface RedisService {
    
    /**
     * 预减库存（原子操作）
     */
    boolean decrStock(Integer trainId, Long departureStopId, Long arrivalStopId, LocalDate travelDate, Integer carriageTypeId, int quantity);
    
    /**
     * 增加库存（原子操作）
     */
    boolean incrStock(Integer trainId, Long departureStopId, Long arrivalStopId, LocalDate travelDate, Integer carriageTypeId, int quantity);
    
    /**
     * 获取库存
     */
    Optional<Integer> getStock(Integer trainId, Long departureStopId, Long arrivalStopId, LocalDate travelDate, Integer carriageTypeId);
    
    /**
     * 设置库存
     */
    void setStock(Integer trainId, Long departureStopId, Long arrivalStopId, LocalDate travelDate, Integer carriageTypeId, int quantity);
    
    /**
     * 获取分布式锁
     */
    boolean tryLock(String lockKey, long waitTime, long leaseTime);
    
    /**
     * 释放分布式锁
     */
    void unlock(String lockKey);
    
    /**
     * 生成订单号
     */
    String generateOrderNumber();
    
    /**
     * 设置改签配对关系
     */
    void setChangeMapping(String mappingKey, String mappingValue);
    
    /**
     * 获取改签配对关系
     */
    Optional<String> getChangeMapping(String mappingKey);
    
    /**
     * 删除改签配对关系
     */
    void deleteChangeMapping(String mappingKey);
    
    /**
     * 缓存订单到Redis
     */
    void cacheOrder(Order order);
    
    /**
     * 从Redis获取订单
     */
    Optional<Order> getCachedOrder(String orderNumber);
    
    /**
     * 缓存车票到Redis
     */
    void cacheTicket(Ticket ticket);
    
    /**
     * 从Redis获取订单下的车票
     */
    java.util.List<Ticket> getCachedTickets(Long orderId);
    
    /**
     * 从Redis删除订单缓存
     */
    void deleteCachedOrder(String orderNumber);
    
    /**
     * 从Redis删除车票缓存
     */
    void deleteCachedTicket(Long ticketId);
} 