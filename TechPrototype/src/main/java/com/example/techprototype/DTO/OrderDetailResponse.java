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
public class OrderDetailResponse {
    
    // 订单基本信息
    private String orderNumber;
    private Byte orderStatus;
    private LocalDateTime orderTime;
    private LocalDateTime paymentTime;
    private String paymentMethod;
    private BigDecimal totalAmount;
    
    // 车次信息（所有车票共享相同信息）
    private String trainNumber;
    private LocalDate travelDate;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private String departureStation;
    private String arrivalStation;
    
    // 车票列表
    private List<TicketDetail> tickets;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketDetail {
        private Long ticketId;
        private String ticketNumber;
        private String passengerName;
        private String idCardNumber;
        private Byte passengerType;
        private Byte ticketType;
        private String carriageType;
        private String carriageNumber;
        private String seatNumber;
        private BigDecimal price;
        private Byte ticketStatus;
    }
} 