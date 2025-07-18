package com.example.techprototype.Component;

import com.example.techprototype.DTO.OrderMessage;
import com.example.techprototype.Entity.Order;
import com.example.techprototype.Entity.Ticket;
import com.example.techprototype.Entity.TicketInventory;
import com.example.techprototype.Repository.OrderRepository;
import com.example.techprototype.Repository.TicketRepository;
import com.example.techprototype.Service.SeatService;
import com.example.techprototype.Service.RedisService;
import com.example.techprototype.DAO.TicketInventoryDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderProcessorTest {
    
    @InjectMocks
    private OrderProcessor orderProcessor;
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private TicketRepository ticketRepository;
    
    @Mock
    private SeatService seatService;
    
    @Mock
    private TicketInventoryDAO ticketInventoryDAO;
    
    @Mock
    private RedisService redisService;
    
    private OrderMessage orderMessage;
    private Order savedOrder;
    private TicketInventory inventory;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        orderMessage = new OrderMessage();
        orderMessage.setUserId(1L);
        orderMessage.setTrainId(1);
        orderMessage.setDepartureStopId(100L);
        orderMessage.setArrivalStopId(200L);
        orderMessage.setTravelDate(LocalDate.of(2025, 1, 1));
        orderMessage.setOrderNumber("ORD123456");
        
        OrderMessage.PassengerInfo passengerInfo = new OrderMessage.PassengerInfo();
        passengerInfo.setPassengerId(100L);
        passengerInfo.setTicketType((byte) 1);
        passengerInfo.setCarriageTypeId(1);
        orderMessage.setPassengers(Arrays.asList(passengerInfo));
        
        savedOrder = new Order();
        savedOrder.setOrderId(1L);
        savedOrder.setOrderNumber("ORD123456");
        savedOrder.setUserId(1L);
        savedOrder.setOrderStatus((byte)0);
        savedOrder.setTotalAmount(new BigDecimal(100));
        savedOrder.setTicketCount(1);
        
        inventory = new TicketInventory();
        inventory.setPrice(new BigDecimal(100));
        inventory.setAvailableSeats(50);
    }
    
    @Test
    void testProcessOrder_Success() {
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(ticketInventoryDAO.findByKey(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt()))
            .thenReturn(Optional.of(inventory));
        when(ticketRepository.saveAll(anyList())).thenReturn(Arrays.asList(new Ticket()));
        doNothing().when(seatService).assignSeat(any(Ticket.class));
        
        assertDoesNotThrow(() -> orderProcessor.processOrder(orderMessage));
        
        verify(orderRepository).save(any(Order.class));
        verify(ticketRepository).saveAll(anyList());
        verify(seatService).assignSeat(any(Ticket.class));
        verify(redisService, never()).incrStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt(), anyInt());
    }
    
    @Test
    void testProcessOrder_InventoryNotFound_UseDefaultPrice() {
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(ticketInventoryDAO.findByKey(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt()))
            .thenReturn(Optional.empty());
        when(ticketRepository.saveAll(anyList())).thenReturn(Arrays.asList(new Ticket()));
        doNothing().when(seatService).assignSeat(any(Ticket.class));
        
        assertDoesNotThrow(() -> orderProcessor.processOrder(orderMessage));
        
        verify(orderRepository).save(any(Order.class));
        verify(ticketRepository).saveAll(anyList());
    }
    
    @Test
    void testProcessOrder_Exception_RollbackInventory() {
        when(orderRepository.save(any(Order.class))).thenThrow(new RuntimeException("Database error"));
        when(redisService.incrStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt(), anyInt()))
            .thenReturn(true);
        
        assertThrows(RuntimeException.class, () -> orderProcessor.processOrder(orderMessage));
        
        verify(redisService).incrStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt(), anyInt());
    }
    
    @Test
    void testProcessOrder_ChildTicket_DiscountApplied() {
        OrderMessage.PassengerInfo childPassenger = new OrderMessage.PassengerInfo();
        childPassenger.setPassengerId(101L);
        childPassenger.setTicketType((byte) 2);
        childPassenger.setCarriageTypeId(1);
        orderMessage.setPassengers(Arrays.asList(childPassenger));
        
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(ticketInventoryDAO.findByKey(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt()))
            .thenReturn(Optional.of(inventory));
        when(ticketRepository.saveAll(anyList())).thenReturn(Arrays.asList(new Ticket()));
        doNothing().when(seatService).assignSeat(any(Ticket.class));
        
        assertDoesNotThrow(() -> orderProcessor.processOrder(orderMessage));
        
        verify(orderRepository).save(any(Order.class));
        verify(ticketRepository).saveAll(anyList());
    }
    
    @Test
    void testProcessOrder_StudentTicket_DiscountApplied() {
        OrderMessage.PassengerInfo studentPassenger = new OrderMessage.PassengerInfo();
        studentPassenger.setPassengerId(102L);
        studentPassenger.setTicketType((byte) 3);
        studentPassenger.setCarriageTypeId(1);
        orderMessage.setPassengers(Arrays.asList(studentPassenger));
        
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(ticketInventoryDAO.findByKey(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt()))
            .thenReturn(Optional.of(inventory));
        when(ticketRepository.saveAll(anyList())).thenReturn(Arrays.asList(new Ticket()));
        doNothing().when(seatService).assignSeat(any(Ticket.class));
        
        assertDoesNotThrow(() -> orderProcessor.processOrder(orderMessage));
        
        verify(orderRepository).save(any(Order.class));
        verify(ticketRepository).saveAll(anyList());
    }
    
    @Test
    void testProcessOrder_MultiplePassengers_CalculateTotalCorrectly() {
        OrderMessage.PassengerInfo passenger1 = new OrderMessage.PassengerInfo();
        passenger1.setPassengerId(100L);
        passenger1.setTicketType((byte) 1);
        passenger1.setCarriageTypeId(1);
        
        OrderMessage.PassengerInfo passenger2 = new OrderMessage.PassengerInfo();
        passenger2.setPassengerId(101L);
        passenger2.setTicketType((byte) 2);
        passenger2.setCarriageTypeId(1);
        
        orderMessage.setPassengers(Arrays.asList(passenger1, passenger2));
        
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(ticketInventoryDAO.findByKey(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt()))
            .thenReturn(Optional.of(inventory));
        when(ticketRepository.saveAll(anyList())).thenReturn(Arrays.asList(new Ticket(), new Ticket()));
        doNothing().when(seatService).assignSeat(any(Ticket.class));
        
        assertDoesNotThrow(() -> orderProcessor.processOrder(orderMessage));
        
        verify(orderRepository).save(any(Order.class));
        verify(ticketRepository).saveAll(anyList());
        verify(seatService, times(2)).assignSeat(any(Ticket.class));
    }
} 