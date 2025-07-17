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
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import com.example.techprototype.Entity.Order;
import com.example.techprototype.Entity.Ticket;
import com.example.techprototype.Entity.WaitlistOrder;
import com.example.techprototype.Entity.WaitlistItem;
import com.example.techprototype.Enums.WaitlistOrderStatus;
import com.example.techprototype.Enums.WaitlistItemStatus;
import com.example.techprototype.Enums.OrderStatus;
import com.example.techprototype.Enums.TicketStatus;
import com.example.techprototype.Repository.WaitlistOrderRepository;
import com.example.techprototype.Repository.WaitlistItemRepository;
import com.example.techprototype.Repository.OrderRepository;
import com.example.techprototype.Repository.TicketRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Async;
import java.math.BigDecimal;

@Service
public class RedisServiceImpl implements RedisService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private RedissonClient redissonClient;
    
    @Autowired
    private WaitlistOrderRepository waitlistOrderRepository;
    
    @Autowired
    private WaitlistItemRepository waitlistItemRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private TicketRepository ticketRepository;
    
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
        
        boolean success = result != null && result == 1;
        
        // 如果库存增加成功，触发候补订单兑现检查
        if (success) {
            System.out.println("库存回滚成功，触发候补订单兑现检查: " + key);
            // 异步触发候补订单兑现检查
            triggerWaitlistFulfillment(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId);
        }
        
        return success;
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
    
    /**
     * 触发候补订单兑现检查
     * 当库存回滚时，检查是否有对应的候补订单可以兑现
     */
    @Async
    public void triggerWaitlistFulfillment(Integer trainId, Long departureStopId, Long arrivalStopId, 
                                         LocalDate travelDate, Integer carriageTypeId) {
        try {
            System.out.println("开始检查候补订单兑现: 车次" + trainId + ", 席别" + carriageTypeId);
            
            // 查询待兑现的候补订单（按创建时间升序，早的优先）
            List<WaitlistOrder> pendingOrders = waitlistOrderRepository.findPendingFulfillmentOrders(LocalDateTime.now());
            
            // 按时间顺序处理候补订单，确保先创建的优先兑现
            for (WaitlistOrder order : pendingOrders) {
                // 检查这个候补订单是否可以完全兑现
                boolean fulfilled = checkAndFulfillWaitlistOrder(order);
                if (fulfilled) {
                    System.out.println("候补订单兑现成功: " + order.getWaitlistId() + ", 创建时间: " + order.getOrderTime());
                    // 如果这个候补订单兑现成功，继续检查下一个
                    continue;
                } else {
                    System.out.println("候补订单暂无法兑现: " + order.getWaitlistId() + ", 创建时间: " + order.getOrderTime());
                    // 如果这个候补订单无法兑现，继续检查下一个（可能有其他席别的库存）
                }
            }
        } catch (Exception e) {
            System.err.println("候补订单兑现检查失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查并兑现候补订单
     * 只有当候补订单的所有项都有库存时，才创建完整订单
     * @return true 如果候补订单兑现成功，false 如果无法兑现
     */
    private boolean checkAndFulfillWaitlistOrder(WaitlistOrder order) {
        try {
            List<WaitlistItem> pendingItems = waitlistItemRepository.findPendingItemsByWaitlistId(order.getWaitlistId());
            
            // 检查所有待处理项是否都有库存
            boolean allItemsAvailable = true;
            for (WaitlistItem item : pendingItems) {
                Optional<Integer> stock = getStock(item.getTrainId(), item.getDepartureStopId(), 
                        item.getArrivalStopId(), item.getTravelDate(), item.getCarriageTypeId());
                if (stock.isEmpty() || stock.get() <= 0) {
                    allItemsAvailable = false;
                    break;
                }
            }
            
            // 如果所有项都有库存，创建完整订单
            if (allItemsAvailable) {
                System.out.println("候补订单所有项都有库存，开始创建完整订单: " + order.getWaitlistId());
                createCompleteOrderFromWaitlist(order, pendingItems);
                return true; // 兑现成功
            }
            
            return false; // 无法兑现
        } catch (Exception e) {
            System.err.println("检查候补订单兑现失败: " + e.getMessage());
            return false; // 异常情况，无法兑现
        }
    }
    
    /**
     * 从候补订单创建完整订单
     * 只有当候补订单的所有项都有库存时，才创建完整订单
     */
    private void createCompleteOrderFromWaitlist(WaitlistOrder order, List<WaitlistItem> items) {
        try {
            // 1. 创建正式订单
            Order newOrder = new Order();
            newOrder.setOrderNumber(generateOrderNumber());
            newOrder.setUserId(order.getUserId());
            newOrder.setOrderStatus((byte) OrderStatus.PAID.getCode()); // 已支付状态
            newOrder.setOrderTime(LocalDateTime.now());
            newOrder.setPaymentTime(LocalDateTime.now()); // 设置支付时间
            newOrder.setPaymentMethod("候补订单兑现"); // 设置支付方式
            
            // 计算总金额
            BigDecimal totalAmount = items.stream()
                    .map(WaitlistItem::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            newOrder.setTotalAmount(totalAmount);
            newOrder.setTicketCount(items.size()); // 设置票数为候补项数量
            
            orderRepository.save(newOrder);
            
            // 2. 为每个候补项创建车票记录
            for (WaitlistItem item : items) {
                Ticket ticket = new Ticket();
                ticket.setOrderId(newOrder.getOrderId());
                ticket.setTrainId(item.getTrainId());
                ticket.setDepartureStopId(item.getDepartureStopId());
                ticket.setArrivalStopId(item.getArrivalStopId());
                ticket.setTravelDate(item.getTravelDate());
                ticket.setCarriageTypeId(item.getCarriageTypeId());
                ticket.setPassengerId(item.getPassengerId());
                ticket.setTicketType(item.getTicketType());
                ticket.setTicketStatus((byte) TicketStatus.UNUSED.getCode());
                ticket.setTicketNumber("T" + System.currentTimeMillis() + (int)(Math.random() * 1000));
                ticket.setCreatedTime(LocalDateTime.now());
                ticket.setPrice(item.getPrice());
                
                ticketRepository.save(ticket);
                
                // 3. 扣减Redis库存
                boolean stockReduced = decrStock(item.getTrainId(), item.getDepartureStopId(), 
                        item.getArrivalStopId(), item.getTravelDate(), item.getCarriageTypeId(), 1);
                if (stockReduced) {
                    System.out.println("候补订单兑现时扣减库存成功: 车次" + item.getTrainId() + 
                                     ", 席别" + item.getCarriageTypeId() + ", 数量-1");
                } else {
                    System.err.println("候补订单兑现时扣减库存失败: 车次" + item.getTrainId() + 
                                     ", 席别" + item.getCarriageTypeId());
                }
                
                // 4. 更新候补订单项状态
                item.setItemStatus((byte) WaitlistItemStatus.FULFILLED.getCode());
                waitlistItemRepository.save(item);
            }
            
            // 5. 更新候补订单状态为已兑现
            order.setOrderStatus((byte) WaitlistOrderStatus.FULFILLED.getCode());
            waitlistOrderRepository.save(order);
            
            System.out.println("候补订单完整兑现完成: 订单" + newOrder.getOrderNumber() + 
                             ", 包含" + items.size() + "张车票, 总金额" + totalAmount);
            
        } catch (Exception e) {
            System.err.println("创建完整订单失败: " + e.getMessage());
        }
    }
} 