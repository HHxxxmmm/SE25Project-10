package com.example.techprototype.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketDetailResponse {
    
    private String status;
    private String message;
    private TicketDetailInfo ticket;
    private LocalDateTime timestamp;
    
    public static TicketDetailResponse success(TicketDetailInfo ticket) {
        return new TicketDetailResponse("SUCCESS", "获取成功", ticket, LocalDateTime.now());
    }
    
    public static TicketDetailResponse failure(String message) {
        return new TicketDetailResponse("FAILURE", message, null, LocalDateTime.now());
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketDetailInfo {
        private Long ticketId;
        private String ticketNumber;
        private Long orderId;
        private String orderNumber;
        private Integer trainId;
        private String trainNumber;
        private Long departureStopId;
        private String departureStationName;
        private String departureCity;
        private Long arrivalStopId;
        private String arrivalStationName;
        private String arrivalCity;
        private LocalDate travelDate;
        private String carriageNumber;
        private String seatNumber;
        private BigDecimal price;
        private Byte ticketStatus;
        private String ticketStatusText;
        private Byte ticketType;
        private String ticketTypeText;
        private LocalDateTime createdTime;
        private LocalDateTime paymentTime;
        private String orderStatusText;
        private String passengerName;
        private String passengerIdCard;
        private String passengerPhone;
        private Byte passengerType;
        private String passengerTypeText;
        private String digitalSignature;
        private Integer runningDays;
        private Integer carriageTypeId;
        private String carriageTypeName;
    }
} 