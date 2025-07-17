package com.example.techprototype.Controller;

import com.example.techprototype.DTO.*;
import com.example.techprototype.Entity.Order;
import com.example.techprototype.Repository.OrderRepository;
import com.example.techprototype.Service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class OrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderService orderService;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderController orderController;

    private ObjectMapper objectMapper;

    @ControllerAdvice
    public static class TestExceptionHandler {
        @ExceptionHandler(RuntimeException.class)
        @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        @ResponseBody
        public Map<String, Object> handleRuntimeException(RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "ERROR");
            response.put("message", e.getMessage());
            return response;
        }
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(orderController)
                .setControllerAdvice(new TestExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testGetMyOrders_Success() throws Exception {
        MyOrderResponse.MyOrderInfo orderInfo = new MyOrderResponse.MyOrderInfo();
        orderInfo.setOrderId(1L);
        orderInfo.setOrderNumber("ORDER123");
        orderInfo.setTrainNumber("G101");
        orderInfo.setDepartureStationName("北京");
        orderInfo.setArrivalStationName("上海");
        orderInfo.setDepartureDate(LocalDate.of(2025, 7, 20));
        orderInfo.setDepartureTime(LocalTime.of(9, 0));
        orderInfo.setArrivalTime(LocalTime.of(14, 30));
        orderInfo.setTotalAmount(BigDecimal.valueOf(553.5));
        orderInfo.setOrderStatus((byte) 1);
        orderInfo.setOrderStatusText("已支付");
        orderInfo.setOrderTime(LocalDateTime.now());
        orderInfo.setTicketCount(2);

        MyOrderResponse successResponse = MyOrderResponse.success(Arrays.asList(orderInfo));

        when(orderService.getMyOrders(anyLong())).thenReturn(successResponse);

        mockMvc.perform(get("/api/orders/my")
                .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.orders").isArray())
                .andExpect(jsonPath("$.orders[0].orderNumber").value("ORDER123"));
    }

    @Test
    void testGetMyOrders_Failure() throws Exception {
        MyOrderResponse failureResponse = MyOrderResponse.failure("获取失败");

        when(orderService.getMyOrders(anyLong())).thenReturn(failureResponse);

        mockMvc.perform(get("/api/orders/my")
                .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILURE"));
    }

    @Test
    void testGetMyOrdersByConditions_Success() throws Exception {
        MyOrderResponse.MyOrderInfo orderInfo = new MyOrderResponse.MyOrderInfo();
        orderInfo.setOrderId(1L);
        orderInfo.setOrderNumber("ORDER123");
        orderInfo.setTrainNumber("G101");
        orderInfo.setDepartureStationName("北京");
        orderInfo.setArrivalStationName("上海");
        orderInfo.setDepartureDate(LocalDate.of(2025, 7, 20));
        orderInfo.setDepartureTime(LocalTime.of(9, 0));
        orderInfo.setArrivalTime(LocalTime.of(14, 30));
        orderInfo.setTotalAmount(BigDecimal.valueOf(553.5));
        orderInfo.setOrderStatus((byte) 1);
        orderInfo.setOrderStatusText("已支付");
        orderInfo.setOrderTime(LocalDateTime.now());
        orderInfo.setTicketCount(2);

        MyOrderResponse successResponse = MyOrderResponse.success(Arrays.asList(orderInfo));

        when(orderService.getMyOrdersByConditions(eq(1L), isNull(), isNull(), isNull(), eq((byte) 1), isNull()))
                .thenReturn(successResponse);

        mockMvc.perform(get("/api/orders/my/filter")
                .param("userId", "1")
                .param("orderStatus", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.orders").isArray())
                .andExpect(jsonPath("$.orders[0].orderNumber").value("ORDER123"));
    }

    @Test
    void testGetMyOrdersByConditions_Failure() throws Exception {
        MyOrderResponse failureResponse = MyOrderResponse.failure("获取失败");

        when(orderService.getMyOrdersByConditions(eq(1L), isNull(), isNull(), isNull(), eq((byte) 1), isNull()))
                .thenReturn(failureResponse);

        mockMvc.perform(get("/api/orders/my/filter")
                .param("userId", "1")
                .param("orderStatus", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILURE"));
    }

    @Test
    void testGetOrderDetail_Success() throws Exception {
        OrderDetailResponse detailResponse = new OrderDetailResponse();
        detailResponse.setOrderId(1L);
        detailResponse.setOrderNumber("ORDER123");
        detailResponse.setTrainNumber("G101");
        detailResponse.setDepartureStation("北京");
        detailResponse.setArrivalStation("上海");
        detailResponse.setTravelDate(LocalDate.of(2025, 7, 20));
        detailResponse.setTotalAmount(BigDecimal.valueOf(553.5));

        when(orderService.getOrderDetail(anyLong(), anyLong())).thenReturn(detailResponse);

        mockMvc.perform(get("/api/orders/detail")
                .param("orderId", "1")
                .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("ORDER123"));
    }

    @Test
    void testGetOrderDetail_Failure() throws Exception {
        when(orderService.getOrderDetail(anyLong(), anyLong()))
                .thenThrow(new RuntimeException("订单不存在"));

        mockMvc.perform(get("/api/orders/detail")
                .param("orderId", "1")
                .param("userId", "1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("订单不存在"));
    }

    @Test
    void testCancelOrder_Success() throws Exception {
        CancelOrderRequest request = new CancelOrderRequest();
        request.setUserId(1L);
        request.setOrderId(1L);

        BookingResponse successResponse = BookingResponse.successWithMessage("订单取消成功", null, null, null, LocalDateTime.now());

        when(orderService.cancelOrder(any(CancelOrderRequest.class))).thenReturn(successResponse);

        mockMvc.perform(post("/api/orders/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("订单取消成功"));
    }

    @Test
    void testCancelOrder_Failure() throws Exception {
        CancelOrderRequest request = new CancelOrderRequest();
        request.setUserId(1L);
        request.setOrderId(1L);

        BookingResponse failureResponse = BookingResponse.failure("订单取消失败");

        when(orderService.cancelOrder(any(CancelOrderRequest.class))).thenReturn(failureResponse);

        mockMvc.perform(post("/api/orders/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.message").value("订单取消失败"));
    }

    @Test
    void testCreateOrder_Success() throws Exception {
        BookingRequest request = new BookingRequest();
        request.setUserId(1L);
        request.setTrainId(100);
        request.setDepartureStopId(497L);
        request.setArrivalStopId(500L);
        request.setTravelDate(LocalDate.of(2025, 7, 20));
        request.setCarriageTypeId(5);

        BookingResponse successResponse = BookingResponse.successWithMessage("订单创建成功", "ORDER123", null, null, LocalDateTime.now());

        when(orderService.createOrder(any(BookingRequest.class))).thenReturn(successResponse);

        mockMvc.perform(post("/api/orders/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("订单创建成功"));
    }

    @Test
    void testGetRefundPreparation_Success() throws Exception {
        RefundPreparationRequest request = new RefundPreparationRequest();
        request.setUserId(1L);
        request.setOrderId(1L);

        RefundPreparationResponse successResponse = new RefundPreparationResponse();
        successResponse.setOrderNumber("ORDER123");
        successResponse.setTrainNumber("G101");
        successResponse.setDepartureStation("北京");
        successResponse.setArrivalStation("上海");
        successResponse.setTotalAmount(BigDecimal.valueOf(553.5));

        when(orderService.getRefundPreparation(any(RefundPreparationRequest.class))).thenReturn(successResponse);

        mockMvc.perform(post("/api/orders/refund/preparation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("ORDER123"));
    }

    @Test
    void testGetOrderIdByOrderNumber_Success() throws Exception {
        Order order = new Order();
        order.setOrderId(1L);
        order.setOrderNumber("ORDER123");

        when(orderRepository.findByOrderNumber("ORDER123")).thenReturn(Optional.of(order));

        mockMvc.perform(get("/api/orders/order-id")
                .param("orderNumber", "ORDER123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.orderNumber").value("ORDER123"));
    }

    @Test
    void testGetOrderIdByOrderNumber_NotFound() throws Exception {
        when(orderRepository.findByOrderNumber("ORDER999")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/orders/order-id")
                .param("orderNumber", "ORDER999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("订单不存在"));
    }

    @Test
    void testGetOrderIdByOrderNumber_Exception() throws Exception {
        when(orderRepository.findByOrderNumber("ORDER123")).thenThrow(new RuntimeException("数据库错误"));

        mockMvc.perform(get("/api/orders/order-id")
                .param("orderNumber", "ORDER123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("查询失败: 数据库错误"));
    }

    @Test
    void testCheckOrderTimeout_Success_NotTimeout() throws Exception {
        Order order = new Order();
        order.setOrderId(1L);
        order.setUserId(1L);
        order.setOrderStatus((byte) 0); // 待支付状态
        order.setOrderTime(LocalDateTime.now().minusMinutes(10)); // 10分钟前创建，未超时

        when(orderRepository.findByOrderIdAndUserId(1L, 1L)).thenReturn(Optional.of(order));

        mockMvc.perform(get("/api/orders/check-timeout")
                .param("orderId", "1")
                .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("订单未超时"));
    }

    @Test
    void testCheckOrderTimeout_Success_Timeout() throws Exception {
        Order order = new Order();
        order.setOrderId(1L);
        order.setUserId(1L);
        order.setOrderStatus((byte) 0); // 待支付状态
        order.setOrderTime(LocalDateTime.now().minusMinutes(20)); // 20分钟前创建，已超时

        when(orderRepository.findByOrderIdAndUserId(1L, 1L)).thenReturn(Optional.of(order));

        mockMvc.perform(get("/api/orders/check-timeout")
                .param("orderId", "1")
                .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILURE"))
                .andExpect(jsonPath("$.message").value("订单已超时"));
    }

    @Test
    void testCheckOrderTimeout_OrderNotFound() throws Exception {
        when(orderRepository.findByOrderIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/orders/check-timeout")
                .param("orderId", "1")
                .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILURE"))
                .andExpect(jsonPath("$.message").value("订单不存在"));
    }

    @Test
    void testCheckOrderTimeout_WrongStatus() throws Exception {
        Order order = new Order();
        order.setOrderId(1L);
        order.setUserId(1L);
        order.setOrderStatus((byte) 1); // 已支付状态，不是待支付

        when(orderRepository.findByOrderIdAndUserId(1L, 1L)).thenReturn(Optional.of(order));

        mockMvc.perform(get("/api/orders/check-timeout")
                .param("orderId", "1")
                .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILURE"))
                .andExpect(jsonPath("$.message").value("订单状态不正确"));
    }

    @Test
    void testCheckOrderTimeout_Exception() throws Exception {
        when(orderRepository.findByOrderIdAndUserId(1L, 1L)).thenThrow(new RuntimeException("数据库错误"));

        mockMvc.perform(get("/api/orders/check-timeout")
                .param("orderId", "1")
                .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILURE"))
                .andExpect(jsonPath("$.message").value("检查订单超时失败: 数据库错误"));
    }
} 