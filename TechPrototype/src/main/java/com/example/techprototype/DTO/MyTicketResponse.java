package com.example.techprototype.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyTicketResponse {
    
    private String status;
    private String message;
    private List<MyTicketInfo> tickets;
    private LocalDateTime timestamp;
    
    public static MyTicketResponse success(List<MyTicketInfo> tickets) {
        return new MyTicketResponse("SUCCESS", "获取成功", tickets, LocalDateTime.now());
    }
    
    public static MyTicketResponse failure(String message) {
        return new MyTicketResponse("FAILURE", message, null, LocalDateTime.now());
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MyTicketInfo {
        private Long ticketId;
        private String ticketNumber;
        private Long orderId;
        private String orderNumber;
        private Integer trainId;
        private String trainNumber;
        private Long departureStopId;
        private String departureStationName;
        private Long arrivalStopId;
        private String arrivalStationName;
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
    }
} 