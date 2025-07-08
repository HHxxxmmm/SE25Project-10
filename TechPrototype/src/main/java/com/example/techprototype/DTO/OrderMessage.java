package com.example.techprototype.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderMessage {
    
    private Long userId;
    private Integer trainId;
    private Long departureStopId;
    private Long arrivalStopId;
    private LocalDate travelDate;
    private Integer carriageTypeId;
    private List<PassengerInfo> passengers;
    private String orderNumber;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PassengerInfo {
        private Long passengerId;
        private Byte ticketType;
    }
} 