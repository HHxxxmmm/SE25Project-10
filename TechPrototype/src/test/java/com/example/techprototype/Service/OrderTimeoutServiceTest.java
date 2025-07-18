package com.example.techprototype.Service;

import com.example.techprototype.Entity.Order;
import com.example.techprototype.Entity.Ticket;
import com.example.techprototype.Entity.WaitlistOrder;
import com.example.techprototype.Entity.WaitlistItem;
import com.example.techprototype.Repository.OrderRepository;
import com.example.techprototype.Repository.TicketRepository;
import com.example.techprototype.Repository.WaitlistOrderRepository;
import com.example.techprototype.Repository.WaitlistItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderTimeoutServiceTest {
    @InjectMocks
    private OrderTimeoutService orderTimeoutService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private SeatService seatService;

    @Mock
    private RedisService redisService;

    @Mock
    private WaitlistOrderRepository waitlistOrderRepository;

    @Mock
    private WaitlistItemRepository waitlistItemRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testHandleTimeoutOrders_NoTimeoutOrders() {
        when(orderRepository.findTimeoutOrders(any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> orderTimeoutService.handleTimeoutOrders());
        
        verify(orderRepository).findTimeoutOrders(any(LocalDateTime.class));
        verify(ticketRepository, never()).findByOrderId(anyLong());
    }

    @Test
    void testHandleTimeoutOrders_WithTimeoutOrders() {
        // 创建超时订单
        Order timeoutOrder = new Order();
        timeoutOrder.setOrderId(1L);
        timeoutOrder.setOrderNumber("TEST001");
        
        // 创建车票
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setTrainId(1);
        ticket.setDepartureStopId(1L);
        ticket.setArrivalStopId(2L);
        ticket.setTravelDate(java.time.LocalDate.now().plusDays(1));
        ticket.setCarriageTypeId(1);
        ticket.setSeatNumber("1A");
        ticket.setCarriageNumber("1");
        
        when(orderRepository.findTimeoutOrders(any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(timeoutOrder));
        when(ticketRepository.findByOrderId(1L))
            .thenReturn(Arrays.asList(ticket));
        when(orderRepository.save(any(Order.class))).thenReturn(timeoutOrder);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);

        assertDoesNotThrow(() -> orderTimeoutService.handleTimeoutOrders());
        
        verify(orderRepository).findTimeoutOrders(any(LocalDateTime.class));
        verify(ticketRepository).findByOrderId(1L);
        verify(orderRepository).save(any(Order.class));
        verify(ticketRepository).save(any(Ticket.class));
        verify(seatService).releaseSeat(any(Ticket.class));
        verify(redisService).incrStock(anyInt(), anyLong(), anyLong(), any(java.time.LocalDate.class), anyInt(), eq(1));
    }

    @Test
    void testHandleTimeoutOrders_OrderProcessingException() {
        Order timeoutOrder = new Order();
        timeoutOrder.setOrderId(1L);
        timeoutOrder.setOrderNumber("TEST001");
        
        when(orderRepository.findTimeoutOrders(any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(timeoutOrder));
        when(ticketRepository.findByOrderId(1L))
            .thenThrow(new RuntimeException("Ticket processing error"));

        assertDoesNotThrow(() -> orderTimeoutService.handleTimeoutOrders());
        
        verify(orderRepository).findTimeoutOrders(any(LocalDateTime.class));
        verify(ticketRepository).findByOrderId(1L);
    }

    @Test
    void testHandleTimeoutOrders_ExceptionHandling() {
        when(orderRepository.findTimeoutOrders(any(LocalDateTime.class)))
            .thenThrow(new RuntimeException("Database error"));

        assertDoesNotThrow(() -> orderTimeoutService.handleTimeoutOrders());
        
        verify(orderRepository).findTimeoutOrders(any(LocalDateTime.class));
    }

    @Test
    void testHandleTimeoutOrder_WithNullSeatInfo() {
        // 创建超时订单
        Order timeoutOrder = new Order();
        timeoutOrder.setOrderId(1L);
        timeoutOrder.setOrderNumber("TEST001");
        
        // 创建没有座位信息的车票
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setTrainId(1);
        ticket.setDepartureStopId(1L);
        ticket.setArrivalStopId(2L);
        ticket.setTravelDate(java.time.LocalDate.now().plusDays(1));
        ticket.setCarriageTypeId(1);
        // 座位号和车厢号为null
        ticket.setSeatNumber(null);
        ticket.setCarriageNumber(null);
        
        when(orderRepository.findTimeoutOrders(any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(timeoutOrder));
        when(ticketRepository.findByOrderId(1L))
            .thenReturn(Arrays.asList(ticket));
        when(orderRepository.save(any(Order.class))).thenReturn(timeoutOrder);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);

        assertDoesNotThrow(() -> orderTimeoutService.handleTimeoutOrders());
        
        verify(orderRepository).findTimeoutOrders(any(LocalDateTime.class));
        verify(ticketRepository).findByOrderId(1L);
        verify(orderRepository).save(any(Order.class));
        verify(ticketRepository).save(any(Ticket.class));
        // 不应该调用releaseSeat，因为座位信息为null
        verify(seatService, never()).releaseSeat(any(Ticket.class));
        verify(redisService).incrStock(anyInt(), anyLong(), anyLong(), any(java.time.LocalDate.class), anyInt(), eq(1));
    }

    @Test
    void testHandleTimeoutOrder_WithNullSeatNumber() {
        // 创建超时订单
        Order timeoutOrder = new Order();
        timeoutOrder.setOrderId(1L);
        timeoutOrder.setOrderNumber("TEST001");
        
        // 创建只有车厢号没有座位号的车票
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setTrainId(1);
        ticket.setDepartureStopId(1L);
        ticket.setArrivalStopId(2L);
        ticket.setTravelDate(java.time.LocalDate.now().plusDays(1));
        ticket.setCarriageTypeId(1);
        ticket.setSeatNumber(null); // 座位号为null
        ticket.setCarriageNumber("1"); // 车厢号不为null
        
        when(orderRepository.findTimeoutOrders(any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(timeoutOrder));
        when(ticketRepository.findByOrderId(1L))
            .thenReturn(Arrays.asList(ticket));
        when(orderRepository.save(any(Order.class))).thenReturn(timeoutOrder);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);

        assertDoesNotThrow(() -> orderTimeoutService.handleTimeoutOrders());
        
        verify(orderRepository).findTimeoutOrders(any(LocalDateTime.class));
        verify(ticketRepository).findByOrderId(1L);
        verify(orderRepository).save(any(Order.class));
        verify(ticketRepository).save(any(Ticket.class));
        // 不应该调用releaseSeat，因为座位号为null
        verify(seatService, never()).releaseSeat(any(Ticket.class));
        verify(redisService).incrStock(anyInt(), anyLong(), anyLong(), any(java.time.LocalDate.class), anyInt(), eq(1));
    }

    @Test
    void testHandleTimeoutOrder_WithNullCarriageNumber() {
        // 创建超时订单
        Order timeoutOrder = new Order();
        timeoutOrder.setOrderId(1L);
        timeoutOrder.setOrderNumber("TEST001");
        
        // 创建只有座位号没有车厢号的车票
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setTrainId(1);
        ticket.setDepartureStopId(1L);
        ticket.setArrivalStopId(2L);
        ticket.setTravelDate(java.time.LocalDate.now().plusDays(1));
        ticket.setCarriageTypeId(1);
        ticket.setSeatNumber("1A"); // 座位号不为null
        ticket.setCarriageNumber(null); // 车厢号为null
        
        when(orderRepository.findTimeoutOrders(any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(timeoutOrder));
        when(ticketRepository.findByOrderId(1L))
            .thenReturn(Arrays.asList(ticket));
        when(orderRepository.save(any(Order.class))).thenReturn(timeoutOrder);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);

        assertDoesNotThrow(() -> orderTimeoutService.handleTimeoutOrders());
        
        verify(orderRepository).findTimeoutOrders(any(LocalDateTime.class));
        verify(ticketRepository).findByOrderId(1L);
        verify(orderRepository).save(any(Order.class));
        verify(ticketRepository).save(any(Ticket.class));
        // 不应该调用releaseSeat，因为车厢号为null
        verify(seatService, never()).releaseSeat(any(Ticket.class));
        verify(redisService).incrStock(anyInt(), anyLong(), anyLong(), any(java.time.LocalDate.class), anyInt(), eq(1));
    }

    @Test
    void testProcessWaitlistFulfillment() throws Exception {
        // 使用反射调用私有方法
        java.lang.reflect.Method method = OrderTimeoutService.class.getDeclaredMethod("processWaitlistFulfillment");
        method.setAccessible(true);
        
        assertDoesNotThrow(() -> method.invoke(orderTimeoutService));
    }

    @Test
    void testFulfillWaitlistItem_Success() throws Exception {
        // 创建候补订单项和候补订单
        WaitlistItem item = new WaitlistItem();
        item.setItemId(1L);
        item.setWaitlistId(1L);
        
        WaitlistOrder order = new WaitlistOrder();
        order.setWaitlistId(1L);
        
        when(waitlistItemRepository.save(any(WaitlistItem.class))).thenReturn(item);
        when(waitlistItemRepository.findPendingItemsByWaitlistId(1L))
            .thenReturn(Collections.emptyList()); // 没有剩余项，表示全部兑现
        when(waitlistOrderRepository.save(any(WaitlistOrder.class))).thenReturn(order);
        
        // 使用反射调用私有方法
        java.lang.reflect.Method method = OrderTimeoutService.class.getDeclaredMethod("fulfillWaitlistItem", WaitlistItem.class, WaitlistOrder.class);
        method.setAccessible(true);
        
        Boolean result = (Boolean) method.invoke(orderTimeoutService, item, order);
        
        assertTrue(result);
        verify(waitlistItemRepository).save(any(WaitlistItem.class));
        verify(waitlistItemRepository).findPendingItemsByWaitlistId(1L);
        verify(waitlistOrderRepository).save(any(WaitlistOrder.class));
    }

    @Test
    void testFulfillWaitlistItem_WithRemainingItems() throws Exception {
        // 创建候补订单项和候补订单
        WaitlistItem item = new WaitlistItem();
        item.setItemId(1L);
        item.setWaitlistId(1L);
        
        WaitlistOrder order = new WaitlistOrder();
        order.setWaitlistId(1L);
        
        // 还有剩余项未兑现
        WaitlistItem remainingItem = new WaitlistItem();
        remainingItem.setItemId(2L);
        remainingItem.setWaitlistId(1L);
        
        when(waitlistItemRepository.save(any(WaitlistItem.class))).thenReturn(item);
        when(waitlistItemRepository.findPendingItemsByWaitlistId(1L))
            .thenReturn(Arrays.asList(remainingItem)); // 还有剩余项
        when(waitlistOrderRepository.save(any(WaitlistOrder.class))).thenReturn(order);
        
        // 使用反射调用私有方法
        java.lang.reflect.Method method = OrderTimeoutService.class.getDeclaredMethod("fulfillWaitlistItem", WaitlistItem.class, WaitlistOrder.class);
        method.setAccessible(true);
        
        Boolean result = (Boolean) method.invoke(orderTimeoutService, item, order);
        
        assertTrue(result);
        verify(waitlistItemRepository).save(any(WaitlistItem.class));
        verify(waitlistItemRepository).findPendingItemsByWaitlistId(1L);
        // 不应该更新候补订单状态，因为还有剩余项
        verify(waitlistOrderRepository, never()).save(any(WaitlistOrder.class));
    }

    @Test
    void testFulfillWaitlistItem_Exception() throws Exception {
        // 创建候补订单项和候补订单
        WaitlistItem item = new WaitlistItem();
        item.setItemId(1L);
        item.setWaitlistId(1L);
        
        WaitlistOrder order = new WaitlistOrder();
        order.setWaitlistId(1L);
        
        when(waitlistItemRepository.save(any(WaitlistItem.class)))
            .thenThrow(new RuntimeException("Database error"));
        
        // 使用反射调用私有方法
        java.lang.reflect.Method method = OrderTimeoutService.class.getDeclaredMethod("fulfillWaitlistItem", WaitlistItem.class, WaitlistOrder.class);
        method.setAccessible(true);
        
        Boolean result = (Boolean) method.invoke(orderTimeoutService, item, order);
        
        assertFalse(result);
        verify(waitlistItemRepository).save(any(WaitlistItem.class));
    }
} 