package com.example.techprototype.Service.Impl;

import com.example.techprototype.DTO.BookingResponse;
import com.example.techprototype.Entity.Order;
import com.example.techprototype.Entity.Ticket;
import com.example.techprototype.Enums.OrderStatus;
import com.example.techprototype.Enums.TicketStatus;
import com.example.techprototype.Repository.OrderRepository;
import com.example.techprototype.Repository.TicketRepository;
import com.example.techprototype.Service.RedisService;
import com.example.techprototype.Service.SeatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentServiceImplTest {
    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private RedisService redisService;
    @Mock
    private SeatService seatService;

    private Order order;
    private Ticket ticket;
    private List<Ticket> ticketList;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        order = new Order();
        order.setOrderId(1L);
        order.setUserId(2L);
        order.setOrderStatus((byte) OrderStatus.PENDING_PAYMENT.getCode());
        order.setOrderNumber("ORD123");
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setTicketCount(1);
        order.setPaymentMethod(null);
        order.setPaymentTime(null);

        ticket = new Ticket();
        ticket.setTicketId(10L);
        ticket.setOrderId(1L);
        ticket.setTicketStatus((byte) TicketStatus.UNUSED.getCode());
        ticket.setPassengerId(100L);
        ticket.setTrainId(1);
        ticket.setDepartureStopId(1L);
        ticket.setArrivalStopId(2L);
        ticket.setTravelDate(LocalDate.of(2025, 1, 1));
        ticket.setCarriageTypeId(1);
        ticket.setCarriageNumber("1");
        ticket.setSeatNumber("1A");
        ticket.setPrice(new BigDecimal("100.00"));

        ticketList = new ArrayList<>();
        ticketList.add(ticket);
    }

    @Test
    void testPayOrder_Success_RedisHit() {
        when(redisService.getCachedOrder(anyString())).thenReturn(Optional.of(order));
        when(redisService.getCachedTickets(anyLong())).thenReturn(ticketList);
        when(ticketRepository.findByOrderId(anyLong())).thenReturn(ticketList);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);

        BookingResponse response = paymentService.payOrder(1L, 2L);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("支付成功", response.getMessage());
        verify(orderRepository).save(any(Order.class));
        verify(ticketRepository, atLeastOnce()).save(any(Ticket.class));
    }

    @Test
    void testPayOrder_Success_RedisMiss() {
        when(redisService.getCachedOrder(anyString())).thenReturn(Optional.empty());
        when(orderRepository.findByOrderIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.of(order));
        when(ticketRepository.findByOrderId(anyLong())).thenReturn(ticketList);
        when(redisService.getCachedTickets(anyLong())).thenReturn(Collections.emptyList());
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);

        BookingResponse response = paymentService.payOrder(1L, 2L);
        assertEquals("SUCCESS", response.getStatus());
        verify(redisService).cacheOrder(any(Order.class));
        verify(redisService, atLeastOnce()).cacheTicket(any(Ticket.class));
    }

    @Test
    void testPayOrder_OrderNotFound() {
        when(redisService.getCachedOrder(anyString())).thenReturn(Optional.empty());
        when(orderRepository.findByOrderIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.empty());
        BookingResponse response = paymentService.payOrder(1L, 2L);
        assertEquals("FAILED", response.getStatus());
        assertEquals("订单不存在", response.getMessage());
    }

    @Test
    void testPayOrder_OrderStatusNotPending() {
        order.setOrderStatus((byte) OrderStatus.PAID.getCode());
        when(redisService.getCachedOrder(anyString())).thenReturn(Optional.of(order));
        BookingResponse response = paymentService.payOrder(1L, 2L);
        assertEquals("FAILED", response.getStatus());
        assertEquals("订单状态不正确", response.getMessage());
    }

    @Test
    void testPayOrder_TicketCountNullOrZero() {
        order.setTicketCount(0);
        when(redisService.getCachedOrder(anyString())).thenReturn(Optional.of(order));
        when(ticketRepository.findByOrderId(anyLong())).thenReturn(ticketList);
        when(redisService.getCachedTickets(anyLong())).thenReturn(ticketList);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        BookingResponse response = paymentService.payOrder(1L, 2L);
        assertEquals("SUCCESS", response.getStatus());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void testPayOrder_ChangeTicket_Success() {
        // 模拟改签配对关系
        when(redisService.getCachedOrder(anyString())).thenReturn(Optional.of(order));
        when(redisService.getCachedTickets(anyLong())).thenReturn(ticketList);
        when(ticketRepository.findByOrderId(anyLong())).thenReturn(ticketList);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        // 改签配对关系
        when(redisService.getChangeMapping(anyString())).thenReturn(Optional.of("20:100"));
        Ticket originalTicket = new Ticket();
        originalTicket.setTicketId(20L);
        originalTicket.setPassengerId(100L);
        originalTicket.setOrderId(99L);
        originalTicket.setTrainId(1);
        originalTicket.setDepartureStopId(1L);
        originalTicket.setArrivalStopId(2L);
        originalTicket.setTravelDate(LocalDate.of(2025, 1, 1));
        originalTicket.setCarriageTypeId(1);
        originalTicket.setCarriageNumber("1");
        originalTicket.setSeatNumber("1A");
        originalTicket.setTicketStatus((byte) TicketStatus.UNUSED.getCode());
        originalTicket.setPrice(new BigDecimal("50.00"));
        when(ticketRepository.findById(20L)).thenReturn(Optional.of(originalTicket));
        Order originalOrder = new Order();
        originalOrder.setOrderId(99L);
        originalOrder.setOrderNumber("ORD999");
        originalOrder.setTotalAmount(new BigDecimal("200.00"));
        originalOrder.setTicketCount(2);
        when(orderRepository.findById(99L)).thenReturn(Optional.of(originalOrder));
        when(ticketRepository.findValidTicketsByOrderId(99L)).thenReturn(Arrays.asList());
        BookingResponse response = paymentService.payOrder(1L, 2L);
        assertEquals("SUCCESS", response.getStatus());
        verify(redisService).deleteChangeMapping(anyString());
        verify(seatService).releaseSeat(any(Ticket.class));
        verify(redisService).incrStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt(), eq(1));
    }

    @Test
    void testPayOrder_ChangeTicket_OriginalTicketNotFound() {
        when(redisService.getCachedOrder(anyString())).thenReturn(Optional.of(order));
        when(redisService.getCachedTickets(anyLong())).thenReturn(ticketList);
        when(ticketRepository.findByOrderId(anyLong())).thenReturn(ticketList);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(redisService.getChangeMapping(anyString())).thenReturn(Optional.of("20:100"));
        when(ticketRepository.findById(20L)).thenReturn(Optional.empty());
        BookingResponse response = paymentService.payOrder(1L, 2L);
        assertEquals("SUCCESS", response.getStatus());
        // 只要主流程不抛异常即可
    }

    @Test
    void testPayOrder_ChangeTicket_PassengerIdNotMatch() {
        when(redisService.getCachedOrder(anyString())).thenReturn(Optional.of(order));
        when(redisService.getCachedTickets(anyLong())).thenReturn(ticketList);
        when(ticketRepository.findByOrderId(anyLong())).thenReturn(ticketList);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(redisService.getChangeMapping(anyString())).thenReturn(Optional.of("20:999"));
        Ticket originalTicket = new Ticket();
        originalTicket.setTicketId(20L);
        originalTicket.setPassengerId(100L);
        when(ticketRepository.findById(20L)).thenReturn(Optional.of(originalTicket));
        BookingResponse response = paymentService.payOrder(1L, 2L);
        assertEquals("SUCCESS", response.getStatus());
    }

    @Test
    void testPayOrder_ChangeTicket_OriginalOrderNotFound() {
        when(redisService.getCachedOrder(anyString())).thenReturn(Optional.of(order));
        when(redisService.getCachedTickets(anyLong())).thenReturn(ticketList);
        when(ticketRepository.findByOrderId(anyLong())).thenReturn(ticketList);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(redisService.getChangeMapping(anyString())).thenReturn(Optional.of("20:100"));
        Ticket originalTicket = new Ticket();
        originalTicket.setTicketId(20L);
        originalTicket.setPassengerId(100L);
        originalTicket.setOrderId(99L);
        when(ticketRepository.findById(20L)).thenReturn(Optional.of(originalTicket));
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());
        BookingResponse response = paymentService.payOrder(1L, 2L);
        assertEquals("SUCCESS", response.getStatus());
    }

    @Test
    void testPayOrder_ChangeTicket_OriginalOrderNoValidTickets() {
        when(redisService.getCachedOrder(anyString())).thenReturn(Optional.of(order));
        when(redisService.getCachedTickets(anyLong())).thenReturn(ticketList);
        when(ticketRepository.findByOrderId(anyLong())).thenReturn(ticketList);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(redisService.getChangeMapping(anyString())).thenReturn(Optional.of("20:100"));
        Ticket originalTicket = new Ticket();
        originalTicket.setTicketId(20L);
        originalTicket.setPassengerId(100L);
        originalTicket.setOrderId(99L);
        originalTicket.setPrice(new BigDecimal("50.00"));
        when(ticketRepository.findById(20L)).thenReturn(Optional.of(originalTicket));
        Order originalOrder = new Order();
        originalOrder.setOrderId(99L);
        originalOrder.setOrderNumber("ORD999");
        originalOrder.setTotalAmount(new BigDecimal("200.00"));
        originalOrder.setTicketCount(1);
        when(orderRepository.findById(99L)).thenReturn(Optional.of(originalOrder));
        when(ticketRepository.findValidTicketsByOrderId(99L)).thenReturn(Collections.emptyList());
        BookingResponse response = paymentService.payOrder(1L, 2L);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(0, (int) originalOrder.getTicketCount());
        assertEquals((byte)OrderStatus.CANCELLED.getCode(), originalOrder.getOrderStatus());
    }

    @Test
    void testPayOrder_ChangeTicket_MappingFormatError() {
        when(redisService.getCachedOrder(anyString())).thenReturn(Optional.of(order));
        when(redisService.getCachedTickets(anyLong())).thenReturn(ticketList);
        when(ticketRepository.findByOrderId(anyLong())).thenReturn(ticketList);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(redisService.getChangeMapping(anyString())).thenReturn(Optional.of("badformat"));
        BookingResponse response = paymentService.payOrder(1L, 2L);
        assertEquals("SUCCESS", response.getStatus());
    }

    @Test
    void testPayOrder_ChangeTicket_Exception() {
        when(redisService.getCachedOrder(anyString())).thenReturn(Optional.of(order));
        when(redisService.getCachedTickets(anyLong())).thenReturn(ticketList);
        when(ticketRepository.findByOrderId(anyLong())).thenReturn(ticketList);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(redisService.getChangeMapping(anyString())).thenThrow(new RuntimeException("test exception"));
        BookingResponse response = paymentService.payOrder(1L, 2L);
        assertEquals("SUCCESS", response.getStatus());
    }

    @Test
    void testPayOrder_TicketCountNull() {
        order.setTicketCount(null);
        when(redisService.getCachedOrder(anyString())).thenReturn(Optional.of(order));
        when(ticketRepository.findByOrderId(anyLong())).thenReturn(ticketList);
        when(redisService.getCachedTickets(anyLong())).thenReturn(ticketList);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        
        BookingResponse response = paymentService.payOrder(1L, 2L);
        assertEquals("SUCCESS", response.getStatus());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void testPayOrder_ChangeTicket_OriginalTicketNoSeatInfo() {
        when(redisService.getCachedOrder(anyString())).thenReturn(Optional.of(order));
        when(redisService.getCachedTickets(anyLong())).thenReturn(ticketList);
        when(ticketRepository.findByOrderId(anyLong())).thenReturn(ticketList);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(redisService.getChangeMapping(anyString())).thenReturn(Optional.of("20:100"));
        
        Ticket originalTicket = new Ticket();
        originalTicket.setTicketId(20L);
        originalTicket.setPassengerId(100L);
        originalTicket.setOrderId(99L);
        originalTicket.setTrainId(1);
        originalTicket.setDepartureStopId(1L);
        originalTicket.setArrivalStopId(2L);
        originalTicket.setTravelDate(LocalDate.of(2025, 1, 1));
        originalTicket.setCarriageTypeId(1);
        originalTicket.setCarriageNumber(null); // 没有座位信息
        originalTicket.setSeatNumber(null);
        originalTicket.setTicketStatus((byte) TicketStatus.UNUSED.getCode());
        originalTicket.setPrice(new BigDecimal("50.00"));
        
        when(ticketRepository.findById(20L)).thenReturn(Optional.of(originalTicket));
        Order originalOrder = new Order();
        originalOrder.setOrderId(99L);
        originalOrder.setOrderNumber("ORD999");
        originalOrder.setTotalAmount(new BigDecimal("200.00"));
        originalOrder.setTicketCount(2);
        when(orderRepository.findById(99L)).thenReturn(Optional.of(originalOrder));
        when(ticketRepository.findValidTicketsByOrderId(99L)).thenReturn(Arrays.asList());
        
        BookingResponse response = paymentService.payOrder(1L, 2L);
        assertEquals("SUCCESS", response.getStatus());
        verify(redisService).deleteChangeMapping(anyString());
        verify(seatService, never()).releaseSeat(any(Ticket.class)); // 不应该调用releaseSeat
        verify(redisService).incrStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt(), eq(1));
    }

    @Test
    void testPayOrder_ChangeTicket_OriginalOrderTicketCountNull() {
        when(redisService.getCachedOrder(anyString())).thenReturn(Optional.of(order));
        when(redisService.getCachedTickets(anyLong())).thenReturn(ticketList);
        when(ticketRepository.findByOrderId(anyLong())).thenReturn(ticketList);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(redisService.getChangeMapping(anyString())).thenReturn(Optional.of("20:100"));
        
        Ticket originalTicket = new Ticket();
        originalTicket.setTicketId(20L);
        originalTicket.setPassengerId(100L);
        originalTicket.setOrderId(99L);
        originalTicket.setTrainId(1);
        originalTicket.setDepartureStopId(1L);
        originalTicket.setArrivalStopId(2L);
        originalTicket.setTravelDate(LocalDate.of(2025, 1, 1));
        originalTicket.setCarriageTypeId(1);
        originalTicket.setCarriageNumber("1");
        originalTicket.setSeatNumber("1A");
        originalTicket.setTicketStatus((byte) TicketStatus.UNUSED.getCode());
        originalTicket.setPrice(new BigDecimal("50.00"));
        
        when(ticketRepository.findById(20L)).thenReturn(Optional.of(originalTicket));
        Order originalOrder = new Order();
        originalOrder.setOrderId(99L);
        originalOrder.setOrderNumber("ORD999");
        originalOrder.setTotalAmount(new BigDecimal("200.00"));
        originalOrder.setTicketCount(null); // ticketCount为null
        when(orderRepository.findById(99L)).thenReturn(Optional.of(originalOrder));
        when(ticketRepository.findValidTicketsByOrderId(99L)).thenReturn(Arrays.asList());
        
        BookingResponse response = paymentService.payOrder(1L, 2L);
        assertEquals("SUCCESS", response.getStatus());
        verify(redisService).deleteChangeMapping(anyString());
        verify(seatService).releaseSeat(any(Ticket.class));
        verify(redisService).incrStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt(), eq(1));
    }

    @Test
    void testPayOrder_ChangeTicket_OriginalOrderTicketCountZero() {
        when(redisService.getCachedOrder(anyString())).thenReturn(Optional.of(order));
        when(redisService.getCachedTickets(anyLong())).thenReturn(ticketList);
        when(ticketRepository.findByOrderId(anyLong())).thenReturn(ticketList);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(redisService.getChangeMapping(anyString())).thenReturn(Optional.of("20:100"));
        
        Ticket originalTicket = new Ticket();
        originalTicket.setTicketId(20L);
        originalTicket.setPassengerId(100L);
        originalTicket.setOrderId(99L);
        originalTicket.setTrainId(1);
        originalTicket.setDepartureStopId(1L);
        originalTicket.setArrivalStopId(2L);
        originalTicket.setTravelDate(LocalDate.of(2025, 1, 1));
        originalTicket.setCarriageTypeId(1);
        originalTicket.setCarriageNumber("1");
        originalTicket.setSeatNumber("1A");
        originalTicket.setTicketStatus((byte) TicketStatus.UNUSED.getCode());
        originalTicket.setPrice(new BigDecimal("50.00"));
        
        when(ticketRepository.findById(20L)).thenReturn(Optional.of(originalTicket));
        Order originalOrder = new Order();
        originalOrder.setOrderId(99L);
        originalOrder.setOrderNumber("ORD999");
        originalOrder.setTotalAmount(new BigDecimal("200.00"));
        originalOrder.setTicketCount(0); // ticketCount为0
        when(orderRepository.findById(99L)).thenReturn(Optional.of(originalOrder));
        when(ticketRepository.findValidTicketsByOrderId(99L)).thenReturn(Arrays.asList());
        
        BookingResponse response = paymentService.payOrder(1L, 2L);
        assertEquals("SUCCESS", response.getStatus());
        verify(redisService).deleteChangeMapping(anyString());
        verify(seatService).releaseSeat(any(Ticket.class));
        verify(redisService).incrStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt(), eq(1));
    }

    @Test
    void testPayOrder_ChangeTicket_OriginalOrderHasValidTickets() {
        when(redisService.getCachedOrder(anyString())).thenReturn(Optional.of(order));
        when(redisService.getCachedTickets(anyLong())).thenReturn(ticketList);
        when(ticketRepository.findByOrderId(anyLong())).thenReturn(ticketList);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(redisService.getChangeMapping(anyString())).thenReturn(Optional.of("20:100"));
        
        Ticket originalTicket = new Ticket();
        originalTicket.setTicketId(20L);
        originalTicket.setPassengerId(100L);
        originalTicket.setOrderId(99L);
        originalTicket.setTrainId(1);
        originalTicket.setDepartureStopId(1L);
        originalTicket.setArrivalStopId(2L);
        originalTicket.setTravelDate(LocalDate.of(2025, 1, 1));
        originalTicket.setCarriageTypeId(1);
        originalTicket.setCarriageNumber("1");
        originalTicket.setSeatNumber("1A");
        originalTicket.setTicketStatus((byte) TicketStatus.UNUSED.getCode());
        originalTicket.setPrice(new BigDecimal("50.00"));
        
        when(ticketRepository.findById(20L)).thenReturn(Optional.of(originalTicket));
        Order originalOrder = new Order();
        originalOrder.setOrderId(99L);
        originalOrder.setOrderNumber("ORD999");
        originalOrder.setTotalAmount(new BigDecimal("200.00"));
        originalOrder.setTicketCount(2);
        when(orderRepository.findById(99L)).thenReturn(Optional.of(originalOrder));
        
        // 原订单还有有效车票
        Ticket remainingTicket = new Ticket();
        remainingTicket.setTicketId(21L);
        remainingTicket.setTicketStatus((byte) TicketStatus.UNUSED.getCode());
        when(ticketRepository.findValidTicketsByOrderId(99L)).thenReturn(Arrays.asList(remainingTicket));
        
        BookingResponse response = paymentService.payOrder(1L, 2L);
        assertEquals("SUCCESS", response.getStatus());
        verify(redisService).deleteChangeMapping(anyString());
        verify(seatService).releaseSeat(any(Ticket.class));
        verify(redisService).incrStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt(), eq(1));
        
        // 验证原订单没有被取消（因为还有有效车票）
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, atLeastOnce()).save(orderCaptor.capture());
        boolean orderCancelled = orderCaptor.getAllValues().stream()
                .anyMatch(o -> o.getOrderId().equals(99L) && o.getOrderStatus() == OrderStatus.CANCELLED.getCode());
        assertFalse(orderCancelled, "原订单不应该被取消，因为还有有效车票");
    }

    @Test
    void testPayOrder_ChangeTicket_OriginalTicketSeatNumberNotNullButCarriageNumberNull() {
        when(redisService.getCachedOrder(anyString())).thenReturn(Optional.of(order));
        when(redisService.getCachedTickets(anyLong())).thenReturn(ticketList);
        when(ticketRepository.findByOrderId(anyLong())).thenReturn(ticketList);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(redisService.getChangeMapping(anyString())).thenReturn(Optional.of("20:100"));
        
        Ticket originalTicket = new Ticket();
        originalTicket.setTicketId(20L);
        originalTicket.setPassengerId(100L);
        originalTicket.setOrderId(99L);
        originalTicket.setTrainId(1);
        originalTicket.setDepartureStopId(1L);
        originalTicket.setArrivalStopId(2L);
        originalTicket.setTravelDate(LocalDate.of(2025, 1, 1));
        originalTicket.setCarriageTypeId(1);
        originalTicket.setCarriageNumber(null); // carriageNumber为null
        originalTicket.setSeatNumber("1A"); // seatNumber不为null
        originalTicket.setTicketStatus((byte) TicketStatus.UNUSED.getCode());
        originalTicket.setPrice(new BigDecimal("50.00"));
        
        when(ticketRepository.findById(20L)).thenReturn(Optional.of(originalTicket));
        Order originalOrder = new Order();
        originalOrder.setOrderId(99L);
        originalOrder.setOrderNumber("ORD999");
        originalOrder.setTotalAmount(new BigDecimal("200.00"));
        originalOrder.setTicketCount(2);
        when(orderRepository.findById(99L)).thenReturn(Optional.of(originalOrder));
        when(ticketRepository.findValidTicketsByOrderId(99L)).thenReturn(Arrays.asList());
        
        BookingResponse response = paymentService.payOrder(1L, 2L);
        assertEquals("SUCCESS", response.getStatus());
        verify(redisService).deleteChangeMapping(anyString());
        verify(seatService, never()).releaseSeat(any(Ticket.class)); // 不应该调用releaseSeat
        verify(redisService).incrStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt(), eq(1));
    }
} 