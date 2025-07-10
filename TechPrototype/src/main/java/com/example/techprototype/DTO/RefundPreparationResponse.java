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
public class RefundPreparationResponse {
    
    // 订单基本信息
    private String orderNumber;
    private Byte orderStatus;
    private LocalDateTime orderTime;
    private LocalDateTime paymentTime;
    private String paymentMethod;
    private BigDecimal totalAmount;
    private Integer ticketCount;
    
    // 车次信息
    private String trainNumber;
    private LocalDate travelDate;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private String departureStation;
    private String arrivalStation;
    
    // 退票规则信息
    private RefundRules refundRules;
    
    // 可退票的车票列表
    private List<RefundableTicket> refundableTickets;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundRules {
        private String description; // 退票规则描述
        private BigDecimal refundRate; // 退票费率
        private String notice; // 退票须知
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundableTicket {
        private Long ticketId;
        private String ticketNumber;
        private String passengerName;
        private String idCardNumber;
        private Byte passengerType;
        private Byte ticketType;
        private String carriageType;
        private String carriageNumber;
        private String seatNumber;
        private BigDecimal originalPrice; // 原票价
        private BigDecimal refundAmount; // 可退金额
        private Byte ticketStatus;
        private Boolean canRefund; // 是否可退票
        private String refundReason; // 不可退票的原因
    }
} 