package com.example.techprototype.Controller;

import com.example.techprototype.DTO.*;
import com.example.techprototype.Service.WaitlistOrderService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class WaitlistOrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private WaitlistOrderService waitlistOrderService;

    @InjectMocks
    private WaitlistOrderController waitlistOrderController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(waitlistOrderController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testCreateWaitlistOrder_Success() throws Exception {
        BookingRequest request = new BookingRequest();
        request.setUserId(1L);
        request.setTrainId(100);
        request.setDepartureStopId(497L);
        request.setArrivalStopId(500L);
        request.setTravelDate(LocalDate.of(2025, 7, 20));
        request.setCarriageTypeId(5);

        BookingResponse successResponse = BookingResponse.successWithMessage("候补订单创建成功", "WL123", null, null, LocalDateTime.now());

        when(waitlistOrderService.createWaitlistOrder(any(BookingRequest.class))).thenReturn(successResponse);

        mockMvc.perform(post("/api/waitlist/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("候补订单创建成功"));
    }

    @Test
    void testCreateWaitlistOrder_Failure() throws Exception {
        BookingRequest request = new BookingRequest();
        request.setUserId(1L);
        request.setTrainId(100);
        request.setDepartureStopId(497L);
        request.setArrivalStopId(500L);
        request.setTravelDate(LocalDate.of(2025, 7, 20));
        request.setCarriageTypeId(5);

        BookingResponse failureResponse = BookingResponse.failure("候补订单创建失败");

        when(waitlistOrderService.createWaitlistOrder(any(BookingRequest.class))).thenReturn(failureResponse);

        mockMvc.perform(post("/api/waitlist/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.message").value("候补订单创建失败"));
    }

    @Test
    void testGetMyWaitlistOrders_Success() throws Exception {
        WaitlistOrderResponse.WaitlistOrderInfo orderInfo = new WaitlistOrderResponse.WaitlistOrderInfo();
        orderInfo.setWaitlistId(1L);
        orderInfo.setOrderNumber("WL123");
        orderInfo.setTrainNumber("G101");
        orderInfo.setDepartureStationName("北京");
        orderInfo.setArrivalStationName("上海");
        orderInfo.setDepartureDate(LocalDateTime.of(2025, 7, 20, 9, 0));
        orderInfo.setDepartureTime("09:00");
        orderInfo.setArrivalTime("14:30");
        orderInfo.setTotalAmount(BigDecimal.valueOf(553.5));
        orderInfo.setOrderStatus((byte) 1);
        orderInfo.setOrderStatusText("已支付");
        orderInfo.setOrderTime(LocalDateTime.now());
        orderInfo.setExpireTime(LocalDateTime.now().plusHours(1));
        orderInfo.setTicketCount(2);

        WaitlistOrderResponse successResponse = WaitlistOrderResponse.success(Arrays.asList(orderInfo));

        when(waitlistOrderService.getMyWaitlistOrders(anyLong())).thenReturn(successResponse);

        mockMvc.perform(get("/api/waitlist/my")
                .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.waitlistOrders").isArray())
                .andExpect(jsonPath("$.waitlistOrders[0].orderNumber").value("WL123"));
    }

    @Test
    void testGetMyWaitlistOrders_Failure() throws Exception {
        WaitlistOrderResponse failureResponse = WaitlistOrderResponse.failure("获取失败");

        when(waitlistOrderService.getMyWaitlistOrders(anyLong())).thenReturn(failureResponse);

        mockMvc.perform(get("/api/waitlist/my")
                .param("userId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILURE"));
    }

    @Test
    void testGetWaitlistOrderDetail_Success() throws Exception {
        WaitlistOrderDetailResponse.WaitlistOrderDetail detail = new WaitlistOrderDetailResponse.WaitlistOrderDetail();
        detail.setWaitlistId(1L);
        detail.setOrderNumber("WL123");
        detail.setTrainNumber("G101");
        detail.setDepartureStation("北京");
        detail.setArrivalStation("上海");
        detail.setTravelDate(LocalDate.of(2025, 7, 20));
        detail.setDepartureTime("09:00");
        detail.setArrivalTime("14:30");
        detail.setTotalAmount(BigDecimal.valueOf(553.5));
        detail.setOrderStatus((byte) 1);
        detail.setOrderStatusText("已支付");
        detail.setOrderTime(LocalDateTime.now());
        detail.setExpireTime(LocalDateTime.now().plusHours(1));
        detail.setTicketCount(2);
        detail.setItems(Collections.emptyList());

        WaitlistOrderDetailResponse successResponse = WaitlistOrderDetailResponse.success(detail);

        when(waitlistOrderService.getWaitlistOrderDetail(anyLong(), anyLong())).thenReturn(successResponse);

        mockMvc.perform(get("/api/waitlist/detail")
                .param("waitlistId", "1")
                .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.waitlistOrder.orderNumber").value("WL123"));
    }

    @Test
    void testGetWaitlistOrderDetail_Failure() throws Exception {
        WaitlistOrderDetailResponse failureResponse = WaitlistOrderDetailResponse.failure("获取失败");

        when(waitlistOrderService.getWaitlistOrderDetail(anyLong(), anyLong())).thenReturn(failureResponse);

        mockMvc.perform(get("/api/waitlist/detail")
                .param("waitlistId", "1")
                .param("userId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILURE"));
    }

    @Test
    void testPayWaitlistOrder_Success() throws Exception {
        BookingResponse successResponse = BookingResponse.successWithMessage("候补订单支付成功", "WL123", null, null, LocalDateTime.now());

        when(waitlistOrderService.payWaitlistOrder(anyLong(), anyLong())).thenReturn(successResponse);

        mockMvc.perform(post("/api/waitlist/pay")
                .param("waitlistId", "1")
                .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("候补订单支付成功"));
    }

    @Test
    void testPayWaitlistOrder_Failure() throws Exception {
        BookingResponse failureResponse = BookingResponse.failure("候补订单支付失败");

        when(waitlistOrderService.payWaitlistOrder(anyLong(), anyLong())).thenReturn(failureResponse);

        mockMvc.perform(post("/api/waitlist/pay")
                .param("waitlistId", "1")
                .param("userId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.message").value("候补订单支付失败"));
    }

    @Test
    void testCancelWaitlistOrder_Success() throws Exception {
        BookingResponse successResponse = BookingResponse.successWithMessage("候补订单取消成功", "WL123", null, null, LocalDateTime.now());

        when(waitlistOrderService.cancelWaitlistOrder(anyLong(), anyLong())).thenReturn(successResponse);

        mockMvc.perform(post("/api/waitlist/cancel")
                .param("waitlistId", "1")
                .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("候补订单取消成功"));
    }

    @Test
    void testCancelWaitlistOrder_Failure() throws Exception {
        BookingResponse failureResponse = BookingResponse.failure("候补订单取消失败");

        when(waitlistOrderService.cancelWaitlistOrder(anyLong(), anyLong())).thenReturn(failureResponse);

        mockMvc.perform(post("/api/waitlist/cancel")
                .param("waitlistId", "1")
                .param("userId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.message").value("候补订单取消失败"));
    }

    @Test
    void testRefundWaitlistOrder_Success() throws Exception {
        BookingResponse successResponse = BookingResponse.successWithMessage("候补订单退款成功", "WL123", null, null, LocalDateTime.now());

        when(waitlistOrderService.refundWaitlistOrder(anyLong(), anyLong())).thenReturn(successResponse);

        mockMvc.perform(post("/api/waitlist/refund")
                .param("waitlistId", "1")
                .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("候补订单退款成功"));
    }

    @Test
    void testRefundWaitlistOrder_Failure() throws Exception {
        BookingResponse failureResponse = BookingResponse.failure("候补订单退款失败");

        when(waitlistOrderService.refundWaitlistOrder(anyLong(), anyLong())).thenReturn(failureResponse);

        mockMvc.perform(post("/api/waitlist/refund")
                .param("waitlistId", "1")
                .param("userId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.message").value("候补订单退款失败"));
    }

    @Test
    void testRefundWaitlistOrderItems_Success() throws Exception {
        List<Long> itemIds = Arrays.asList(1L, 2L);
        BookingResponse successResponse = BookingResponse.successWithMessage("候补订单项退款成功", "WL123", null, null, LocalDateTime.now());

        when(waitlistOrderService.refundWaitlistOrderItems(anyLong(), anyLong(), anyList())).thenReturn(successResponse);

        mockMvc.perform(post("/api/waitlist/refund-items")
                .param("waitlistId", "1")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(itemIds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("候补订单项退款成功"));
    }

    @Test
    void testRefundWaitlistOrderItems_Failure() throws Exception {
        List<Long> itemIds = Arrays.asList(1L, 2L);
        BookingResponse failureResponse = BookingResponse.failure("候补订单项退款失败");

        when(waitlistOrderService.refundWaitlistOrderItems(anyLong(), anyLong(), anyList())).thenReturn(failureResponse);

        mockMvc.perform(post("/api/waitlist/refund-items")
                .param("waitlistId", "1")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(itemIds)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.message").value("候补订单项退款失败"));
    }
} 