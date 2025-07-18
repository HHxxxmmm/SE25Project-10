package com.example.techprototype.Service.Impl;

import com.example.techprototype.Service.RedisService;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.test.context.ActiveProfiles;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.mockito.MockedConstruction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
class RedisServiceImplTest {
    @InjectMocks
    private RedisServiceImpl redisService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ListOperations<String, Object> listOperations;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    @Mock
    private WaitlistOrderRepository waitlistOrderRepository;

    @Mock
    private WaitlistItemRepository waitlistItemRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TicketRepository ticketRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
    }

    @Test
    void testGetStock() {
        when(valueOperations.get(anyString())).thenReturn(10);
        
        Optional<Integer> result = redisService.getStock(1, 2L, 3L, LocalDate.now(), 1);
        
        assertTrue(result.isPresent());
        assertEquals(10, result.get());
    }

    @Test
    void testGetStock_NullValue() {
        when(valueOperations.get(anyString())).thenReturn(null);
        
        Optional<Integer> result = redisService.getStock(1, 2L, 3L, LocalDate.now(), 1);
        
        assertFalse(result.isPresent());
    }

    @Test
    void testSetStock() {
        doNothing().when(valueOperations).set(anyString(), anyString());
        
        assertDoesNotThrow(() -> redisService.setStock(1, 2L, 3L, LocalDate.now(), 1, 10));
        
        verify(valueOperations).set(anyString(), eq("10"));
    }

    @Test
    void testDecrStock() {
        // Mock Lua脚本执行结果，返回1表示成功
        when(redisTemplate.execute(any(), anyList(), anyString())).thenReturn(1L);
        
        boolean result = redisService.decrStock(1, 2L, 3L, LocalDate.now(), 1, 1);
        
        assertTrue(result);
    }

    @Test
    void testDecrStock_NullResult() {
        // Mock Lua脚本执行结果，返回null
        when(redisTemplate.execute(any(), anyList(), anyString())).thenReturn(null);
        
        boolean result = redisService.decrStock(1, 2L, 3L, LocalDate.now(), 1, 1);
        
        assertFalse(result);
    }

    @Test
    void testDecrStock_InsufficientStock() {
        // Mock Lua脚本执行结果，返回0表示库存不足
        when(redisTemplate.execute(any(), anyList(), anyString())).thenReturn(0L);
        
        boolean result = redisService.decrStock(1, 2L, 3L, LocalDate.now(), 1, 1);
        
        assertFalse(result);
    }

    @Test
    void testDecrStock_KeyNotExists() {
        // Mock Lua脚本执行结果，返回-1表示key不存在
        when(redisTemplate.execute(any(), anyList(), anyString())).thenReturn(-1L);
        
        boolean result = redisService.decrStock(1, 2L, 3L, LocalDate.now(), 1, 1);
        
        assertFalse(result);
    }

    @Test
    void testDecrStock_InvalidQuantity() {
        // Mock Lua脚本执行结果，返回-2表示数量参数无效
        when(redisTemplate.execute(any(), anyList(), anyString())).thenReturn(-2L);
        
        boolean result = redisService.decrStock(1, 2L, 3L, LocalDate.now(), 1, 1);
        
        assertFalse(result);
    }

    @Test
    void testDecrStock_InvalidStockValue() {
        // Mock Lua脚本执行结果，返回-3表示库存值格式错误
        when(redisTemplate.execute(any(), anyList(), anyString())).thenReturn(-3L);
        
        boolean result = redisService.decrStock(1, 2L, 3L, LocalDate.now(), 1, 1);
        
        assertFalse(result);
    }

    @Test
    void testDecrStock_UnknownError() {
        // Mock Lua脚本执行结果，返回未知错误码
        when(redisTemplate.execute(any(), anyList(), anyString())).thenReturn(999L);
        
        boolean result = redisService.decrStock(1, 2L, 3L, LocalDate.now(), 1, 1);
        
        assertFalse(result);
    }

    @Test
    void testIncrStock() {
        // Mock Lua脚本执行结果，返回1表示成功
        when(redisTemplate.execute(any(), anyList(), anyString())).thenReturn(1L);
        
        boolean result = redisService.incrStock(1, 2L, 3L, LocalDate.now(), 1, 1);
        
        assertTrue(result);
    }

    @Test
    void testIncrStock_NullResult() {
        // Mock Lua脚本执行结果，返回null
        when(redisTemplate.execute(any(), anyList(), anyString())).thenReturn(null);
        
        boolean result = redisService.incrStock(1, 2L, 3L, LocalDate.now(), 1, 1);
        
        assertFalse(result);
    }

    @Test
    void testIncrStock_NonOneResult() {
        // Mock Lua脚本执行结果，返回非1值
        when(redisTemplate.execute(any(), anyList(), anyString())).thenReturn(0L);
        
        boolean result = redisService.incrStock(1, 2L, 3L, LocalDate.now(), 1, 1);
        
        assertFalse(result);
    }

    @Test
    void testTryLock() throws Exception {
        when(rLock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        
        boolean result = redisService.tryLock("test-lock", 10, 30);
        
        assertTrue(result);
        verify(rLock).tryLock(10, 30, java.util.concurrent.TimeUnit.SECONDS);
    }

    @Test
    void testTryLock_InterruptedException() throws Exception {
        when(rLock.tryLock(anyLong(), anyLong(), any())).thenThrow(new InterruptedException("Interrupted"));
        
        boolean result = redisService.tryLock("test-lock", 10, 30);
        
        assertFalse(result);
        verify(rLock).tryLock(10, 30, java.util.concurrent.TimeUnit.SECONDS);
    }

    @Test
    void testUnlock_HeldByCurrentThread() {
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        doNothing().when(rLock).unlock();
        
        redisService.unlock("test-lock");
        
        verify(rLock).isHeldByCurrentThread();
        verify(rLock).unlock();
    }

    @Test
    void testUnlock_NotHeldByCurrentThread() {
        when(rLock.isHeldByCurrentThread()).thenReturn(false);
        
        redisService.unlock("test-lock");
        
        verify(rLock).isHeldByCurrentThread();
        verify(rLock, never()).unlock();
    }

    @Test
    void testGenerateOrderNumber() {
        String orderNumber = redisService.generateOrderNumber();
        
        assertNotNull(orderNumber);
        assertFalse(orderNumber.isEmpty());
    }

    @Test
    void testSetChangeMapping() {
        doNothing().when(valueOperations).set(anyString(), anyString());
        
        redisService.setChangeMapping("mapping-key", "mapping-value");
        
        verify(valueOperations).set("mapping-key", "mapping-value");
    }

    @Test
    void testGetChangeMapping_Exists() {
        when(valueOperations.get("mapping-key")).thenReturn("mapping-value");
        
        Optional<String> result = redisService.getChangeMapping("mapping-key");
        
        assertTrue(result.isPresent());
        assertEquals("mapping-value", result.get());
    }

    @Test
    void testGetChangeMapping_NotExists() {
        when(valueOperations.get("mapping-key")).thenReturn(null);
        
        Optional<String> result = redisService.getChangeMapping("mapping-key");
        
        assertFalse(result.isPresent());
    }

    @Test
    void testDeleteChangeMapping() {
        when(redisTemplate.delete("mapping-key")).thenReturn(true);
        
        redisService.deleteChangeMapping("mapping-key");
        
        verify(redisTemplate).delete("mapping-key");
    }

    @Test
    void testCacheOrder() {
        Order order = new Order();
        order.setOrderNumber("TEST123");
        order.setUserId(1L);
        order.setOrderStatus((byte) 1);
        // 不设置LocalDateTime，避免序列化问题
        order.setTotalAmount(new BigDecimal("100.00"));
        
        doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any());
        
        assertDoesNotThrow(() -> redisService.cacheOrder(order));
        
        verify(valueOperations).set(eq("order:TEST123"), anyString(), eq(30L), eq(java.util.concurrent.TimeUnit.MINUTES));
    }

    @Test
    void testCacheOrder_JsonProcessingException() {
        Order order = new Order();
        order.setOrderNumber("TEST123");
        
        // 这里我们无法直接模拟JsonProcessingException，因为ObjectMapper是内部创建的
        // 但我们可以测试方法不会抛出异常
        doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any());
        
        assertDoesNotThrow(() -> redisService.cacheOrder(order));
    }

    @Test
    void testGetCachedOrder_Exists() {
        Order order = new Order();
        order.setOrderNumber("TEST123");
        order.setUserId(1L);
        order.setOrderStatus((byte) 1);
        order.setOrderTime(LocalDateTime.now());
        order.setTotalAmount(new BigDecimal("100.00"));
        
        String orderJson = "{\"orderNumber\":\"TEST123\",\"userId\":1,\"orderStatus\":1}";
        when(valueOperations.get("order:TEST123")).thenReturn(orderJson);
        
        Optional<Order> result = redisService.getCachedOrder("TEST123");
        
        assertTrue(result.isPresent());
        assertEquals("TEST123", result.get().getOrderNumber());
    }

    @Test
    void testGetCachedOrder_NotExists() {
        when(valueOperations.get("order:TEST123")).thenReturn(null);
        
        Optional<Order> result = redisService.getCachedOrder("TEST123");
        
        assertFalse(result.isPresent());
    }

    @Test
    void testGetCachedOrder_Exception() {
        when(valueOperations.get("order:TEST123")).thenReturn("invalid-json");
        
        Optional<Order> result = redisService.getCachedOrder("TEST123");
        
        assertFalse(result.isPresent());
    }

    @Test
    void testCacheTicket() {
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setOrderId(1L);
        ticket.setTrainId(1);
        ticket.setPassengerId(100L);
        ticket.setPrice(new BigDecimal("50.00"));
        
        doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any());
        when(listOperations.rightPush(anyString(), any())).thenReturn(1L);
        when(redisTemplate.expire(anyString(), anyLong(), any())).thenReturn(true);
        
        assertDoesNotThrow(() -> redisService.cacheTicket(ticket));
        
        verify(valueOperations).set(eq("ticket:1"), anyString(), eq(30L), eq(java.util.concurrent.TimeUnit.MINUTES));
        verify(listOperations).rightPush(eq("order_tickets:1"), eq(1L));
        verify(redisTemplate).expire(eq("order_tickets:1"), eq(30L), eq(java.util.concurrent.TimeUnit.MINUTES));
    }

    @Test
    void testCacheTicket_JsonProcessingException() {
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setOrderId(1L);

        try (MockedConstruction<ObjectMapper> mocked = mockConstruction(ObjectMapper.class,
                (mock, context) -> {
                    try {
                        when(mock.writeValueAsString(any(Ticket.class)))
                                .thenThrow(new JsonProcessingException("mock error") {});
                    } catch (JsonProcessingException e) {
                        // 不会发生
                    }
                })) {
            assertDoesNotThrow(() -> redisService.cacheTicket(ticket));
        }
    }

    @Test
    void testGetCachedTickets_EmptyList() {
        when(listOperations.range("order_tickets:1", 0, -1)).thenReturn(new ArrayList<>());
        
        List<Ticket> result = redisService.getCachedTickets(1L);
        
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetCachedTickets_NullList() {
        when(listOperations.range("order_tickets:1", 0, -1)).thenReturn(null);
        
        List<Ticket> result = redisService.getCachedTickets(1L);
        
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetCachedTickets_WithTickets() {
        List<Object> ticketIds = new ArrayList<>();
        ticketIds.add(1L);
        ticketIds.add(2L);
        
        Ticket ticket1 = new Ticket();
        ticket1.setTicketId(1L);
        ticket1.setOrderId(1L);
        ticket1.setTrainId(1);
        
        Ticket ticket2 = new Ticket();
        ticket2.setTicketId(2L);
        ticket2.setOrderId(1L);
        ticket2.setTrainId(1);
        
        String ticket1Json = "{\"ticketId\":1,\"orderId\":1,\"trainId\":1}";
        String ticket2Json = "{\"ticketId\":2,\"orderId\":1,\"trainId\":1}";
        
        when(listOperations.range("order_tickets:1", 0, -1)).thenReturn(ticketIds);
        when(valueOperations.get("ticket:1")).thenReturn(ticket1Json);
        when(valueOperations.get("ticket:2")).thenReturn(ticket2Json);
        
        List<Ticket> result = redisService.getCachedTickets(1L);
        
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getTicketId());
        assertEquals(2L, result.get(1).getTicketId());
    }

    @Test
    void testGetCachedTickets_SomeTicketsNotFound() {
        List<Object> ticketIds = new ArrayList<>();
        ticketIds.add(1L);
        ticketIds.add(2L);
        
        Ticket ticket1 = new Ticket();
        ticket1.setTicketId(1L);
        ticket1.setOrderId(1L);
        ticket1.setTrainId(1);
        
        String ticket1Json = "{\"ticketId\":1,\"orderId\":1,\"trainId\":1}";
        
        when(listOperations.range("order_tickets:1", 0, -1)).thenReturn(ticketIds);
        when(valueOperations.get("ticket:1")).thenReturn(ticket1Json);
        when(valueOperations.get("ticket:2")).thenReturn(null);
        
        List<Ticket> result = redisService.getCachedTickets(1L);
        
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getTicketId());
    }

    @Test
    void testGetCachedTickets_Exception() {
        when(listOperations.range("order_tickets:1", 0, -1)).thenThrow(new RuntimeException("Redis error"));
        
        List<Ticket> result = redisService.getCachedTickets(1L);
        
        assertTrue(result.isEmpty());
    }

    @Test
    void testDeleteCachedOrder() {
        when(redisTemplate.delete("order:TEST123")).thenReturn(true);
        
        redisService.deleteCachedOrder("TEST123");
        
        verify(redisTemplate).delete("order:TEST123");
    }

    @Test
    void testDeleteCachedTicket() {
        when(redisTemplate.delete("ticket:1")).thenReturn(true);
        
        redisService.deleteCachedTicket(1L);
        
        verify(redisTemplate).delete("ticket:1");
    }

    @Test
    void testTriggerWaitlistFulfillment_NoPendingOrders() {
        when(waitlistOrderRepository.findPendingFulfillmentOrders(any(LocalDateTime.class)))
            .thenReturn(new ArrayList<>());
        
        assertDoesNotThrow(() -> redisService.triggerWaitlistFulfillment(1, 2L, 3L, LocalDate.now(), 1));
        
        verify(waitlistOrderRepository).findPendingFulfillmentOrders(any(LocalDateTime.class));
    }

    @Test
    void testTriggerWaitlistFulfillment_WithPendingOrders() {
        WaitlistOrder order1 = new WaitlistOrder();
        order1.setWaitlistId(1L);
        order1.setUserId(1L);
        order1.setOrderTime(LocalDateTime.now());
        
        WaitlistOrder order2 = new WaitlistOrder();
        order2.setWaitlistId(2L);
        order2.setUserId(2L);
        order2.setOrderTime(LocalDateTime.now());
        
        List<WaitlistOrder> pendingOrders = new ArrayList<>();
        pendingOrders.add(order1);
        pendingOrders.add(order2);
        
        when(waitlistOrderRepository.findPendingFulfillmentOrders(any(LocalDateTime.class)))
            .thenReturn(pendingOrders);
        when(waitlistItemRepository.findPendingItemsByWaitlistId(1L)).thenReturn(new ArrayList<>());
        when(waitlistItemRepository.findPendingItemsByWaitlistId(2L)).thenReturn(new ArrayList<>());
        
        assertDoesNotThrow(() -> redisService.triggerWaitlistFulfillment(1, 2L, 3L, LocalDate.now(), 1));
        
        verify(waitlistOrderRepository).findPendingFulfillmentOrders(any(LocalDateTime.class));
        verify(waitlistItemRepository).findPendingItemsByWaitlistId(1L);
        verify(waitlistItemRepository).findPendingItemsByWaitlistId(2L);
    }

    @Test
    void testTriggerWaitlistFulfillment_Exception() {
        when(waitlistOrderRepository.findPendingFulfillmentOrders(any(LocalDateTime.class)))
            .thenThrow(new RuntimeException("Database error"));
        
        assertDoesNotThrow(() -> redisService.triggerWaitlistFulfillment(1, 2L, 3L, LocalDate.now(), 1));
        
        verify(waitlistOrderRepository).findPendingFulfillmentOrders(any(LocalDateTime.class));
    }

    @Test
    void testTriggerWaitlistFulfillment_OrderFulfilled() {
        WaitlistOrder order = new WaitlistOrder();
        order.setWaitlistId(1L);
        order.setUserId(1L);
        order.setOrderTime(LocalDateTime.now());
        
        WaitlistItem item = new WaitlistItem();
        item.setItemId(1L);
        item.setWaitlistId(1L);
        item.setTrainId(1);
        item.setDepartureStopId(2L);
        item.setArrivalStopId(3L);
        item.setTravelDate(LocalDate.now());
        item.setCarriageTypeId(1);
        item.setPassengerId(100L);
        item.setTicketType((byte) 1);
        item.setPrice(new BigDecimal("50.00"));
        item.setItemStatus((byte) WaitlistItemStatus.PENDING_FULFILLMENT.getCode());
        
        List<WaitlistOrder> pendingOrders = new ArrayList<>();
        pendingOrders.add(order);
        
        List<WaitlistItem> pendingItems = new ArrayList<>();
        pendingItems.add(item);
        
        when(waitlistOrderRepository.findPendingFulfillmentOrders(any(LocalDateTime.class)))
            .thenReturn(pendingOrders);
        when(waitlistItemRepository.findPendingItemsByWaitlistId(1L)).thenReturn(pendingItems);
        when(valueOperations.get(anyString())).thenReturn(10); // 有库存
        when(redisTemplate.execute(any(), anyList(), anyString())).thenReturn(1L); // 扣减库存成功
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order savedOrder = invocation.getArgument(0);
            savedOrder.setOrderId(1L);
            return savedOrder;
        });
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket savedTicket = invocation.getArgument(0);
            savedTicket.setTicketId(1L);
            return savedTicket;
        });
        when(waitlistItemRepository.save(any(WaitlistItem.class))).thenReturn(item);
        when(waitlistOrderRepository.save(any(WaitlistOrder.class))).thenReturn(order);
        
        assertDoesNotThrow(() -> redisService.triggerWaitlistFulfillment(1, 2L, 3L, LocalDate.now(), 1));
        
        verify(waitlistOrderRepository).findPendingFulfillmentOrders(any(LocalDateTime.class));
        verify(waitlistItemRepository).findPendingItemsByWaitlistId(1L);
        verify(orderRepository).save(any(Order.class));
        verify(ticketRepository).save(any(Ticket.class));
        verify(waitlistItemRepository).save(any(WaitlistItem.class));
        verify(waitlistOrderRepository).save(any(WaitlistOrder.class));
    }

    @Test
    void testTriggerWaitlistFulfillment_OrderNotFulfilled() {
        WaitlistOrder order = new WaitlistOrder();
        order.setWaitlistId(1L);
        order.setUserId(1L);
        order.setOrderTime(LocalDateTime.now());
        
        WaitlistItem item = new WaitlistItem();
        item.setItemId(1L);
        item.setWaitlistId(1L);
        item.setTrainId(1);
        item.setDepartureStopId(2L);
        item.setArrivalStopId(3L);
        item.setTravelDate(LocalDate.now());
        item.setCarriageTypeId(1);
        item.setPassengerId(100L);
        item.setTicketType((byte) 1);
        item.setPrice(new BigDecimal("50.00"));
        item.setItemStatus((byte) WaitlistItemStatus.PENDING_FULFILLMENT.getCode());
        
        List<WaitlistOrder> pendingOrders = new ArrayList<>();
        pendingOrders.add(order);
        
        List<WaitlistItem> pendingItems = new ArrayList<>();
        pendingItems.add(item);
        
        when(waitlistOrderRepository.findPendingFulfillmentOrders(any(LocalDateTime.class)))
            .thenReturn(pendingOrders);
        when(waitlistItemRepository.findPendingItemsByWaitlistId(1L)).thenReturn(pendingItems);
        when(valueOperations.get(anyString())).thenReturn(null); // 无库存
        
        assertDoesNotThrow(() -> redisService.triggerWaitlistFulfillment(1, 2L, 3L, LocalDate.now(), 1));
        
        verify(waitlistOrderRepository).findPendingFulfillmentOrders(any(LocalDateTime.class));
        verify(waitlistItemRepository).findPendingItemsByWaitlistId(1L);
        verify(orderRepository, never()).save(any(Order.class));
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void testTriggerWaitlistFulfillment_StockReductionFailed() {
        // 设置候补订单
        WaitlistOrder order = new WaitlistOrder();
        order.setWaitlistId(1L);
        order.setUserId(1L);
        order.setOrderTime(LocalDateTime.now());
        order.setOrderStatus((byte) WaitlistOrderStatus.PENDING_FULFILLMENT.getCode());

        // 设置候补订单项
        WaitlistItem item = new WaitlistItem();
        item.setWaitlistId(1L);
        item.setTrainId(1);
        item.setDepartureStopId(2L);
        item.setArrivalStopId(3L);
        item.setTravelDate(LocalDate.now());
        item.setCarriageTypeId(1);
        item.setPassengerId(1L);
        item.setTicketType((byte) 1);
        item.setPrice(new BigDecimal("50.00"));
        item.setItemStatus((byte) WaitlistItemStatus.PENDING_FULFILLMENT.getCode());

        List<WaitlistItem> items = List.of(item);

        // Mock仓库方法
        when(waitlistOrderRepository.findPendingFulfillmentOrders(any(LocalDateTime.class)))
                .thenReturn(List.of(order));
        when(waitlistItemRepository.findPendingItemsByWaitlistId(1L)).thenReturn(items);
        when(valueOperations.get(anyString())).thenReturn(1); // 有库存
        when(redisTemplate.execute(any(), anyList(), anyString())).thenReturn(0L); // 减库存失败

        // 执行方法
        redisService.triggerWaitlistFulfillment(1, 2L, 3L, LocalDate.now(), 1);

        // 验证调用
        verify(waitlistOrderRepository).findPendingFulfillmentOrders(any(LocalDateTime.class));
        verify(waitlistItemRepository).findPendingItemsByWaitlistId(1L);
    }

    @Test
    void testCheckAndFulfillWaitlistOrder_StockZero() {
        // 设置候补订单
        WaitlistOrder order = new WaitlistOrder();
        order.setWaitlistId(1L);
        order.setUserId(1L);
        order.setOrderTime(LocalDateTime.now());
        order.setOrderStatus((byte) WaitlistOrderStatus.PENDING_FULFILLMENT.getCode());

        // 设置候补订单项
        WaitlistItem item = new WaitlistItem();
        item.setWaitlistId(1L);
        item.setTrainId(1);
        item.setDepartureStopId(2L);
        item.setArrivalStopId(3L);
        item.setTravelDate(LocalDate.now());
        item.setCarriageTypeId(1);
        item.setPassengerId(1L);
        item.setTicketType((byte) 1);
        item.setPrice(new BigDecimal("50.00"));
        item.setItemStatus((byte) WaitlistItemStatus.PENDING_FULFILLMENT.getCode());

        List<WaitlistItem> items = List.of(item);

        // Mock仓库方法
        when(waitlistItemRepository.findPendingItemsByWaitlistId(1L)).thenReturn(items);
        when(valueOperations.get(anyString())).thenReturn(0); // 库存为0

        // 使用反射调用私有方法
        try {
            var method = RedisServiceImpl.class.getDeclaredMethod("checkAndFulfillWaitlistOrder", WaitlistOrder.class);
            method.setAccessible(true);
            boolean result = (boolean) method.invoke(redisService, order);
            
            assertFalse(result);
        } catch (Exception e) {
            fail("反射调用失败: " + e.getMessage());
        }
    }

    @Test
    void testCheckAndFulfillWaitlistOrder_Exception() {
        // 设置候补订单
        WaitlistOrder order = new WaitlistOrder();
        order.setWaitlistId(1L);
        order.setUserId(1L);
        order.setOrderTime(LocalDateTime.now());
        order.setOrderStatus((byte) WaitlistOrderStatus.PENDING_FULFILLMENT.getCode());

        // Mock仓库方法抛出异常
        when(waitlistItemRepository.findPendingItemsByWaitlistId(1L))
                .thenThrow(new RuntimeException("Database error"));

        // 使用反射调用私有方法
        try {
            var method = RedisServiceImpl.class.getDeclaredMethod("checkAndFulfillWaitlistOrder", WaitlistOrder.class);
            method.setAccessible(true);
            boolean result = (boolean) method.invoke(redisService, order);
            
            assertFalse(result);
        } catch (Exception e) {
            fail("反射调用失败: " + e.getMessage());
        }
    }

    @Test
    void testCreateCompleteOrderFromWaitlist_Exception() {
        // 设置候补订单
        WaitlistOrder order = new WaitlistOrder();
        order.setWaitlistId(1L);
        order.setUserId(1L);
        order.setOrderTime(LocalDateTime.now());
        order.setOrderStatus((byte) WaitlistOrderStatus.PENDING_FULFILLMENT.getCode());

        // 设置候补订单项
        WaitlistItem item = new WaitlistItem();
        item.setWaitlistId(1L);
        item.setTrainId(1);
        item.setDepartureStopId(2L);
        item.setArrivalStopId(3L);
        item.setTravelDate(LocalDate.now());
        item.setCarriageTypeId(1);
        item.setPassengerId(1L);
        item.setTicketType((byte) 1);
        item.setPrice(new BigDecimal("50.00"));
        item.setItemStatus((byte) WaitlistItemStatus.PENDING_FULFILLMENT.getCode());

        List<WaitlistItem> items = List.of(item);

        // Mock仓库方法抛出异常
        when(orderRepository.save(any(Order.class)))
                .thenThrow(new RuntimeException("Database error"));

        // 使用反射调用私有方法
        try {
            var method = RedisServiceImpl.class.getDeclaredMethod("createCompleteOrderFromWaitlist", WaitlistOrder.class, List.class);
            method.setAccessible(true);
            
            assertDoesNotThrow(() -> method.invoke(redisService, order, items));
        } catch (Exception e) {
            fail("反射调用失败: " + e.getMessage());
        }
    }
} 