package com.example.techprototype.Service.Impl;

import com.example.techprototype.DTO.MyTicketResponse;
import com.example.techprototype.DTO.RefundRequest;
import com.example.techprototype.Entity.Ticket;
import com.example.techprototype.Repository.TicketRepository;
import com.example.techprototype.Repository.TrainRepository;
import com.example.techprototype.Repository.TrainStopRepository;
import com.example.techprototype.Repository.StationRepository;
import com.example.techprototype.Repository.OrderRepository;
import com.example.techprototype.Repository.CarriageTypeRepository;
import com.example.techprototype.Service.RedisService;
import com.example.techprototype.DAO.TicketInventoryDAO;
import com.example.techprototype.Service.SeatService;
import com.example.techprototype.Service.TimeConflictService;
import com.example.techprototype.Repository.PassengerRepository;
import com.example.techprototype.Repository.UserPassengerRelationRepository;
import com.example.techprototype.Repository.UserRepository;
import com.example.techprototype.Enums.OrderStatus;
import com.example.techprototype.Enums.TicketStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Arrays;
import java.lang.reflect.Method;
import com.example.techprototype.DTO.BookingRequest;
import com.example.techprototype.DTO.BookingResponse;
import com.example.techprototype.DTO.ChangeTicketRequest;
import com.example.techprototype.DTO.OrderMessage;
import com.example.techprototype.DTO.TicketDetailResponse;
import com.example.techprototype.Entity.User;
import com.example.techprototype.Entity.Passenger;
import com.example.techprototype.Entity.Order;
import java.time.LocalTime;
import java.math.BigDecimal;
import com.example.techprototype.Entity.CarriageType;
import com.example.techprototype.Entity.Train;
import com.example.techprototype.Entity.TrainStop;
import com.example.techprototype.Entity.Station;
import com.example.techprototype.Entity.TicketInventory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class TicketServiceImplTest {
    @InjectMocks
    private TicketServiceImpl ticketService;

    @Mock private RedisService redisService;
    @Mock private TicketInventoryDAO ticketInventoryDAO;
    @Mock private OrderRepository orderRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private TrainRepository trainRepository;
    @Mock private TrainStopRepository trainStopRepository;
    @Mock private StationRepository stationRepository;
    @Mock private CarriageTypeRepository carriageTypeRepository;
    @Mock private SeatService seatService;
    @Mock private TimeConflictService timeConflictService;
    @Mock private PassengerRepository passengerRepository;
    @Mock private UserPassengerRelationRepository userPassengerRelationRepository;
    @Mock private UserRepository userRepository;
    @Mock private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetMyTickets_Success() {
        Long userId = 1L;
        Long passengerId = 10L;
        // mock user
        com.example.techprototype.Entity.User user = new com.example.techprototype.Entity.User();
        user.setUserId(userId);
        user.setPassengerId(passengerId);
        user.setRealName("张三");
        user.setPhoneNumber("13800138000");
        user.setEmail("zhangsan@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        // mock passenger
        com.example.techprototype.Entity.Passenger passenger = new com.example.techprototype.Entity.Passenger();
        passenger.setPassengerId(passengerId);
        passenger.setRealName("李四");
        passenger.setIdCardNumber("110101199001011235");
        passenger.setPhoneNumber("13800138001");
        when(passengerRepository.findById(passengerId)).thenReturn(Optional.of(passenger));
        // mock ticket
        com.example.techprototype.Entity.Ticket ticket = new com.example.techprototype.Entity.Ticket();
        ticket.setTicketId(1L);
        ticket.setOrderId(100L);
        ticket.setPassengerId(passengerId);
        ticket.setTrainId(1);
        ticket.setDepartureStopId(10L);
        ticket.setArrivalStopId(20L);
        ticket.setTravelDate(LocalDate.of(2025, 1, 1));
        ticket.setCarriageTypeId(1);
        ticket.setTicketStatus((byte)1);
        ticket.setTicketType((byte)1);
        when(ticketRepository.findValidTicketsByPassengerId(passengerId)).thenReturn(Arrays.asList(ticket));
        // mock order
        com.example.techprototype.Entity.Order order = new com.example.techprototype.Entity.Order();
        order.setOrderId(100L);
        order.setOrderNumber("ORD123456");
        order.setOrderStatus((byte)1);
        order.setPaymentTime(null);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        // 其它依赖返回默认
        when(trainStopRepository.findByStopId(anyLong())).thenReturn(Optional.empty());
        when(stationRepository.findById(anyInt())).thenReturn(Optional.empty());
        when(trainRepository.findById(anyInt())).thenReturn(Optional.empty());
        when(carriageTypeRepository.findById(anyInt())).thenReturn(Optional.empty());
        MyTicketResponse response = ticketService.getMyTickets(userId);
        assertNotNull(response);
        assertEquals(1, response.getTickets().size());
        assertEquals("ORD123456", response.getTickets().get(0).getOrderNumber());
        assertEquals("未知车次", response.getTickets().get(0).getTrainNumber());
        assertEquals(userId, response.getUserInfo().getUserId());
        assertEquals(passengerId, response.getUserInfo().getPassengerId());
    }

    @Test
    void testGetMyTickets_UserNotExists() {
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        MyTicketResponse response = ticketService.getMyTickets(userId);
        assertNotNull(response);
        assertEquals("FAILURE", response.getStatus());
        assertEquals("用户不存在", response.getMessage());
    }

    @Test
    void testGetMyTickets_UserNotRelatedToPassenger() {
        Long userId = 1L;
        com.example.techprototype.Entity.User user = new com.example.techprototype.Entity.User();
        user.setUserId(userId);
        user.setPassengerId(null); // 用户未关联乘客
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        MyTicketResponse response = ticketService.getMyTickets(userId);
        assertNotNull(response);
        assertEquals("FAILURE", response.getStatus());
        assertEquals("用户未关联乘客信息", response.getMessage());
    }

    @Test
    void testGetMyTickets_PassengerNotExists() {
        Long userId = 1L;
        Long passengerId = 999L;
        com.example.techprototype.Entity.User user = new com.example.techprototype.Entity.User();
        user.setUserId(userId);
        user.setPassengerId(passengerId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passengerRepository.findById(passengerId)).thenReturn(Optional.empty());
        MyTicketResponse response = ticketService.getMyTickets(userId);
        assertNotNull(response);
        assertEquals("FAILURE", response.getStatus());
        assertEquals("乘客信息不存在", response.getMessage());
    }

    @Test
    void testGetMyTickets_Exception() {
        Long userId = 1L;
        when(userRepository.findById(userId)).thenThrow(new RuntimeException("数据库异常"));
        MyTicketResponse response = ticketService.getMyTickets(userId);
        assertNotNull(response);
        assertEquals("FAILURE", response.getStatus());
        assertTrue(response.getMessage().contains("获取本人车票失败"));
    }

    @Test
    void testGetMyTicketsByStatus_Success() {
        Long userId = 1L;
        Long passengerId = 10L;
        Byte ticketStatus = (byte)1; // 未使用状态
        
        // mock user
        com.example.techprototype.Entity.User user = new com.example.techprototype.Entity.User();
        user.setUserId(userId);
        user.setPassengerId(passengerId);
        user.setRealName("张三");
        user.setPhoneNumber("13800138000");
        user.setEmail("zhangsan@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        
        // mock passenger
        com.example.techprototype.Entity.Passenger passenger = new com.example.techprototype.Entity.Passenger();
        passenger.setPassengerId(passengerId);
        passenger.setRealName("李四");
        passenger.setIdCardNumber("110101199001011235");
        passenger.setPhoneNumber("13800138001");
        when(passengerRepository.findById(passengerId)).thenReturn(Optional.of(passenger));
        
        // mock ticket
        com.example.techprototype.Entity.Ticket ticket = new com.example.techprototype.Entity.Ticket();
        ticket.setTicketId(1L);
        ticket.setOrderId(100L);
        ticket.setPassengerId(passengerId);
        ticket.setTrainId(1);
        ticket.setDepartureStopId(10L);
        ticket.setArrivalStopId(20L);
        ticket.setTravelDate(LocalDate.of(2025, 1, 1));
        ticket.setCarriageTypeId(1);
        ticket.setTicketStatus(ticketStatus);
        ticket.setTicketType((byte)1);
        when(ticketRepository.findByPassengerIdAndTicketStatusOrderByCreatedTimeDesc(passengerId, ticketStatus))
            .thenReturn(Arrays.asList(ticket));
        
        // mock order
        com.example.techprototype.Entity.Order order = new com.example.techprototype.Entity.Order();
        order.setOrderId(100L);
        order.setOrderNumber("ORD123456");
        order.setOrderStatus((byte)1);
        order.setPaymentTime(null);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        
        // 其它依赖返回默认
        when(trainStopRepository.findByStopId(anyLong())).thenReturn(Optional.empty());
        when(stationRepository.findById(anyInt())).thenReturn(Optional.empty());
        when(trainRepository.findById(anyInt())).thenReturn(Optional.empty());
        when(carriageTypeRepository.findById(anyInt())).thenReturn(Optional.empty());
        
        MyTicketResponse response = ticketService.getMyTicketsByStatus(userId, ticketStatus);
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(1, response.getTickets().size());
        assertEquals(ticketStatus, response.getTickets().get(0).getTicketStatus());
    }

    @Test
    void testGetMyTicketsByStatus_UserNotExists() {
        Long userId = 999L;
        Byte ticketStatus = (byte)1;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        MyTicketResponse response = ticketService.getMyTicketsByStatus(userId, ticketStatus);
        assertNotNull(response);
        assertEquals("FAILURE", response.getStatus());
        assertEquals("用户不存在", response.getMessage());
    }

    @Test
    void testGetMyTicketsByStatus_Exception() {
        Long userId = 1L;
        Byte ticketStatus = (byte)1;
        when(userRepository.findById(userId)).thenThrow(new RuntimeException("数据库异常"));
        MyTicketResponse response = ticketService.getMyTicketsByStatus(userId, ticketStatus);
        assertNotNull(response);
        assertEquals("FAILURE", response.getStatus());
        assertTrue(response.getMessage().contains("获取本人车票失败"));
    }

    @Test
    void testGetMyTicketsByStatus_UserNotRelatedToPassenger() {
        Long userId = 1L;
        Byte ticketStatus = (byte)1;
        com.example.techprototype.Entity.User user = new com.example.techprototype.Entity.User();
        user.setUserId(userId);
        user.setPassengerId(null); // 用户未关联乘客
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        MyTicketResponse response = ticketService.getMyTicketsByStatus(userId, ticketStatus);
        assertNotNull(response);
        assertEquals("FAILURE", response.getStatus());
        assertEquals("用户未关联乘客信息", response.getMessage());
    }

    @Test
    void testGetMyTicketsByStatus_PassengerNotExists() {
        Long userId = 1L;
        Long passengerId = 999L;
        Byte ticketStatus = (byte)1;
        com.example.techprototype.Entity.User user = new com.example.techprototype.Entity.User();
        user.setUserId(userId);
        user.setPassengerId(passengerId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passengerRepository.findById(passengerId)).thenReturn(Optional.empty());
        MyTicketResponse response = ticketService.getMyTicketsByStatus(userId, ticketStatus);
        assertNotNull(response);
        assertEquals("FAILURE", response.getStatus());
        assertEquals("乘客信息不存在", response.getMessage());
    }

    @Test
    void testGetAllInventory() {
        com.example.techprototype.Entity.TicketInventory inventory1 = new com.example.techprototype.Entity.TicketInventory();
        inventory1.setInventoryId(1L);
        inventory1.setTrainId(1);
        inventory1.setAvailableSeats(50);
        
        com.example.techprototype.Entity.TicketInventory inventory2 = new com.example.techprototype.Entity.TicketInventory();
        inventory2.setInventoryId(2L);
        inventory2.setTrainId(2);
        inventory2.setAvailableSeats(30);
        
        when(ticketInventoryDAO.findAll()).thenReturn(Arrays.asList(inventory1, inventory2));
        
        List<com.example.techprototype.Entity.TicketInventory> result = ticketService.getAllInventory();
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getTrainId());
        assertEquals(2, result.get(1).getTrainId());
    }

    @Test
    void testGetInventory() {
        Integer trainId = 1;
        Long departureStopId = 10L;
        Long arrivalStopId = 20L;
        LocalDate travelDate = LocalDate.of(2025, 1, 1);
        Integer carriageTypeId = 1;
        
        com.example.techprototype.Entity.TicketInventory inventory = new com.example.techprototype.Entity.TicketInventory();
        inventory.setInventoryId(1L);
        inventory.setTrainId(trainId);
        inventory.setAvailableSeats(50);
        inventory.setPrice(java.math.BigDecimal.valueOf(100.0));
        
        when(ticketInventoryDAO.findByKey(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId))
            .thenReturn(Optional.of(inventory));
        
        Optional<com.example.techprototype.Entity.TicketInventory> result = ticketService.getInventory(
            trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId);
        
        assertTrue(result.isPresent());
        assertEquals(trainId, result.get().getTrainId());
        assertEquals(50, result.get().getAvailableSeats());
    }

    @Test
    void testGetTicketPrice() {
        Integer trainId = 1;
        Long departureStopId = 10L;
        Long arrivalStopId = 20L;
        LocalDate travelDate = LocalDate.of(2025, 1, 1);
        Integer carriageTypeId = 1;
        
        com.example.techprototype.Entity.TicketInventory inventory = new com.example.techprototype.Entity.TicketInventory();
        inventory.setPrice(java.math.BigDecimal.valueOf(100.0));
        
        when(ticketInventoryDAO.findByKey(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId))
            .thenReturn(Optional.of(inventory));
        
        Optional<java.math.BigDecimal> result = ticketService.getTicketPrice(
            trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId);
        
        assertTrue(result.isPresent());
        assertEquals(java.math.BigDecimal.valueOf(100.0), result.get());
    }

    @Test
    void testReserveSeats() {
        Integer trainId = 1;
        Long departureStopId = 10L;
        Long arrivalStopId = 20L;
        LocalDate travelDate = LocalDate.of(2025, 1, 1);
        Integer carriageTypeId = 1;
        int quantity = 2;
        
        when(redisService.decrStock(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId, quantity))
            .thenReturn(true);
        
        boolean result = ticketService.reserveSeats(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId, quantity);
        assertTrue(result);
    }

    @Test
    void testReleaseSeats() {
        Integer trainId = 1;
        Long departureStopId = 10L;
        Long arrivalStopId = 20L;
        LocalDate travelDate = LocalDate.of(2025, 1, 1);
        Integer carriageTypeId = 1;
        int quantity = 2;
        
        ticketService.releaseSeats(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId, quantity);
        
        verify(redisService).incrStock(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId, quantity);
    }

    @Test
    void testUpdateInventory() {
        Integer trainId = 1;
        Long departureStopId = 10L;
        Long arrivalStopId = 20L;
        LocalDate travelDate = LocalDate.of(2025, 1, 1);
        Integer carriageTypeId = 1;
        int availableSeats = 50;
        
        com.example.techprototype.Entity.TicketInventory inventory = new com.example.techprototype.Entity.TicketInventory();
        inventory.setInventoryId(1L);
        inventory.setCacheVersion(1L);
        inventory.setDbVersion(1);
        
        when(ticketInventoryDAO.findByKey(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId))
            .thenReturn(Optional.of(inventory));
        
        ticketService.updateInventory(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId, availableSeats);
        
        verify(redisService).setStock(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId, availableSeats);
        verify(ticketInventoryDAO).save(any(com.example.techprototype.Entity.TicketInventory.class));
    }

    @Test
    void testGetMyTickets_WithInvalidOrder() {
        Long userId = 1L;
        Long passengerId = 10L;
        
        // mock user
        com.example.techprototype.Entity.User user = new com.example.techprototype.Entity.User();
        user.setUserId(userId);
        user.setPassengerId(passengerId);
        user.setRealName("张三");
        user.setPhoneNumber("13800138000");
        user.setEmail("zhangsan@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        
        // mock passenger
        com.example.techprototype.Entity.Passenger passenger = new com.example.techprototype.Entity.Passenger();
        passenger.setPassengerId(passengerId);
        passenger.setRealName("李四");
        passenger.setIdCardNumber("110101199001011235");
        passenger.setPhoneNumber("13800138001");
        when(passengerRepository.findById(passengerId)).thenReturn(Optional.of(passenger));
        
        // mock ticket with invalid order
        com.example.techprototype.Entity.Ticket ticket = new com.example.techprototype.Entity.Ticket();
        ticket.setTicketId(1L);
        ticket.setOrderId(999L); // 不存在的订单ID
        ticket.setPassengerId(passengerId);
        ticket.setTrainId(1);
        ticket.setDepartureStopId(10L);
        ticket.setArrivalStopId(20L);
        ticket.setTravelDate(LocalDate.of(2025, 1, 1));
        ticket.setCarriageTypeId(1);
        ticket.setTicketStatus((byte)1);
        ticket.setTicketType((byte)1);
        when(ticketRepository.findValidTicketsByPassengerId(passengerId)).thenReturn(Arrays.asList(ticket));
        
        // mock order not found
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());
        
        // 其它依赖返回默认
        when(trainStopRepository.findByStopId(anyLong())).thenReturn(Optional.empty());
        when(stationRepository.findById(anyInt())).thenReturn(Optional.empty());
        when(trainRepository.findById(anyInt())).thenReturn(Optional.empty());
        when(carriageTypeRepository.findById(anyInt())).thenReturn(Optional.empty());
        
        MyTicketResponse response = ticketService.getMyTickets(userId);
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(0, response.getTickets().size()); // 因为订单不存在，所以票会被过滤掉
    }

    @Test
    void testGetMyTickets_WithDifferentTicketStatuses() {
        Long userId = 1L;
        Long passengerId = 10L;
        
        // mock user
        com.example.techprototype.Entity.User user = new com.example.techprototype.Entity.User();
        user.setUserId(userId);
        user.setPassengerId(passengerId);
        user.setRealName("张三");
        user.setPhoneNumber("13800138000");
        user.setEmail("zhangsan@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        
        // mock passenger
        com.example.techprototype.Entity.Passenger passenger = new com.example.techprototype.Entity.Passenger();
        passenger.setPassengerId(passengerId);
        passenger.setRealName("李四");
        passenger.setIdCardNumber("110101199001011235");
        passenger.setPhoneNumber("13800138001");
        when(passengerRepository.findById(passengerId)).thenReturn(Optional.of(passenger));
        
        // mock multiple tickets with different statuses
        com.example.techprototype.Entity.Ticket ticket1 = new com.example.techprototype.Entity.Ticket();
        ticket1.setTicketId(1L);
        ticket1.setOrderId(100L);
        ticket1.setPassengerId(passengerId);
        ticket1.setTrainId(1);
        ticket1.setDepartureStopId(10L);
        ticket1.setArrivalStopId(20L);
        ticket1.setTravelDate(LocalDate.of(2025, 1, 1));
        ticket1.setCarriageTypeId(1);
        ticket1.setTicketStatus((byte)0); // 待支付
        ticket1.setTicketType((byte)1); // 成人票
        
        com.example.techprototype.Entity.Ticket ticket2 = new com.example.techprototype.Entity.Ticket();
        ticket2.setTicketId(2L);
        ticket2.setOrderId(101L);
        ticket2.setPassengerId(passengerId);
        ticket2.setTrainId(1);
        ticket2.setDepartureStopId(10L);
        ticket2.setArrivalStopId(20L);
        ticket2.setTravelDate(LocalDate.of(2025, 1, 1));
        ticket2.setCarriageTypeId(1);
        ticket2.setTicketStatus((byte)2); // 已使用
        ticket2.setTicketType((byte)2); // 儿童票
        
        com.example.techprototype.Entity.Ticket ticket3 = new com.example.techprototype.Entity.Ticket();
        ticket3.setTicketId(3L);
        ticket3.setOrderId(102L);
        ticket3.setPassengerId(passengerId);
        ticket3.setTrainId(1);
        ticket3.setDepartureStopId(10L);
        ticket3.setArrivalStopId(20L);
        ticket3.setTravelDate(LocalDate.of(2025, 1, 1));
        ticket3.setCarriageTypeId(1);
        ticket3.setTicketStatus((byte)3); // 已退票
        ticket3.setTicketType((byte)3); // 学生票
        
        when(ticketRepository.findValidTicketsByPassengerId(passengerId))
            .thenReturn(Arrays.asList(ticket1, ticket2, ticket3));
        
        // mock orders
        com.example.techprototype.Entity.Order order1 = new com.example.techprototype.Entity.Order();
        order1.setOrderId(100L);
        order1.setOrderNumber("ORD123456");
        order1.setOrderStatus((byte)0); // 待支付
        order1.setPaymentTime(null);
        
        com.example.techprototype.Entity.Order order2 = new com.example.techprototype.Entity.Order();
        order2.setOrderId(101L);
        order2.setOrderNumber("ORD123457");
        order2.setOrderStatus((byte)1); // 已支付
        order2.setPaymentTime(LocalDateTime.now());
        
        com.example.techprototype.Entity.Order order3 = new com.example.techprototype.Entity.Order();
        order3.setOrderId(102L);
        order3.setOrderNumber("ORD123458");
        order3.setOrderStatus((byte)2); // 已完成
        order3.setPaymentTime(LocalDateTime.now());
        
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order1));
        when(orderRepository.findById(101L)).thenReturn(Optional.of(order2));
        when(orderRepository.findById(102L)).thenReturn(Optional.of(order3));
        
        // 其它依赖返回默认
        when(trainStopRepository.findByStopId(anyLong())).thenReturn(Optional.empty());
        when(stationRepository.findById(anyInt())).thenReturn(Optional.empty());
        when(trainRepository.findById(anyInt())).thenReturn(Optional.empty());
        when(carriageTypeRepository.findById(anyInt())).thenReturn(Optional.empty());
        
        MyTicketResponse response = ticketService.getMyTickets(userId);
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(3, response.getTickets().size());
        
        // 验证不同的票状态文本
        assertEquals("待支付", response.getTickets().get(0).getTicketStatusText());
        assertEquals("已使用", response.getTickets().get(1).getTicketStatusText());
        assertEquals("已退票", response.getTickets().get(2).getTicketStatusText());
        
        // 验证不同的票种文本
        assertEquals("成人票", response.getTickets().get(0).getTicketTypeText());
        assertEquals("儿童票", response.getTickets().get(1).getTicketTypeText());
        assertEquals("学生票", response.getTickets().get(2).getTicketTypeText());
        
        // 验证不同的订单状态文本
        assertEquals("待支付", response.getTickets().get(0).getOrderStatusText());
        assertEquals("已支付", response.getTickets().get(1).getOrderStatusText());
        assertEquals("已完成", response.getTickets().get(2).getOrderStatusText());
    }

    @Test
    void testGetAvailableSeats_WithRedisData() {
        Integer trainId = 1;
        Long departureStopId = 10L;
        Long arrivalStopId = 20L;
        LocalDate travelDate = LocalDate.of(2025, 1, 1);
        Integer carriageTypeId = 1;
        
        // mock Redis有数据
        when(redisService.getStock(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId))
            .thenReturn(Optional.of(50));
        
        Optional<Integer> result = ticketService.getAvailableSeats(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId);
        
        assertTrue(result.isPresent());
        assertEquals(50, result.get());
        verify(redisService).getStock(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId);
        verify(ticketInventoryDAO, never()).findByKey(any(), any(), any(), any(), any());
    }

    @Test
    void testGetAvailableSeats_WithoutRedisData() {
        Integer trainId = 1;
        Long departureStopId = 10L;
        Long arrivalStopId = 20L;
        LocalDate travelDate = LocalDate.of(2025, 1, 1);
        Integer carriageTypeId = 1;
        
        // mock Redis没有数据
        when(redisService.getStock(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId))
            .thenReturn(Optional.empty());
        
        // mock 数据库有数据
        com.example.techprototype.Entity.TicketInventory inventory = new com.example.techprototype.Entity.TicketInventory();
        inventory.setAvailableSeats(30);
        when(ticketInventoryDAO.findByKey(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId))
            .thenReturn(Optional.of(inventory));
        
        Optional<Integer> result = ticketService.getAvailableSeats(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId);
        
        assertTrue(result.isPresent());
        assertEquals(30, result.get());
        verify(redisService).setStock(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId, 30);
    }

    @Test
    void testGetAvailableSeats_NoData() {
        Integer trainId = 1;
        Long departureStopId = 10L;
        Long arrivalStopId = 20L;
        LocalDate travelDate = LocalDate.of(2025, 1, 1);
        Integer carriageTypeId = 1;
        
        // mock Redis没有数据
        when(redisService.getStock(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId))
            .thenReturn(Optional.empty());
        
        // mock 数据库也没有数据
        when(ticketInventoryDAO.findByKey(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId))
            .thenReturn(Optional.empty());
        
        Optional<Integer> result = ticketService.getAvailableSeats(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId);
        
        assertFalse(result.isPresent());
    }

    @Test
    void testGetMyTickets_WithStationData() {
        Long userId = 1L;
        Long passengerId = 10L;
        
        // mock user
        com.example.techprototype.Entity.User user = new com.example.techprototype.Entity.User();
        user.setUserId(userId);
        user.setPassengerId(passengerId);
        user.setRealName("张三");
        user.setPhoneNumber("13800138000");
        user.setEmail("zhangsan@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        
        // mock passenger
        com.example.techprototype.Entity.Passenger passenger = new com.example.techprototype.Entity.Passenger();
        passenger.setPassengerId(passengerId);
        passenger.setRealName("李四");
        passenger.setIdCardNumber("110101199001011235");
        passenger.setPhoneNumber("13800138001");
        when(passengerRepository.findById(passengerId)).thenReturn(Optional.of(passenger));
        
        // mock ticket
        com.example.techprototype.Entity.Ticket ticket = new com.example.techprototype.Entity.Ticket();
        ticket.setTicketId(1L);
        ticket.setOrderId(100L);
        ticket.setPassengerId(passengerId);
        ticket.setTrainId(1);
        ticket.setDepartureStopId(10L);
        ticket.setArrivalStopId(20L);
        ticket.setTravelDate(LocalDate.of(2025, 1, 1));
        ticket.setCarriageTypeId(1);
        ticket.setTicketStatus((byte)1);
        ticket.setTicketType((byte)1);
        when(ticketRepository.findValidTicketsByPassengerId(passengerId)).thenReturn(Arrays.asList(ticket));
        
        // mock order
        com.example.techprototype.Entity.Order order = new com.example.techprototype.Entity.Order();
        order.setOrderId(100L);
        order.setOrderNumber("ORD123456");
        order.setOrderStatus((byte)1);
        order.setPaymentTime(null);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        
        // mock train stop data
        com.example.techprototype.Entity.TrainStop trainStop1 = new com.example.techprototype.Entity.TrainStop();
        trainStop1.setStopId(10L);
        trainStop1.setStationId(1);
        trainStop1.setDepartureTime(java.time.LocalTime.of(8, 0));
        
        com.example.techprototype.Entity.TrainStop trainStop2 = new com.example.techprototype.Entity.TrainStop();
        trainStop2.setStopId(20L);
        trainStop2.setStationId(2);
        trainStop2.setArrivalTime(java.time.LocalTime.of(10, 0));
        
        when(trainStopRepository.findByStopId(10L)).thenReturn(Optional.of(trainStop1));
        when(trainStopRepository.findByStopId(20L)).thenReturn(Optional.of(trainStop2));
        
        // mock station data
        com.example.techprototype.Entity.Station station1 = new com.example.techprototype.Entity.Station();
        station1.setStationId(1);
        station1.setStationName("北京站");
        station1.setCity("北京");
        
        com.example.techprototype.Entity.Station station2 = new com.example.techprototype.Entity.Station();
        station2.setStationId(2);
        station2.setStationName("上海站");
        station2.setCity("上海");
        
        when(stationRepository.findById(1)).thenReturn(Optional.of(station1));
        when(stationRepository.findById(2)).thenReturn(Optional.of(station2));
        
        // mock train data
        com.example.techprototype.Entity.Train train = new com.example.techprototype.Entity.Train();
        train.setTrainId(1);
        train.setTrainNumber("G11");
        when(trainRepository.findById(1)).thenReturn(Optional.of(train));
        
        // mock carriage type data
        com.example.techprototype.Entity.CarriageType carriageType = new com.example.techprototype.Entity.CarriageType();
        carriageType.setTypeId(1);
        carriageType.setTypeName("一等座");
        when(carriageTypeRepository.findById(1)).thenReturn(Optional.of(carriageType));
        
        MyTicketResponse response = ticketService.getMyTickets(userId);
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(1, response.getTickets().size());
        
        // 验证车站名称
        assertEquals("北京站", response.getTickets().get(0).getDepartureStationName());
        assertEquals("上海站", response.getTickets().get(0).getArrivalStationName());
        
        // 验证车次号
        assertEquals("G11", response.getTickets().get(0).getTrainNumber());
        
        // 验证车厢类型名称
        assertEquals("一等座", response.getTickets().get(0).getCarriageTypeName());
        
        // 验证时间
        assertEquals(java.time.LocalTime.of(8, 0), response.getTickets().get(0).getDepartureTime());
        assertEquals(java.time.LocalTime.of(10, 0), response.getTickets().get(0).getArrivalTime());
    }

    @Test
    void testReserveSeats_Failure() {
        Integer trainId = 1;
        Long departureStopId = 10L;
        Long arrivalStopId = 20L;
        LocalDate travelDate = LocalDate.of(2025, 1, 1);
        Integer carriageTypeId = 1;
        int quantity = 2;
        
        when(redisService.decrStock(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId, quantity))
            .thenReturn(false); // Redis扣减失败
        
        boolean result = ticketService.reserveSeats(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId, quantity);
        assertFalse(result);
    }

    @Test
    void testUpdateInventory_InventoryNotExists() {
        Integer trainId = 1;
        Long departureStopId = 10L;
        Long arrivalStopId = 20L;
        LocalDate travelDate = LocalDate.of(2025, 1, 1);
        Integer carriageTypeId = 1;
        int availableSeats = 50;
        
        // mock inventory不存在
        when(ticketInventoryDAO.findByKey(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId))
            .thenReturn(Optional.empty());
        
        ticketService.updateInventory(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId, availableSeats);
        
        verify(redisService).setStock(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId, availableSeats);
        verify(ticketInventoryDAO, never()).save(any(com.example.techprototype.Entity.TicketInventory.class));
    }

    @Test
    public void testGetOrderStatusText() throws Exception {
        // 使用反射测试私有方法
        Method getOrderStatusTextMethod = TicketServiceImpl.class.getDeclaredMethod("getOrderStatusText", Byte.class);
        getOrderStatusTextMethod.setAccessible(true);
        
        // 测试各种订单状态
        assertEquals("待支付", getOrderStatusTextMethod.invoke(ticketService, (byte) 0));
        assertEquals("已支付", getOrderStatusTextMethod.invoke(ticketService, (byte) 1));
        assertEquals("已完成", getOrderStatusTextMethod.invoke(ticketService, (byte) 2));
        assertEquals("已取消", getOrderStatusTextMethod.invoke(ticketService, (byte) 3));
        assertEquals("未知状态", getOrderStatusTextMethod.invoke(ticketService, (byte) 99));
    }
    
    @Test
    public void testGetTicketStatusText() throws Exception {
        // 使用反射测试私有方法
        Method getTicketStatusTextMethod = TicketServiceImpl.class.getDeclaredMethod("getTicketStatusText", Byte.class);
        getTicketStatusTextMethod.setAccessible(true);
        
        // 测试各种车票状态
        assertEquals("待支付", getTicketStatusTextMethod.invoke(ticketService, (byte) 0));
        assertEquals("未使用", getTicketStatusTextMethod.invoke(ticketService, (byte) 1));
        assertEquals("已使用", getTicketStatusTextMethod.invoke(ticketService, (byte) 2));
        assertEquals("已退票", getTicketStatusTextMethod.invoke(ticketService, (byte) 3));
        assertEquals("已改签", getTicketStatusTextMethod.invoke(ticketService, (byte) 4));
        assertEquals("未知状态", getTicketStatusTextMethod.invoke(ticketService, (byte) 99));
    }
    
    @Test
    public void testValidateStationInSameCity() throws Exception {
        // 使用反射测试私有方法
        Method validateStationInSameCityMethod = TicketServiceImpl.class.getDeclaredMethod("validateStationsInSameCity", Long.class, Long.class);
        validateStationInSameCityMethod.setAccessible(true);
        
        // 测试正常情况
        assertTrue((Boolean) validateStationInSameCityMethod.invoke(ticketService, 1L, 2L));
        
        // 测试异常情况 - 模拟异常
        // 由于方法内部有try-catch，我们需要模拟异常情况
        // 这里我们测试正常的分支覆盖
    }
    
    @Test
    public void testValidateChangeTicketCities() throws Exception {
        // 使用反射测试私有方法
        Method validateChangeTicketCitiesMethod = TicketServiceImpl.class.getDeclaredMethod("validateChangeTicketCities", Ticket.class, Long.class, Long.class);
        validateChangeTicketCitiesMethod.setAccessible(true);
        
        // 创建测试车票
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setTrainId(1);
        ticket.setDepartureStopId(1L);
        ticket.setArrivalStopId(2L);
        
        // Mock站点城市信息 - 原票：北京 → 上海
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.of(new TrainStop() {{
            setStationId(1);
        }}));
        when(trainStopRepository.findByStopId(2L)).thenReturn(Optional.of(new TrainStop() {{
            setStationId(2);
        }}));
        when(stationRepository.findById(1)).thenReturn(Optional.of(new Station() {{
            setCity("北京");
        }}));
        when(stationRepository.findById(2)).thenReturn(Optional.of(new Station() {{
            setCity("上海");
        }}));
        
        // Mock新站点城市信息 - 新票：北京 → 上海（相同城市）
        when(trainStopRepository.findByStopId(3L)).thenReturn(Optional.of(new TrainStop() {{
            setStationId(3);
        }}));
        when(trainStopRepository.findByStopId(4L)).thenReturn(Optional.of(new TrainStop() {{
            setStationId(4);
        }}));
        when(stationRepository.findById(3)).thenReturn(Optional.of(new Station() {{
            setCity("北京");
        }}));
        when(stationRepository.findById(4)).thenReturn(Optional.of(new Station() {{
            setCity("上海");
        }}));
        
        // 测试相同城市，应该为true
        assertTrue((Boolean) validateChangeTicketCitiesMethod.invoke(ticketService, ticket, 3L, 4L));
        
        // Mock不同城市的新站点 - 新票：广州 → 深圳（不同城市）
        when(trainStopRepository.findByStopId(5L)).thenReturn(Optional.of(new TrainStop() {{
            setStationId(5);
        }}));
        when(trainStopRepository.findByStopId(6L)).thenReturn(Optional.of(new TrainStop() {{
            setStationId(6);
        }}));
        when(stationRepository.findById(5)).thenReturn(Optional.of(new Station() {{
            setCity("广州");
        }}));
        when(stationRepository.findById(6)).thenReturn(Optional.of(new Station() {{
            setCity("深圳");
        }}));
        
        // 测试不同城市，应该为false
        assertFalse((Boolean) validateChangeTicketCitiesMethod.invoke(ticketService, ticket, 5L, 6L));
    }
    
    @Test
    public void testValidateChangeTicketCities_DepartureStopDifferent() throws Exception {
        // 使用反射测试私有方法
        Method validateChangeTicketCitiesMethod = TicketServiceImpl.class.getDeclaredMethod("validateChangeTicketCities", Ticket.class, Long.class, Long.class);
        validateChangeTicketCitiesMethod.setAccessible(true);
        
        // 创建测试车票
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setTrainId(1);
        ticket.setDepartureStopId(1L);
        ticket.setArrivalStopId(2L);
        
        // Mock原票城市信息 - 北京 → 上海
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.of(new TrainStop() {{
            setStationId(1);
        }}));
        when(trainStopRepository.findByStopId(2L)).thenReturn(Optional.of(new TrainStop() {{
            setStationId(2);
        }}));
        when(stationRepository.findById(1)).thenReturn(Optional.of(new Station() {{
            setCity("北京");
        }}));
        when(stationRepository.findById(2)).thenReturn(Optional.of(new Station() {{
            setCity("上海");
        }}));
        
        // Mock新站点城市信息 - 广州 → 上海（出发站城市不同）
        when(trainStopRepository.findByStopId(3L)).thenReturn(Optional.of(new TrainStop() {{
            setStationId(3);
        }}));
        when(stationRepository.findById(3)).thenReturn(Optional.of(new Station() {{
            setCity("广州");
        }}));
        
        // 测试出发站城市不同，应该为false
        assertFalse((Boolean) validateChangeTicketCitiesMethod.invoke(ticketService, ticket, 3L, 2L));
    }
    
    @Test
    public void testValidateChangeTicketCities_ArrivalStopDifferent() throws Exception {
        // 使用反射测试私有方法
        Method validateChangeTicketCitiesMethod = TicketServiceImpl.class.getDeclaredMethod("validateChangeTicketCities", Ticket.class, Long.class, Long.class);
        validateChangeTicketCitiesMethod.setAccessible(true);
        
        // 创建测试车票
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setTrainId(1);
        ticket.setDepartureStopId(1L);
        ticket.setArrivalStopId(2L);
        
        // Mock原票城市信息 - 北京 → 上海
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.of(new TrainStop() {{
            setStationId(1);
        }}));
        when(trainStopRepository.findByStopId(2L)).thenReturn(Optional.of(new TrainStop() {{
            setStationId(2);
        }}));
        when(stationRepository.findById(1)).thenReturn(Optional.of(new Station() {{
            setCity("北京");
        }}));
        when(stationRepository.findById(2)).thenReturn(Optional.of(new Station() {{
            setCity("上海");
        }}));
        
        // Mock新站点城市信息 - 北京 → 广州（到达站城市不同）
        when(trainStopRepository.findByStopId(4L)).thenReturn(Optional.of(new TrainStop() {{
            setStationId(4);
        }}));
        when(stationRepository.findById(4)).thenReturn(Optional.of(new Station() {{
            setCity("广州");
        }}));
        
        // 测试到达站城市不同，应该为false
        assertFalse((Boolean) validateChangeTicketCitiesMethod.invoke(ticketService, ticket, 1L, 4L));
    }
    
    @Test
    public void testValidateChangeTicketCities_Exception() throws Exception {
        // 使用反射测试私有方法
        Method validateChangeTicketCitiesMethod = TicketServiceImpl.class.getDeclaredMethod("validateChangeTicketCities", Ticket.class, Long.class, Long.class);
        validateChangeTicketCitiesMethod.setAccessible(true);
        
        // 创建一个会抛出异常的Ticket对象
        Ticket ticket = new Ticket() {
            @Override
            public Long getDepartureStopId() {
                throw new RuntimeException("模拟异常");
            }
        };
        ticket.setTicketId(1L);
        ticket.setTrainId(1);
        ticket.setArrivalStopId(2L);
        
        // 测试异常情况，应该为false
        assertFalse((Boolean) validateChangeTicketCitiesMethod.invoke(ticketService, ticket, 1L, 2L));
    }
    
    @Test
    public void testGetTicketTypeText() throws Exception {
        // 使用反射测试私有方法
        Method getTicketTypeTextMethod = TicketServiceImpl.class.getDeclaredMethod("getTicketTypeText", Byte.class);
        getTicketTypeTextMethod.setAccessible(true);
        
        // 测试各种票种
        assertEquals("成人票", getTicketTypeTextMethod.invoke(ticketService, (byte) 1));
        assertEquals("儿童票", getTicketTypeTextMethod.invoke(ticketService, (byte) 2));
        assertEquals("学生票", getTicketTypeTextMethod.invoke(ticketService, (byte) 3));
        assertEquals("残疾票", getTicketTypeTextMethod.invoke(ticketService, (byte) 4));
        assertEquals("军人票", getTicketTypeTextMethod.invoke(ticketService, (byte) 5));
        assertEquals("未知票种", getTicketTypeTextMethod.invoke(ticketService, (byte) 99));
    }
    
    @Test
    public void testGetCarriageTypeName() throws Exception {
        // 使用反射测试私有方法
        Method getCarriageTypeNameMethod = TicketServiceImpl.class.getDeclaredMethod("getCarriageTypeName", Integer.class);
        getCarriageTypeNameMethod.setAccessible(true);
        
        // 模拟车厢类型存在
        CarriageType carriageType = new CarriageType();
        carriageType.setTypeId(1);
        carriageType.setTypeName("硬座");
        when(carriageTypeRepository.findById(1)).thenReturn(Optional.of(carriageType));
        
        assertEquals("硬座", getCarriageTypeNameMethod.invoke(ticketService, 1));
        
        // 测试车厢类型不存在
        when(carriageTypeRepository.findById(999)).thenReturn(Optional.empty());
        assertEquals("未知车厢类型", getCarriageTypeNameMethod.invoke(ticketService, 999));
        
        // 测试异常情况
        when(carriageTypeRepository.findById(888)).thenThrow(new RuntimeException("数据库异常"));
        assertEquals("未知车厢类型", getCarriageTypeNameMethod.invoke(ticketService, 888));
    }
    
    @Test
    public void testGetTrainNumber() throws Exception {
        // 使用反射测试私有方法
        Method getTrainNumberMethod = TicketServiceImpl.class.getDeclaredMethod("getTrainNumber", Integer.class);
        getTrainNumberMethod.setAccessible(true);
        
        // 模拟车次存在
        Train train = new Train();
        train.setTrainId(1);
        train.setTrainNumber("G101");
        when(trainRepository.findById(1)).thenReturn(Optional.of(train));
        
        assertEquals("G101", getTrainNumberMethod.invoke(ticketService, 1));
        
        // 测试车次不存在
        when(trainRepository.findById(999)).thenReturn(Optional.empty());
        assertEquals("未知车次", getTrainNumberMethod.invoke(ticketService, 999));
        
        // 测试异常情况
        when(trainRepository.findById(888)).thenThrow(new RuntimeException("数据库异常"));
        assertEquals("未知车次", getTrainNumberMethod.invoke(ticketService, 888));
    }
    
    @Test
    public void testGetArrivalTime() throws Exception {
        // 使用反射测试私有方法
        Method getArrivalTimeMethod = TicketServiceImpl.class.getDeclaredMethod("getArrivalTime", Long.class);
        getArrivalTimeMethod.setAccessible(true);
        
        // 模拟站点存在
        TrainStop trainStop = new TrainStop();
        trainStop.setStopId(1L);
        trainStop.setArrivalTime(LocalTime.of(10, 30));
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.of(trainStop));
        
        assertEquals(LocalTime.of(10, 30), getArrivalTimeMethod.invoke(ticketService, 1L));
        
        // 测试站点不存在
        when(trainStopRepository.findByStopId(999L)).thenReturn(Optional.empty());
        assertNull(getArrivalTimeMethod.invoke(ticketService, 999L));
        
        // 测试异常情况
        when(trainStopRepository.findByStopId(888L)).thenThrow(new RuntimeException("数据库异常"));
        assertNull(getArrivalTimeMethod.invoke(ticketService, 888L));
    }
    
    @Test
    public void testGetDepartureTime() throws Exception {
        // 使用反射测试私有方法
        Method getDepartureTimeMethod = TicketServiceImpl.class.getDeclaredMethod("getDepartureTime", Long.class);
        getDepartureTimeMethod.setAccessible(true);
        
        // 模拟站点存在
        TrainStop trainStop = new TrainStop();
        trainStop.setStopId(1L);
        trainStop.setDepartureTime(LocalTime.of(9, 0));
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.of(trainStop));
        
        assertEquals(LocalTime.of(9, 0), getDepartureTimeMethod.invoke(ticketService, 1L));
        
        // 测试站点不存在
        when(trainStopRepository.findByStopId(999L)).thenReturn(Optional.empty());
        assertNull(getDepartureTimeMethod.invoke(ticketService, 999L));
        
        // 测试异常情况
        when(trainStopRepository.findByStopId(888L)).thenThrow(new RuntimeException("数据库异常"));
        assertNull(getDepartureTimeMethod.invoke(ticketService, 888L));
    }
    
    @Test
    public void testGetStationName() throws Exception {
        // 使用反射测试私有方法
        Method getStationNameMethod = TicketServiceImpl.class.getDeclaredMethod("getStationName", Long.class);
        getStationNameMethod.setAccessible(true);
        
        // 模拟站点和车站都存在
        TrainStop trainStop = new TrainStop();
        trainStop.setStopId(1L);
        trainStop.setStationId(100);
        
        Station station = new Station();
        station.setStationId(100);
        station.setStationName("北京站");
        
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.of(trainStop));
        when(stationRepository.findById(100)).thenReturn(Optional.of(station));
        
        assertEquals("北京站", getStationNameMethod.invoke(ticketService, 1L));
        
        // 测试站点不存在
        when(trainStopRepository.findByStopId(999L)).thenReturn(Optional.empty());
        assertEquals("未知车站", getStationNameMethod.invoke(ticketService, 999L));
        
        // 测试车站不存在
        TrainStop trainStop2 = new TrainStop();
        trainStop2.setStopId(2L);
        trainStop2.setStationId(200);
        when(trainStopRepository.findByStopId(2L)).thenReturn(Optional.of(trainStop2));
        when(stationRepository.findById(200)).thenReturn(Optional.empty());
        assertEquals("未知车站", getStationNameMethod.invoke(ticketService, 2L));
        
        // 测试异常情况
        when(trainStopRepository.findByStopId(888L)).thenThrow(new RuntimeException("数据库异常"));
        assertEquals("未知车站", getStationNameMethod.invoke(ticketService, 888L));
    }
    
    @Test
    public void testGenerateOrderNumber() throws Exception {
        // 使用反射测试私有方法
        Method generateOrderNumberMethod = TicketServiceImpl.class.getDeclaredMethod("generateOrderNumber");
        generateOrderNumberMethod.setAccessible(true);
        
        // 测试生成订单号
        String orderNumber1 = (String) generateOrderNumberMethod.invoke(ticketService);
        String orderNumber2 = (String) generateOrderNumberMethod.invoke(ticketService);
        
        assertNotNull(orderNumber1);
        assertNotNull(orderNumber2);
        assertTrue(orderNumber1.startsWith("O"));
        assertTrue(orderNumber2.startsWith("O"));
        assertNotEquals(orderNumber1, orderNumber2); // 应该生成不同的订单号
    }
    
    @Test
    public void testGenerateTicketNumber() throws Exception {
        // 使用反射测试私有方法
        Method generateTicketNumberMethod = TicketServiceImpl.class.getDeclaredMethod("generateTicketNumber");
        generateTicketNumberMethod.setAccessible(true);
        
        // 测试生成车票号
        String ticketNumber1 = (String) generateTicketNumberMethod.invoke(ticketService);
        String ticketNumber2 = (String) generateTicketNumberMethod.invoke(ticketService);
        
        assertNotNull(ticketNumber1);
        assertNotNull(ticketNumber2);
        assertTrue(ticketNumber1.startsWith("T"));
        assertTrue(ticketNumber2.startsWith("T"));
        assertNotEquals(ticketNumber1, ticketNumber2); // 应该生成不同的车票号
    }
    
    @Test
    public void testGetPassengerTypeText() throws Exception {
        // 使用反射测试私有方法
        Method getPassengerTypeTextMethod = TicketServiceImpl.class.getDeclaredMethod("getPassengerTypeText", Byte.class);
        getPassengerTypeTextMethod.setAccessible(true);
        
        // 测试各种乘客类型
        assertEquals("成人", getPassengerTypeTextMethod.invoke(ticketService, (byte) 1));
        assertEquals("儿童", getPassengerTypeTextMethod.invoke(ticketService, (byte) 2));
        assertEquals("学生", getPassengerTypeTextMethod.invoke(ticketService, (byte) 3));
        assertEquals("残疾军人", getPassengerTypeTextMethod.invoke(ticketService, (byte) 4));
        assertEquals("未知类型", getPassengerTypeTextMethod.invoke(ticketService, (byte) 99));
    }
    
    @Test
    public void testGetStationCity() throws Exception {
        // 使用反射测试私有方法
        Method getStationCityMethod = TicketServiceImpl.class.getDeclaredMethod("getStationCity", Long.class);
        getStationCityMethod.setAccessible(true);
        
        // 模拟站点和车站都存在
        TrainStop trainStop = new TrainStop();
        trainStop.setStopId(1L);
        trainStop.setStationId(100);
        
        Station station = new Station();
        station.setStationId(100);
        station.setCity("北京");
        
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.of(trainStop));
        when(stationRepository.findById(100)).thenReturn(Optional.of(station));
        
        assertEquals("北京", getStationCityMethod.invoke(ticketService, 1L));
        
        // 测试站点不存在
        when(trainStopRepository.findByStopId(999L)).thenReturn(Optional.empty());
        assertEquals("未知城市", getStationCityMethod.invoke(ticketService, 999L));
        
        // 测试车站不存在
        TrainStop trainStop2 = new TrainStop();
        trainStop2.setStopId(2L);
        trainStop2.setStationId(200);
        when(trainStopRepository.findByStopId(2L)).thenReturn(Optional.of(trainStop2));
        when(stationRepository.findById(200)).thenReturn(Optional.empty());
        assertEquals("未知城市", getStationCityMethod.invoke(ticketService, 2L));
        
        // 测试异常情况
        when(trainStopRepository.findByStopId(888L)).thenThrow(new RuntimeException("数据库异常"));
        assertEquals("未知城市", getStationCityMethod.invoke(ticketService, 888L));
    }
    
    @Test
    public void testCalculateTicketPrice() throws Exception {
        // 使用反射测试私有方法
        Method calculateTicketPriceMethod = TicketServiceImpl.class.getDeclaredMethod("calculateTicketPrice", 
            Integer.class, Long.class, Long.class, LocalDate.class, Integer.class, Byte.class);
        calculateTicketPriceMethod.setAccessible(true);
        
        LocalDate travelDate = LocalDate.now();
        
        // 模拟库存存在
        TicketInventory inventory = new TicketInventory();
        inventory.setPrice(BigDecimal.valueOf(100.0));
        when(ticketInventoryDAO.findByKey(1, 1L, 2L, travelDate, 1)).thenReturn(Optional.of(inventory));
        
        // 测试成人票
        BigDecimal adultPrice = (BigDecimal) calculateTicketPriceMethod.invoke(ticketService, 1, 1L, 2L, travelDate, 1, (byte) 1);
        assertEquals(BigDecimal.valueOf(100.0), adultPrice);
        
        // 测试儿童票
        BigDecimal childPrice = (BigDecimal) calculateTicketPriceMethod.invoke(ticketService, 1, 1L, 2L, travelDate, 1, (byte) 2);
        assertEquals(0, childPrice.compareTo(BigDecimal.valueOf(50.0)));
        
        // 测试学生票
        BigDecimal studentPrice = (BigDecimal) calculateTicketPriceMethod.invoke(ticketService, 1, 1L, 2L, travelDate, 1, (byte) 3);
        assertEquals(0, studentPrice.compareTo(BigDecimal.valueOf(80.0)));
        
        // 测试残疾票
        BigDecimal disabledPrice = (BigDecimal) calculateTicketPriceMethod.invoke(ticketService, 1, 1L, 2L, travelDate, 1, (byte) 4);
        assertEquals(0, disabledPrice.compareTo(BigDecimal.valueOf(50.0)));
        
        // 测试军人票
        BigDecimal militaryPrice = (BigDecimal) calculateTicketPriceMethod.invoke(ticketService, 1, 1L, 2L, travelDate, 1, (byte) 5);
        assertEquals(0, militaryPrice.compareTo(BigDecimal.valueOf(50.0)));
        
        // 测试未知票种
        BigDecimal unknownPrice = (BigDecimal) calculateTicketPriceMethod.invoke(ticketService, 1, 1L, 2L, travelDate, 1, (byte) 99);
        assertEquals(BigDecimal.valueOf(100.0), unknownPrice);
        
        // 测试库存不存在
        when(ticketInventoryDAO.findByKey(999, 1L, 2L, travelDate, 1)).thenReturn(Optional.empty());
        BigDecimal defaultPrice = (BigDecimal) calculateTicketPriceMethod.invoke(ticketService, 999, 1L, 2L, travelDate, 1, (byte) 1);
        assertEquals(BigDecimal.valueOf(100.0), defaultPrice);
    }
    
    @Test
    public void testCalculateNewTicketPrice() throws Exception {
        // 使用反射测试私有方法
        Method calculateNewTicketPriceMethod = TicketServiceImpl.class.getDeclaredMethod("calculateNewTicketPrice", 
            Integer.class, Long.class, Long.class, LocalDate.class, Integer.class, Byte.class);
        calculateNewTicketPriceMethod.setAccessible(true);
        
        LocalDate travelDate = LocalDate.now();
        
        // 模拟库存存在
        TicketInventory inventory = new TicketInventory();
        inventory.setPrice(BigDecimal.valueOf(100.0));
        when(ticketInventoryDAO.findByKey(1, 1L, 2L, travelDate, 1)).thenReturn(Optional.of(inventory));
        
        // 测试各种票种
        assertEquals(0, ((BigDecimal) calculateNewTicketPriceMethod.invoke(ticketService, 1, 1L, 2L, travelDate, 1, (byte) 1)).compareTo(BigDecimal.valueOf(100.0)));
        assertEquals(0, ((BigDecimal) calculateNewTicketPriceMethod.invoke(ticketService, 1, 1L, 2L, travelDate, 1, (byte) 2)).compareTo(BigDecimal.valueOf(50.0)));
        assertEquals(0, ((BigDecimal) calculateNewTicketPriceMethod.invoke(ticketService, 1, 1L, 2L, travelDate, 1, (byte) 3)).compareTo(BigDecimal.valueOf(80.0)));
        assertEquals(BigDecimal.ZERO, calculateNewTicketPriceMethod.invoke(ticketService, 1, 1L, 2L, travelDate, 1, (byte) 4));
        assertEquals(BigDecimal.ZERO, calculateNewTicketPriceMethod.invoke(ticketService, 1, 1L, 2L, travelDate, 1, (byte) 5));
        assertEquals(0, ((BigDecimal) calculateNewTicketPriceMethod.invoke(ticketService, 1, 1L, 2L, travelDate, 1, (byte) 99)).compareTo(BigDecimal.valueOf(100.0)));
        
        // 测试库存不存在
        when(ticketInventoryDAO.findByKey(999, 1L, 2L, travelDate, 1)).thenReturn(Optional.empty());
        assertEquals(BigDecimal.ZERO, calculateNewTicketPriceMethod.invoke(ticketService, 999, 1L, 2L, travelDate, 1, (byte) 1));
    }
    
    @Test
    public void testRollbackStockReductions() throws Exception {
        // 使用反射测试私有方法
        Method rollbackStockReductionsMethod = TicketServiceImpl.class.getDeclaredMethod("rollbackStockReductions", 
            List.class, BookingRequest.class);
        rollbackStockReductionsMethod.setAccessible(true);
        
        // 创建测试数据
        BookingRequest request = new BookingRequest();
        request.setTrainId(1);
        request.setDepartureStopId(1L);
        request.setArrivalStopId(2L);
        request.setTravelDate(LocalDate.now());
        
        List<BookingRequest.PassengerInfo> successfulReductions = new ArrayList<>();
        BookingRequest.PassengerInfo passengerInfo = new BookingRequest.PassengerInfo();
        passengerInfo.setCarriageTypeId(1);
        successfulReductions.add(passengerInfo);
        
        // 模拟Redis服务成功
        when(redisService.incrStock(1, 1L, 2L, request.getTravelDate(), 1, 1)).thenReturn(true);
        
        // 测试回滚成功
        rollbackStockReductionsMethod.invoke(ticketService, successfulReductions, request);
        
        // 验证调用
        verify(redisService).incrStock(1, 1L, 2L, request.getTravelDate(), 1, 1);
        
        // 测试空列表
        rollbackStockReductionsMethod.invoke(ticketService, new ArrayList<>(), request);
        
        // 测试Redis服务失败
        when(redisService.incrStock(1, 1L, 2L, request.getTravelDate(), 1, 1)).thenReturn(false);
        rollbackStockReductionsMethod.invoke(ticketService, successfulReductions, request);
        
        // 测试异常情况
        when(redisService.incrStock(1, 1L, 2L, request.getTravelDate(), 1, 1)).thenThrow(new RuntimeException("Redis异常"));
        rollbackStockReductionsMethod.invoke(ticketService, successfulReductions, request);
    }

    @Test
    public void testValidateStationInSameCity_Exception() throws Exception {
        // 使用反射测试私有方法
        Method validateStationInSameCityMethod = TicketServiceImpl.class.getDeclaredMethod("validateStationsInSameCity", Long.class, Long.class);
        validateStationInSameCityMethod.setAccessible(true);
        
        // 测试正常情况
        assertTrue((Boolean) validateStationInSameCityMethod.invoke(ticketService, 1L, 2L));
        
        // 为了覆盖catch分支，我们需要创建一个会抛出异常的TicketServiceImpl实例
        // 但是由于方法内部没有实际的数据库操作，我们无法直接触发异常
        
        // 由于方法内部只是简单的try-catch，我们需要通过其他方式来触发异常
        // 这里我们测试正常情况，因为方法内部没有实际的数据库操作
        // 实际上，这个方法的catch分支很难通过单元测试触发，因为方法内部没有实际的数据库操作
        // 但我们可以通过修改方法实现来增加测试覆盖
    }
    

    
    @Test
    public void testGetMyTicketsByDateRange_Success() {
        // 创建测试用户
        User user = new User();
        user.setUserId(1L);
        user.setRealName("张三");
        user.setPhoneNumber("13800138000");
        user.setEmail("zhangsan@example.com");
        user.setPassengerId(100L);
        
        // 创建测试乘客
        Passenger passenger = new Passenger();
        passenger.setPassengerId(100L);
        passenger.setRealName("张三");
        passenger.setIdCardNumber("110101199001011234");
        passenger.setPhoneNumber("13800138000");
        
        // 创建测试车票
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setOrderId(1L);
        ticket.setTrainId(1);
        ticket.setDepartureStopId(1L);
        ticket.setArrivalStopId(2L);
        ticket.setTravelDate(LocalDate.of(2024, 1, 15));
        ticket.setCarriageNumber("1");
        ticket.setSeatNumber("1A");
        ticket.setPrice(BigDecimal.valueOf(100.0));
        ticket.setTicketStatus((byte) 1);
        ticket.setTicketType((byte) 1);
        ticket.setPassengerId(100L);
        ticket.setCreatedTime(LocalDateTime.now());
        
        // 创建测试订单
        Order order = new Order();
        order.setOrderId(1L);
        order.setOrderNumber("O123456789");
        order.setUserId(1L);
        order.setOrderStatus((byte) 1);
        order.setPaymentTime(LocalDateTime.now());
        
        // 模拟Repository调用
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passengerRepository.findById(100L)).thenReturn(Optional.of(passenger));
        when(ticketRepository.findValidTicketsByPassengerAndDateRange(100L, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31)))
            .thenReturn(Arrays.asList(ticket));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        
        // 模拟其他依赖方法
        when(trainRepository.findById(1)).thenReturn(Optional.empty());
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.empty());
        when(trainStopRepository.findByStopId(2L)).thenReturn(Optional.empty());
        when(carriageTypeRepository.findById(1)).thenReturn(Optional.empty());
        
        // 执行测试
        MyTicketResponse response = ticketService.getMyTicketsByDateRange(1L, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));
        
        // 验证结果
        assertTrue("SUCCESS".equals(response.getStatus()));
        assertNotNull(response.getTickets());
        assertEquals(1, response.getTickets().size());
        
        // 验证Repository调用
        verify(userRepository).findById(1L);
        verify(ticketRepository).findValidTicketsByPassengerAndDateRange(100L, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));
    }
    
    @Test
    public void testGetMyTicketsByDateRange_UserNotExists() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        
        MyTicketResponse response = ticketService.getMyTicketsByDateRange(999L, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));
        
        assertFalse("SUCCESS".equals(response.getStatus()));
        assertEquals("用户不存在", response.getMessage());
    }
    
    @Test
    public void testGetMyTicketsByDateRange_UserNotRelatedToPassenger() {
        User user = new User();
        user.setUserId(1L);
        user.setPassengerId(null);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        MyTicketResponse response = ticketService.getMyTicketsByDateRange(1L, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));
        
        assertFalse("SUCCESS".equals(response.getStatus()));
        assertEquals("用户未关联乘客信息", response.getMessage());
    }
    
    @Test
    public void testGetMyTicketsByDateRange_NullStartDate() {
        User user = new User();
        user.setUserId(1L);
        user.setPassengerId(100L);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        MyTicketResponse response = ticketService.getMyTicketsByDateRange(1L, null, LocalDate.of(2024, 1, 31));
        
        assertFalse("SUCCESS".equals(response.getStatus()));
        assertEquals("开始日期和结束日期不能为空", response.getMessage());
    }
    
    @Test
    public void testGetMyTicketsByDateRange_NullEndDate() {
        User user = new User();
        user.setUserId(1L);
        user.setPassengerId(100L);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        MyTicketResponse response = ticketService.getMyTicketsByDateRange(1L, LocalDate.of(2024, 1, 1), null);
        
        assertFalse("SUCCESS".equals(response.getStatus()));
        assertEquals("开始日期和结束日期不能为空", response.getMessage());
    }
    
    @Test
    public void testGetMyTicketsByDateRange_InvalidDateRange() {
        User user = new User();
        user.setUserId(1L);
        user.setPassengerId(100L);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        MyTicketResponse response = ticketService.getMyTicketsByDateRange(1L, LocalDate.of(2024, 1, 31), LocalDate.of(2024, 1, 1));
        
        assertFalse("SUCCESS".equals(response.getStatus()));
        assertEquals("开始日期不能晚于结束日期", response.getMessage());
    }
    
    @Test
    public void testGetMyTicketsByDateRange_Exception() {
        when(userRepository.findById(1L)).thenThrow(new RuntimeException("数据库异常"));
        
        MyTicketResponse response = ticketService.getMyTicketsByDateRange(1L, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));
        
        assertFalse("SUCCESS".equals(response.getStatus()));
        assertTrue(response.getMessage().contains("获取本人车票失败"));
    }
    
    @Test
    public void testGetMyTicketsByStatusAndDateRange_Success() {
        // 创建测试用户
        User user = new User();
        user.setUserId(1L);
        user.setRealName("张三");
        user.setPhoneNumber("13800138000");
        user.setEmail("zhangsan@example.com");
        user.setPassengerId(100L);
        
        // 创建测试乘客
        Passenger passenger = new Passenger();
        passenger.setPassengerId(100L);
        passenger.setRealName("张三");
        passenger.setIdCardNumber("110101199001011234");
        passenger.setPhoneNumber("13800138000");
        
        // 创建测试车票
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setOrderId(1L);
        ticket.setTrainId(1);
        ticket.setDepartureStopId(1L);
        ticket.setArrivalStopId(2L);
        ticket.setTravelDate(LocalDate.of(2024, 1, 15));
        ticket.setCarriageNumber("1");
        ticket.setSeatNumber("1A");
        ticket.setPrice(BigDecimal.valueOf(100.0));
        ticket.setTicketStatus((byte) 1);
        ticket.setTicketType((byte) 1);
        ticket.setPassengerId(100L);
        ticket.setCreatedTime(LocalDateTime.now());
        
        // 创建测试订单
        Order order = new Order();
        order.setOrderId(1L);
        order.setOrderNumber("O123456789");
        order.setUserId(1L);
        order.setOrderStatus((byte) 1);
        order.setPaymentTime(LocalDateTime.now());
        
        // 模拟Repository调用
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passengerRepository.findById(100L)).thenReturn(Optional.of(passenger));
        when(ticketRepository.findByPassengerIdAndStatusAndDateRange(100L, (byte) 1, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31)))
            .thenReturn(Arrays.asList(ticket));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        
        // 模拟其他依赖方法
        when(trainRepository.findById(1)).thenReturn(Optional.empty());
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.empty());
        when(trainStopRepository.findByStopId(2L)).thenReturn(Optional.empty());
        when(carriageTypeRepository.findById(1)).thenReturn(Optional.empty());
        
        // 执行测试
        MyTicketResponse response = ticketService.getMyTicketsByStatusAndDateRange(1L, (byte) 1, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));
        
        // 验证结果
        assertTrue("SUCCESS".equals(response.getStatus()));
        assertNotNull(response.getTickets());
        assertEquals(1, response.getTickets().size());
        
        // 验证Repository调用
        verify(userRepository).findById(1L);
        verify(ticketRepository).findByPassengerIdAndStatusAndDateRange(100L, (byte) 1, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));
    }
    
    @Test
    public void testGetMyTicketsByStatusAndDateRange_UserNotExists() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        
        MyTicketResponse response = ticketService.getMyTicketsByStatusAndDateRange(999L, (byte) 1, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));
        
        assertFalse("SUCCESS".equals(response.getStatus()));
        assertEquals("用户不存在", response.getMessage());
    }
    
    @Test
    public void testGetMyTicketsByStatusAndDateRange_UserNotRelatedToPassenger() {
        User user = new User();
        user.setUserId(1L);
        user.setPassengerId(null);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        MyTicketResponse response = ticketService.getMyTicketsByStatusAndDateRange(1L, (byte) 1, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));
        
        assertFalse("SUCCESS".equals(response.getStatus()));
        assertEquals("用户未关联乘客信息", response.getMessage());
    }
    
    @Test
    public void testGetMyTicketsByStatusAndDateRange_NullStartDate() {
        User user = new User();
        user.setUserId(1L);
        user.setPassengerId(100L);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        MyTicketResponse response = ticketService.getMyTicketsByStatusAndDateRange(1L, (byte) 1, null, LocalDate.of(2024, 1, 31));
        
        assertFalse("SUCCESS".equals(response.getStatus()));
        assertEquals("开始日期和结束日期不能为空", response.getMessage());
    }
    
    @Test
    public void testGetMyTicketsByStatusAndDateRange_NullEndDate() {
        User user = new User();
        user.setUserId(1L);
        user.setPassengerId(100L);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        MyTicketResponse response = ticketService.getMyTicketsByStatusAndDateRange(1L, (byte) 1, LocalDate.of(2024, 1, 1), null);
        
        assertFalse("SUCCESS".equals(response.getStatus()));
        assertEquals("开始日期和结束日期不能为空", response.getMessage());
    }
    
    @Test
    public void testGetMyTicketsByStatusAndDateRange_InvalidDateRange() {
        User user = new User();
        user.setUserId(1L);
        user.setPassengerId(100L);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        MyTicketResponse response = ticketService.getMyTicketsByStatusAndDateRange(1L, (byte) 1, LocalDate.of(2024, 1, 31), LocalDate.of(2024, 1, 1));
        
        assertFalse("SUCCESS".equals(response.getStatus()));
        assertEquals("开始日期不能晚于结束日期", response.getMessage());
    }
    
    @Test
    public void testGetMyTicketsByStatusAndDateRange_Exception() {
        when(userRepository.findById(1L)).thenThrow(new RuntimeException("数据库异常"));
        
        MyTicketResponse response = ticketService.getMyTicketsByStatusAndDateRange(1L, (byte) 1, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));
        
        assertFalse("SUCCESS".equals(response.getStatus()));
        assertTrue(response.getMessage().contains("获取本人车票失败"));
    }

    @Test
    public void testGetTicketDetail_Success_ByPassengerId() {
        // 创建测试数据
        User user = new User();
        user.setUserId(1L);
        user.setPassengerId(100L);
        
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setOrderId(1L);
        ticket.setTrainId(1);
        ticket.setDepartureStopId(1L);
        ticket.setArrivalStopId(2L);
        ticket.setTravelDate(LocalDate.of(2024, 1, 15));
        ticket.setCarriageNumber("1");
        ticket.setSeatNumber("1A");
        ticket.setPrice(BigDecimal.valueOf(100.0));
        ticket.setTicketStatus((byte) 1);
        ticket.setTicketType((byte) 1);
        ticket.setPassengerId(100L);
        ticket.setCarriageTypeId(1); // 添加车厢类型ID
        ticket.setCreatedTime(LocalDateTime.now());
        
        Order order = new Order();
        order.setOrderId(1L);
        order.setOrderNumber("O123456789");
        order.setUserId(1L);
        order.setOrderStatus((byte) 1);
        order.setPaymentTime(LocalDateTime.now());
        
        Passenger passenger = new Passenger();
        passenger.setPassengerId(100L);
        passenger.setRealName("张三");
        passenger.setIdCardNumber("110101199001011234");
        passenger.setPhoneNumber("13800138000");
        passenger.setPassengerType((byte) 1);
        
        // 模拟Repository调用
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(passengerRepository.findById(100L)).thenReturn(Optional.of(passenger));
        
        // 模拟其他依赖方法
        when(trainRepository.findById(1)).thenReturn(Optional.empty());
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.empty());
        when(trainStopRepository.findByStopId(2L)).thenReturn(Optional.empty());
        when(carriageTypeRepository.findById(1)).thenReturn(Optional.empty());
        when(stationRepository.findById(any())).thenReturn(Optional.empty());
        
        // 执行测试
        TicketDetailResponse response = ticketService.getTicketDetail(1L, 1L);
        
        // 验证结果
        assertTrue("SUCCESS".equals(response.getStatus()));
        assertNotNull(response.getTicket());
        assertEquals(1L, response.getTicket().getTicketId());
        assertEquals("张三", response.getTicket().getPassengerName());
        
        // 验证Repository调用
        verify(ticketRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(orderRepository).findById(1L);
        verify(passengerRepository).findById(100L);
    }
    
    @Test
    public void testGetTicketDetail_Success_ByOrderUserId() {
        // 创建测试数据 - 用户乘客ID不匹配，但订单用户ID匹配
        User user = new User();
        user.setUserId(1L);
        user.setPassengerId(999L); // 不匹配的乘客ID
        
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setOrderId(1L);
        ticket.setTrainId(1);
        ticket.setDepartureStopId(1L);
        ticket.setArrivalStopId(2L);
        ticket.setTravelDate(LocalDate.of(2024, 1, 15));
        ticket.setCarriageNumber("1");
        ticket.setSeatNumber("1A");
        ticket.setPrice(BigDecimal.valueOf(100.0));
        ticket.setTicketStatus((byte) 1);
        ticket.setTicketType((byte) 1);
        ticket.setPassengerId(100L);
        ticket.setCreatedTime(LocalDateTime.now());
        
        Order order = new Order();
        order.setOrderId(1L);
        order.setOrderNumber("O123456789");
        order.setUserId(1L); // 匹配的用户ID
        order.setOrderStatus((byte) 1);
        order.setPaymentTime(LocalDateTime.now());
        
        Passenger passenger = new Passenger();
        passenger.setPassengerId(100L);
        passenger.setRealName("张三");
        passenger.setIdCardNumber("110101199001011234");
        passenger.setPhoneNumber("13800138000");
        passenger.setPassengerType((byte) 1);
        
        // 模拟Repository调用
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(passengerRepository.findById(100L)).thenReturn(Optional.of(passenger));
        
        // 模拟其他依赖方法
        when(trainRepository.findById(1)).thenReturn(Optional.empty());
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.empty());
        when(trainStopRepository.findByStopId(2L)).thenReturn(Optional.empty());
        when(carriageTypeRepository.findById(1)).thenReturn(Optional.empty());
        when(stationRepository.findById(any())).thenReturn(Optional.empty());
        
        // 执行测试
        TicketDetailResponse response = ticketService.getTicketDetail(1L, 1L);
        
        // 验证结果
        assertTrue("SUCCESS".equals(response.getStatus()));
        assertNotNull(response.getTicket());
        assertEquals(1L, response.getTicket().getTicketId());
        
        // 验证Repository调用
        verify(ticketRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(orderRepository, times(2)).findById(1L); // 权限验证和获取订单信息各一次
        verify(passengerRepository).findById(100L);
    }
    
    @Test
    public void testGetTicketDetail_TicketNotExists() {
        when(ticketRepository.findById(999L)).thenReturn(Optional.empty());
        
        TicketDetailResponse response = ticketService.getTicketDetail(999L, 1L);
        
        assertFalse("SUCCESS".equals(response.getStatus()));
        assertEquals("车票不存在", response.getMessage());
        
        verify(ticketRepository).findById(999L);
        verify(userRepository, never()).findById(any());
    }
    
    @Test
    public void testGetTicketDetail_UserNotExists() {
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setPassengerId(100L);
        
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        
        TicketDetailResponse response = ticketService.getTicketDetail(1L, 999L);
        
        assertFalse("SUCCESS".equals(response.getStatus()));
        assertEquals("用户不存在", response.getMessage());
        
        verify(ticketRepository).findById(1L);
        verify(userRepository).findById(999L);
    }
    
    @Test
    public void testGetTicketDetail_NoPermission_UserPassengerIdNull() {
        User user = new User();
        user.setUserId(1L);
        user.setPassengerId(null); // 用户没有关联乘客ID
        
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setOrderId(1L);
        ticket.setPassengerId(100L);
        
        Order order = new Order();
        order.setOrderId(1L);
        order.setUserId(999L); // 订单用户ID不匹配
        
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        
        TicketDetailResponse response = ticketService.getTicketDetail(1L, 1L);
        
        assertFalse("SUCCESS".equals(response.getStatus()));
        assertEquals("无权限查看该车票详情", response.getMessage());
        
        verify(ticketRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(orderRepository).findById(1L);
        verify(passengerRepository, never()).findById(any());
    }
    
    @Test
    public void testGetTicketDetail_NoPermission_PassengerIdNotMatch() {
        User user = new User();
        user.setUserId(1L);
        user.setPassengerId(999L); // 不匹配的乘客ID
        
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setOrderId(1L);
        ticket.setPassengerId(100L); // 车票的乘客ID
        
        Order order = new Order();
        order.setOrderId(1L);
        order.setUserId(999L); // 订单用户ID也不匹配
        
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        
        TicketDetailResponse response = ticketService.getTicketDetail(1L, 1L);
        
        assertFalse("SUCCESS".equals(response.getStatus()));
        assertEquals("无权限查看该车票详情", response.getMessage());
        
        verify(ticketRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(orderRepository).findById(1L);
        verify(passengerRepository, never()).findById(any());
    }
    
    @Test
    public void testGetTicketDetail_NoPermission_OrderNotExists() {
        User user = new User();
        user.setUserId(1L);
        user.setPassengerId(999L); // 不匹配的乘客ID，强制走订单验证路径
        
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setOrderId(1L);
        ticket.setPassengerId(100L);
        
        Order order = new Order();
        order.setOrderId(1L);
        order.setUserId(1L);
        
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findById(1L))
            .thenReturn(Optional.of(order)) // 第一次查找，权限校验
            .thenReturn(Optional.empty());  // 第二次查找，订单信息不存在
        
        TicketDetailResponse response = ticketService.getTicketDetail(1L, 1L);
        
        assertFalse("SUCCESS".equals(response.getStatus()));
        assertEquals("订单信息不存在", response.getMessage());
        
        verify(ticketRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(orderRepository, times(2)).findById(1L); // 权限验证和获取订单信息各一次
        verify(passengerRepository, never()).findById(any());
    }
    
    @Test
    public void testGetTicketDetail_OrderNotExists() {
        User user = new User();
        user.setUserId(1L);
        user.setPassengerId(999L); // 不匹配的乘客ID，强制走订单验证路径
        
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setOrderId(1L);
        ticket.setPassengerId(100L);
        
        Order order = new Order();
        order.setOrderId(1L);
        order.setUserId(1L);
        
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findById(1L))
            .thenReturn(Optional.of(order)) // 第一次查找，权限校验
            .thenReturn(Optional.empty());  // 第二次查找，订单信息不存在
        
        TicketDetailResponse response = ticketService.getTicketDetail(1L, 1L);
        
        assertFalse("SUCCESS".equals(response.getStatus()));
        assertEquals("订单信息不存在", response.getMessage());
        
        verify(ticketRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(orderRepository, times(2)).findById(1L); // 权限验证和获取订单信息各一次
        verify(passengerRepository, never()).findById(any());
    }
    
    @Test
    public void testGetTicketDetail_PassengerNotExists() {
        User user = new User();
        user.setUserId(1L);
        user.setPassengerId(999L); // 不匹配的乘客ID，强制走订单验证路径
        
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setOrderId(1L);
        ticket.setPassengerId(100L);
        
        Order order = new Order();
        order.setOrderId(1L);
        order.setUserId(1L);
        
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(passengerRepository.findById(100L)).thenReturn(Optional.empty());
        
        TicketDetailResponse response = ticketService.getTicketDetail(1L, 1L);
        
        assertFalse("SUCCESS".equals(response.getStatus()));
        assertEquals("乘客信息不存在", response.getMessage());
        
        verify(ticketRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(orderRepository, times(2)).findById(1L);
        verify(passengerRepository).findById(100L);
    }
    
    @Test
    public void testGetTicketDetail_WithStationData() {
        // 创建测试数据
        User user = new User();
        user.setUserId(1L);
        user.setPassengerId(100L);
        
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setOrderId(1L);
        ticket.setTrainId(1);
        ticket.setDepartureStopId(1L);
        ticket.setArrivalStopId(2L);
        ticket.setTravelDate(LocalDate.of(2024, 1, 15));
        ticket.setCarriageNumber("1");
        ticket.setSeatNumber("1A");
        ticket.setPrice(BigDecimal.valueOf(100.0));
        ticket.setTicketStatus((byte) 1);
        ticket.setTicketType((byte) 1);
        ticket.setPassengerId(100L);
        ticket.setCarriageTypeId(1); // 添加车厢类型ID
        ticket.setCreatedTime(LocalDateTime.now());
        
        Order order = new Order();
        order.setOrderId(1L);
        order.setOrderNumber("O123456789");
        order.setUserId(1L);
        order.setOrderStatus((byte) 1);
        order.setPaymentTime(LocalDateTime.now());
        
        Passenger passenger = new Passenger();
        passenger.setPassengerId(100L);
        passenger.setRealName("张三");
        passenger.setIdCardNumber("110101199001011234");
        passenger.setPhoneNumber("13800138000");
        passenger.setPassengerType((byte) 1);
        
        // 模拟站点数据
        TrainStop departureStop = new TrainStop();
        departureStop.setStopId(1L);
        departureStop.setStationId(100);
        departureStop.setDepartureTime(LocalTime.of(9, 0));
        
        TrainStop arrivalStop = new TrainStop();
        arrivalStop.setStopId(2L);
        arrivalStop.setStationId(200);
        arrivalStop.setArrivalTime(LocalTime.of(10, 30));
        
        Station departureStation = new Station();
        departureStation.setStationId(100);
        departureStation.setStationName("北京站");
        departureStation.setCity("北京");
        
        Station arrivalStation = new Station();
        arrivalStation.setStationId(200);
        arrivalStation.setStationName("上海站");
        arrivalStation.setCity("上海");
        
        Train train = new Train();
        train.setTrainId(1);
        train.setTrainNumber("G101");
        
        CarriageType carriageType = new CarriageType();
        carriageType.setTypeId(1);
        carriageType.setTypeName("硬座");
        
        // 模拟Repository调用
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(passengerRepository.findById(100L)).thenReturn(Optional.of(passenger));
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findByStopId(2L)).thenReturn(Optional.of(arrivalStop));
        when(stationRepository.findById(100)).thenReturn(Optional.of(departureStation));
        when(stationRepository.findById(200)).thenReturn(Optional.of(arrivalStation));
        when(trainRepository.findById(1)).thenReturn(Optional.of(train));
        when(carriageTypeRepository.findById(1)).thenReturn(Optional.of(carriageType));
        
        // 执行测试
        TicketDetailResponse response = ticketService.getTicketDetail(1L, 1L);
        
        // 验证结果
        assertTrue("SUCCESS".equals(response.getStatus()));
        assertNotNull(response.getTicket());
        assertEquals("北京站", response.getTicket().getDepartureStationName());
        assertEquals("北京", response.getTicket().getDepartureCity());
        assertEquals("上海站", response.getTicket().getArrivalStationName());
        assertEquals("上海", response.getTicket().getArrivalCity());
        assertEquals("G101", response.getTicket().getTrainNumber());
        assertEquals("硬座", response.getTicket().getCarriageTypeName());
        assertEquals(LocalTime.of(9, 0), response.getTicket().getDepartureTime());
        assertEquals(LocalTime.of(10, 30), response.getTicket().getArrivalTime());
    }
    
    @Test
    public void testGetTicketDetail_Exception() {
        when(ticketRepository.findById(1L)).thenThrow(new RuntimeException("数据库异常"));
        
        TicketDetailResponse response = ticketService.getTicketDetail(1L, 1L);
        
        assertFalse("SUCCESS".equals(response.getStatus()));
        assertTrue(response.getMessage().contains("获取车票详情失败"));
        
        verify(ticketRepository).findById(1L);
    }

    @Test
    public void testBookTickets_Success() {
        // 创建测试数据
        BookingRequest request = new BookingRequest();
        request.setUserId(1L);
        request.setTrainId(1);
        request.setDepartureStopId(1L);
        request.setArrivalStopId(2L);
        request.setTravelDate(LocalDate.of(2024, 1, 15));
        request.setCarriageTypeId(1);
        
        List<BookingRequest.PassengerInfo> passengers = new ArrayList<>();
        BookingRequest.PassengerInfo passenger1 = new BookingRequest.PassengerInfo();
        passenger1.setPassengerId(100L);
        passenger1.setTicketType((byte) 1);
        passenger1.setCarriageTypeId(1);
        passengers.add(passenger1);
        
        BookingRequest.PassengerInfo passenger2 = new BookingRequest.PassengerInfo();
        passenger2.setPassengerId(101L);
        passenger2.setTicketType((byte) 2);
        passenger2.setCarriageTypeId(2);
        passengers.add(passenger2);
        
        request.setPassengers(passengers);
        
        // Mock 乘客关系验证
        when(userPassengerRelationRepository.existsByUserIdAndPassengerId(1L, 100L)).thenReturn(true);
        when(userPassengerRelationRepository.existsByUserIdAndPassengerId(1L, 101L)).thenReturn(true);
        
        // Mock 时间冲突检查
        when(timeConflictService.checkTimeConflict(100L, LocalDate.of(2024, 1, 15), 1, 1L, 2L))
            .thenReturn(new ArrayList<>());
        when(timeConflictService.checkTimeConflict(101L, LocalDate.of(2024, 1, 15), 1, 1L, 2L))
            .thenReturn(new ArrayList<>());
        
        // Mock 分布式锁
        when(redisService.tryLock("booking:1:2024-01-15:1", 5, 30)).thenReturn(true);
        when(redisService.tryLock("booking:1:2024-01-15:2", 5, 30)).thenReturn(true);
        
        // Mock 库存扣减
        when(redisService.decrStock(1, 1L, 2L, LocalDate.of(2024, 1, 15), 1, 1)).thenReturn(true);
        when(redisService.decrStock(1, 1L, 2L, LocalDate.of(2024, 1, 15), 2, 1)).thenReturn(true);
        
        // Mock 订单号生成
        when(redisService.generateOrderNumber()).thenReturn("O123456789");
        
        // Mock RabbitMQ消息发送
        doNothing().when(rabbitTemplate).convertAndSend(eq("order.exchange"), eq("order.create"), any(OrderMessage.class));
        
        // 执行测试
        BookingResponse response = ticketService.bookTickets(request);
        
        // 验证结果
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("购票成功", response.getMessage());
        assertEquals("O123456789", response.getOrderNumber());
        assertNotNull(response.getOrderTime());
        
        // 验证方法调用
        verify(userPassengerRelationRepository).existsByUserIdAndPassengerId(1L, 100L);
        verify(userPassengerRelationRepository).existsByUserIdAndPassengerId(1L, 101L);
        verify(timeConflictService).checkTimeConflict(100L, LocalDate.of(2024, 1, 15), 1, 1L, 2L);
        verify(timeConflictService).checkTimeConflict(101L, LocalDate.of(2024, 1, 15), 1, 1L, 2L);
        verify(redisService).tryLock("booking:1:2024-01-15:1", 5, 30);
        verify(redisService).tryLock("booking:1:2024-01-15:2", 5, 30);
        verify(redisService).decrStock(1, 1L, 2L, LocalDate.of(2024, 1, 15), 1, 1);
        verify(redisService).decrStock(1, 1L, 2L, LocalDate.of(2024, 1, 15), 2, 1);
        verify(redisService).generateOrderNumber();
        verify(rabbitTemplate).convertAndSend(eq("order.exchange"), eq("order.create"), any(OrderMessage.class));
        verify(redisService, times(2)).unlock(anyString());
    }
    
    @Test
    public void testBookTickets_PassengerRelationNotExists() {
        // 创建测试数据
        BookingRequest request = new BookingRequest();
        request.setUserId(1L);
        request.setTrainId(1);
        request.setDepartureStopId(1L);
        request.setArrivalStopId(2L);
        request.setTravelDate(LocalDate.of(2024, 1, 15));
        request.setCarriageTypeId(1);
        
        List<BookingRequest.PassengerInfo> passengers = new ArrayList<>();
        BookingRequest.PassengerInfo passenger = new BookingRequest.PassengerInfo();
        passenger.setPassengerId(100L);
        passenger.setTicketType((byte) 1);
        passenger.setCarriageTypeId(1);
        passengers.add(passenger);
        request.setPassengers(passengers);
        
        // Mock 乘客关系验证失败
        when(userPassengerRelationRepository.existsByUserIdAndPassengerId(1L, 100L)).thenReturn(false);
        
        // 执行测试
        BookingResponse response = ticketService.bookTickets(request);
        
        // 验证结果
        assertEquals("FAILED", response.getStatus());
        assertEquals("乘客ID 100 与用户无关联关系", response.getMessage());
        
        // 验证方法调用
        verify(userPassengerRelationRepository).existsByUserIdAndPassengerId(1L, 100L);
        verify(timeConflictService, never()).checkTimeConflict(any(), any(), any(), any(), any());
        verify(redisService, never()).tryLock(anyString(), anyInt(), anyInt());
    }
    
    @Test
    public void testBookTickets_TimeConflict() {
        // 创建测试数据
        BookingRequest request = new BookingRequest();
        request.setUserId(1L);
        request.setTrainId(1);
        request.setDepartureStopId(1L);
        request.setArrivalStopId(2L);
        request.setTravelDate(LocalDate.of(2024, 1, 15));
        request.setCarriageTypeId(1);
        
        List<BookingRequest.PassengerInfo> passengers = new ArrayList<>();
        BookingRequest.PassengerInfo passenger = new BookingRequest.PassengerInfo();
        passenger.setPassengerId(100L);
        passenger.setTicketType((byte) 1);
        passenger.setCarriageTypeId(1);
        passengers.add(passenger);
        request.setPassengers(passengers);
        
        // Mock 乘客关系验证通过
        when(userPassengerRelationRepository.existsByUserIdAndPassengerId(1L, 100L)).thenReturn(true);
        
        // Mock 时间冲突检查失败
        List<Ticket> conflictTickets = new ArrayList<>();
        Ticket conflictTicket = new Ticket();
        conflictTicket.setTicketId(1L);
        conflictTickets.add(conflictTicket);
        
        when(timeConflictService.checkTimeConflict(100L, LocalDate.of(2024, 1, 15), 1, 1L, 2L))
            .thenReturn(conflictTickets);
        when(timeConflictService.generateConflictMessage(conflictTickets))
            .thenReturn("存在时间冲突的车票");
        
        // 执行测试
        BookingResponse response = ticketService.bookTickets(request);
        
        // 验证结果
        assertEquals("FAILED", response.getStatus());
        assertEquals("乘客ID 100 存在时间冲突的车票", response.getMessage());
        
        // 验证方法调用
        verify(userPassengerRelationRepository).existsByUserIdAndPassengerId(1L, 100L);
        verify(timeConflictService).checkTimeConflict(100L, LocalDate.of(2024, 1, 15), 1, 1L, 2L);
        verify(timeConflictService).generateConflictMessage(conflictTickets);
        verify(redisService, never()).tryLock(anyString(), anyInt(), anyInt());
    }
    
    @Test
    public void testBookTickets_LockAcquisitionFailed() {
        // 创建测试数据
        BookingRequest request = new BookingRequest();
        request.setUserId(1L);
        request.setTrainId(1);
        request.setDepartureStopId(1L);
        request.setArrivalStopId(2L);
        request.setTravelDate(LocalDate.of(2024, 1, 15));
        request.setCarriageTypeId(1);
        
        List<BookingRequest.PassengerInfo> passengers = new ArrayList<>();
        BookingRequest.PassengerInfo passenger = new BookingRequest.PassengerInfo();
        passenger.setPassengerId(100L);
        passenger.setTicketType((byte) 1);
        passenger.setCarriageTypeId(1);
        passengers.add(passenger);
        request.setPassengers(passengers);
        
        // Mock 乘客关系验证通过
        when(userPassengerRelationRepository.existsByUserIdAndPassengerId(1L, 100L)).thenReturn(true);
        
        // Mock 时间冲突检查通过
        when(timeConflictService.checkTimeConflict(100L, LocalDate.of(2024, 1, 15), 1, 1L, 2L))
            .thenReturn(new ArrayList<>());
        
        // Mock 分布式锁获取失败
        when(redisService.tryLock("booking:1:2024-01-15:1", 5, 30)).thenReturn(false);
        
        // 执行测试
        BookingResponse response = ticketService.bookTickets(request);
        
        // 验证结果
        assertEquals("FAILED", response.getStatus());
        assertEquals("系统繁忙，请稍后重试", response.getMessage());
        
        // 验证方法调用
        verify(userPassengerRelationRepository).existsByUserIdAndPassengerId(1L, 100L);
        verify(timeConflictService).checkTimeConflict(100L, LocalDate.of(2024, 1, 15), 1, 1L, 2L);
        verify(redisService).tryLock("booking:1:2024-01-15:1", 5, 30);
        verify(redisService, never()).decrStock(any(), any(), any(), any(), any(), anyInt());
    }
    
    @Test
    public void testBookTickets_InsufficientStock() {
        // 创建测试数据
        BookingRequest request = new BookingRequest();
        request.setUserId(1L);
        request.setTrainId(1);
        request.setDepartureStopId(1L);
        request.setArrivalStopId(2L);
        request.setTravelDate(LocalDate.of(2024, 1, 15));
        request.setCarriageTypeId(1);
        
        List<BookingRequest.PassengerInfo> passengers = new ArrayList<>();
        BookingRequest.PassengerInfo passenger = new BookingRequest.PassengerInfo();
        passenger.setPassengerId(100L);
        passenger.setTicketType((byte) 1);
        passenger.setCarriageTypeId(1);
        passengers.add(passenger);
        request.setPassengers(passengers);
        
        // Mock 乘客关系验证通过
        when(userPassengerRelationRepository.existsByUserIdAndPassengerId(1L, 100L)).thenReturn(true);
        
        // Mock 时间冲突检查通过
        when(timeConflictService.checkTimeConflict(100L, LocalDate.of(2024, 1, 15), 1, 1L, 2L))
            .thenReturn(new ArrayList<>());
        
        // Mock 分布式锁获取成功
        when(redisService.tryLock("booking:1:2024-01-15:1", 5, 30)).thenReturn(true);
        
        // Mock 库存扣减失败
        when(redisService.decrStock(1, 1L, 2L, LocalDate.of(2024, 1, 15), 1, 1)).thenReturn(false);
        
        // 执行测试
        BookingResponse response = ticketService.bookTickets(request);
        
        // 验证结果
        assertEquals("INSUFFICIENT_STOCK", response.getStatus());
        assertEquals("乘客ID 100 选择的席别余票不足", response.getMessage());
        
        // 验证方法调用
        verify(userPassengerRelationRepository).existsByUserIdAndPassengerId(1L, 100L);
        verify(timeConflictService).checkTimeConflict(100L, LocalDate.of(2024, 1, 15), 1, 1L, 2L);
        verify(redisService).tryLock("booking:1:2024-01-15:1", 5, 30);
        verify(redisService).decrStock(1, 1L, 2L, LocalDate.of(2024, 1, 15), 1, 1);
        verify(redisService).unlock("booking:1:2024-01-15:1");
        verify(redisService, never()).generateOrderNumber();
    }
    
    @Test
    public void testBookTickets_RabbitMQSendFailed() {
        // 创建测试数据
        BookingRequest request = new BookingRequest();
        request.setUserId(1L);
        request.setTrainId(1);
        request.setDepartureStopId(1L);
        request.setArrivalStopId(2L);
        request.setTravelDate(LocalDate.of(2024, 1, 15));
        request.setCarriageTypeId(1);
        
        List<BookingRequest.PassengerInfo> passengers = new ArrayList<>();
        BookingRequest.PassengerInfo passenger = new BookingRequest.PassengerInfo();
        passenger.setPassengerId(100L);
        passenger.setTicketType((byte) 1);
        passenger.setCarriageTypeId(1);
        passengers.add(passenger);
        request.setPassengers(passengers);
        
        // Mock 乘客关系验证通过
        when(userPassengerRelationRepository.existsByUserIdAndPassengerId(1L, 100L)).thenReturn(true);
        
        // Mock 时间冲突检查通过
        when(timeConflictService.checkTimeConflict(100L, LocalDate.of(2024, 1, 15), 1, 1L, 2L))
            .thenReturn(new ArrayList<>());
        
        // Mock 分布式锁获取成功
        when(redisService.tryLock("booking:1:2024-01-15:1", 5, 30)).thenReturn(true);
        
        // Mock 库存扣减成功
        when(redisService.decrStock(1, 1L, 2L, LocalDate.of(2024, 1, 15), 1, 1)).thenReturn(true);
        
        // Mock 订单号生成
        when(redisService.generateOrderNumber()).thenReturn("O123456789");
        
        // Mock RabbitMQ消息发送失败
        doThrow(new RuntimeException("RabbitMQ连接失败"))
            .when(rabbitTemplate).convertAndSend(eq("order.exchange"), eq("order.create"), any(OrderMessage.class));
        
        // 执行测试
        BookingResponse response = ticketService.bookTickets(request);
        
        // 验证结果
        assertEquals("FAILED", response.getStatus());
        assertEquals("系统繁忙，请稍后重试", response.getMessage());
        
        // 验证方法调用
        verify(userPassengerRelationRepository).existsByUserIdAndPassengerId(1L, 100L);
        verify(timeConflictService).checkTimeConflict(100L, LocalDate.of(2024, 1, 15), 1, 1L, 2L);
        verify(redisService).tryLock("booking:1:2024-01-15:1", 5, 30);
        verify(redisService).decrStock(1, 1L, 2L, LocalDate.of(2024, 1, 15), 1, 1);
        verify(redisService).generateOrderNumber();
        verify(rabbitTemplate).convertAndSend(eq("order.exchange"), eq("order.create"), any(OrderMessage.class));
        verify(redisService).unlock("booking:1:2024-01-15:1");
    }
    
    @Test
    public void testBookTickets_Exception() {
        // 创建测试数据
        BookingRequest request = new BookingRequest();
        request.setUserId(1L);
        request.setTrainId(1);
        request.setDepartureStopId(1L);
        request.setArrivalStopId(2L);
        request.setTravelDate(LocalDate.of(2024, 1, 15));
        request.setCarriageTypeId(1);
        
        List<BookingRequest.PassengerInfo> passengers = new ArrayList<>();
        BookingRequest.PassengerInfo passenger = new BookingRequest.PassengerInfo();
        passenger.setPassengerId(100L);
        passenger.setTicketType((byte) 1);
        passenger.setCarriageTypeId(1);
        passengers.add(passenger);
        request.setPassengers(passengers);
        
        // Mock 乘客关系验证抛出异常
        when(userPassengerRelationRepository.existsByUserIdAndPassengerId(1L, 100L))
            .thenThrow(new RuntimeException("数据库连接异常"));
        
        // 执行测试
        BookingResponse response = ticketService.bookTickets(request);
        
        // 验证结果
        assertEquals("FAILED", response.getStatus());
        assertEquals("系统异常，请稍后重试", response.getMessage());
        
        // 验证方法调用
        verify(userPassengerRelationRepository).existsByUserIdAndPassengerId(1L, 100L);
        verify(timeConflictService, never()).checkTimeConflict(any(), any(), any(), any(), any());
        verify(redisService, never()).tryLock(anyString(), anyInt(), anyInt());
    }
    
    @Test
    public void testBookTickets_MultipleCarriageTypes() {
        // 创建测试数据 - 多个不同席别
        BookingRequest request = new BookingRequest();
        request.setUserId(1L);
        request.setTrainId(1);
        request.setDepartureStopId(1L);
        request.setArrivalStopId(2L);
        request.setTravelDate(LocalDate.of(2024, 1, 15));
        request.setCarriageTypeId(1);
        
        List<BookingRequest.PassengerInfo> passengers = new ArrayList<>();
        
        // 第一个乘客 - 硬座
        BookingRequest.PassengerInfo passenger1 = new BookingRequest.PassengerInfo();
        passenger1.setPassengerId(100L);
        passenger1.setTicketType((byte) 1);
        passenger1.setCarriageTypeId(1);
        passengers.add(passenger1);
        
        // 第二个乘客 - 硬卧
        BookingRequest.PassengerInfo passenger2 = new BookingRequest.PassengerInfo();
        passenger2.setPassengerId(101L);
        passenger2.setTicketType((byte) 2);
        passenger2.setCarriageTypeId(2);
        passengers.add(passenger2);
        
        // 第三个乘客 - 软卧
        BookingRequest.PassengerInfo passenger3 = new BookingRequest.PassengerInfo();
        passenger3.setPassengerId(102L);
        passenger3.setTicketType((byte) 3);
        passenger3.setCarriageTypeId(3);
        passengers.add(passenger3);
        
        request.setPassengers(passengers);
        
        // Mock 乘客关系验证
        when(userPassengerRelationRepository.existsByUserIdAndPassengerId(1L, 100L)).thenReturn(true);
        when(userPassengerRelationRepository.existsByUserIdAndPassengerId(1L, 101L)).thenReturn(true);
        when(userPassengerRelationRepository.existsByUserIdAndPassengerId(1L, 102L)).thenReturn(true);
        
        // Mock 时间冲突检查
        when(timeConflictService.checkTimeConflict(100L, LocalDate.of(2024, 1, 15), 1, 1L, 2L))
            .thenReturn(new ArrayList<>());
        when(timeConflictService.checkTimeConflict(101L, LocalDate.of(2024, 1, 15), 1, 1L, 2L))
            .thenReturn(new ArrayList<>());
        when(timeConflictService.checkTimeConflict(102L, LocalDate.of(2024, 1, 15), 1, 1L, 2L))
            .thenReturn(new ArrayList<>());
        
        // Mock 分布式锁 - 三个不同席别
        when(redisService.tryLock("booking:1:2024-01-15:1", 5, 30)).thenReturn(true);
        when(redisService.tryLock("booking:1:2024-01-15:2", 5, 30)).thenReturn(true);
        when(redisService.tryLock("booking:1:2024-01-15:3", 5, 30)).thenReturn(true);
        
        // Mock 库存扣减
        when(redisService.decrStock(1, 1L, 2L, LocalDate.of(2024, 1, 15), 1, 1)).thenReturn(true);
        when(redisService.decrStock(1, 1L, 2L, LocalDate.of(2024, 1, 15), 2, 1)).thenReturn(true);
        when(redisService.decrStock(1, 1L, 2L, LocalDate.of(2024, 1, 15), 3, 1)).thenReturn(true);
        
        // Mock 订单号生成
        when(redisService.generateOrderNumber()).thenReturn("O123456789");
        
        // Mock RabbitMQ消息发送
        doNothing().when(rabbitTemplate).convertAndSend(eq("order.exchange"), eq("order.create"), any(OrderMessage.class));
        
        // 执行测试
        BookingResponse response = ticketService.bookTickets(request);
        
        // 验证结果
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("购票成功", response.getMessage());
        assertEquals("O123456789", response.getOrderNumber());
        
        // 验证方法调用 - 确保三个不同席别都被处理
        verify(redisService).tryLock("booking:1:2024-01-15:1", 5, 30);
        verify(redisService).tryLock("booking:1:2024-01-15:2", 5, 30);
        verify(redisService).tryLock("booking:1:2024-01-15:3", 5, 30);
        verify(redisService).decrStock(1, 1L, 2L, LocalDate.of(2024, 1, 15), 1, 1);
        verify(redisService).decrStock(1, 1L, 2L, LocalDate.of(2024, 1, 15), 2, 1);
        verify(redisService).decrStock(1, 1L, 2L, LocalDate.of(2024, 1, 15), 3, 1);
        verify(redisService, times(3)).unlock(anyString());
    }
    
    @Test
    public void testBookTickets_LockAcquisitionFailedForSecondCarriageType() {
        // 创建测试数据 - 两个不同席别
        BookingRequest request = new BookingRequest();
        request.setUserId(1L);
        request.setTrainId(1);
        request.setDepartureStopId(1L);
        request.setArrivalStopId(2L);
        request.setTravelDate(LocalDate.of(2024, 1, 15));
        request.setCarriageTypeId(1);
        
        List<BookingRequest.PassengerInfo> passengers = new ArrayList<>();
        
        // 第一个乘客 - 硬座
        BookingRequest.PassengerInfo passenger1 = new BookingRequest.PassengerInfo();
        passenger1.setPassengerId(100L);
        passenger1.setTicketType((byte) 1);
        passenger1.setCarriageTypeId(1);
        passengers.add(passenger1);
        
        // 第二个乘客 - 硬卧
        BookingRequest.PassengerInfo passenger2 = new BookingRequest.PassengerInfo();
        passenger2.setPassengerId(101L);
        passenger2.setTicketType((byte) 2);
        passenger2.setCarriageTypeId(2);
        passengers.add(passenger2);
        
        request.setPassengers(passengers);
        
        // Mock 乘客关系验证
        when(userPassengerRelationRepository.existsByUserIdAndPassengerId(1L, 100L)).thenReturn(true);
        when(userPassengerRelationRepository.existsByUserIdAndPassengerId(1L, 101L)).thenReturn(true);
        
        // Mock 时间冲突检查
        when(timeConflictService.checkTimeConflict(100L, LocalDate.of(2024, 1, 15), 1, 1L, 2L))
            .thenReturn(new ArrayList<>());
        when(timeConflictService.checkTimeConflict(101L, LocalDate.of(2024, 1, 15), 1, 1L, 2L))
            .thenReturn(new ArrayList<>());
        
        // Mock 分布式锁 - 第一个成功，第二个失败
        when(redisService.tryLock("booking:1:2024-01-15:1", 5, 30)).thenReturn(true);
        when(redisService.tryLock("booking:1:2024-01-15:2", 5, 30)).thenReturn(false);
        
        // 执行测试
        BookingResponse response = ticketService.bookTickets(request);
        
        // 验证结果
        assertEquals("FAILED", response.getStatus());
        assertEquals("系统繁忙，请稍后重试", response.getMessage());
        
        // 验证方法调用 - 确保第一个锁被释放
        verify(redisService).tryLock("booking:1:2024-01-15:1", 5, 30);
        verify(redisService).tryLock("booking:1:2024-01-15:2", 5, 30);
        verify(redisService, times(2)).unlock("booking:1:2024-01-15:1");
        verify(redisService, never()).decrStock(any(), any(), any(), any(), any(), anyInt());
    }
    
    @Test
    public void testBookTickets_StockReductionFailedForSecondPassenger() {
        // 创建测试数据 - 两个乘客
        BookingRequest request = new BookingRequest();
        request.setUserId(1L);
        request.setTrainId(1);
        request.setDepartureStopId(1L);
        request.setArrivalStopId(2L);
        request.setTravelDate(LocalDate.of(2024, 1, 15));
        request.setCarriageTypeId(1);
        
        List<BookingRequest.PassengerInfo> passengers = new ArrayList<>();
        
        // 第一个乘客
        BookingRequest.PassengerInfo passenger1 = new BookingRequest.PassengerInfo();
        passenger1.setPassengerId(100L);
        passenger1.setTicketType((byte) 1);
        passenger1.setCarriageTypeId(1);
        passengers.add(passenger1);
        
        // 第二个乘客
        BookingRequest.PassengerInfo passenger2 = new BookingRequest.PassengerInfo();
        passenger2.setPassengerId(101L);
        passenger2.setTicketType((byte) 2);
        passenger2.setCarriageTypeId(1);
        passengers.add(passenger2);
        
        request.setPassengers(passengers);
        
        // Mock 乘客关系验证
        when(userPassengerRelationRepository.existsByUserIdAndPassengerId(1L, 100L)).thenReturn(true);
        when(userPassengerRelationRepository.existsByUserIdAndPassengerId(1L, 101L)).thenReturn(true);
        
        // Mock 时间冲突检查
        when(timeConflictService.checkTimeConflict(100L, LocalDate.of(2024, 1, 15), 1, 1L, 2L))
            .thenReturn(new ArrayList<>());
        when(timeConflictService.checkTimeConflict(101L, LocalDate.of(2024, 1, 15), 1, 1L, 2L))
            .thenReturn(new ArrayList<>());
        
        // Mock 分布式锁
        when(redisService.tryLock("booking:1:2024-01-15:1", 5, 30)).thenReturn(true);
        
        // Mock 库存扣减 - 第一个成功，第二个失败
        when(redisService.decrStock(1, 1L, 2L, LocalDate.of(2024, 1, 15), 1, 1))
            .thenReturn(true)  // 第一次调用成功
            .thenReturn(false); // 第二次调用失败
        
        // 执行测试
        BookingResponse response = ticketService.bookTickets(request);
        
        // 验证结果
        assertEquals("INSUFFICIENT_STOCK", response.getStatus());
        assertEquals("乘客ID 101 选择的席别余票不足", response.getMessage());
        
        // 验证方法调用 - 确保第一个库存扣减被回滚
        verify(redisService, times(2)).decrStock(1, 1L, 2L, LocalDate.of(2024, 1, 15), 1, 1);
        verify(redisService).incrStock(1, 1L, 2L, LocalDate.of(2024, 1, 15), 1, 1); // 回滚
        verify(redisService).unlock("booking:1:2024-01-15:1");
    }
    
    // ==================== refundTickets 方法测试 ====================
    
    @Test
    @DisplayName("退票成功 - 订单还有剩余车票")
    public void testRefundTickets_Success_OrderHasRemainingTickets() {
        // 准备测试数据
        Long userId = 1L;
        Long orderId = 100L;
        List<Long> ticketIds = Arrays.asList(1L, 2L);
        
        RefundRequest request = new RefundRequest();
        request.setUserId(userId);
        request.setOrderId(orderId);
        request.setTicketIds(ticketIds);
        
        // Mock Redis锁
        when(redisService.tryLock(anyString(), anyLong(), anyLong())).thenReturn(true);
        
        // Mock订单查询
        Order order = new Order();
        order.setOrderId(orderId);
        order.setOrderNumber("ORD20241201001");
        order.setUserId(userId);
        order.setOrderStatus((byte) OrderStatus.PAID.getCode());
        order.setTotalAmount(new BigDecimal("200.00"));
        order.setTicketCount(3);
        when(orderRepository.findByOrderIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        
        // Mock车票查询
        Ticket ticket1 = new Ticket();
        ticket1.setTicketId(1L);
        ticket1.setOrderId(orderId);
        ticket1.setPrice(new BigDecimal("100.00"));
        ticket1.setTicketStatus((byte) TicketStatus.UNUSED.getCode());
        ticket1.setTrainId(1);
        ticket1.setDepartureStopId(1L);
        ticket1.setArrivalStopId(2L);
        ticket1.setTravelDate(LocalDate.now().plusDays(1));
        ticket1.setCarriageTypeId(1);
        ticket1.setSeatNumber("01A");
        ticket1.setCarriageNumber("1");
        
        Ticket ticket2 = new Ticket();
        ticket2.setTicketId(2L);
        ticket2.setOrderId(orderId);
        ticket2.setPrice(new BigDecimal("100.00"));
        ticket2.setTicketStatus((byte) TicketStatus.UNUSED.getCode());
        ticket2.setTrainId(1);
        ticket2.setDepartureStopId(1L);
        ticket2.setArrivalStopId(2L);
        ticket2.setTravelDate(LocalDate.now().plusDays(1));
        ticket2.setCarriageTypeId(1);
        ticket2.setSeatNumber("01B");
        ticket2.setCarriageNumber("1");
        
        List<Ticket> tickets = Arrays.asList(ticket1, ticket2);
        when(ticketRepository.findByOrderIdAndTicketIdIn(orderId, ticketIds)).thenReturn(tickets);
        
        // Mock有效车票查询（还有剩余车票）
        Ticket remainingTicket = new Ticket();
        remainingTicket.setTicketId(3L);
        remainingTicket.setTicketStatus((byte) TicketStatus.UNUSED.getCode());
        when(ticketRepository.findValidTicketsByOrderId(orderId)).thenReturn(Arrays.asList(remainingTicket));
        
        // 执行测试
        BookingResponse response = ticketService.refundTickets(request);
        
        // 验证结果
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getMessage()).isEqualTo("退票成功");
        assertThat(response.getOrderNumber()).isEqualTo("ORD20241201001");
        
        // 验证车票状态更新
        verify(ticketRepository, times(2)).save(any(Ticket.class));
        assertThat(ticket1.getTicketStatus()).isEqualTo((byte) TicketStatus.REFUNDED.getCode());
        assertThat(ticket2.getTicketStatus()).isEqualTo((byte) TicketStatus.REFUNDED.getCode());
        
        // 验证库存回滚
        verify(redisService, times(2)).incrStock(
            eq(1), eq(1L), eq(2L), eq(LocalDate.now().plusDays(1)), eq(1), eq(1)
        );
        
        // 验证座位释放
        verify(seatService, times(2)).releaseSeat(any(Ticket.class));
        
        // 验证订单更新
        verify(orderRepository, times(1)).save(any(Order.class));
        assertThat(order.getTotalAmount()).isEqualTo(new BigDecimal("0.00"));
        assertThat(order.getTicketCount()).isEqualTo(1);
        
        // 验证锁释放
        verify(redisService, times(1)).unlock("refund:" + orderId);
    }
    
    @Test
    @DisplayName("退票成功 - 订单无剩余车票，订单被取消")
    public void testRefundTickets_Success_OrderCancelled() {
        // 准备测试数据
        Long userId = 1L;
        Long orderId = 100L;
        List<Long> ticketIds = Arrays.asList(1L, 2L);
        
        RefundRequest request = new RefundRequest();
        request.setUserId(userId);
        request.setOrderId(orderId);
        request.setTicketIds(ticketIds);
        
        // Mock Redis锁
        when(redisService.tryLock(anyString(), anyLong(), anyLong())).thenReturn(true);
        
        // Mock订单查询
        Order order = new Order();
        order.setOrderId(orderId);
        order.setOrderNumber("ORD20241201001");
        order.setUserId(userId);
        order.setOrderStatus((byte) OrderStatus.PAID.getCode());
        order.setTotalAmount(new BigDecimal("200.00"));
        order.setTicketCount(2);
        when(orderRepository.findByOrderIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        
        // Mock车票查询
        Ticket ticket1 = new Ticket();
        ticket1.setTicketId(1L);
        ticket1.setOrderId(orderId);
        ticket1.setPrice(new BigDecimal("100.00"));
        ticket1.setTicketStatus((byte) TicketStatus.UNUSED.getCode());
        ticket1.setTrainId(1);
        ticket1.setDepartureStopId(1L);
        ticket1.setArrivalStopId(2L);
        ticket1.setTravelDate(LocalDate.now().plusDays(1));
        ticket1.setCarriageTypeId(1);
        ticket1.setSeatNumber("01A");
        ticket1.setCarriageNumber("1");
        
        Ticket ticket2 = new Ticket();
        ticket2.setTicketId(2L);
        ticket2.setOrderId(orderId);
        ticket2.setPrice(new BigDecimal("100.00"));
        ticket2.setTicketStatus((byte) TicketStatus.UNUSED.getCode());
        ticket2.setTrainId(1);
        ticket2.setDepartureStopId(1L);
        ticket2.setArrivalStopId(2L);
        ticket2.setTravelDate(LocalDate.now().plusDays(1));
        ticket2.setCarriageTypeId(1);
        ticket2.setSeatNumber("01B");
        ticket2.setCarriageNumber("1");
        
        List<Ticket> tickets = Arrays.asList(ticket1, ticket2);
        when(ticketRepository.findByOrderIdAndTicketIdIn(orderId, ticketIds)).thenReturn(tickets);
        
        // Mock有效车票查询（无剩余车票）
        when(ticketRepository.findValidTicketsByOrderId(orderId)).thenReturn(Collections.emptyList());
        
        // 执行测试
        BookingResponse response = ticketService.refundTickets(request);
        
        // 验证结果
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getMessage()).isEqualTo("退票成功，订单已取消");
        assertThat(response.getOrderNumber()).isEqualTo("ORD20241201001");
        
        // 验证订单状态更新为已取消
        verify(orderRepository, times(2)).save(any(Order.class));
        assertThat(order.getOrderStatus()).isEqualTo((byte) OrderStatus.CANCELLED.getCode());
        assertThat(order.getTicketCount()).isEqualTo(0);
        
        // 验证锁释放
        verify(redisService, times(1)).unlock("refund:" + orderId);
    }
    
    @Test
    @DisplayName("退票失败 - Redis锁获取失败")
    public void testRefundTickets_Failure_RedisLockFailed() {
        // 准备测试数据
        Long userId = 1L;
        Long orderId = 100L;
        List<Long> ticketIds = Arrays.asList(1L);
        
        RefundRequest request = new RefundRequest();
        request.setUserId(userId);
        request.setOrderId(orderId);
        request.setTicketIds(ticketIds);
        
        // Mock Redis锁失败
        when(redisService.tryLock(anyString(), anyLong(), anyLong())).thenReturn(false);
        
        // 执行测试
        BookingResponse response = ticketService.refundTickets(request);
        
        // 验证结果
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getMessage()).isEqualTo("系统繁忙，请稍后重试");
        
        // 验证没有其他操作
        verify(orderRepository, never()).findByOrderIdAndUserId(anyLong(), anyLong());
        verify(ticketRepository, never()).findByOrderIdAndTicketIdIn(anyLong(), anyList());
        verify(redisService, times(1)).unlock("refund:" + orderId);
    }
    
    @Test
    @DisplayName("退票失败 - 订单不存在")
    public void testRefundTickets_Failure_OrderNotFound() {
        // 准备测试数据
        Long userId = 1L;
        Long orderId = 100L;
        List<Long> ticketIds = Arrays.asList(1L);
        
        RefundRequest request = new RefundRequest();
        request.setUserId(userId);
        request.setOrderId(orderId);
        request.setTicketIds(ticketIds);
        
        // Mock Redis锁
        when(redisService.tryLock(anyString(), anyLong(), anyLong())).thenReturn(true);
        
        // Mock订单不存在
        when(orderRepository.findByOrderIdAndUserId(orderId, userId)).thenReturn(Optional.empty());
        
        // 执行测试
        BookingResponse response = ticketService.refundTickets(request);
        
        // 验证结果
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getMessage()).isEqualTo("订单不存在");
        
        // 验证锁释放
        verify(redisService, times(1)).unlock("refund:" + orderId);
    }
    
    @Test
    @DisplayName("退票失败 - 订单状态不允许退票")
    public void testRefundTickets_Failure_OrderStatusNotAllowed() {
        // 准备测试数据
        Long userId = 1L;
        Long orderId = 100L;
        List<Long> ticketIds = Arrays.asList(1L);
        
        RefundRequest request = new RefundRequest();
        request.setUserId(userId);
        request.setOrderId(orderId);
        request.setTicketIds(ticketIds);
        
        // Mock Redis锁
        when(redisService.tryLock(anyString(), anyLong(), anyLong())).thenReturn(true);
        
        // Mock订单状态不允许退票
        Order order = new Order();
        order.setOrderId(orderId);
        order.setUserId(userId);
        order.setOrderStatus((byte) OrderStatus.PENDING_PAYMENT.getCode()); // 待支付状态
        when(orderRepository.findByOrderIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        
        // 执行测试
        BookingResponse response = ticketService.refundTickets(request);
        
        // 验证结果
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getMessage()).isEqualTo("订单状态不允许退票");
        
        // 验证锁释放
        verify(redisService, times(1)).unlock("refund:" + orderId);
    }
    
    @Test
    @DisplayName("退票失败 - 车票不存在")
    public void testRefundTickets_Failure_TicketsNotFound() {
        // 准备测试数据
        Long userId = 1L;
        Long orderId = 100L;
        List<Long> ticketIds = Arrays.asList(1L);
        
        RefundRequest request = new RefundRequest();
        request.setUserId(userId);
        request.setOrderId(orderId);
        request.setTicketIds(ticketIds);
        
        // Mock Redis锁
        when(redisService.tryLock(anyString(), anyLong(), anyLong())).thenReturn(true);
        
        // Mock订单查询
        Order order = new Order();
        order.setOrderId(orderId);
        order.setUserId(userId);
        order.setOrderStatus((byte) OrderStatus.PAID.getCode());
        when(orderRepository.findByOrderIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        
        // Mock车票不存在
        when(ticketRepository.findByOrderIdAndTicketIdIn(orderId, ticketIds)).thenReturn(Collections.emptyList());
        
        // 执行测试
        BookingResponse response = ticketService.refundTickets(request);
        
        // 验证结果
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getMessage()).isEqualTo("车票不存在");
        
        // 验证锁释放
        verify(redisService, times(1)).unlock("refund:" + orderId);
    }
    
    @Test
    @DisplayName("退票失败 - 车票状态不允许退票")
    public void testRefundTickets_Failure_TicketStatusNotAllowed() {
        // 准备测试数据
        Long userId = 1L;
        Long orderId = 100L;
        List<Long> ticketIds = Arrays.asList(1L);
        
        RefundRequest request = new RefundRequest();
        request.setUserId(userId);
        request.setOrderId(orderId);
        request.setTicketIds(ticketIds);
        
        // Mock Redis锁
        when(redisService.tryLock(anyString(), anyLong(), anyLong())).thenReturn(true);
        
        // Mock订单查询
        Order order = new Order();
        order.setOrderId(orderId);
        order.setUserId(userId);
        order.setOrderStatus((byte) OrderStatus.PAID.getCode());
        when(orderRepository.findByOrderIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        
        // Mock车票状态不允许退票
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setOrderId(orderId);
        ticket.setTicketStatus((byte) TicketStatus.USED.getCode()); // 已使用状态
        when(ticketRepository.findByOrderIdAndTicketIdIn(orderId, ticketIds)).thenReturn(Arrays.asList(ticket));
        
        // 执行测试
        BookingResponse response = ticketService.refundTickets(request);
        
        // 验证结果
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getMessage()).isEqualTo("车票状态不允许退票");
        
        // 验证锁释放
        verify(redisService, times(1)).unlock("refund:" + orderId);
    }
    
    @Test
    @DisplayName("退票成功 - 车票无座位信息")
    public void testRefundTickets_Success_TicketsWithoutSeats() {
        // 准备测试数据
        Long userId = 1L;
        Long orderId = 100L;
        List<Long> ticketIds = Arrays.asList(1L);
        
        RefundRequest request = new RefundRequest();
        request.setUserId(userId);
        request.setOrderId(orderId);
        request.setTicketIds(ticketIds);
        
        // Mock Redis锁
        when(redisService.tryLock(eq("refund:" + orderId), eq(5L), eq(30L))).thenReturn(true);
        // Mock库存回滚
        when(redisService.incrStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt(), anyInt())).thenReturn(true);
        
        // Mock订单查询
        Order order = new Order();
        order.setOrderId(orderId);
        order.setOrderNumber("ORD20241201001");
        order.setUserId(userId);
        order.setOrderStatus((byte) OrderStatus.PAID.getCode());
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setTicketCount(1);
        when(orderRepository.findByOrderIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        
        // Mock车票查询（无座位信息）
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setOrderId(orderId);
        ticket.setPrice(new BigDecimal("100.00"));
        ticket.setTicketStatus((byte) TicketStatus.UNUSED.getCode());
        ticket.setTrainId(1);
        ticket.setDepartureStopId(1L);
        ticket.setArrivalStopId(2L);
        ticket.setTravelDate(LocalDate.now().plusDays(1));
        ticket.setCarriageTypeId(1);
        ticket.setSeatNumber(null); // 无座位号
        ticket.setCarriageNumber(null); // 无车厢号
        
        when(ticketRepository.findByOrderIdAndTicketIdIn(orderId, ticketIds)).thenReturn(Arrays.asList(ticket));
        
        // Mock有效车票查询（无剩余车票）
        when(ticketRepository.findValidTicketsByOrderId(orderId)).thenReturn(Collections.emptyList());
        
        // 执行测试
        BookingResponse response = ticketService.refundTickets(request);
        System.out.println("退票无座位信息用例返回: status=" + response.getStatus() + ", message=" + response.getMessage());
        
        // 验证结果
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getMessage()).isEqualTo("退票成功，订单已取消");
        
        // 验证车票状态更新
        verify(ticketRepository, times(1)).save(any(Ticket.class));
        assertThat(ticket.getTicketStatus()).isEqualTo((byte) TicketStatus.REFUNDED.getCode());
        
        // 验证库存回滚
        verify(redisService, times(1)).incrStock(
            eq(1), eq(1L), eq(2L), eq(LocalDate.now().plusDays(1)), eq(1), eq(1)
        );
        
        // 验证不调用座位释放（因为无座位信息）
        verify(seatService, never()).releaseSeat(any(Ticket.class));
        
        // 验证锁释放
        verify(redisService, times(1)).unlock("refund:" + orderId);
    }
    
    @Test
    @DisplayName("退票异常处理 - 数据库异常")
    public void testRefundTickets_Exception_DatabaseError() {
        // 准备测试数据
        Long userId = 1L;
        Long orderId = 100L;
        List<Long> ticketIds = Arrays.asList(1L);
        
        RefundRequest request = new RefundRequest();
        request.setUserId(userId);
        request.setOrderId(orderId);
        request.setTicketIds(ticketIds);
        
        // 检查实例类型
        System.out.println("TicketServiceImpl实例类型: " + ticketService.getClass().getName());
        System.out.println("RedisService实例类型: " + redisService.getClass().getName());
        
        // Mock Redis锁 - 使用正确的参数类型
        String expectedLockKey = "refund:" + orderId;
        System.out.println("设置Redis锁mock，期望锁键: " + expectedLockKey);
        
        // 使用正确的参数类型：anyLong()而不是anyInt()
        when(redisService.tryLock(eq(expectedLockKey), eq(5L), eq(30L))).thenReturn(true);
        
        // 验证mock设置
        boolean mockResult = redisService.tryLock(expectedLockKey, 5L, 30L);
        System.out.println("直接调用mock结果: " + mockResult);
        
        // 尝试更宽松的mock，使用正确的参数类型
        when(redisService.tryLock(anyString(), anyLong(), anyLong())).thenReturn(true);
        
        // Mock订单查询抛出异常
        when(orderRepository.findByOrderIdAndUserId(orderId, userId))
            .thenThrow(new RuntimeException("数据库连接失败"));
        
        // 执行测试并验证异常处理
        System.out.println("开始执行退票测试，期望返回失败响应...");
        System.out.println("期望的锁键: " + expectedLockKey);
        
        BookingResponse response = ticketService.refundTickets(request);
        System.out.println("实际返回响应: " + response.getStatus() + " - " + response.getMessage());
        
        // 验证mock调用，使用正确的参数类型
        System.out.println("验证Redis锁mock调用...");
        verify(redisService, atLeastOnce()).tryLock(anyString(), anyLong(), anyLong());
        
        // 验证返回失败响应
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getMessage()).contains("数据库异常");
        assertThat(response.getMessage()).contains("数据库连接失败");
        
        // 验证锁释放
        verify(redisService, atLeastOnce()).unlock(anyString());
    }
    
    @Test
    @DisplayName("退票异常处理 - 座位释放异常")
    public void testRefundTickets_Exception_SeatReleaseError() {
        // 准备测试数据
        Long userId = 1L;
        Long orderId = 100L;
        List<Long> ticketIds = Arrays.asList(1L);
        
        RefundRequest request = new RefundRequest();
        request.setUserId(userId);
        request.setOrderId(orderId);
        request.setTicketIds(ticketIds);
        
        // Mock Redis锁
        when(redisService.tryLock(anyString(), anyLong(), anyLong())).thenReturn(true);
        
        // Mock订单查询
        Order order = new Order();
        order.setOrderId(orderId);
        order.setOrderNumber("ORD20241201001");
        order.setUserId(userId);
        order.setOrderStatus((byte) OrderStatus.PAID.getCode());
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setTicketCount(1);
        when(orderRepository.findByOrderIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        
        // Mock车票查询
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setOrderId(orderId);
        ticket.setPrice(new BigDecimal("100.00"));
        ticket.setTicketStatus((byte) TicketStatus.UNUSED.getCode());
        ticket.setTrainId(1);
        ticket.setDepartureStopId(1L);
        ticket.setArrivalStopId(2L);
        ticket.setTravelDate(LocalDate.now().plusDays(1));
        ticket.setCarriageTypeId(1);
        ticket.setSeatNumber("01A");
        ticket.setCarriageNumber("1");
        
        when(ticketRepository.findByOrderIdAndTicketIdIn(orderId, ticketIds)).thenReturn(Arrays.asList(ticket));
        
        // Mock有效车票查询（无剩余车票）
        when(ticketRepository.findValidTicketsByOrderId(orderId)).thenReturn(Collections.emptyList());
        
        // Mock座位释放抛出异常
        doThrow(new RuntimeException("座位释放失败")).when(seatService).releaseSeat(any(Ticket.class));
        
        // 执行测试
        BookingResponse response = ticketService.refundTickets(request);
        
        // 验证结果
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getMessage()).contains("数据库异常");
        
        // 验证锁释放
        verify(redisService, times(1)).unlock("refund:" + orderId);
    }
    
    @Test
    @DisplayName("退票异常处理 - 库存回滚异常")
    public void testRefundTickets_Exception_StockRollbackError() {
        // 准备测试数据
        Long userId = 1L;
        Long orderId = 100L;
        List<Long> ticketIds = Arrays.asList(1L);
        
        RefundRequest request = new RefundRequest();
        request.setUserId(userId);
        request.setOrderId(orderId);
        request.setTicketIds(ticketIds);
        
        // Mock Redis锁
        when(redisService.tryLock(anyString(), anyLong(), anyLong())).thenReturn(true);
        
        // Mock订单查询
        Order order = new Order();
        order.setOrderId(orderId);
        order.setOrderNumber("ORD20241201001");
        order.setUserId(userId);
        order.setOrderStatus((byte) OrderStatus.PAID.getCode());
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setTicketCount(1);
        when(orderRepository.findByOrderIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        
        // Mock车票查询
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setOrderId(orderId);
        ticket.setPrice(new BigDecimal("100.00"));
        ticket.setTicketStatus((byte) TicketStatus.UNUSED.getCode());
        ticket.setTrainId(1);
        ticket.setDepartureStopId(1L);
        ticket.setArrivalStopId(2L);
        ticket.setTravelDate(LocalDate.now().plusDays(1));
        ticket.setCarriageTypeId(1);
        ticket.setSeatNumber("01A");
        ticket.setCarriageNumber("1");
        
        when(ticketRepository.findByOrderIdAndTicketIdIn(orderId, ticketIds)).thenReturn(Arrays.asList(ticket));
        
        // Mock有效车票查询（无剩余车票）
        when(ticketRepository.findValidTicketsByOrderId(orderId)).thenReturn(Collections.emptyList());
        
        // Mock库存回滚抛出异常
        doThrow(new RuntimeException("库存回滚失败")).when(redisService).incrStock(
            anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt(), anyInt()
        );
        
        // 执行测试
        BookingResponse response = ticketService.refundTickets(request);
        
        // 验证结果
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getMessage()).contains("数据库异常");
        
        // 验证锁释放
        verify(redisService, times(1)).unlock("refund:" + orderId);
    }
    
    @Test
    @DisplayName("退票异常处理 - 车票保存异常")
    public void testRefundTickets_Exception_TicketSaveError() {
        // 准备测试数据
        Long userId = 1L;
        Long orderId = 100L;
        List<Long> ticketIds = Arrays.asList(1L);
        
        RefundRequest request = new RefundRequest();
        request.setUserId(userId);
        request.setOrderId(orderId);
        request.setTicketIds(ticketIds);
        
        // Mock Redis锁
        when(redisService.tryLock(anyString(), anyLong(), anyLong())).thenReturn(true);
        
        // Mock订单查询
        Order order = new Order();
        order.setOrderId(orderId);
        order.setOrderNumber("ORD20241201001");
        order.setUserId(userId);
        order.setOrderStatus((byte) OrderStatus.PAID.getCode());
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setTicketCount(1);
        when(orderRepository.findByOrderIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        
        // Mock车票查询
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setOrderId(orderId);
        ticket.setPrice(new BigDecimal("100.00"));
        ticket.setTicketStatus((byte) TicketStatus.UNUSED.getCode());
        ticket.setTrainId(1);
        ticket.setDepartureStopId(1L);
        ticket.setArrivalStopId(2L);
        ticket.setTravelDate(LocalDate.now().plusDays(1));
        ticket.setCarriageTypeId(1);
        ticket.setSeatNumber("01A");
        ticket.setCarriageNumber("1");
        
        when(ticketRepository.findByOrderIdAndTicketIdIn(orderId, ticketIds)).thenReturn(Arrays.asList(ticket));
        
        // Mock车票保存抛出异常
        when(ticketRepository.save(any(Ticket.class))).thenThrow(new RuntimeException("车票保存失败"));
        
        // 执行测试
        BookingResponse response = ticketService.refundTickets(request);
        
        // 验证结果
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getMessage()).contains("数据库异常");
        
        // 验证锁释放
        verify(redisService, times(1)).unlock("refund:" + orderId);
    }
    
    @Test
    @DisplayName("退票异常处理 - 订单保存异常")
    public void testRefundTickets_Exception_OrderSaveError() {
        // 准备测试数据
        Long userId = 1L;
        Long orderId = 100L;
        List<Long> ticketIds = Arrays.asList(1L);
        
        RefundRequest request = new RefundRequest();
        request.setUserId(userId);
        request.setOrderId(orderId);
        request.setTicketIds(ticketIds);
        
        // Mock Redis锁
        when(redisService.tryLock(anyString(), anyLong(), anyLong())).thenReturn(true);
        
        // Mock订单查询
        Order order = new Order();
        order.setOrderId(orderId);
        order.setOrderNumber("ORD20241201001");
        order.setUserId(userId);
        order.setOrderStatus((byte) OrderStatus.PAID.getCode());
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setTicketCount(1);
        when(orderRepository.findByOrderIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        
        // Mock车票查询
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setOrderId(orderId);
        ticket.setPrice(new BigDecimal("100.00"));
        ticket.setTicketStatus((byte) TicketStatus.UNUSED.getCode());
        ticket.setTrainId(1);
        ticket.setDepartureStopId(1L);
        ticket.setArrivalStopId(2L);
        ticket.setTravelDate(LocalDate.now().plusDays(1));
        ticket.setCarriageTypeId(1);
        ticket.setSeatNumber("01A");
        ticket.setCarriageNumber("1");
        
        when(ticketRepository.findByOrderIdAndTicketIdIn(orderId, ticketIds)).thenReturn(Arrays.asList(ticket));
        
        // Mock有效车票查询（无剩余车票）
        when(ticketRepository.findValidTicketsByOrderId(orderId)).thenReturn(Collections.emptyList());
        
        // Mock订单保存抛出异常
        when(orderRepository.save(any(Order.class))).thenThrow(new RuntimeException("订单保存失败"));
        
        // 执行测试
        BookingResponse response = ticketService.refundTickets(request);
        
        // 验证结果
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getMessage()).contains("数据库异常");
        
        // 验证锁释放
        verify(redisService, times(1)).unlock("refund:" + orderId);
    }
    
    @Test
    @DisplayName("退票成功 - 多张车票部分有座位部分无座位")
    public void testRefundTickets_Success_MixedSeatTickets() {
        // 准备测试数据
        Long userId = 1L;
        Long orderId = 100L;
        List<Long> ticketIds = Arrays.asList(1L, 2L);
        
        RefundRequest request = new RefundRequest();
        request.setUserId(userId);
        request.setOrderId(orderId);
        request.setTicketIds(ticketIds);
        
        // Mock Redis锁
        when(redisService.tryLock(anyString(), anyLong(), anyLong())).thenReturn(true);
        
        // Mock订单查询
        Order order = new Order();
        order.setOrderId(orderId);
        order.setOrderNumber("ORD20241201001");
        order.setUserId(userId);
        order.setOrderStatus((byte) OrderStatus.PAID.getCode());
        order.setTotalAmount(new BigDecimal("200.00"));
        order.setTicketCount(2);
        when(orderRepository.findByOrderIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        
        // Mock车票查询（一张有座位，一张无座位）
        Ticket ticket1 = new Ticket();
        ticket1.setTicketId(1L);
        ticket1.setOrderId(orderId);
        ticket1.setPrice(new BigDecimal("100.00"));
        ticket1.setTicketStatus((byte) TicketStatus.UNUSED.getCode());
        ticket1.setTrainId(1);
        ticket1.setDepartureStopId(1L);
        ticket1.setArrivalStopId(2L);
        ticket1.setTravelDate(LocalDate.now().plusDays(1));
        ticket1.setCarriageTypeId(1);
        ticket1.setSeatNumber("01A");
        ticket1.setCarriageNumber("1");
        
        Ticket ticket2 = new Ticket();
        ticket2.setTicketId(2L);
        ticket2.setOrderId(orderId);
        ticket2.setPrice(new BigDecimal("100.00"));
        ticket2.setTicketStatus((byte) TicketStatus.UNUSED.getCode());
        ticket2.setTrainId(1);
        ticket2.setDepartureStopId(1L);
        ticket2.setArrivalStopId(2L);
        ticket2.setTravelDate(LocalDate.now().plusDays(1));
        ticket2.setCarriageTypeId(1);
        ticket2.setSeatNumber(null); // 无座位号
        ticket2.setCarriageNumber(null); // 无车厢号
        
        List<Ticket> tickets = Arrays.asList(ticket1, ticket2);
        when(ticketRepository.findByOrderIdAndTicketIdIn(orderId, ticketIds)).thenReturn(tickets);
        
        // Mock有效车票查询（无剩余车票）
        when(ticketRepository.findValidTicketsByOrderId(orderId)).thenReturn(Collections.emptyList());
        
        // 执行测试
        BookingResponse response = ticketService.refundTickets(request);
        
        // 验证结果
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getMessage()).isEqualTo("退票成功，订单已取消");
        
        // 验证车票状态更新
        verify(ticketRepository, times(2)).save(any(Ticket.class));
        assertThat(ticket1.getTicketStatus()).isEqualTo((byte) TicketStatus.REFUNDED.getCode());
        assertThat(ticket2.getTicketStatus()).isEqualTo((byte) TicketStatus.REFUNDED.getCode());
        
        // 验证库存回滚
        verify(redisService, times(2)).incrStock(
            eq(1), eq(1L), eq(2L), eq(LocalDate.now().plusDays(1)), eq(1), eq(1)
        );
        
        // 验证只对有座位的车票调用座位释放
        verify(seatService, times(1)).releaseSeat(ticket1);
        verify(seatService, never()).releaseSeat(ticket2);
        
        // 验证锁释放
        verify(redisService, times(1)).unlock("refund:" + orderId);
    }

    @Test
    @DisplayName("获取车票详情 - 权限验证分支覆盖")
    public void testGetTicketDetail_PermissionBranchCoverage() {
        // 准备测试数据
        Long ticketId = 1L;
        Long userId = 1L;
        Long differentUserId = 2L;
        
        // Mock车票查询
        Ticket ticket = new Ticket();
        ticket.setTicketId(ticketId);
        ticket.setOrderId(100L);
        ticket.setPassengerId(20L); // 不同的乘客ID
        ticket.setTrainId(1);
        ticket.setDepartureStopId(1L);
        ticket.setArrivalStopId(2L);
        ticket.setTravelDate(LocalDate.now().plusDays(1));
        ticket.setCarriageTypeId(1);
        ticket.setSeatNumber("01A");
        ticket.setCarriageNumber("1");
        ticket.setPrice(new BigDecimal("100.00"));
        ticket.setTicketStatus((byte) TicketStatus.UNUSED.getCode());
        ticket.setTicketNumber("T123456789");
        ticket.setCreatedTime(LocalDateTime.now());
        
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        
        // Mock用户查询
        User user = new User();
        user.setUserId(userId);
        user.setPassengerId(10L); // 用户的乘客ID与车票的乘客ID不同
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        
        // Mock订单查询 - 返回不同用户ID的订单
        Order order = new Order();
        order.setOrderId(100L);
        order.setUserId(differentUserId); // 不同用户ID
        order.setOrderNumber("ORD20241201001");
        order.setOrderStatus((byte) OrderStatus.PAID.getCode());
        order.setPaymentTime(LocalDateTime.now());
        when(orderRepository.findById(ticket.getOrderId())).thenReturn(Optional.of(order));
        
        // Mock乘客查询
        Passenger passenger = new Passenger();
        passenger.setPassengerId(20L);
        passenger.setRealName("李四");
        passenger.setIdCardNumber("123456789012345679");
        passenger.setPhoneNumber("13800138001");
        passenger.setPassengerType((byte) 1);
        when(passengerRepository.findById(ticket.getPassengerId())).thenReturn(Optional.of(passenger));
        
        // Mock车站查询
        when(stationRepository.findById(anyInt())).thenReturn(Optional.of(new Station()));
        when(trainStopRepository.findByStopId(anyLong())).thenReturn(Optional.of(new TrainStop()));
        when(trainRepository.findById(anyInt())).thenReturn(Optional.of(new Train()));
        when(carriageTypeRepository.findById(anyInt())).thenReturn(Optional.of(new CarriageType()));
        
        // 执行测试
        TicketDetailResponse response = ticketService.getTicketDetail(ticketId, userId);
        
        // 验证结果 - 应该返回权限不足
        assertThat(response.getStatus()).isEqualTo("FAILURE");
        assertThat(response.getMessage()).isEqualTo("无权限查看该车票详情");
    }

    @Test
    @DisplayName("退票分支覆盖 - 座位信息部分为空")
    public void testRefundTickets_BranchCoverage_PartialSeatInfo() {
        // 准备测试数据
        Long userId = 1L;
        Long orderId = 100L;
        List<Long> ticketIds = Arrays.asList(1L);
        
        RefundRequest request = new RefundRequest();
        request.setUserId(userId);
        request.setOrderId(orderId);
        request.setTicketIds(ticketIds);
        
        // Mock Redis锁
        when(redisService.tryLock(eq("refund:" + orderId), eq(5L), eq(30L))).thenReturn(true);
        // Mock库存回滚
        when(redisService.incrStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt(), anyInt())).thenReturn(true);
        
        // Mock订单查询
        Order order = new Order();
        order.setOrderId(orderId);
        order.setOrderNumber("ORD20241201001");
        order.setUserId(userId);
        order.setOrderStatus((byte) OrderStatus.PAID.getCode());
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setTicketCount(1);
        when(orderRepository.findByOrderIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        
        // Mock车票查询（座位号为空，车厢号不为空）
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setOrderId(orderId);
        ticket.setPrice(new BigDecimal("100.00"));
        ticket.setTicketStatus((byte) TicketStatus.UNUSED.getCode());
        ticket.setTrainId(1);
        ticket.setDepartureStopId(1L);
        ticket.setArrivalStopId(2L);
        ticket.setTravelDate(LocalDate.now().plusDays(1));
        ticket.setCarriageTypeId(1);
        ticket.setSeatNumber(null); // 座位号为空
        ticket.setCarriageNumber("1"); // 车厢号不为空
        
        when(ticketRepository.findByOrderIdAndTicketIdIn(orderId, ticketIds)).thenReturn(Arrays.asList(ticket));
        
        // Mock有效车票查询（无剩余车票）
        when(ticketRepository.findValidTicketsByOrderId(orderId)).thenReturn(Collections.emptyList());
        
        // 执行测试
        BookingResponse response = ticketService.refundTickets(request);
        
        // 验证结果
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getMessage()).isEqualTo("退票成功，订单已取消");
        
        // 验证车票状态更新
        verify(ticketRepository, times(1)).save(any(Ticket.class));
        assertThat(ticket.getTicketStatus()).isEqualTo((byte) TicketStatus.REFUNDED.getCode());
        
        // 验证库存回滚
        verify(redisService, times(1)).incrStock(
            eq(1), eq(1L), eq(2L), eq(LocalDate.now().plusDays(1)), eq(1), eq(1)
        );
        
        // 验证不调用座位释放（因为座位号为空）
        verify(seatService, never()).releaseSeat(any(Ticket.class));
        
        // 验证锁释放
        verify(redisService, times(1)).unlock("refund:" + orderId);
    }

    @Test
    @DisplayName("获取车票详情 - 订单不存在分支覆盖")
    public void testGetTicketDetail_OrderNotExistsBranchCoverage() {
        // 准备测试数据
        Long ticketId = 1L;
        Long userId = 1L;
        
        // Mock车票查询
        Ticket ticket = new Ticket();
        ticket.setTicketId(ticketId);
        ticket.setOrderId(100L);
        ticket.setPassengerId(20L); // 不同的乘客ID
        ticket.setTrainId(1);
        ticket.setDepartureStopId(1L);
        ticket.setArrivalStopId(2L);
        ticket.setTravelDate(LocalDate.now().plusDays(1));
        ticket.setCarriageTypeId(1);
        ticket.setSeatNumber("01A");
        ticket.setCarriageNumber("1");
        ticket.setPrice(new BigDecimal("100.00"));
        ticket.setTicketStatus((byte) TicketStatus.UNUSED.getCode());
        ticket.setTicketNumber("T123456789");
        ticket.setCreatedTime(LocalDateTime.now());
        
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        
        // Mock用户查询
        User user = new User();
        user.setUserId(userId);
        user.setPassengerId(10L); // 用户的乘客ID与车票的乘客ID不同
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        
        // Mock订单查询 - 返回空，模拟订单不存在
        when(orderRepository.findById(ticket.getOrderId())).thenReturn(Optional.empty());
        
        // Mock乘客查询
        Passenger passenger = new Passenger();
        passenger.setPassengerId(20L);
        passenger.setRealName("李四");
        passenger.setIdCardNumber("123456789012345679");
        passenger.setPhoneNumber("13800138001");
        passenger.setPassengerType((byte) 1);
        when(passengerRepository.findById(ticket.getPassengerId())).thenReturn(Optional.of(passenger));
        
        // Mock车站查询
        when(stationRepository.findById(anyInt())).thenReturn(Optional.of(new Station()));
        when(trainStopRepository.findByStopId(anyLong())).thenReturn(Optional.of(new TrainStop()));
        when(trainRepository.findById(anyInt())).thenReturn(Optional.of(new Train()));
        when(carriageTypeRepository.findById(anyInt())).thenReturn(Optional.of(new CarriageType()));
        
        // 执行测试
        TicketDetailResponse response = ticketService.getTicketDetail(ticketId, userId);
        
        // 验证结果 - 应该返回权限不足
        assertThat(response.getStatus()).isEqualTo("FAILURE");
        assertThat(response.getMessage()).isEqualTo("无权限查看该车票详情");
    }

    @Test
    @DisplayName("退票分支覆盖 - 座位号不为空但车厢号为空")
    public void testRefundTickets_BranchCoverage_SeatNumberNotNullButCarriageNumberNull() {
        // 准备测试数据
        Long userId = 1L;
        Long orderId = 100L;
        List<Long> ticketIds = Arrays.asList(1L);
        
        RefundRequest request = new RefundRequest();
        request.setUserId(userId);
        request.setOrderId(orderId);
        request.setTicketIds(ticketIds);
        
        // Mock Redis锁
        when(redisService.tryLock(eq("refund:" + orderId), eq(5L), eq(30L))).thenReturn(true);
        // Mock库存回滚
        when(redisService.incrStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt(), anyInt())).thenReturn(true);
        
        // Mock订单查询
        Order order = new Order();
        order.setOrderId(orderId);
        order.setOrderNumber("ORD20241201001");
        order.setUserId(userId);
        order.setOrderStatus((byte) OrderStatus.PAID.getCode());
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setTicketCount(1);
        when(orderRepository.findByOrderIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        
        // Mock车票查询（座位号不为空，车厢号为空）
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setOrderId(orderId);
        ticket.setPrice(new BigDecimal("100.00"));
        ticket.setTicketStatus((byte) TicketStatus.UNUSED.getCode());
        ticket.setTrainId(1);
        ticket.setDepartureStopId(1L);
        ticket.setArrivalStopId(2L);
        ticket.setTravelDate(LocalDate.now().plusDays(1));
        ticket.setCarriageTypeId(1);
        ticket.setSeatNumber("01A"); // 座位号不为空
        ticket.setCarriageNumber(null); // 车厢号为空
        
        when(ticketRepository.findByOrderIdAndTicketIdIn(orderId, ticketIds)).thenReturn(Arrays.asList(ticket));
        
        // Mock有效车票查询（无剩余车票）
        when(ticketRepository.findValidTicketsByOrderId(orderId)).thenReturn(Collections.emptyList());
        
        // 执行测试
        BookingResponse response = ticketService.refundTickets(request);
        
        // 验证结果
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getMessage()).isEqualTo("退票成功，订单已取消");
        
        // 验证车票状态更新
        verify(ticketRepository, times(1)).save(any(Ticket.class));
        assertThat(ticket.getTicketStatus()).isEqualTo((byte) TicketStatus.REFUNDED.getCode());
        
        // 验证库存回滚
        verify(redisService, times(1)).incrStock(
            eq(1), eq(1L), eq(2L), eq(LocalDate.now().plusDays(1)), eq(1), eq(1)
        );
        
        // 验证不调用座位释放（因为车厢号为空）
        verify(seatService, never()).releaseSeat(any(Ticket.class));
        
        // 验证锁释放
        verify(redisService, times(1)).unlock("refund:" + orderId);
    }

    // ==================== 改签功能测试 ====================

    @Test
    @DisplayName("改签成功 - 有乘客信息")
    public void testChangeTickets_Success_WithPassengers() {
        // 准备测试数据
        Long userId = 1L;
        Long originalOrderId = 100L;
        List<Long> ticketIds = Arrays.asList(1L, 2L);
        
        ChangeTicketRequest request = new ChangeTicketRequest();
        request.setUserId(userId);
        request.setOriginalOrderId(originalOrderId);
        request.setTicketIds(ticketIds);
        request.setNewTrainId(2);
        request.setNewDepartureStopId(1L);
        request.setNewArrivalStopId(2L);
        request.setNewTravelDate(LocalDate.now().plusDays(2));
        request.setNewCarriageTypeId(1);
        
        // 设置乘客信息
        List<ChangeTicketRequest.ChangeTicketPassenger> passengers = Arrays.asList(
            new ChangeTicketRequest.ChangeTicketPassenger(10L, 1, 1), // 成人票，商务座
            new ChangeTicketRequest.ChangeTicketPassenger(11L, 2, 2)  // 儿童票，一等座
        );
        request.setPassengers(passengers);
        
        // Mock Redis锁
        when(redisService.tryLock(eq("change:" + originalOrderId), eq(5L), eq(30L))).thenReturn(true);
        
        // Mock原订单查询
        Order originalOrder = new Order();
        originalOrder.setOrderId(originalOrderId);
        originalOrder.setUserId(userId);
        originalOrder.setOrderStatus((byte) OrderStatus.PAID.getCode());
        originalOrder.setOrderNumber("ORD20241201001");
        originalOrder.setTotalAmount(new BigDecimal("400.00"));
        originalOrder.setTicketCount(2);
        when(orderRepository.findByOrderIdAndUserId(originalOrderId, userId)).thenReturn(Optional.of(originalOrder));
        
        // Mock原车票查询
        Ticket ticket1 = new Ticket();
        ticket1.setTicketId(1L);
        ticket1.setOrderId(originalOrderId);
        ticket1.setPassengerId(10L);
        ticket1.setTrainId(1);
        ticket1.setDepartureStopId(1L);
        ticket1.setArrivalStopId(2L);
        ticket1.setTravelDate(LocalDate.now().plusDays(1));
        ticket1.setCarriageTypeId(1);
        ticket1.setTicketStatus((byte) TicketStatus.UNUSED.getCode());
        ticket1.setPrice(new BigDecimal("200.00"));
        
        Ticket ticket2 = new Ticket();
        ticket2.setTicketId(2L);
        ticket2.setOrderId(originalOrderId);
        ticket2.setPassengerId(11L);
        ticket2.setTrainId(1);
        ticket2.setDepartureStopId(1L);
        ticket2.setArrivalStopId(2L);
        ticket2.setTravelDate(LocalDate.now().plusDays(1));
        ticket2.setCarriageTypeId(1);
        ticket2.setTicketStatus((byte) TicketStatus.UNUSED.getCode());
        ticket2.setPrice(new BigDecimal("200.00"));
        
        when(ticketRepository.findByOrderIdAndTicketIdIn(originalOrderId, ticketIds))
            .thenReturn(Arrays.asList(ticket1, ticket2));
        
        // Mock时间冲突检查（无冲突）
        when(timeConflictService.checkTimeConflict(anyLong(), any(LocalDate.class), anyInt(), anyLong(), anyLong(), anyLong()))
            .thenReturn(Collections.emptyList());
        
        // Mock库存检查
        when(redisService.decrStock(eq(2), eq(1L), eq(2L), eq(LocalDate.now().plusDays(2)), eq(1), eq(1)))
            .thenReturn(true);
        when(redisService.decrStock(eq(2), eq(1L), eq(2L), eq(LocalDate.now().plusDays(2)), eq(2), eq(1)))
            .thenReturn(true);
        
        // Mock订单号生成
        when(redisService.generateOrderNumber()).thenReturn("ORD20241201002");
        
        // Mock新订单保存
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(200L);
            order.setOrderNumber("ORD20241201002"); // 确保订单号被正确设置
            return order;
        });
        
        // Mock车票号生成
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            if (ticket.getTicketId() == null) {
                ticket.setTicketId(System.currentTimeMillis());
            }
            return ticket;
        });
        
        // Mock库存查询（用于价格计算）
        TicketInventory inventory1 = new TicketInventory();
        inventory1.setPrice(new BigDecimal("250.00"));
        TicketInventory inventory2 = new TicketInventory();
        inventory2.setPrice(new BigDecimal("180.00"));
        
        when(ticketInventoryDAO.findByKey(eq(2), eq(1L), eq(2L), eq(LocalDate.now().plusDays(2)), eq(1)))
            .thenReturn(Optional.of(inventory1));
        when(ticketInventoryDAO.findByKey(eq(2), eq(1L), eq(2L), eq(LocalDate.now().plusDays(2)), eq(2)))
            .thenReturn(Optional.of(inventory2));
        
        // Mock座位分配
        doNothing().when(seatService).assignSeat(any(Ticket.class));
        
        // Mock改签配对关系存储
        doNothing().when(redisService).setChangeMapping(anyString(), anyString());
        
        // 执行测试
        BookingResponse response = ticketService.changeTickets(request);
        
        // 验证结果
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getMessage()).isEqualTo("改签成功");
        assertThat(response.getOrderNumber()).isEqualTo("ORD20241201002");
        assertThat(response.getOrderId()).isEqualTo(200L);
        
        // 验证Redis锁调用
        verify(redisService, times(1)).tryLock("change:" + originalOrderId, 5L, 30L);
        verify(redisService, times(1)).unlock("change:" + originalOrderId);
        
        // 验证库存扣减
        verify(redisService, times(1)).decrStock(2, 1L, 2L, LocalDate.now().plusDays(2), 1, 1);
        verify(redisService, times(1)).decrStock(2, 1L, 2L, LocalDate.now().plusDays(2), 2, 1);
        
        // 验证新订单创建
        verify(orderRepository, times(2)).save(any(Order.class)); // 创建和更新总价
        
        // 验证新车票创建
        verify(ticketRepository, times(2)).save(any(Ticket.class));
        
        // 验证座位分配
        verify(seatService, times(2)).assignSeat(any(Ticket.class));
        
        // 验证改签配对关系存储
        verify(redisService, times(2)).setChangeMapping(anyString(), anyString());
    }

    @Test
    @DisplayName("改签成功 - 无乘客信息（兼容旧版本）")
    public void testChangeTickets_Success_WithoutPassengers() {
        // 准备测试数据
        Long userId = 1L;
        Long originalOrderId = 100L;
        List<Long> ticketIds = Arrays.asList(1L);
        
        ChangeTicketRequest request = new ChangeTicketRequest();
        request.setUserId(userId);
        request.setOriginalOrderId(originalOrderId);
        request.setTicketIds(ticketIds);
        request.setNewTrainId(2);
        request.setNewDepartureStopId(1L);
        request.setNewArrivalStopId(2L);
        request.setNewTravelDate(LocalDate.now().plusDays(2));
        request.setNewCarriageTypeId(1);
        request.setPassengers(null); // 无乘客信息
        
        // Mock Redis锁
        when(redisService.tryLock(eq("change:" + originalOrderId), eq(5L), eq(30L))).thenReturn(true);
        
        // Mock原订单查询
        Order originalOrder = new Order();
        originalOrder.setOrderId(originalOrderId);
        originalOrder.setUserId(userId);
        originalOrder.setOrderStatus((byte) OrderStatus.PAID.getCode());
        originalOrder.setOrderNumber("ORD20241201001");
        originalOrder.setTotalAmount(new BigDecimal("200.00"));
        originalOrder.setTicketCount(1);
        when(orderRepository.findByOrderIdAndUserId(originalOrderId, userId)).thenReturn(Optional.of(originalOrder));
        
        // Mock原车票查询
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setOrderId(originalOrderId);
        ticket.setPassengerId(10L);
        ticket.setTrainId(1);
        ticket.setDepartureStopId(1L);
        ticket.setArrivalStopId(2L);
        ticket.setTravelDate(LocalDate.now().plusDays(1));
        ticket.setCarriageTypeId(1);
        ticket.setTicketStatus((byte) TicketStatus.UNUSED.getCode());
        ticket.setPrice(new BigDecimal("200.00"));
        ticket.setTicketType((byte) 1); // 成人票
        
        when(ticketRepository.findByOrderIdAndTicketIdIn(originalOrderId, ticketIds))
            .thenReturn(Arrays.asList(ticket));
        
        // Mock时间冲突检查（无冲突）
        when(timeConflictService.checkTimeConflict(anyLong(), any(LocalDate.class), anyInt(), anyLong(), anyLong(), anyLong()))
            .thenReturn(Collections.emptyList());
        
        // Mock库存检查
        when(redisService.decrStock(eq(2), eq(1L), eq(2L), eq(LocalDate.now().plusDays(2)), eq(1), eq(1)))
            .thenReturn(true);
        
        // Mock订单号生成
        when(redisService.generateOrderNumber()).thenReturn("ORD20241201002");
        
        // Mock新订单保存
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(200L);
            return order;
        });
        
        // Mock车票号生成
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket newTicket = invocation.getArgument(0);
            if (newTicket.getTicketId() == null) {
                newTicket.setTicketId(System.currentTimeMillis());
            }
            return newTicket;
        });
        
        // Mock库存查询（用于价格计算）
        TicketInventory inventory = new TicketInventory();
        inventory.setPrice(new BigDecimal("250.00"));
        when(ticketInventoryDAO.findByKey(eq(2), eq(1L), eq(2L), eq(LocalDate.now().plusDays(2)), eq(1)))
            .thenReturn(Optional.of(inventory));
        
        // Mock座位分配
        doNothing().when(seatService).assignSeat(any(Ticket.class));
        
        // Mock改签配对关系存储
        doNothing().when(redisService).setChangeMapping(anyString(), anyString());
        
        // 执行测试
        BookingResponse response = ticketService.changeTickets(request);
        
        // 验证结果
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getMessage()).isEqualTo("改签成功");
        
        // 验证库存扣减（使用统一的席别）
        verify(redisService, times(1)).decrStock(2, 1L, 2L, LocalDate.now().plusDays(2), 1, 1);
    }

    @Test
    @DisplayName("改签失败 - Redis锁获取失败")
    public void testChangeTickets_Failure_RedisLockFailed() {
        // 准备测试数据
        Long userId = 1L;
        Long originalOrderId = 100L;
        List<Long> ticketIds = Arrays.asList(1L);
        
        ChangeTicketRequest request = new ChangeTicketRequest();
        request.setUserId(userId);
        request.setOriginalOrderId(originalOrderId);
        request.setTicketIds(ticketIds);
        request.setNewTrainId(2);
        request.setNewDepartureStopId(1L);
        request.setNewArrivalStopId(2L);
        request.setNewTravelDate(LocalDate.now().plusDays(2));
        request.setNewCarriageTypeId(1);
        
        // Mock Redis锁失败
        when(redisService.tryLock(eq("change:" + originalOrderId), eq(5L), eq(30L))).thenReturn(false);
        
        // 执行测试
        BookingResponse response = ticketService.changeTickets(request);
        
        // 验证结果
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getMessage()).isEqualTo("系统繁忙，请稍后重试");
        
        // 验证锁释放
        verify(redisService, times(1)).unlock("change:" + originalOrderId);
    }

    @Test
    @DisplayName("改签失败 - 原订单不存在")
    public void testChangeTickets_Failure_OriginalOrderNotFound() {
        // 准备测试数据
        Long userId = 1L;
        Long originalOrderId = 100L;
        List<Long> ticketIds = Arrays.asList(1L);
        
        ChangeTicketRequest request = new ChangeTicketRequest();
        request.setUserId(userId);
        request.setOriginalOrderId(originalOrderId);
        request.setTicketIds(ticketIds);
        request.setNewTrainId(2);
        request.setNewDepartureStopId(1L);
        request.setNewArrivalStopId(2L);
        request.setNewTravelDate(LocalDate.now().plusDays(2));
        request.setNewCarriageTypeId(1);
        
        // Mock Redis锁
        when(redisService.tryLock(eq("change:" + originalOrderId), eq(5L), eq(30L))).thenReturn(true);
        
        // Mock原订单查询失败
        when(orderRepository.findByOrderIdAndUserId(originalOrderId, userId)).thenReturn(Optional.empty());
        
        // 执行测试
        BookingResponse response = ticketService.changeTickets(request);
        
        // 验证结果
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getMessage()).isEqualTo("原订单不存在");
        
        // 验证锁释放
        verify(redisService, times(1)).unlock("change:" + originalOrderId);
    }

    @Test
    @DisplayName("改签失败 - 原订单状态不允许改签")
    public void testChangeTickets_Failure_OrderStatusNotAllowed() {
        // 准备测试数据
        Long userId = 1L;
        Long originalOrderId = 100L;
        List<Long> ticketIds = Arrays.asList(1L);
        
        ChangeTicketRequest request = new ChangeTicketRequest();
        request.setUserId(userId);
        request.setOriginalOrderId(originalOrderId);
        request.setTicketIds(ticketIds);
        request.setNewTrainId(2);
        request.setNewDepartureStopId(1L);
        request.setNewArrivalStopId(2L);
        request.setNewTravelDate(LocalDate.now().plusDays(2));
        request.setNewCarriageTypeId(1);
        
        // Mock Redis锁
        when(redisService.tryLock(eq("change:" + originalOrderId), eq(5L), eq(30L))).thenReturn(true);
        
        // Mock原订单查询（状态为待支付）
        Order originalOrder = new Order();
        originalOrder.setOrderId(originalOrderId);
        originalOrder.setUserId(userId);
        originalOrder.setOrderStatus((byte) OrderStatus.PENDING_PAYMENT.getCode());
        when(orderRepository.findByOrderIdAndUserId(originalOrderId, userId)).thenReturn(Optional.of(originalOrder));
        
        // 执行测试
        BookingResponse response = ticketService.changeTickets(request);
        
        // 验证结果
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getMessage()).isEqualTo("原订单状态不允许改签");
        
        // 验证锁释放
        verify(redisService, times(1)).unlock("change:" + originalOrderId);
    }

    @Test
    @DisplayName("改签失败 - 车票不存在")
    public void testChangeTickets_Failure_TicketsNotFound() {
        // 准备测试数据
        Long userId = 1L;
        Long originalOrderId = 100L;
        List<Long> ticketIds = Arrays.asList(1L);
        
        ChangeTicketRequest request = new ChangeTicketRequest();
        request.setUserId(userId);
        request.setOriginalOrderId(originalOrderId);
        request.setTicketIds(ticketIds);
        request.setNewTrainId(2);
        request.setNewDepartureStopId(1L);
        request.setNewArrivalStopId(2L);
        request.setNewTravelDate(LocalDate.now().plusDays(2));
        request.setNewCarriageTypeId(1);
        
        // Mock Redis锁
        when(redisService.tryLock(eq("change:" + originalOrderId), eq(5L), eq(30L))).thenReturn(true);
        
        // Mock原订单查询
        Order originalOrder = new Order();
        originalOrder.setOrderId(originalOrderId);
        originalOrder.setUserId(userId);
        originalOrder.setOrderStatus((byte) OrderStatus.PAID.getCode());
        when(orderRepository.findByOrderIdAndUserId(originalOrderId, userId)).thenReturn(Optional.of(originalOrder));
        
        // Mock车票查询失败
        when(ticketRepository.findByOrderIdAndTicketIdIn(originalOrderId, ticketIds))
            .thenReturn(Collections.emptyList());
        
        // 执行测试
        BookingResponse response = ticketService.changeTickets(request);
        
        // 验证结果
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getMessage()).isEqualTo("车票不存在");
        
        // 验证锁释放
        verify(redisService, times(1)).unlock("change:" + originalOrderId);
    }

    @Test
    @DisplayName("改签失败 - 车票状态不允许改签")
    public void testChangeTickets_Failure_TicketStatusNotAllowed() {
        // 准备测试数据
        Long userId = 1L;
        Long originalOrderId = 100L;
        List<Long> ticketIds = Arrays.asList(1L);
        
        ChangeTicketRequest request = new ChangeTicketRequest();
        request.setUserId(userId);
        request.setOriginalOrderId(originalOrderId);
        request.setTicketIds(ticketIds);
        request.setNewTrainId(2);
        request.setNewDepartureStopId(1L);
        request.setNewArrivalStopId(2L);
        request.setNewTravelDate(LocalDate.now().plusDays(2));
        request.setNewCarriageTypeId(1);
        
        // Mock Redis锁
        when(redisService.tryLock(eq("change:" + originalOrderId), eq(5L), eq(30L))).thenReturn(true);
        
        // Mock原订单查询
        Order originalOrder = new Order();
        originalOrder.setOrderId(originalOrderId);
        originalOrder.setUserId(userId);
        originalOrder.setOrderStatus((byte) OrderStatus.PAID.getCode());
        when(orderRepository.findByOrderIdAndUserId(originalOrderId, userId)).thenReturn(Optional.of(originalOrder));
        
        // Mock车票查询（状态为已使用）
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setOrderId(originalOrderId);
        ticket.setPassengerId(10L);
        ticket.setTicketStatus((byte) TicketStatus.USED.getCode());
        
        when(ticketRepository.findByOrderIdAndTicketIdIn(originalOrderId, ticketIds))
            .thenReturn(Arrays.asList(ticket));
        
        // 执行测试
        BookingResponse response = ticketService.changeTickets(request);
        
        // 验证结果
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getMessage()).isEqualTo("车票状态不允许改签");
        
        // 验证锁释放
        verify(redisService, times(1)).unlock("change:" + originalOrderId);
    }

    @Test
    @DisplayName("改签失败 - 时间冲突")
    public void testChangeTickets_Failure_TimeConflict() {
        // 准备测试数据
        Long userId = 1L;
        Long originalOrderId = 100L;
        List<Long> ticketIds = Arrays.asList(1L);
        
        ChangeTicketRequest request = new ChangeTicketRequest();
        request.setUserId(userId);
        request.setOriginalOrderId(originalOrderId);
        request.setTicketIds(ticketIds);
        request.setNewTrainId(2);
        request.setNewDepartureStopId(1L);
        request.setNewArrivalStopId(2L);
        request.setNewTravelDate(LocalDate.now().plusDays(2));
        request.setNewCarriageTypeId(1);
        
        // Mock Redis锁
        when(redisService.tryLock(eq("change:" + originalOrderId), eq(5L), eq(30L))).thenReturn(true);
        
        // Mock原订单查询
        Order originalOrder = new Order();
        originalOrder.setOrderId(originalOrderId);
        originalOrder.setUserId(userId);
        originalOrder.setOrderStatus((byte) OrderStatus.PAID.getCode());
        when(orderRepository.findByOrderIdAndUserId(originalOrderId, userId)).thenReturn(Optional.of(originalOrder));
        
        // Mock车票查询
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setOrderId(originalOrderId);
        ticket.setPassengerId(10L);
        ticket.setTicketStatus((byte) TicketStatus.UNUSED.getCode());
        
        when(ticketRepository.findByOrderIdAndTicketIdIn(originalOrderId, ticketIds))
            .thenReturn(Arrays.asList(ticket));
        
        // Mock时间冲突检查（有冲突）
        List<Ticket> conflictTickets = Arrays.asList(ticket);
        when(timeConflictService.checkTimeConflict(anyLong(), any(LocalDate.class), anyInt(), anyLong(), anyLong(), anyLong()))
            .thenReturn(conflictTickets);
        when(timeConflictService.generateConflictMessage(anyList())).thenReturn("存在时间冲突");
        
        // 执行测试
        BookingResponse response = ticketService.changeTickets(request);
        
        // 验证结果
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getMessage()).isEqualTo("乘客ID 10 存在时间冲突");
        
        // 验证锁释放
        verify(redisService, times(1)).unlock("change:" + originalOrderId);
    }

    @Test
    @DisplayName("改签失败 - 城市验证失败")
    public void testChangeTickets_Failure_CityValidationFailed() {
        // 准备测试数据
        Long userId = 1L;
        Long originalOrderId = 100L;
        List<Long> ticketIds = Arrays.asList(1L);
        
        ChangeTicketRequest request = new ChangeTicketRequest();
        request.setUserId(userId);
        request.setOriginalOrderId(originalOrderId);
        request.setTicketIds(ticketIds);
        request.setNewTrainId(2);
        request.setNewDepartureStopId(999L); // 使用不同的站ID，触发城市验证失败
        request.setNewArrivalStopId(998L);
        request.setNewTravelDate(LocalDate.now().plusDays(2));
        request.setNewCarriageTypeId(1);
        
        // Mock Redis锁
        when(redisService.tryLock(eq("change:" + originalOrderId), eq(5L), eq(30L))).thenReturn(true);
        
        // Mock原订单查询
        Order originalOrder = new Order();
        originalOrder.setOrderId(originalOrderId);
        originalOrder.setUserId(userId);
        originalOrder.setOrderStatus((byte) OrderStatus.PAID.getCode());
        when(orderRepository.findByOrderIdAndUserId(originalOrderId, userId)).thenReturn(Optional.of(originalOrder));
        
        // Mock车票查询
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setOrderId(originalOrderId);
        ticket.setPassengerId(10L);
        ticket.setTicketStatus((byte) TicketStatus.UNUSED.getCode());
        ticket.setDepartureStopId(1L);
        ticket.setArrivalStopId(2L);
        
        when(ticketRepository.findByOrderIdAndTicketIdIn(originalOrderId, ticketIds))
            .thenReturn(Arrays.asList(ticket));
        
        // Mock时间冲突检查（无冲突）
        when(timeConflictService.checkTimeConflict(anyLong(), any(LocalDate.class), anyInt(), anyLong(), anyLong(), anyLong()))
            .thenReturn(Collections.emptyList());
        
        // Mock原票站点城市信息 - 北京 → 上海
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.of(new TrainStop() {{
            setStationId(1);
        }}));
        when(trainStopRepository.findByStopId(2L)).thenReturn(Optional.of(new TrainStop() {{
            setStationId(2);
        }}));
        when(stationRepository.findById(1)).thenReturn(Optional.of(new Station() {{
            setCity("北京");
        }}));
        when(stationRepository.findById(2)).thenReturn(Optional.of(new Station() {{
            setCity("上海");
        }}));
        
        // Mock新站点城市信息 - 广州 → 深圳（不同城市）
        when(trainStopRepository.findByStopId(999L)).thenReturn(Optional.of(new TrainStop() {{
            setStationId(999);
        }}));
        when(trainStopRepository.findByStopId(998L)).thenReturn(Optional.of(new TrainStop() {{
            setStationId(998);
        }}));
        when(stationRepository.findById(999)).thenReturn(Optional.of(new Station() {{
            setCity("广州");
        }}));
        when(stationRepository.findById(998)).thenReturn(Optional.of(new Station() {{
            setCity("深圳");
        }}));
        
        // 执行测试
        BookingResponse response = ticketService.changeTickets(request);
        
        // 验证结果 - 应该会失败，因为城市不同
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getMessage()).isEqualTo("改签的出发站和到达站城市必须与原票一致");
        
        // 验证锁释放
        verify(redisService, times(1)).unlock("change:" + originalOrderId);
    }

    @Test
    @DisplayName("改签失败 - 新车次余票不足")
    public void testChangeTickets_Failure_InsufficientStock() {
        // 准备测试数据
        Long userId = 1L;
        Long originalOrderId = 100L;
        List<Long> ticketIds = Arrays.asList(1L);
        
        ChangeTicketRequest request = new ChangeTicketRequest();
        request.setUserId(userId);
        request.setOriginalOrderId(originalOrderId);
        request.setTicketIds(ticketIds);
        request.setNewTrainId(2);
        request.setNewDepartureStopId(1L);
        request.setNewArrivalStopId(2L);
        request.setNewTravelDate(LocalDate.now().plusDays(2));
        request.setNewCarriageTypeId(1);
        
        // Mock Redis锁
        when(redisService.tryLock(eq("change:" + originalOrderId), eq(5L), eq(30L))).thenReturn(true);
        
        // Mock原订单查询
        Order originalOrder = new Order();
        originalOrder.setOrderId(originalOrderId);
        originalOrder.setUserId(userId);
        originalOrder.setOrderStatus((byte) OrderStatus.PAID.getCode());
        when(orderRepository.findByOrderIdAndUserId(originalOrderId, userId)).thenReturn(Optional.of(originalOrder));
        
        // Mock车票查询
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setOrderId(originalOrderId);
        ticket.setPassengerId(10L);
        ticket.setTicketStatus((byte) TicketStatus.UNUSED.getCode());
        ticket.setDepartureStopId(1L);
        ticket.setArrivalStopId(2L);
        
        when(ticketRepository.findByOrderIdAndTicketIdIn(originalOrderId, ticketIds))
            .thenReturn(Arrays.asList(ticket));
        
        // Mock时间冲突检查（无冲突）
        when(timeConflictService.checkTimeConflict(anyLong(), any(LocalDate.class), anyInt(), anyLong(), anyLong(), anyLong()))
            .thenReturn(Collections.emptyList());
        
        // Mock车厢类型查询（用于错误消息）
        CarriageType carriageType = new CarriageType();
        carriageType.setTypeId(1);
        carriageType.setTypeName("商务座");
        when(carriageTypeRepository.findById(1)).thenReturn(Optional.of(carriageType));
        
        // Mock库存检查失败
        when(redisService.decrStock(eq(2), eq(1L), eq(2L), eq(LocalDate.now().plusDays(2)), eq(1), eq(1)))
            .thenReturn(false);
        
        // 执行测试
        BookingResponse response = ticketService.changeTickets(request);
        
        // 验证结果
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getMessage()).isEqualTo("新车次席别 商务座 余票不足");
        
        // 验证锁释放
        verify(redisService, times(1)).unlock("change:" + originalOrderId);
    }

    @Test
    @DisplayName("改签异常处理")
    public void testChangeTickets_Exception() {
        // 准备测试数据
        Long userId = 1L;
        Long originalOrderId = 100L;
        List<Long> ticketIds = Arrays.asList(1L);
        
        ChangeTicketRequest request = new ChangeTicketRequest();
        request.setUserId(userId);
        request.setOriginalOrderId(originalOrderId);
        request.setTicketIds(ticketIds);
        request.setNewTrainId(2);
        request.setNewDepartureStopId(1L);
        request.setNewArrivalStopId(2L);
        request.setNewTravelDate(LocalDate.now().plusDays(2));
        request.setNewCarriageTypeId(1);
        
        // Mock Redis锁
        when(redisService.tryLock(eq("change:" + originalOrderId), eq(5L), eq(30L))).thenReturn(true);
        
        // Mock原订单查询抛出异常
        when(orderRepository.findByOrderIdAndUserId(originalOrderId, userId))
            .thenThrow(new RuntimeException("数据库连接失败"));
        
        // 执行测试
        BookingResponse response = ticketService.changeTickets(request);
        
        // 验证结果
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getMessage()).isEqualTo("数据库异常: 数据库连接失败");
        
        // 验证锁释放
        verify(redisService, times(1)).unlock("change:" + originalOrderId);
    }

    @Test
    @DisplayName("改签成功 - passengers为null（兼容旧版本）")
    public void testChangeTickets_Success_PassengersNull() {
        // 准备测试数据
        Long userId = 1L;
        Long originalOrderId = 100L;
        List<Long> ticketIds = Arrays.asList(1L);
        
        ChangeTicketRequest request = new ChangeTicketRequest();
        request.setUserId(userId);
        request.setOriginalOrderId(originalOrderId);
        request.setTicketIds(ticketIds);
        request.setNewTrainId(2);
        request.setNewDepartureStopId(1L);
        request.setNewArrivalStopId(2L);
        request.setNewTravelDate(LocalDate.now().plusDays(2));
        request.setNewCarriageTypeId(1);
        request.setPassengers(null); // 明确设置为null，测试第339行分支
        
        // Mock Redis锁
        when(redisService.tryLock(eq("change:" + originalOrderId), eq(5L), eq(30L))).thenReturn(true);
        
        // Mock原订单查询
        Order originalOrder = new Order();
        originalOrder.setOrderId(originalOrderId);
        originalOrder.setUserId(userId);
        originalOrder.setOrderStatus((byte) OrderStatus.PAID.getCode());
        when(orderRepository.findByOrderIdAndUserId(originalOrderId, userId)).thenReturn(Optional.of(originalOrder));
        
        // Mock车票查询
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setOrderId(originalOrderId);
        ticket.setPassengerId(10L);
        ticket.setTicketStatus((byte) TicketStatus.UNUSED.getCode());
        ticket.setDepartureStopId(1L);
        ticket.setArrivalStopId(2L);
        
        when(ticketRepository.findByOrderIdAndTicketIdIn(originalOrderId, ticketIds))
            .thenReturn(Arrays.asList(ticket));
        
        // Mock时间冲突检查（无冲突）
        when(timeConflictService.checkTimeConflict(anyLong(), any(LocalDate.class), anyInt(), anyLong(), anyLong(), anyLong()))
            .thenReturn(Collections.emptyList());
        
        // Mock库存检查
        when(redisService.decrStock(eq(2), eq(1L), eq(2L), eq(LocalDate.now().plusDays(2)), eq(1), eq(1)))
            .thenReturn(true);
        
        // Mock订单号生成
        when(redisService.generateOrderNumber()).thenReturn("ORD20241201003");
        
        // Mock新订单保存
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(300L);
            order.setOrderNumber("ORD20241201003");
            return order;
        });
        
        // Mock车票保存
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket newTicket = invocation.getArgument(0);
            if (newTicket.getTicketId() == null) {
                newTicket.setTicketId(System.currentTimeMillis());
            }
            return newTicket;
        });
        
        // Mock库存查询（用于价格计算）
        TicketInventory inventory = new TicketInventory();
        inventory.setPrice(new BigDecimal("300.00"));
        when(ticketInventoryDAO.findByKey(eq(2), eq(1L), eq(2L), eq(LocalDate.now().plusDays(2)), eq(1)))
            .thenReturn(Optional.of(inventory));
        
        // Mock座位分配
        doNothing().when(seatService).assignSeat(any(Ticket.class));
        
        // Mock改签配对关系存储
        doNothing().when(redisService).setChangeMapping(anyString(), anyString());
        
        // 执行测试
        BookingResponse response = ticketService.changeTickets(request);
        
        // 验证结果
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getMessage()).isEqualTo("改签成功");
        
        // 验证锁释放
        verify(redisService, times(1)).unlock("change:" + originalOrderId);
    }

    @Test
    @DisplayName("改签成功 - passengers为空列表（兼容旧版本）")
    public void testChangeTickets_Success_PassengersEmpty() {
        // 准备测试数据
        Long userId = 1L;
        Long originalOrderId = 100L;
        List<Long> ticketIds = Arrays.asList(1L);
        
        ChangeTicketRequest request = new ChangeTicketRequest();
        request.setUserId(userId);
        request.setOriginalOrderId(originalOrderId);
        request.setTicketIds(ticketIds);
        request.setNewTrainId(2);
        request.setNewDepartureStopId(1L);
        request.setNewArrivalStopId(2L);
        request.setNewTravelDate(LocalDate.now().plusDays(2));
        request.setNewCarriageTypeId(1);
        request.setPassengers(new ArrayList<>()); // 设置为空列表，测试第340行指令
        
        // Mock Redis锁
        when(redisService.tryLock(eq("change:" + originalOrderId), eq(5L), eq(30L))).thenReturn(true);
        
        // Mock原订单查询
        Order originalOrder = new Order();
        originalOrder.setOrderId(originalOrderId);
        originalOrder.setUserId(userId);
        originalOrder.setOrderStatus((byte) OrderStatus.PAID.getCode());
        when(orderRepository.findByOrderIdAndUserId(originalOrderId, userId)).thenReturn(Optional.of(originalOrder));
        
        // Mock车票查询
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setOrderId(originalOrderId);
        ticket.setPassengerId(10L);
        ticket.setTicketStatus((byte) TicketStatus.UNUSED.getCode());
        ticket.setDepartureStopId(1L);
        ticket.setArrivalStopId(2L);
        
        when(ticketRepository.findByOrderIdAndTicketIdIn(originalOrderId, ticketIds))
            .thenReturn(Arrays.asList(ticket));
        
        // Mock时间冲突检查（无冲突）
        when(timeConflictService.checkTimeConflict(anyLong(), any(LocalDate.class), anyInt(), anyLong(), anyLong(), anyLong()))
            .thenReturn(Collections.emptyList());
        
        // Mock库存检查
        when(redisService.decrStock(eq(2), eq(1L), eq(2L), eq(LocalDate.now().plusDays(2)), eq(1), eq(1)))
            .thenReturn(true);
        
        // Mock订单号生成
        when(redisService.generateOrderNumber()).thenReturn("ORD20241201004");
        
        // Mock新订单保存
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(400L);
            order.setOrderNumber("ORD20241201004");
            return order;
        });
        
        // Mock车票保存
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket newTicket = invocation.getArgument(0);
            if (newTicket.getTicketId() == null) {
                newTicket.setTicketId(System.currentTimeMillis());
            }
            return newTicket;
        });
        
        // Mock库存查询（用于价格计算）
        TicketInventory inventory = new TicketInventory();
        inventory.setPrice(new BigDecimal("350.00"));
        when(ticketInventoryDAO.findByKey(eq(2), eq(1L), eq(2L), eq(LocalDate.now().plusDays(2)), eq(1)))
            .thenReturn(Optional.of(inventory));
        
        // Mock座位分配
        doNothing().when(seatService).assignSeat(any(Ticket.class));
        
        // Mock改签配对关系存储
        doNothing().when(redisService).setChangeMapping(anyString(), anyString());
        
        // 执行测试
        BookingResponse response = ticketService.changeTickets(request);
        
        // 验证结果
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getMessage()).isEqualTo("改签成功");
        
        // 验证锁释放
        verify(redisService, times(1)).unlock("change:" + originalOrderId);
    }
    
    @Test
    @DisplayName("改签成功 - passengers为空列表（兼容旧版本）")
    public void testChangeTickets_Success_PassengersEmptyList() {
        // 准备测试数据
        Long userId = 1L;
        Long originalOrderId = 100L;
        List<Long> ticketIds = Arrays.asList(1L);
        
        ChangeTicketRequest request = new ChangeTicketRequest();
        request.setUserId(userId);
        request.setOriginalOrderId(originalOrderId);
        request.setTicketIds(ticketIds);
        request.setNewTrainId(2);
        request.setNewDepartureStopId(1L);
        request.setNewArrivalStopId(2L);
        request.setNewTravelDate(LocalDate.now().plusDays(2));
        request.setNewCarriageTypeId(1);
        request.setPassengers(new ArrayList<>()); // 设置为空列表，测试第340行指令
        
        // Mock原订单
        Order originalOrder = new Order();
        originalOrder.setOrderId(originalOrderId);
        originalOrder.setUserId(userId);
        originalOrder.setOrderStatus((byte) OrderStatus.PAID.getCode());
        originalOrder.setOrderNumber("O123456");
        when(orderRepository.findByOrderIdAndUserId(originalOrderId, userId))
            .thenReturn(Optional.of(originalOrder));
        
        // Mock原车票
        Ticket originalTicket = new Ticket();
        originalTicket.setTicketId(1L);
        originalTicket.setOrderId(originalOrderId);
        originalTicket.setPassengerId(1L);
        originalTicket.setTicketStatus((byte) TicketStatus.UNUSED.getCode());
        originalTicket.setTrainId(1);
        originalTicket.setDepartureStopId(1L);
        originalTicket.setArrivalStopId(2L);
        originalTicket.setTravelDate(LocalDate.now().plusDays(1));
        originalTicket.setCarriageTypeId(1);
        originalTicket.setPrice(new BigDecimal("100.00"));
        when(ticketRepository.findByOrderIdAndTicketIdIn(originalOrderId, ticketIds))
            .thenReturn(Arrays.asList(originalTicket));
        
        // Mock时间冲突检查
        when(timeConflictService.checkTimeConflict(anyLong(), any(LocalDate.class), anyInt(), anyLong(), anyLong(), anyLong()))
            .thenReturn(new ArrayList<>());
        
        // Mock Redis锁
        when(redisService.tryLock(eq("change:" + originalOrderId), eq(5L), eq(30L)))
            .thenReturn(true);
        
        // Mock库存检查
        when(redisService.decrStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt(), anyInt()))
            .thenReturn(true);
        
        // Mock新订单保存
        Order newOrder = new Order();
        newOrder.setOrderId(200L);
        newOrder.setOrderNumber("O654321");
        newOrder.setTotalAmount(new BigDecimal("120.00"));
        when(orderRepository.save(any(Order.class)))
            .thenReturn(newOrder);
        
        // Mock新票保存
        Ticket newTicket = new Ticket();
        newTicket.setTicketId(2L);
        newTicket.setPrice(new BigDecimal("120.00"));
        when(ticketRepository.save(any(Ticket.class)))
            .thenReturn(newTicket);
        
        // Mock座位分配
        doNothing().when(seatService).assignSeat(any(Ticket.class));
        
        // Mock改签映射
        doNothing().when(redisService).setChangeMapping(anyString(), anyString());
        
        // 执行测试
        BookingResponse response = ticketService.changeTickets(request);
        
        // 验证结果
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getMessage()).isEqualTo("改签成功");
        
        // 验证锁释放
        verify(redisService, times(1)).unlock("change:" + originalOrderId);
    }
    
    @Test
    public void testValidateStationInSameCity_ExceptionBranch() throws Exception {
        // 使用反射测试私有方法
        Method validateStationInSameCityMethod = TicketServiceImpl.class.getDeclaredMethod("validateStationsInSameCity", Long.class, Long.class);
        validateStationInSameCityMethod.setAccessible(true);
        
        // 测试正常情况
        assertTrue((Boolean) validateStationInSameCityMethod.invoke(ticketService, 1L, 2L));
        
        // 测试异常分支 - 传入null参数触发异常
        assertFalse((Boolean) validateStationInSameCityMethod.invoke(ticketService, null, 2L));
        assertFalse((Boolean) validateStationInSameCityMethod.invoke(ticketService, 1L, null));
        assertFalse((Boolean) validateStationInSameCityMethod.invoke(ticketService, null, null));
    }
} 
