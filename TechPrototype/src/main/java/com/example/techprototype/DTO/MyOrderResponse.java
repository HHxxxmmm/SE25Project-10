package com.example.techprototype.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyOrderResponse {
    
    private String status;
    private String message;
    private List<MyOrderInfo> orders;
    private LocalDateTime timestamp;
    
    public static MyOrderResponse success(List<MyOrderInfo> orders) {
        return new MyOrderResponse("SUCCESS", "获取成功", orders, LocalDateTime.now());
    }
    
    public static MyOrderResponse failure(String message) {
        return new MyOrderResponse("FAILURE", message, null, LocalDateTime.now());
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MyOrderInfo {
        // 订单基本信息
        private Long orderId;
        private String orderNumber;
        private LocalDateTime orderTime;
        private BigDecimal totalAmount;
        private Byte orderStatus;
        private String orderStatusText;
        private String paymentMethod;
        private LocalDateTime paymentTime;
        
        // 车次信息（从订单中任意一张车票获取）
        private Integer trainId;
        private String trainNumber;
        private LocalDate departureDate;
        private LocalTime departureTime;
        private LocalTime arrivalTime;
        private String departureStationName;
        private String departureCity;
        private String arrivalStationName;
        private String arrivalCity;
        
        // 车票数量
        private Integer ticketCount;
    }
} 