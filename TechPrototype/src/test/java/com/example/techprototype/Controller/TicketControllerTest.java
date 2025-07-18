package com.example.techprototype.Controller;

import com.example.techprototype.DTO.*;
import com.example.techprototype.Service.TicketService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class TicketControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TicketService ticketService;

    @InjectMocks
    private TicketController ticketController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(ticketController).build();
        objectMapper = new ObjectMapper();
        // 配置ObjectMapper支持Java 8时间类型
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testBookTickets_Success() throws Exception {
        // 准备测试数据
        BookingRequest request = new BookingRequest();
        request.setUserId(1L);
        request.setTrainId(100);
        request.setDepartureStopId(497L);
        request.setArrivalStopId(500L);
        request.setTravelDate(LocalDate.of(2025, 7, 20));
        request.setCarriageTypeId(5);
        
        BookingRequest.PassengerInfo passengerInfo = new BookingRequest.PassengerInfo();
        passengerInfo.setPassengerId(1L);
        passengerInfo.setTicketType((byte) 1);
        passengerInfo.setCarriageTypeId(5);
        request.setPassengers(Arrays.asList(passengerInfo));

        BookingResponse successResponse = BookingResponse.successWithMessage(
            "购票成功", "ORDER123", null, null, LocalDateTime.now());

        when(ticketService.bookTickets(any(BookingRequest.class))).thenReturn(successResponse);

        // 执行测试
        mockMvc.perform(post("/api/ticket/book")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("购票成功"));
    }

    @Test
    void testBookTickets_Failure() throws Exception {
        BookingRequest request = new BookingRequest();
        request.setUserId(1L);
        request.setTrainId(100);
        request.setDepartureStopId(497L);
        request.setArrivalStopId(500L);
        request.setTravelDate(LocalDate.of(2025, 7, 20));
        request.setCarriageTypeId(5);
        
        BookingRequest.PassengerInfo passengerInfo = new BookingRequest.PassengerInfo();
        passengerInfo.setPassengerId(1L);
        passengerInfo.setTicketType((byte) 1);
        passengerInfo.setCarriageTypeId(5);
        request.setPassengers(Arrays.asList(passengerInfo));

        BookingResponse failureResponse = BookingResponse.failure("库存不足");

        when(ticketService.bookTickets(any(BookingRequest.class))).thenReturn(failureResponse);

        mockMvc.perform(post("/api/ticket/book")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.message").value("库存不足"));
    }

    @Test
    void testRefundTickets_Success() throws Exception {
        RefundRequest request = new RefundRequest();
        request.setUserId(1L);
        request.setOrderId(1L);
        request.setTicketIds(Arrays.asList(1L, 2L));

        BookingResponse successResponse = BookingResponse.successWithMessage(
            "退票成功", null, null, null, LocalDateTime.now());

        when(ticketService.refundTickets(any(RefundRequest.class))).thenReturn(successResponse);

        mockMvc.perform(post("/api/ticket/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void testRefundTickets_Failure() throws Exception {
        RefundRequest request = new RefundRequest();
        request.setUserId(1L);
        request.setOrderId(1L);
        request.setTicketIds(Arrays.asList(1L, 2L));

        BookingResponse failureResponse = BookingResponse.failure("退票失败");

        when(ticketService.refundTickets(any(RefundRequest.class))).thenReturn(failureResponse);

        mockMvc.perform(post("/api/ticket/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void testChangeTickets_Success() throws Exception {
        ChangeTicketRequest request = new ChangeTicketRequest();
        request.setUserId(1L);
        request.setOriginalOrderId(1L);
        request.setTicketIds(Arrays.asList(1L, 2L));
        request.setNewTrainId(200);
        request.setNewDepartureStopId(498L);
        request.setNewArrivalStopId(501L);
        request.setNewTravelDate(LocalDate.of(2025, 7, 21));
        request.setNewCarriageTypeId(6);

        BookingResponse successResponse = BookingResponse.successWithMessage(
            "改签成功", "ORDER456", null, null, LocalDateTime.now());

        when(ticketService.changeTickets(any(ChangeTicketRequest.class))).thenReturn(successResponse);

        mockMvc.perform(post("/api/ticket/change")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void testChangeTickets_Failure() throws Exception {
        ChangeTicketRequest request = new ChangeTicketRequest();
        request.setUserId(1L);
        request.setOriginalOrderId(1L);
        request.setTicketIds(Arrays.asList(1L, 2L));
        // 添加必要的字段以避免验证失败
        request.setNewTrainId(200);
        request.setNewDepartureStopId(498L);
        request.setNewArrivalStopId(501L);
        request.setNewTravelDate(LocalDate.of(2025, 7, 21));
        request.setNewCarriageTypeId(6);

        BookingResponse failureResponse = BookingResponse.failure("改签失败");

        when(ticketService.changeTickets(any(ChangeTicketRequest.class))).thenReturn(failureResponse);

        mockMvc.perform(post("/api/ticket/change")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void testGetMyTickets_Success() throws Exception {
        MyTicketResponse.MyTicketInfo ticketInfo = new MyTicketResponse.MyTicketInfo();
        ticketInfo.setTicketId(1L);
        ticketInfo.setTicketNumber("T123456");
        ticketInfo.setTrainNumber("G101");
        ticketInfo.setDepartureStationName("北京");
        ticketInfo.setArrivalStationName("上海");
        ticketInfo.setTravelDate(LocalDate.of(2025, 7, 20));
        ticketInfo.setDepartureTime(LocalTime.of(9, 0));
        ticketInfo.setArrivalTime(LocalTime.of(14, 30));
        ticketInfo.setPrice(BigDecimal.valueOf(553.5));
        ticketInfo.setTicketStatus((byte) 1);
        ticketInfo.setTicketStatusText("已支付");
        ticketInfo.setTicketType((byte) 1);
        ticketInfo.setTicketTypeText("成人票");
        ticketInfo.setCreatedTime(LocalDateTime.now());

        MyTicketResponse successResponse = MyTicketResponse.success(Arrays.asList(ticketInfo));

        when(ticketService.getMyTickets(anyLong())).thenReturn(successResponse);

        mockMvc.perform(get("/api/ticket/my-tickets")
                .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.tickets").isArray())
                .andExpect(jsonPath("$.tickets[0].ticketNumber").value("T123456"));
    }

    @Test
    void testGetMyTickets_Failure() throws Exception {
        MyTicketResponse failureResponse = MyTicketResponse.failure("获取失败");

        when(ticketService.getMyTickets(anyLong())).thenReturn(failureResponse);

        mockMvc.perform(get("/api/ticket/my-tickets")
                .param("userId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILURE"));
    }

    @Test
    void testGetMyTicketsByStatus_Success() throws Exception {
        MyTicketResponse successResponse = MyTicketResponse.success(Collections.emptyList());

        when(ticketService.getMyTicketsByStatus(anyLong(), anyByte())).thenReturn(successResponse);

        mockMvc.perform(get("/api/ticket/my-tickets/status")
                .param("userId", "1")
                .param("ticketStatus", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void testGetMyTicketsByStatus_Failure() throws Exception {
        MyTicketResponse failureResponse = MyTicketResponse.failure("获取失败");

        when(ticketService.getMyTicketsByStatus(anyLong(), anyByte())).thenReturn(failureResponse);

        mockMvc.perform(get("/api/ticket/my-tickets/status")
                .param("userId", "1")
                .param("ticketStatus", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILURE"));
    }

    @Test
    void testGetMyTicketsByDateRange_Success() throws Exception {
        MyTicketResponse successResponse = MyTicketResponse.success(Collections.emptyList());

        when(ticketService.getMyTicketsByDateRange(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(successResponse);

        mockMvc.perform(get("/api/ticket/my-tickets/date-range")
                .param("userId", "1")
                .param("startDate", "2025-07-01")
                .param("endDate", "2025-07-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void testGetMyTicketsByDateRange_ServiceFailure() throws Exception {
        MyTicketResponse failureResponse = MyTicketResponse.failure("服务错误");

        when(ticketService.getMyTicketsByDateRange(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(failureResponse);

        mockMvc.perform(get("/api/ticket/my-tickets/date-range")
                .param("userId", "1")
                .param("startDate", "2025-07-01")
                .param("endDate", "2025-07-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILURE"))
                .andExpect(jsonPath("$.message").value("服务错误"));
    }

    @Test
    void testGetMyTicketsByDateRange_InvalidDate() throws Exception {
        mockMvc.perform(get("/api/ticket/my-tickets/date-range")
                .param("userId", "1")
                .param("startDate", "invalid-date")
                .param("endDate", "2025-07-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILURE"))
                .andExpect(jsonPath("$.message").value("日期格式错误，请使用yyyy-MM-dd格式"));
    }

    @Test
    void testGetMyTicketsByStatusAndDateRange_Success() throws Exception {
        MyTicketResponse successResponse = MyTicketResponse.success(Collections.emptyList());

        when(ticketService.getMyTicketsByStatusAndDateRange(anyLong(), anyByte(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(successResponse);

        mockMvc.perform(get("/api/ticket/my-tickets/status-date-range")
                .param("userId", "1")
                .param("ticketStatus", "1")
                .param("startDate", "2025-07-01")
                .param("endDate", "2025-07-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void testGetMyTicketsByStatusAndDateRange_ServiceFailure() throws Exception {
        MyTicketResponse failureResponse = MyTicketResponse.failure("服务错误");

        when(ticketService.getMyTicketsByStatusAndDateRange(anyLong(), anyByte(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(failureResponse);

        mockMvc.perform(get("/api/ticket/my-tickets/status-date-range")
                .param("userId", "1")
                .param("ticketStatus", "1")
                .param("startDate", "2025-07-01")
                .param("endDate", "2025-07-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILURE"))
                .andExpect(jsonPath("$.message").value("服务错误"));
    }

    @Test
    void testGetMyTicketsByStatusAndDateRange_InvalidDate() throws Exception {
        mockMvc.perform(get("/api/ticket/my-tickets/status-date-range")
                .param("userId", "1")
                .param("ticketStatus", "1")
                .param("startDate", "invalid-date")
                .param("endDate", "2025-07-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILURE"))
                .andExpect(jsonPath("$.message").value("日期格式错误，请使用yyyy-MM-dd格式"));
    }

    @Test
    void testGetTicketDetail_Success() throws Exception {
        TicketDetailResponse.TicketDetailInfo detailInfo = new TicketDetailResponse.TicketDetailInfo();
        detailInfo.setTicketId(1L);
        detailInfo.setTicketNumber("T123456");
        detailInfo.setTrainNumber("G101");
        detailInfo.setDepartureStationName("北京");
        detailInfo.setArrivalStationName("上海");
        detailInfo.setTravelDate(LocalDate.of(2025, 7, 20));
        detailInfo.setPrice(BigDecimal.valueOf(553.5));

        TicketDetailResponse successResponse = TicketDetailResponse.success(detailInfo);

        when(ticketService.getTicketDetail(anyLong(), anyLong())).thenReturn(successResponse);

        mockMvc.perform(get("/api/ticket/detail")
                .param("ticketId", "1")
                .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.ticket.ticketNumber").value("T123456"));
    }

    @Test
    void testGetTicketDetail_Failure() throws Exception {
        TicketDetailResponse failureResponse = TicketDetailResponse.failure("获取失败");

        when(ticketService.getTicketDetail(anyLong(), anyLong())).thenReturn(failureResponse);

        mockMvc.perform(get("/api/ticket/detail")
                .param("ticketId", "1")
                .param("userId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILURE"));
    }
} 