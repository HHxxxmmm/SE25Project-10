package com.example.techprototype.Service.Impl;

import com.example.techprototype.DTO.*;
import com.example.techprototype.Entity.*;
import com.example.techprototype.Enums.WaitlistOrderStatus;
import com.example.techprototype.Enums.WaitlistItemStatus;
import com.example.techprototype.Repository.*;
import com.example.techprototype.Service.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WaitlistOrderServiceImplTest {

    @InjectMocks
    private WaitlistOrderServiceImpl waitlistOrderService;

    @Mock
    private WaitlistOrderRepository waitlistOrderRepository;
    @Mock
    private WaitlistItemRepository waitlistItemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PassengerRepository passengerRepository;
    @Mock
    private TrainRepository trainRepository;
    @Mock
    private TrainStopRepository trainStopRepository;
    @Mock
    private StationRepository stationRepository;
    @Mock
    private CarriageTypeRepository carriageTypeRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private TicketInventoryRepository ticketInventoryRepository;
    @Mock
    private RedisService redisService;

    private BookingRequest bookingRequest;
    private User user;
    private WaitlistOrder waitlistOrder;
    private WaitlistItem waitlistItem;
    private Train train;
    private TicketInventory ticketInventory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 设置测试数据
        user = new User();
        user.setUserId(1L);
        user.setRealName("测试用户");
        
        train = new Train();
        train.setTrainId(1);
        train.setTrainNumber("G101");
        train.setDepartureTime(LocalTime.of(8, 0));
        
        ticketInventory = new TicketInventory();
        ticketInventory.setPrice(new BigDecimal("100"));
        ticketInventory.setAvailableSeats(10);
        
        // 设置 BookingRequest
        bookingRequest = new BookingRequest();
        bookingRequest.setUserId(1L);
        bookingRequest.setTrainId(1);
        bookingRequest.setDepartureStopId(1L);
        bookingRequest.setArrivalStopId(2L);
        bookingRequest.setTravelDate(LocalDate.of(2025, 1, 1));
        
        BookingRequest.PassengerInfo passengerInfo = new BookingRequest.PassengerInfo();
        passengerInfo.setPassengerId(100L);
        passengerInfo.setCarriageTypeId(1);
        passengerInfo.setTicketType((byte) 1);
        bookingRequest.setPassengers(Arrays.asList(passengerInfo));
        
        // 设置 WaitlistOrder
        waitlistOrder = new WaitlistOrder();
        waitlistOrder.setWaitlistId(1L);
        waitlistOrder.setOrderNumber("WL20250110001");
        waitlistOrder.setUserId(1L);
        waitlistOrder.setOrderTime(LocalDateTime.now());
        waitlistOrder.setTotalAmount(new BigDecimal("1000"));
        waitlistOrder.setExpireTime(LocalDateTime.now().plusDays(1));
        waitlistOrder.setOrderStatus((byte) WaitlistOrderStatus.PENDING_PAYMENT.getCode());
        waitlistOrder.setItemCount(1);
        
        // 设置 WaitlistItem
        waitlistItem = new WaitlistItem();
        waitlistItem.setItemId(1L);
        waitlistItem.setWaitlistId(1L);
        waitlistItem.setPassengerId(100L);
        waitlistItem.setTrainId(1);
        waitlistItem.setDepartureStopId(1L);
        waitlistItem.setArrivalStopId(2L);
        waitlistItem.setTravelDate(LocalDate.of(2025, 1, 1));
        waitlistItem.setCarriageTypeId(1);
        waitlistItem.setTicketType((byte) 1);
        waitlistItem.setItemStatus((byte) WaitlistItemStatus.PENDING_PAYMENT.getCode());
        waitlistItem.setCreatedTime(LocalDateTime.now());
        waitlistItem.setPrice(new BigDecimal(1000));
    }

    @Test
    void testCreateWaitlistOrder_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(trainRepository.findById(1)).thenReturn(Optional.of(train));
        when(ticketInventoryRepository.findByKeyWithLock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt()))
            .thenReturn(Optional.of(ticketInventory));
        when(waitlistOrderRepository.save(any(WaitlistOrder.class))).thenReturn(waitlistOrder);
        when(waitlistItemRepository.saveAll(anyList())).thenReturn(Arrays.asList(waitlistItem));

        BookingResponse response = waitlistOrderService.createWaitlistOrder(bookingRequest);
        
        assertEquals("SUCCESS", response.getStatus());
        assertTrue(response.getMessage().contains("候补订单创建成功"));
        verify(waitlistOrderRepository).save(any(WaitlistOrder.class));
        verify(waitlistItemRepository).saveAll(anyList());
    }

    @Test
    void testCreateWaitlistOrder_UserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        BookingResponse response = waitlistOrderService.createWaitlistOrder(bookingRequest);
        
        assertEquals("FAILED", response.getStatus());
        assertEquals("用户不存在", response.getMessage());
        verify(waitlistOrderRepository, never()).save(any(WaitlistOrder.class));
    }

    @Test
    void testCreateWaitlistOrder_Exception() {
        when(userRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));

        BookingResponse response = waitlistOrderService.createWaitlistOrder(bookingRequest);
        
        assertEquals("FAILED", response.getStatus());
        assertTrue(response.getMessage().contains("创建候补订单失败"));
    }

    @Test
    void testGetMyWaitlistOrders_Success() {
        when(waitlistOrderRepository.findByUserIdOrderByOrderTimeDesc(1L))
            .thenReturn(Arrays.asList(waitlistOrder));
        when(waitlistItemRepository.findByWaitlistId(1L))
            .thenReturn(Arrays.asList(waitlistItem));
        when(trainRepository.findById(1)).thenReturn(Optional.of(train));
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.of(new TrainStop()));
        when(stationRepository.findById(anyInt())).thenReturn(Optional.of(new Station()));

        WaitlistOrderResponse response = waitlistOrderService.getMyWaitlistOrders(1L);
        
        assertEquals("SUCCESS", response.getStatus());
        assertNotNull(response.getWaitlistOrders());
        assertEquals(1, response.getWaitlistOrders().size());
    }

    @Test
    void testGetMyWaitlistOrders_Exception() {
        when(waitlistOrderRepository.findByUserIdOrderByOrderTimeDesc(1L))
            .thenThrow(new RuntimeException("Database error"));

        WaitlistOrderResponse response = waitlistOrderService.getMyWaitlistOrders(1L);
        
        assertEquals("FAILURE", response.getStatus());
        assertTrue(response.getMessage().contains("获取候补订单失败"));
    }

    @Test
    void testGetWaitlistOrderDetail_Success() {
        when(waitlistOrderRepository.findById(1L)).thenReturn(Optional.of(waitlistOrder));
        when(waitlistItemRepository.findByWaitlistId(1L))
            .thenReturn(Arrays.asList(waitlistItem));
        when(passengerRepository.findById(100L)).thenReturn(Optional.of(new Passenger()));
        when(trainRepository.findById(1)).thenReturn(Optional.of(train));
        when(trainStopRepository.findByStopId(anyLong())).thenReturn(Optional.of(new TrainStop()));
        when(stationRepository.findById(anyInt())).thenReturn(Optional.of(new Station()));
        when(carriageTypeRepository.findById(1)).thenReturn(Optional.of(new CarriageType()));

        WaitlistOrderDetailResponse response = waitlistOrderService.getWaitlistOrderDetail(1L, 1L);
        
        assertEquals("SUCCESS", response.getStatus());
        assertNotNull(response.getWaitlistOrder());
    }

    @Test
    void testGetWaitlistOrderDetail_OrderNotFound() {
        when(waitlistOrderRepository.findById(1L)).thenReturn(Optional.empty());

        WaitlistOrderDetailResponse response = waitlistOrderService.getWaitlistOrderDetail(1L, 1L);
        
        assertEquals("FAILURE", response.getStatus());
        assertEquals("候补订单不存在", response.getMessage());
    }

    @Test
    void testGetWaitlistOrderDetail_Unauthorized() {
        waitlistOrder.setUserId(999L);
        when(waitlistOrderRepository.findById(1L)).thenReturn(Optional.of(waitlistOrder));

        WaitlistOrderDetailResponse response = waitlistOrderService.getWaitlistOrderDetail(1L, 1L);
        
        assertEquals("FAILURE", response.getStatus());
        assertEquals("无权访问此候补订单", response.getMessage());
    }

    @Test
    void testPayWaitlistOrder_Success() {
        when(waitlistOrderRepository.findById(1L)).thenReturn(Optional.of(waitlistOrder));
        when(waitlistItemRepository.findByWaitlistId(1L))
            .thenReturn(Arrays.asList(waitlistItem));
        when(waitlistOrderRepository.save(any(WaitlistOrder.class))).thenReturn(waitlistOrder);
        when(waitlistItemRepository.saveAll(anyList())).thenReturn(Arrays.asList(waitlistItem));

        BookingResponse response = waitlistOrderService.payWaitlistOrder(1L, 1L);
        
        assertEquals("SUCCESS", response.getStatus());
        assertTrue(response.getMessage().contains("候补订单支付成功"));
        verify(waitlistOrderRepository).save(any(WaitlistOrder.class));
        verify(waitlistItemRepository).saveAll(anyList());
    }

    @Test
    void testPayWaitlistOrder_OrderNotFound() {
        when(waitlistOrderRepository.findById(1L)).thenReturn(Optional.empty());

        BookingResponse response = waitlistOrderService.payWaitlistOrder(1L, 1L);
        
        assertEquals("FAILED", response.getStatus());
        assertEquals("候补订单不存在", response.getMessage());
    }

    @Test
    void testPayWaitlistOrder_Unauthorized() {
        waitlistOrder.setUserId(999L);
        when(waitlistOrderRepository.findById(1L)).thenReturn(Optional.of(waitlistOrder));

        BookingResponse response = waitlistOrderService.payWaitlistOrder(1L, 1L);
        
        assertEquals("FAILED", response.getStatus());
        assertEquals("无权操作此候补订单", response.getMessage());
    }

    @Test
    void testPayWaitlistOrder_WrongStatus() {
        waitlistOrder.setOrderStatus((byte) WaitlistOrderStatus.CANCELLED.getCode());
        when(waitlistOrderRepository.findById(1L)).thenReturn(Optional.of(waitlistOrder));

        BookingResponse response = waitlistOrderService.payWaitlistOrder(1L, 1L);
        
        assertEquals("FAILED", response.getStatus());
        assertEquals("候补订单状态不正确", response.getMessage());
    }

    @Test
    void testCancelWaitlistOrder_Success() {
        when(waitlistOrderRepository.findById(1L)).thenReturn(Optional.of(waitlistOrder));
        when(waitlistItemRepository.findByWaitlistId(1L))
            .thenReturn(Arrays.asList(waitlistItem));
        when(waitlistOrderRepository.save(any(WaitlistOrder.class))).thenReturn(waitlistOrder);
        when(waitlistItemRepository.saveAll(anyList())).thenReturn(Arrays.asList(waitlistItem));

        BookingResponse response = waitlistOrderService.cancelWaitlistOrder(1L, 1L);
        
        assertEquals("SUCCESS", response.getStatus());
        assertTrue(response.getMessage().contains("候补订单取消成功"));
        verify(waitlistOrderRepository).save(any(WaitlistOrder.class));
        verify(waitlistItemRepository).saveAll(anyList());
    }

    @Test
    void testCancelWaitlistOrder_OrderNotFound() {
        when(waitlistOrderRepository.findById(1L)).thenReturn(Optional.empty());

        BookingResponse response = waitlistOrderService.cancelWaitlistOrder(1L, 1L);
        
        assertEquals("FAILED", response.getStatus());
        assertEquals("候补订单不存在", response.getMessage());
    }

    @Test
    void testProcessWaitlistFulfillment_Success() {
        when(waitlistOrderRepository.findPendingFulfillmentOrders(any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(waitlistOrder));
        when(waitlistItemRepository.findPendingItemsByWaitlistId(1L))
            .thenReturn(Arrays.asList(waitlistItem));
        when(ticketInventoryRepository.findByKeyWithLock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt()))
            .thenReturn(Optional.of(ticketInventory));
        when(waitlistItemRepository.save(any(WaitlistItem.class))).thenReturn(waitlistItem);

        assertDoesNotThrow(() -> waitlistOrderService.processWaitlistFulfillment());
        
        verify(waitlistOrderRepository).findPendingFulfillmentOrders(any(LocalDateTime.class));
        verify(waitlistItemRepository).findPendingItemsByWaitlistId(1L);
    }

    @Test
    void testProcessWaitlistFulfillment_NoInventory() {
        when(waitlistOrderRepository.findPendingFulfillmentOrders(any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(waitlistOrder));
        when(waitlistItemRepository.findPendingItemsByWaitlistId(1L))
            .thenReturn(Arrays.asList(waitlistItem));
        when(ticketInventoryRepository.findByKeyWithLock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt()))
            .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> waitlistOrderService.processWaitlistFulfillment());
        
        verify(waitlistItemRepository, never()).save(any(WaitlistItem.class));
    }

    @Test
    void testRefundWaitlistOrder_PendingFulfillment() {
        waitlistOrder.setOrderStatus((byte) WaitlistOrderStatus.PENDING_FULFILLMENT.getCode());
        when(waitlistOrderRepository.findById(1L)).thenReturn(Optional.of(waitlistOrder));
        when(waitlistItemRepository.findByWaitlistId(1L))
            .thenReturn(Arrays.asList(waitlistItem));
        when(waitlistOrderRepository.save(any(WaitlistOrder.class))).thenReturn(waitlistOrder);
        when(waitlistItemRepository.saveAll(anyList())).thenReturn(Arrays.asList(waitlistItem));

        BookingResponse response = waitlistOrderService.refundWaitlistOrder(1L, 1L);
        
        assertEquals("SUCCESS", response.getStatus());
        assertTrue(response.getMessage().contains("候补订单退款成功"));
    }

    @Test
    void testRefundWaitlistOrder_Fulfilled() {
        waitlistOrder.setOrderStatus((byte) WaitlistOrderStatus.FULFILLED.getCode());
        waitlistItem.setItemStatus((byte) WaitlistItemStatus.FULFILLED.getCode());
        
        // 创建模拟的Order和Ticket对象
        Order mockOrder = new Order();
        mockOrder.setOrderId(1L);
        mockOrder.setOrderNumber("WL123456789");
        
        Ticket mockTicket = new Ticket();
        mockTicket.setTicketId(1L);
        mockTicket.setTrainId(1);
        mockTicket.setDepartureStopId(1L);
        mockTicket.setArrivalStopId(2L);
        mockTicket.setTravelDate(LocalDate.now().plusDays(1));
        mockTicket.setCarriageTypeId(1);
        mockTicket.setPrice(new BigDecimal("100"));
        
        when(waitlistOrderRepository.findById(1L)).thenReturn(Optional.of(waitlistOrder));
        when(waitlistItemRepository.findByWaitlistId(1L))
            .thenReturn(Arrays.asList(waitlistItem));
        when(orderRepository.findByOrderNumber(anyString())).thenReturn(Optional.of(mockOrder));
        when(ticketRepository.findByOrderId(anyLong())).thenReturn(Arrays.asList(mockTicket));
        when(waitlistOrderRepository.save(any(WaitlistOrder.class))).thenReturn(waitlistOrder);
        when(waitlistItemRepository.saveAll(anyList())).thenReturn(Arrays.asList(waitlistItem));

        BookingResponse response = waitlistOrderService.refundWaitlistOrder(1L, 1L);
        
        assertEquals("SUCCESS", response.getStatus());
        verify(redisService).incrStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt(), eq(1));
    }

    @Test
    void testRefundWaitlistOrder_WrongStatus() {
        waitlistOrder.setOrderStatus((byte) WaitlistOrderStatus.PENDING_PAYMENT.getCode());
        when(waitlistOrderRepository.findById(1L)).thenReturn(Optional.of(waitlistOrder));

        BookingResponse response = waitlistOrderService.refundWaitlistOrder(1L, 1L);
        
        assertEquals("FAILED", response.getStatus());
        assertEquals("候补订单状态不允许退款", response.getMessage());
    }

    @Test
    void testRefundWaitlistOrderItems_Success() {
        waitlistOrder.setOrderStatus((byte) WaitlistOrderStatus.PENDING_FULFILLMENT.getCode());
        waitlistItem.setItemStatus((byte) WaitlistItemStatus.PENDING_FULFILLMENT.getCode());
        waitlistItem.setPrice(new BigDecimal("100"));
        
        when(waitlistOrderRepository.findById(1L)).thenReturn(Optional.of(waitlistOrder));
        when(waitlistItemRepository.findByItemIdIn(anyList())).thenReturn(Arrays.asList(waitlistItem));
        when(waitlistItemRepository.findByWaitlistId(1L))
            .thenReturn(Arrays.asList(waitlistItem));
        when(waitlistOrderRepository.save(any(WaitlistOrder.class))).thenReturn(waitlistOrder);
        when(waitlistItemRepository.saveAll(anyList())).thenReturn(Arrays.asList(waitlistItem));

        BookingResponse response = waitlistOrderService.refundWaitlistOrderItems(1L, 1L, Arrays.asList(1L));
        
        assertEquals("SUCCESS", response.getStatus());
        assertTrue(response.getMessage().contains("候补订单项退款成功"));
    }

    @Test
    void testRefundWaitlistOrderItems_OrderNotFound() {
        when(waitlistOrderRepository.findById(1L)).thenReturn(Optional.empty());

        BookingResponse response = waitlistOrderService.refundWaitlistOrderItems(1L, 1L, Arrays.asList(1L));
        
        assertEquals("FAILED", response.getStatus());
        assertEquals("候补订单不存在", response.getMessage());
    }

    @Test
    void testRefundWaitlistOrderItems_Unauthorized() {
        waitlistOrder.setUserId(999L);
        when(waitlistOrderRepository.findById(1L)).thenReturn(Optional.of(waitlistOrder));

        BookingResponse response = waitlistOrderService.refundWaitlistOrderItems(1L, 1L, Arrays.asList(1L));
        
        assertEquals("FAILED", response.getStatus());
        assertEquals("无权操作此候补订单", response.getMessage());
    }

    @Test
    void testRefundWaitlistOrderItems_WrongStatus() {
        waitlistOrder.setOrderStatus((byte) WaitlistOrderStatus.PENDING_PAYMENT.getCode());
        when(waitlistOrderRepository.findById(1L)).thenReturn(Optional.of(waitlistOrder));

        BookingResponse response = waitlistOrderService.refundWaitlistOrderItems(1L, 1L, Arrays.asList(1L));
        
        assertEquals("FAILED", response.getStatus());
        assertEquals("候补订单状态不允许退款", response.getMessage());
    }

    @Test
    void testRefundWaitlistOrderItems_ItemsNotFound() {
        waitlistOrder.setOrderStatus((byte) WaitlistOrderStatus.PENDING_FULFILLMENT.getCode());
        when(waitlistOrderRepository.findById(1L)).thenReturn(Optional.of(waitlistOrder));
        when(waitlistItemRepository.findByItemIdIn(anyList())).thenReturn(Collections.emptyList());

        BookingResponse response = waitlistOrderService.refundWaitlistOrderItems(1L, 1L, Arrays.asList(1L));
        
        assertEquals("FAILED", response.getStatus());
        assertEquals("未找到指定的候补订单项", response.getMessage());
    }

    @Test
    void testRefundWaitlistOrderItems_WrongItemStatus() {
        waitlistOrder.setOrderStatus((byte) WaitlistOrderStatus.PENDING_FULFILLMENT.getCode());
        waitlistItem.setItemStatus((byte) WaitlistItemStatus.CANCELLED.getCode());
        
        when(waitlistOrderRepository.findById(1L)).thenReturn(Optional.of(waitlistOrder));
        when(waitlistItemRepository.findByItemIdIn(anyList())).thenReturn(Arrays.asList(waitlistItem));

        BookingResponse response = waitlistOrderService.refundWaitlistOrderItems(1L, 1L, Arrays.asList(1L));
        
        assertEquals("FAILED", response.getStatus());
        assertEquals("候补订单项状态不允许退款", response.getMessage());
    }

    @Test
    void testCalculateTicketPrice_Adult() {
        when(ticketInventoryRepository.findByKeyWithLock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt()))
            .thenReturn(Optional.of(ticketInventory));

        // 通过 createWaitlistOrder 间接测试 calculateTicketPrice
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(trainRepository.findById(1)).thenReturn(Optional.of(train));
        when(waitlistOrderRepository.save(any(WaitlistOrder.class))).thenReturn(waitlistOrder);
        when(waitlistItemRepository.saveAll(anyList())).thenReturn(Arrays.asList(waitlistItem));

        BookingResponse response = waitlistOrderService.createWaitlistOrder(bookingRequest);
        
        assertEquals("SUCCESS", response.getStatus());
        // calculateTicketPrice 被调用两次：一次在 calculateTotalAmount 中，一次在创建 WaitlistItem 时
        verify(ticketInventoryRepository, times(2)).findByKeyWithLock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt());
    }

    @Test
    void testCalculateTicketPrice_Child() {
        ticketInventory.setPrice(new BigDecimal("1000"));
        when(ticketInventoryRepository.findByKeyWithLock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt()))
            .thenReturn(Optional.of(ticketInventory));

        bookingRequest.getPassengers().get(0).setTicketType((byte) 2); // 儿童票

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(trainRepository.findById(1)).thenReturn(Optional.of(train));
        when(waitlistOrderRepository.save(any(WaitlistOrder.class))).thenReturn(waitlistOrder);
        when(waitlistItemRepository.saveAll(anyList())).thenReturn(Arrays.asList(waitlistItem));

        BookingResponse response = waitlistOrderService.createWaitlistOrder(bookingRequest);
        
        assertEquals("SUCCESS", response.getStatus());
    }

    @Test
    void testCalculateTicketPrice_DefaultPrice() {
        when(ticketInventoryRepository.findByKeyWithLock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt()))
            .thenReturn(Optional.empty());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(trainRepository.findById(1)).thenReturn(Optional.of(train));
        when(waitlistOrderRepository.save(any(WaitlistOrder.class))).thenReturn(waitlistOrder);
        when(waitlistItemRepository.saveAll(anyList())).thenReturn(Arrays.asList(waitlistItem));

        BookingResponse response = waitlistOrderService.createWaitlistOrder(bookingRequest);
        
        assertEquals("SUCCESS", response.getStatus());
    }

    @Test
    void testCalculateExpireTime_TrainNotFound() throws Exception {
        when(trainRepository.findById(1)).thenReturn(Optional.empty());
        
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("calculateExpireTime", LocalDate.class, Integer.class);
        method.setAccessible(true);
        LocalDateTime expireTime = (LocalDateTime) method.invoke(waitlistOrderService, LocalDate.of(2025, 1, 1), 1);
        
        assertNotNull(expireTime);
        assertTrue(expireTime.isAfter(LocalDateTime.now()));
    }

    @Test
    void testGetArrivalTime_Success() throws Exception {
        TrainStop trainStop = new TrainStop();
        trainStop.setArrivalTime(LocalTime.of(10, 30));
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.of(trainStop));
        
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("getArrivalTime", Integer.class, Long.class);
        method.setAccessible(true);
        LocalTime arrivalTime = (LocalTime) method.invoke(waitlistOrderService, 1, 1L);
        
        assertEquals(LocalTime.of(10, 30), arrivalTime);
    }

    @Test
    void testGetArrivalTime_TrainStopNotFound() throws Exception {
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.empty());
        
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("getArrivalTime", Integer.class, Long.class);
        method.setAccessible(true);
        LocalTime arrivalTime = (LocalTime) method.invoke(waitlistOrderService, 1, 1L);
        
        assertNull(arrivalTime);
    }

    @Test
    void testGetArrivalTime_Exception() throws Exception {
        when(trainStopRepository.findByStopId(1L)).thenThrow(new RuntimeException("Database error"));
        
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("getArrivalTime", Integer.class, Long.class);
        method.setAccessible(true);
        LocalTime arrivalTime = (LocalTime) method.invoke(waitlistOrderService, 1, 1L);
        
        assertNull(arrivalTime);
    }

    @Test
    void testGetDepartureTime_Success() throws Exception {
        TrainStop trainStop = new TrainStop();
        trainStop.setDepartureTime(LocalTime.of(8, 0));
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.of(trainStop));
        
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("getDepartureTime", Integer.class, Long.class);
        method.setAccessible(true);
        LocalTime departureTime = (LocalTime) method.invoke(waitlistOrderService, 1, 1L);
        
        assertEquals(LocalTime.of(8, 0), departureTime);
    }

    @Test
    void testGetDepartureTime_TrainStopNotFound() throws Exception {
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.empty());
        
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("getDepartureTime", Integer.class, Long.class);
        method.setAccessible(true);
        LocalTime departureTime = (LocalTime) method.invoke(waitlistOrderService, 1, 1L);
        
        assertNull(departureTime);
    }

    @Test
    void testGetDepartureTime_Exception() throws Exception {
        when(trainStopRepository.findByStopId(1L)).thenThrow(new RuntimeException("Database error"));
        
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("getDepartureTime", Integer.class, Long.class);
        method.setAccessible(true);
        LocalTime departureTime = (LocalTime) method.invoke(waitlistOrderService, 1, 1L);
        
        assertNull(departureTime);
    }

    @Test
    void testFulfillWaitlistItem_Success() throws Exception {
        when(waitlistItemRepository.save(any(WaitlistItem.class))).thenReturn(waitlistItem);
        
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("fulfillWaitlistItem", WaitlistItem.class);
        method.setAccessible(true);
        boolean result = (Boolean) method.invoke(waitlistOrderService, waitlistItem);
        
        assertTrue(result);
        assertEquals((byte) WaitlistItemStatus.FULFILLED.getCode(), waitlistItem.getItemStatus());
        verify(waitlistItemRepository).save(waitlistItem);
    }

    @Test
    void testFulfillWaitlistItem_Exception() throws Exception {
        when(waitlistItemRepository.save(any(WaitlistItem.class))).thenThrow(new RuntimeException("Database error"));
        
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("fulfillWaitlistItem", WaitlistItem.class);
        method.setAccessible(true);
        boolean result = (Boolean) method.invoke(waitlistOrderService, waitlistItem);
        
        assertFalse(result);
    }

    @Test
    void testProcessWaitlistFulfillment_NoPendingItems() {
        when(waitlistOrderRepository.findPendingFulfillmentOrders(any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());
        
        waitlistOrderService.processWaitlistFulfillment();
        
        verify(waitlistOrderRepository).findPendingFulfillmentOrders(any(LocalDateTime.class));
        verify(waitlistItemRepository, never()).findPendingItemsByWaitlistId(anyLong());
    }

    @Test
    void testProcessWaitlistFulfillment_InventoryNotFound() {
        when(waitlistOrderRepository.findPendingFulfillmentOrders(any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(waitlistOrder));
        when(waitlistItemRepository.findPendingItemsByWaitlistId(anyLong()))
            .thenReturn(Arrays.asList(waitlistItem));
        when(ticketInventoryRepository.findByKeyWithLock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt()))
            .thenReturn(Optional.empty());
        
        waitlistOrderService.processWaitlistFulfillment();
        
        verify(waitlistOrderRepository).findPendingFulfillmentOrders(any(LocalDateTime.class));
        verify(waitlistItemRepository).findPendingItemsByWaitlistId(anyLong());
        verify(ticketInventoryRepository).findByKeyWithLock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt());
    }

    @Test
    void testProcessWaitlistFulfillment_InsufficientSeats() {
        TicketInventory inventory = new TicketInventory();
        inventory.setAvailableSeats(0);
        when(waitlistOrderRepository.findPendingFulfillmentOrders(any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(waitlistOrder));
        when(waitlistItemRepository.findPendingItemsByWaitlistId(anyLong()))
            .thenReturn(Arrays.asList(waitlistItem));
        when(ticketInventoryRepository.findByKeyWithLock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt()))
            .thenReturn(Optional.of(inventory));
        
        waitlistOrderService.processWaitlistFulfillment();
        
        verify(waitlistOrderRepository).findPendingFulfillmentOrders(any(LocalDateTime.class));
        verify(waitlistItemRepository).findPendingItemsByWaitlistId(anyLong());
        verify(ticketInventoryRepository).findByKeyWithLock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt());
    }

    @Test
    void testGetStationName_Success() throws Exception {
        TrainStop trainStop = new TrainStop();
        trainStop.setStationId(1);
        Station station = new Station();
        station.setStationName("北京站");
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.of(trainStop));
        when(stationRepository.findById(1)).thenReturn(Optional.of(station));
        
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("getStationName", Long.class);
        method.setAccessible(true);
        String stationName = (String) method.invoke(waitlistOrderService, 1L);
        
        assertEquals("北京站", stationName);
    }

    @Test
    void testGetStationName_TrainStopNotFound() throws Exception {
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.empty());
        
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("getStationName", Long.class);
        method.setAccessible(true);
        String stationName = (String) method.invoke(waitlistOrderService, 1L);
        
        assertEquals("", stationName);
    }

    @Test
    void testGetStationName_StationNotFound() throws Exception {
        TrainStop trainStop = new TrainStop();
        trainStop.setStationId(1);
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.of(trainStop));
        when(stationRepository.findById(1)).thenReturn(Optional.empty());
        
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("getStationName", Long.class);
        method.setAccessible(true);
        String stationName = (String) method.invoke(waitlistOrderService, 1L);
        
        assertEquals("", stationName);
    }

    @Test
    void testGetStationName_Exception() throws Exception {
        when(trainStopRepository.findByStopId(1L)).thenThrow(new RuntimeException("Database error"));
        
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("getStationName", Long.class);
        method.setAccessible(true);
        String stationName = (String) method.invoke(waitlistOrderService, 1L);
        
        assertEquals("", stationName);
    }

    @Test
    void testGetPassengerTypeText_Adult() throws Exception {
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("getPassengerTypeText", Byte.class);
        method.setAccessible(true);
        String result = (String) method.invoke(waitlistOrderService, (byte) 1);
        assertEquals("成人", result);
    }

    @Test
    void testGetPassengerTypeText_Child() throws Exception {
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("getPassengerTypeText", Byte.class);
        method.setAccessible(true);
        String result = (String) method.invoke(waitlistOrderService, (byte) 2);
        assertEquals("儿童", result);
    }

    @Test
    void testGetPassengerTypeText_Student() throws Exception {
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("getPassengerTypeText", Byte.class);
        method.setAccessible(true);
        String result = (String) method.invoke(waitlistOrderService, (byte) 3);
        assertEquals("学生", result);
    }

    @Test
    void testGetPassengerTypeText_Disabled() throws Exception {
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("getPassengerTypeText", Byte.class);
        method.setAccessible(true);
        String result = (String) method.invoke(waitlistOrderService, (byte) 4);
        assertEquals("残疾", result);
    }

    @Test
    void testGetPassengerTypeText_Military() throws Exception {
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("getPassengerTypeText", Byte.class);
        method.setAccessible(true);
        String result = (String) method.invoke(waitlistOrderService, (byte) 5);
        assertEquals("军人", result);
    }

    @Test
    void testGetPassengerTypeText_Unknown() throws Exception {
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("getPassengerTypeText", Byte.class);
        method.setAccessible(true);
        String result = (String) method.invoke(waitlistOrderService, (byte) 99);
        assertEquals("未知", result);
    }

    @Test
    void testGetPassengerTypeText_Null() throws Exception {
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("getPassengerTypeText", Byte.class);
        method.setAccessible(true);
        String result = (String) method.invoke(waitlistOrderService, (Byte) null);
        assertEquals("未知", result);
    }

    @Test
    void testCalculateTicketPrice_Student() throws Exception {
        when(ticketInventoryRepository.findByKeyWithLock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt()))
            .thenReturn(Optional.of(ticketInventory));
        
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("calculateTicketPrice", 
            Integer.class, Long.class, Long.class, LocalDate.class, Integer.class, Byte.class);
        method.setAccessible(true);
        BigDecimal price = (BigDecimal) method.invoke(waitlistOrderService, 1, 1L, 2L, LocalDate.of(2025, 1, 1), 1, (byte) 3);
        
        assertEquals(new BigDecimal("80.0"), price);
    }

    @Test
    void testCalculateTicketPrice_Disabled() throws Exception {
        when(ticketInventoryRepository.findByKeyWithLock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt()))
            .thenReturn(Optional.of(ticketInventory));
        
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("calculateTicketPrice", 
            Integer.class, Long.class, Long.class, LocalDate.class, Integer.class, Byte.class);
        method.setAccessible(true);
        BigDecimal price = (BigDecimal) method.invoke(waitlistOrderService, 1, 1L, 2L, LocalDate.of(2025, 1, 1), 1, (byte) 4);
        
        assertEquals(new BigDecimal("50.0"), price);
    }

    @Test
    void testCalculateTicketPrice_Military() throws Exception {
        when(ticketInventoryRepository.findByKeyWithLock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt()))
            .thenReturn(Optional.of(ticketInventory));
        
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("calculateTicketPrice", 
            Integer.class, Long.class, Long.class, LocalDate.class, Integer.class, Byte.class);
        method.setAccessible(true);
        BigDecimal price = (BigDecimal) method.invoke(waitlistOrderService, 1, 1L, 2L, LocalDate.of(2025, 1, 1), 1, (byte) 5);
        
        assertEquals(new BigDecimal("50.0"), price);
    }

    @Test
    void testCalculateTicketPrice_UnknownType() throws Exception {
        when(ticketInventoryRepository.findByKeyWithLock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt()))
            .thenReturn(Optional.of(ticketInventory));
        
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("calculateTicketPrice", 
            Integer.class, Long.class, Long.class, LocalDate.class, Integer.class, Byte.class);
        method.setAccessible(true);
        BigDecimal price = (BigDecimal) method.invoke(waitlistOrderService, 1, 1L, 2L, LocalDate.of(2025, 1, 1), 1, (byte) 99);
        
        assertEquals(new BigDecimal("100"), price);
    }

    @Test
    void testProcessWaitlistFulfillment_FulfillmentFails() {
        when(waitlistOrderRepository.findPendingFulfillmentOrders(any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(waitlistOrder));
        when(waitlistItemRepository.findPendingItemsByWaitlistId(anyLong()))
            .thenReturn(Arrays.asList(waitlistItem));
        when(ticketInventoryRepository.findByKeyWithLock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt()))
            .thenReturn(Optional.of(ticketInventory));
        when(waitlistItemRepository.save(any(WaitlistItem.class))).thenThrow(new RuntimeException("Database error"));
        
        waitlistOrderService.processWaitlistFulfillment();
        
        verify(waitlistOrderRepository).findPendingFulfillmentOrders(any(LocalDateTime.class));
        verify(waitlistItemRepository).findPendingItemsByWaitlistId(anyLong());
        verify(ticketInventoryRepository).findByKeyWithLock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt());
    }

    @Test
    void testProcessWaitlistFulfillment_Exception() {
        when(waitlistOrderRepository.findPendingFulfillmentOrders(any(LocalDateTime.class)))
            .thenThrow(new RuntimeException("Database error"));
        
        waitlistOrderService.processWaitlistFulfillment();
        
        verify(waitlistOrderRepository).findPendingFulfillmentOrders(any(LocalDateTime.class));
    }

    @Test
    void testGetWaitlistOrderDetail_Exception() {
        when(waitlistOrderRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));
        
        WaitlistOrderDetailResponse response = waitlistOrderService.getWaitlistOrderDetail(1L, 1L);
        
        assertEquals("FAILURE", response.getStatus());
        assertTrue(response.getMessage().contains("获取候补订单详情失败"));
    }

    @Test
    void testPayWaitlistOrder_Exception() {
        when(waitlistOrderRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));
        
        BookingResponse response = waitlistOrderService.payWaitlistOrder(1L, 1L);
        
        assertEquals("FAILED", response.getStatus());
        assertTrue(response.getMessage().contains("支付候补订单失败"));
    }

    @Test
    void testConvertToWaitlistOrderDetail_ItemCountNull() throws Exception {
        waitlistOrder.setItemCount(null);
        when(waitlistItemRepository.findByWaitlistId(1L)).thenReturn(Arrays.asList(waitlistItem));
        
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("convertToWaitlistOrderDetail", WaitlistOrder.class, List.class);
        method.setAccessible(true);
        WaitlistOrderDetailResponse.WaitlistOrderDetail detail = (WaitlistOrderDetailResponse.WaitlistOrderDetail) method.invoke(waitlistOrderService, waitlistOrder, Arrays.asList(waitlistItem));
        
        assertEquals(1, detail.getTicketCount());
    }

    @Test
    void testConvertToWaitlistOrderDetail_EmptyItems() throws Exception {
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("convertToWaitlistOrderDetail", WaitlistOrder.class, List.class);
        method.setAccessible(true);
        WaitlistOrderDetailResponse.WaitlistOrderDetail detail = (WaitlistOrderDetailResponse.WaitlistOrderDetail) method.invoke(waitlistOrderService, waitlistOrder, Collections.emptyList());
        
        assertEquals(waitlistOrder.getItemCount(), detail.getTicketCount());
        assertNull(detail.getTrainId());
        assertNull(detail.getTrainNumber());
    }

    @Test
    void testConvertToWaitlistOrderDetail_TrainNotFound() throws Exception {
        when(waitlistItemRepository.findByWaitlistId(1L)).thenReturn(Arrays.asList(waitlistItem));
        when(trainRepository.findById(1)).thenReturn(Optional.empty());
        
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("convertToWaitlistOrderDetail", WaitlistOrder.class, List.class);
        method.setAccessible(true);
        WaitlistOrderDetailResponse.WaitlistOrderDetail detail = (WaitlistOrderDetailResponse.WaitlistOrderDetail) method.invoke(waitlistOrderService, waitlistOrder, Arrays.asList(waitlistItem));
        
        assertNull(detail.getTrainNumber());
    }

    @Test
    void testConvertToWaitlistOrderDetail_DepartureTimeNull() throws Exception {
        when(waitlistItemRepository.findByWaitlistId(1L)).thenReturn(Arrays.asList(waitlistItem));
        when(trainRepository.findById(1)).thenReturn(Optional.of(train));
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.empty());
        when(stationRepository.findById(anyInt())).thenReturn(Optional.of(new Station()));
        when(passengerRepository.findById(anyLong())).thenReturn(Optional.of(new Passenger()));
        when(carriageTypeRepository.findById(anyInt())).thenReturn(Optional.of(new CarriageType()));
        
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("convertToWaitlistOrderDetail", WaitlistOrder.class, List.class);
        method.setAccessible(true);
        WaitlistOrderDetailResponse.WaitlistOrderDetail result = (WaitlistOrderDetailResponse.WaitlistOrderDetail) method.invoke(waitlistOrderService, waitlistOrder, Arrays.asList(waitlistItem));
        
        assertNotNull(result);
        assertNull(result.getDepartureTime());
    }

    @Test
    void testConvertToWaitlistOrderDetail_ArrivalTimeNull() throws Exception {
        when(waitlistItemRepository.findByWaitlistId(1L)).thenReturn(Arrays.asList(waitlistItem));
        when(trainRepository.findById(1)).thenReturn(Optional.of(train));
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.empty());
        when(stationRepository.findById(anyInt())).thenReturn(Optional.of(new Station()));
        when(passengerRepository.findById(anyLong())).thenReturn(Optional.of(new Passenger()));
        when(carriageTypeRepository.findById(anyInt())).thenReturn(Optional.of(new CarriageType()));
        
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("convertToWaitlistOrderDetail", WaitlistOrder.class, List.class);
        method.setAccessible(true);
        WaitlistOrderDetailResponse.WaitlistOrderDetail result = (WaitlistOrderDetailResponse.WaitlistOrderDetail) method.invoke(waitlistOrderService, waitlistOrder, Arrays.asList(waitlistItem));
        
        assertNotNull(result);
        assertNull(result.getArrivalTime());
    }

    @Test
    void testConvertToWaitlistOrderDetail_PassengerNotFound() throws Exception {
        when(waitlistItemRepository.findByWaitlistId(1L)).thenReturn(Arrays.asList(waitlistItem));
        when(trainRepository.findById(1)).thenReturn(Optional.of(train));
        when(trainStopRepository.findByStopId(anyLong())).thenReturn(Optional.of(new TrainStop()));
        when(stationRepository.findById(anyInt())).thenReturn(Optional.of(new Station()));
        when(passengerRepository.findById(100L)).thenReturn(Optional.empty());
        
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("convertToWaitlistOrderDetail", WaitlistOrder.class, List.class);
        method.setAccessible(true);
        WaitlistOrderDetailResponse.WaitlistOrderDetail detail = (WaitlistOrderDetailResponse.WaitlistOrderDetail) method.invoke(waitlistOrderService, waitlistOrder, Arrays.asList(waitlistItem));
        
        assertNotNull(detail.getItems());
        assertEquals(1, detail.getItems().size());
        assertNull(detail.getItems().get(0).getPassengerName());
    }

    @Test
    void testConvertToWaitlistOrderDetail_ItemTrainNotFound() throws Exception {
        when(waitlistItemRepository.findByWaitlistId(1L)).thenReturn(Arrays.asList(waitlistItem));
        when(trainRepository.findById(1)).thenReturn(Optional.of(train));
        when(trainStopRepository.findByStopId(anyLong())).thenReturn(Optional.of(new TrainStop()));
        when(stationRepository.findById(anyInt())).thenReturn(Optional.of(new Station()));
        when(passengerRepository.findById(100L)).thenReturn(Optional.of(new Passenger()));
        when(trainRepository.findById(1)).thenReturn(Optional.empty());
        
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("convertToWaitlistOrderDetail", WaitlistOrder.class, List.class);
        method.setAccessible(true);
        WaitlistOrderDetailResponse.WaitlistOrderDetail detail = (WaitlistOrderDetailResponse.WaitlistOrderDetail) method.invoke(waitlistOrderService, waitlistOrder, Arrays.asList(waitlistItem));
        
        assertNotNull(detail.getItems());
        assertEquals(1, detail.getItems().size());
        assertNull(detail.getItems().get(0).getTrainNumber());
    }

    @Test
    void testConvertToWaitlistOrderDetail_CarriageTypeNotFound() throws Exception {
        when(waitlistItemRepository.findByWaitlistId(1L)).thenReturn(Arrays.asList(waitlistItem));
        when(trainRepository.findById(1)).thenReturn(Optional.of(train));
        when(trainStopRepository.findByStopId(anyLong())).thenReturn(Optional.of(new TrainStop()));
        when(stationRepository.findById(anyInt())).thenReturn(Optional.of(new Station()));
        when(passengerRepository.findById(100L)).thenReturn(Optional.of(new Passenger()));
        when(carriageTypeRepository.findById(1)).thenReturn(Optional.empty());
        
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("convertToWaitlistOrderDetail", WaitlistOrder.class, List.class);
        method.setAccessible(true);
        WaitlistOrderDetailResponse.WaitlistOrderDetail detail = (WaitlistOrderDetailResponse.WaitlistOrderDetail) method.invoke(waitlistOrderService, waitlistOrder, Arrays.asList(waitlistItem));
        
        assertNotNull(detail.getItems());
        assertEquals(1, detail.getItems().size());
        assertNull(detail.getItems().get(0).getCarriageTypeName());
    }

    @Test
    void testRefundWaitlistOrder_OrderNotFound() {
        when(waitlistOrderRepository.findById(1L)).thenReturn(Optional.empty());
        
        BookingResponse response = waitlistOrderService.refundWaitlistOrder(1L, 1L);
        
        assertEquals("FAILED", response.getStatus());
        assertEquals("候补订单不存在", response.getMessage());
    }

    @Test
    void testRefundWaitlistOrder_Unauthorized() {
        WaitlistOrder otherUserOrder = new WaitlistOrder();
        otherUserOrder.setUserId(999L);
        otherUserOrder.setOrderStatus((byte) WaitlistOrderStatus.PENDING_FULFILLMENT.getCode());
        when(waitlistOrderRepository.findById(1L)).thenReturn(Optional.of(otherUserOrder));
        
        BookingResponse response = waitlistOrderService.refundWaitlistOrder(1L, 1L);
        
        assertEquals("FAILED", response.getStatus());
        assertEquals("无权操作此候补订单", response.getMessage());
    }

    @Test
    void testRefundWaitlistOrder_NoFulfilledItems() {
        waitlistOrder.setOrderStatus((byte) WaitlistOrderStatus.FULFILLED.getCode());
        when(waitlistOrderRepository.findById(1L)).thenReturn(Optional.of(waitlistOrder));
        when(waitlistItemRepository.findByWaitlistId(1L)).thenReturn(Arrays.asList(waitlistItem));
        
        BookingResponse response = waitlistOrderService.refundWaitlistOrder(1L, 1L);
        
        assertEquals("FAILED", response.getStatus());
        assertEquals("未找到已兑现的候补订单项", response.getMessage());
    }

    @Test
    void testRefundWaitlistOrder_FormalOrderNotFound() {
        waitlistOrder.setOrderStatus((byte) WaitlistOrderStatus.FULFILLED.getCode());
        waitlistItem.setItemStatus((byte) WaitlistItemStatus.FULFILLED.getCode());
        when(waitlistOrderRepository.findById(1L)).thenReturn(Optional.of(waitlistOrder));
        when(waitlistItemRepository.findByWaitlistId(1L)).thenReturn(Arrays.asList(waitlistItem));
        when(orderRepository.findByOrderNumber(anyString())).thenReturn(Optional.empty());
        
        BookingResponse response = waitlistOrderService.refundWaitlistOrder(1L, 1L);
        
        assertEquals("FAILED", response.getStatus());
        assertEquals("未找到对应的正式订单", response.getMessage());
    }

    @Test
    void testRefundWaitlistOrder_Exception() {
        when(waitlistOrderRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));
        
        BookingResponse response = waitlistOrderService.refundWaitlistOrder(1L, 1L);
        
        assertEquals("FAILED", response.getStatus());
        assertTrue(response.getMessage().contains("退款候补订单失败"));
    }

    @Test
    void testCancelWaitlistOrder_Unauthorized() {
        WaitlistOrder otherUserOrder = new WaitlistOrder();
        otherUserOrder.setUserId(999L);
        when(waitlistOrderRepository.findById(1L)).thenReturn(Optional.of(otherUserOrder));
        
        BookingResponse response = waitlistOrderService.cancelWaitlistOrder(1L, 1L);
        
        assertEquals("FAILED", response.getStatus());
        assertEquals("无权操作此候补订单", response.getMessage());
    }

    @Test
    void testCancelWaitlistOrder_Exception() {
        when(waitlistOrderRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));
        
        BookingResponse response = waitlistOrderService.cancelWaitlistOrder(1L, 1L);
        
        assertEquals("FAILED", response.getStatus());
        assertTrue(response.getMessage().contains("取消候补订单失败"));
    }

    @Test
    void testConvertToWaitlistOrderInfo_EmptyItems() throws Exception {
        when(waitlistItemRepository.findByWaitlistId(1L)).thenReturn(Collections.emptyList());
        
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("convertToWaitlistOrderInfo", List.class);
        method.setAccessible(true);
        List<WaitlistOrderResponse.WaitlistOrderInfo> result = (List<WaitlistOrderResponse.WaitlistOrderInfo>) method.invoke(waitlistOrderService, Arrays.asList(waitlistOrder));
        
        assertEquals(0, result.size());
    }

    @Test
    void testConvertToWaitlistOrderInfo_ItemCountNull() throws Exception {
        waitlistOrder.setItemCount(null);
        when(waitlistItemRepository.findByWaitlistId(1L)).thenReturn(Arrays.asList(waitlistItem));
        when(trainRepository.findById(1)).thenReturn(Optional.of(train));
        when(trainStopRepository.findByStopId(anyLong())).thenReturn(Optional.of(new TrainStop()));
        when(stationRepository.findById(anyInt())).thenReturn(Optional.of(new Station()));
        
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("convertToWaitlistOrderInfo", List.class);
        method.setAccessible(true);
        List<WaitlistOrderResponse.WaitlistOrderInfo> result = (List<WaitlistOrderResponse.WaitlistOrderInfo>) method.invoke(waitlistOrderService, Arrays.asList(waitlistOrder));
        
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getTicketCount());
    }

    @Test
    void testConvertToWaitlistOrderInfo_TrainNotFound() throws Exception {
        when(waitlistItemRepository.findByWaitlistId(1L)).thenReturn(Arrays.asList(waitlistItem));
        when(trainRepository.findById(1)).thenReturn(Optional.empty());
        when(trainStopRepository.findByStopId(anyLong())).thenReturn(Optional.of(new TrainStop()));
        when(stationRepository.findById(anyInt())).thenReturn(Optional.of(new Station()));
        
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("convertToWaitlistOrderInfo", List.class);
        method.setAccessible(true);
        List<WaitlistOrderResponse.WaitlistOrderInfo> result = (List<WaitlistOrderResponse.WaitlistOrderInfo>) method.invoke(waitlistOrderService, Arrays.asList(waitlistOrder));
        
        assertEquals(1, result.size());
        assertNull(result.get(0).getTrainNumber());
    }

    @Test
    void testConvertToWaitlistOrderInfo_DepartureTimeNull() throws Exception {
        when(waitlistItemRepository.findByWaitlistId(1L)).thenReturn(Arrays.asList(waitlistItem));
        when(trainRepository.findById(1)).thenReturn(Optional.of(train));
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.empty());
        when(stationRepository.findById(anyInt())).thenReturn(Optional.of(new Station()));
        
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("convertToWaitlistOrderInfo", List.class);
        method.setAccessible(true);
        List<WaitlistOrderResponse.WaitlistOrderInfo> result = (List<WaitlistOrderResponse.WaitlistOrderInfo>) method.invoke(waitlistOrderService, Arrays.asList(waitlistOrder));
        
        assertEquals(1, result.size());
        assertNull(result.get(0).getDepartureTime());
    }

    @Test
    void testConvertToWaitlistOrderInfo_ArrivalTimeNull() throws Exception {
        when(waitlistItemRepository.findByWaitlistId(1L)).thenReturn(Arrays.asList(waitlistItem));
        when(trainRepository.findById(1)).thenReturn(Optional.of(train));
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.empty());
        when(stationRepository.findById(anyInt())).thenReturn(Optional.of(new Station()));
        
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("convertToWaitlistOrderInfo", List.class);
        method.setAccessible(true);
        List<WaitlistOrderResponse.WaitlistOrderInfo> result = (List<WaitlistOrderResponse.WaitlistOrderInfo>) method.invoke(waitlistOrderService, Arrays.asList(waitlistOrder));
        
        assertEquals(1, result.size());
        assertNull(result.get(0).getArrivalTime());
    }

    @Test
    void testRefundWaitlistOrder_WithSeats() {
        waitlistOrder.setOrderStatus((byte) WaitlistOrderStatus.FULFILLED.getCode());
        waitlistItem.setItemStatus((byte) WaitlistItemStatus.FULFILLED.getCode());
        
        Order formalOrder = new Order();
        formalOrder.setOrderId(1L);
        formalOrder.setOrderNumber("WL20250110001");
        
        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setPassengerId(100L);
        ticket.setTrainId(1);
        ticket.setDepartureStopId(1L);
        ticket.setArrivalStopId(2L);
        ticket.setTravelDate(LocalDate.of(2025, 1, 1));
        ticket.setSeatNumber("01A");
        ticket.setCarriageNumber("1");
        ticket.setCarriageTypeId(1);
        ticket.setPrice(new BigDecimal("100"));
        
        when(waitlistOrderRepository.findById(1L)).thenReturn(Optional.of(waitlistOrder));
        when(waitlistItemRepository.findByWaitlistId(1L)).thenReturn(Arrays.asList(waitlistItem));
        when(orderRepository.findByOrderNumber(anyString())).thenReturn(Optional.of(formalOrder));
        when(ticketRepository.findByOrderId(1L)).thenReturn(Arrays.asList(ticket));
        
        BookingResponse response = waitlistOrderService.refundWaitlistOrder(1L, 1L);
        
        assertEquals("SUCCESS", response.getStatus());
        verify(ticketRepository).save(ticket);
        verify(redisService).incrStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt(), eq(1));
    }

    @Test
    void testRefundWaitlistOrderItems_WrongOrderStatus() {
        waitlistOrder.setOrderStatus((byte) WaitlistOrderStatus.CANCELLED.getCode());
        when(waitlistOrderRepository.findById(1L)).thenReturn(Optional.of(waitlistOrder));
        
        BookingResponse response = waitlistOrderService.refundWaitlistOrderItems(1L, 1L, Arrays.asList(1L));
        
        assertEquals("FAILED", response.getStatus());
        assertEquals("候补订单状态不允许退款", response.getMessage());
    }

    @Test
    void testRefundWaitlistOrderItems_ItemNotBelongToOrder() {
        waitlistOrder.setOrderStatus((byte) WaitlistOrderStatus.PENDING_FULFILLMENT.getCode());
        WaitlistItem otherOrderItem = new WaitlistItem();
        otherOrderItem.setWaitlistId(999L);
        otherOrderItem.setItemStatus((byte) WaitlistItemStatus.PENDING_FULFILLMENT.getCode());
        
        when(waitlistOrderRepository.findById(1L)).thenReturn(Optional.of(waitlistOrder));
        when(waitlistItemRepository.findByItemIdIn(Arrays.asList(1L))).thenReturn(Arrays.asList(otherOrderItem));
        
        BookingResponse response = waitlistOrderService.refundWaitlistOrderItems(1L, 1L, Arrays.asList(1L));
        
        assertEquals("FAILED", response.getStatus());
        assertEquals("候补订单项不属于此候补订单", response.getMessage());
    }

    @Test
    void testRefundWaitlistOrderItems_AllItemsCancelled() {
        waitlistOrder.setOrderStatus((byte) WaitlistOrderStatus.PENDING_FULFILLMENT.getCode());
        waitlistItem.setItemStatus((byte) WaitlistItemStatus.PENDING_FULFILLMENT.getCode());
        
        when(waitlistOrderRepository.findById(1L)).thenReturn(Optional.of(waitlistOrder));
        when(waitlistItemRepository.findByItemIdIn(Arrays.asList(1L))).thenReturn(Arrays.asList(waitlistItem));
        when(waitlistItemRepository.findByWaitlistId(1L)).thenReturn(Arrays.asList(waitlistItem));
        
        BookingResponse response = waitlistOrderService.refundWaitlistOrderItems(1L, 1L, Arrays.asList(1L));
        
        assertEquals("SUCCESS", response.getStatus());
        verify(waitlistOrderRepository).save(waitlistOrder);
        assertEquals((byte) 3, waitlistOrder.getOrderStatus()); // 已取消
        assertEquals(0, waitlistOrder.getItemCount());
    }

    @Test
    void testRefundWaitlistOrderItems_Exception() {
        when(waitlistOrderRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));
        
        BookingResponse response = waitlistOrderService.refundWaitlistOrderItems(1L, 1L, Arrays.asList(1L));
        
        assertEquals("FAILED", response.getStatus());
        assertTrue(response.getMessage().contains("退款候补订单项失败"));
    }

    @Test
    void testRefundWaitlistOrderItems_NotAllItemsCancelled() {
        waitlistOrder.setOrderStatus((byte) WaitlistOrderStatus.PENDING_FULFILLMENT.getCode());
        waitlistOrder.setItemCount(2); // 设置初始数量为2
        waitlistItem.setItemStatus((byte) WaitlistItemStatus.PENDING_FULFILLMENT.getCode());
        
        // Create another item that will remain active
        WaitlistItem remainingItem = new WaitlistItem();
        remainingItem.setItemId(2L);
        remainingItem.setWaitlistId(1L);
        remainingItem.setItemStatus((byte) WaitlistItemStatus.PENDING_FULFILLMENT.getCode());
        remainingItem.setPrice(new BigDecimal("50"));
        
        when(waitlistOrderRepository.findById(1L)).thenReturn(Optional.of(waitlistOrder));
        when(waitlistItemRepository.findByItemIdIn(Arrays.asList(1L))).thenReturn(Arrays.asList(waitlistItem));
        when(waitlistItemRepository.findByWaitlistId(1L)).thenReturn(Arrays.asList(waitlistItem, remainingItem));
        
        BookingResponse response = waitlistOrderService.refundWaitlistOrderItems(1L, 1L, Arrays.asList(1L));
        
        assertEquals("SUCCESS", response.getStatus());
        verify(waitlistOrderRepository).save(waitlistOrder);
        // Order status should remain PENDING_FULFILLMENT since not all items are cancelled
        assertEquals((byte) WaitlistOrderStatus.PENDING_FULFILLMENT.getCode(), waitlistOrder.getOrderStatus());
        assertEquals(1, waitlistOrder.getItemCount()); // Only one item refunded
    }

    @Test
    void testConvertToWaitlistOrderInfo_DepartureTimeNotNull() throws Exception {
        when(waitlistItemRepository.findByWaitlistId(1L)).thenReturn(Arrays.asList(waitlistItem));
        when(trainRepository.findById(1)).thenReturn(Optional.of(train));
        
        TrainStop departureStop = new TrainStop();
        departureStop.setStopId(1L);
        departureStop.setDepartureTime(LocalTime.of(8, 30));
        departureStop.setStationId(1);
        
        TrainStop arrivalStop = new TrainStop();
        arrivalStop.setStopId(2L);
        arrivalStop.setArrivalTime(LocalTime.of(10, 30));
        arrivalStop.setStationId(2);
        
        Station station = new Station();
        station.setStationId(1);
        station.setStationName("北京站");
        
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findByStopId(2L)).thenReturn(Optional.of(arrivalStop));
        when(stationRepository.findById(1)).thenReturn(Optional.of(station));
        when(stationRepository.findById(2)).thenReturn(Optional.of(station));
        
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("convertToWaitlistOrderInfo", List.class);
        method.setAccessible(true);
        List<WaitlistOrderResponse.WaitlistOrderInfo> result = (List<WaitlistOrderResponse.WaitlistOrderInfo>) method.invoke(waitlistOrderService, Arrays.asList(waitlistOrder));
        
        assertEquals(1, result.size());
        assertEquals("08:30", result.get(0).getDepartureTime());
        assertEquals("10:30", result.get(0).getArrivalTime());
    }

    @Test
    void testConvertToWaitlistOrderDetail_DepartureTimeNotNull() throws Exception {
        when(waitlistItemRepository.findByWaitlistId(1L)).thenReturn(Arrays.asList(waitlistItem));
        when(trainRepository.findById(1)).thenReturn(Optional.of(train));
        when(passengerRepository.findById(100L)).thenReturn(Optional.of(new Passenger()));
        when(carriageTypeRepository.findById(1)).thenReturn(Optional.of(new CarriageType()));
        
        TrainStop departureStop = new TrainStop();
        departureStop.setStopId(1L);
        departureStop.setDepartureTime(LocalTime.of(8, 30));
        departureStop.setStationId(1);
        
        TrainStop arrivalStop = new TrainStop();
        arrivalStop.setStopId(2L);
        arrivalStop.setArrivalTime(LocalTime.of(10, 30));
        arrivalStop.setStationId(2);
        
        Station station = new Station();
        station.setStationId(1);
        station.setStationName("北京站");
        
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findByStopId(2L)).thenReturn(Optional.of(arrivalStop));
        when(stationRepository.findById(1)).thenReturn(Optional.of(station));
        when(stationRepository.findById(2)).thenReturn(Optional.of(station));
        
        java.lang.reflect.Method method = WaitlistOrderServiceImpl.class.getDeclaredMethod("convertToWaitlistOrderDetail", WaitlistOrder.class, List.class);
        method.setAccessible(true);
        WaitlistOrderDetailResponse.WaitlistOrderDetail result = (WaitlistOrderDetailResponse.WaitlistOrderDetail) method.invoke(waitlistOrderService, waitlistOrder, Arrays.asList(waitlistItem));
        
        assertEquals("08:30", result.getDepartureTime());
        assertEquals("10:30", result.getArrivalTime());
    }

} 

