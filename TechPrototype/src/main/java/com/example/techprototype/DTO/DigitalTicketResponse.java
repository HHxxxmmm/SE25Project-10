package com.example.techprototype.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * 数字票证API响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DigitalTicketResponse {
    
    private String status;
    private String message;
    private DigitalTicketInfo ticketData;
    private LocalDateTime timestamp;
    
    public static DigitalTicketResponse success(DigitalTicketInfo ticketData) {
        return new DigitalTicketResponse("SUCCESS", "获取成功", ticketData, LocalDateTime.now());
    }
    
    public static DigitalTicketResponse failure(String message) {
        return new DigitalTicketResponse("FAILURE", message, null, LocalDateTime.now());
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DigitalTicketInfo {
        private String qrCodeData;
        private String publicKey;
        private Long ticketId;
        private String ticketNumber;
        private String passengerName;
        private String trainNumber;
        private String departureStationName;
        private String arrivalStationName;
        private String travelDate;
    }
}
