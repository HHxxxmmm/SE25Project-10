package com.example.techprototype.DTO;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;

class MyTicketResponseTest {

    @Test
    void testSuccessWithUserInfo() {
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

        MyTicketResponse.UserInfo userInfo = new MyTicketResponse.UserInfo();
        userInfo.setUserId(1L);
        userInfo.setRealName("张三");
        userInfo.setPhoneNumber("13800138000");
        userInfo.setEmail("test@example.com");
        userInfo.setPassengerId(1L);
        userInfo.setPassengerName("张三");
        userInfo.setPassengerIdCard("110101199001011234");
        userInfo.setPassengerPhone("13800138000");

        MyTicketResponse response = MyTicketResponse.success(Arrays.asList(ticketInfo), userInfo);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("获取成功", response.getMessage());
        assertNotNull(response.getTickets());
        assertEquals(1, response.getTickets().size());
        assertEquals("T123456", response.getTickets().get(0).getTicketNumber());
        assertNotNull(response.getUserInfo());
        assertEquals("张三", response.getUserInfo().getRealName());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void testSuccessWithoutUserInfo() {
        MyTicketResponse.MyTicketInfo ticketInfo = new MyTicketResponse.MyTicketInfo();
        ticketInfo.setTicketId(1L);
        ticketInfo.setTicketNumber("T123456");

        MyTicketResponse response = MyTicketResponse.success(Arrays.asList(ticketInfo));

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("获取成功", response.getMessage());
        assertNotNull(response.getTickets());
        assertEquals(1, response.getTickets().size());
        assertNull(response.getUserInfo());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void testSuccessWithEmptyList() {
        MyTicketResponse response = MyTicketResponse.success(Collections.emptyList());

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("获取成功", response.getMessage());
        assertNotNull(response.getTickets());
        assertTrue(response.getTickets().isEmpty());
        assertNull(response.getUserInfo());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void testFailure() {
        String errorMessage = "获取车票信息失败";
        MyTicketResponse response = MyTicketResponse.failure(errorMessage);

        assertNotNull(response);
        assertEquals("FAILURE", response.getStatus());
        assertEquals(errorMessage, response.getMessage());
        assertNull(response.getTickets());
        assertNull(response.getUserInfo());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void testMyTicketInfoSettersAndGetters() {
        MyTicketResponse.MyTicketInfo ticketInfo = new MyTicketResponse.MyTicketInfo();
        
        // 设置所有字段
        ticketInfo.setTicketId(1L);
        ticketInfo.setTicketNumber("T123456");
        ticketInfo.setOrderId(100L);
        ticketInfo.setOrderNumber("ORDER123");
        ticketInfo.setTrainId(200);
        ticketInfo.setTrainNumber("G101");
        ticketInfo.setDepartureStopId(497L);
        ticketInfo.setDepartureStationName("北京");
        ticketInfo.setArrivalStopId(500L);
        ticketInfo.setArrivalStationName("上海");
        ticketInfo.setTravelDate(LocalDate.of(2025, 7, 20));
        ticketInfo.setDepartureTime(LocalTime.of(9, 0));
        ticketInfo.setArrivalTime(LocalTime.of(14, 30));
        ticketInfo.setCarriageNumber("01");
        ticketInfo.setSeatNumber("01A");
        ticketInfo.setCarriageTypeName("二等座");
        ticketInfo.setPrice(BigDecimal.valueOf(553.5));
        ticketInfo.setTicketStatus((byte) 1);
        ticketInfo.setTicketStatusText("已支付");
        ticketInfo.setTicketType((byte) 1);
        ticketInfo.setTicketTypeText("成人票");
        ticketInfo.setCreatedTime(LocalDateTime.now());
        ticketInfo.setPaymentTime(LocalDateTime.now());
        ticketInfo.setOrderStatusText("已支付");

        // 验证所有字段
        assertEquals(1L, ticketInfo.getTicketId());
        assertEquals("T123456", ticketInfo.getTicketNumber());
        assertEquals(100L, ticketInfo.getOrderId());
        assertEquals("ORDER123", ticketInfo.getOrderNumber());
        assertEquals(200, ticketInfo.getTrainId());
        assertEquals("G101", ticketInfo.getTrainNumber());
        assertEquals(497L, ticketInfo.getDepartureStopId());
        assertEquals("北京", ticketInfo.getDepartureStationName());
        assertEquals(500L, ticketInfo.getArrivalStopId());
        assertEquals("上海", ticketInfo.getArrivalStationName());
        assertEquals(LocalDate.of(2025, 7, 20), ticketInfo.getTravelDate());
        assertEquals(LocalTime.of(9, 0), ticketInfo.getDepartureTime());
        assertEquals(LocalTime.of(14, 30), ticketInfo.getArrivalTime());
        assertEquals("01", ticketInfo.getCarriageNumber());
        assertEquals("01A", ticketInfo.getSeatNumber());
        assertEquals("二等座", ticketInfo.getCarriageTypeName());
        assertEquals(BigDecimal.valueOf(553.5), ticketInfo.getPrice());
        assertEquals((byte) 1, ticketInfo.getTicketStatus());
        assertEquals("已支付", ticketInfo.getTicketStatusText());
        assertEquals((byte) 1, ticketInfo.getTicketType());
        assertEquals("成人票", ticketInfo.getTicketTypeText());
        assertNotNull(ticketInfo.getCreatedTime());
        assertNotNull(ticketInfo.getPaymentTime());
        assertEquals("已支付", ticketInfo.getOrderStatusText());
    }

    @Test
    void testUserInfoSettersAndGetters() {
        MyTicketResponse.UserInfo userInfo = new MyTicketResponse.UserInfo();
        
        // 设置所有字段
        userInfo.setUserId(1L);
        userInfo.setRealName("张三");
        userInfo.setPhoneNumber("13800138000");
        userInfo.setEmail("test@example.com");
        userInfo.setPassengerId(1L);
        userInfo.setPassengerName("张三");
        userInfo.setPassengerIdCard("110101199001011234");
        userInfo.setPassengerPhone("13800138000");

        // 验证所有字段
        assertEquals(1L, userInfo.getUserId());
        assertEquals("张三", userInfo.getRealName());
        assertEquals("13800138000", userInfo.getPhoneNumber());
        assertEquals("test@example.com", userInfo.getEmail());
        assertEquals(1L, userInfo.getPassengerId());
        assertEquals("张三", userInfo.getPassengerName());
        assertEquals("110101199001011234", userInfo.getPassengerIdCard());
        assertEquals("13800138000", userInfo.getPassengerPhone());
    }

    @Test
    void testNoArgsConstructor() {
        MyTicketResponse response = new MyTicketResponse();
        assertNotNull(response);
        
        MyTicketResponse.MyTicketInfo ticketInfo = new MyTicketResponse.MyTicketInfo();
        assertNotNull(ticketInfo);
        
        MyTicketResponse.UserInfo userInfo = new MyTicketResponse.UserInfo();
        assertNotNull(userInfo);
    }

    @Test
    void testAllArgsConstructor() {
        LocalDateTime timestamp = LocalDateTime.now();
        MyTicketResponse.MyTicketInfo ticketInfo = new MyTicketResponse.MyTicketInfo();
        MyTicketResponse.UserInfo userInfo = new MyTicketResponse.UserInfo();
        
        MyTicketResponse response = new MyTicketResponse("SUCCESS", "测试消息", 
                Arrays.asList(ticketInfo), timestamp, userInfo);
        
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("测试消息", response.getMessage());
        assertEquals(1, response.getTickets().size());
        assertEquals(timestamp, response.getTimestamp());
        assertEquals(userInfo, response.getUserInfo());
    }
} 