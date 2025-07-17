package com.example.techprototype.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WaitlistOrderResponse {
    
    private String status;
    private String message;
    private List<WaitlistOrderInfo> waitlistOrders;
    
    public static WaitlistOrderResponse success(List<WaitlistOrderInfo> waitlistOrders) {
        return new WaitlistOrderResponse("SUCCESS", "获取候补订单成功", waitlistOrders);
    }
    
    public static WaitlistOrderResponse failure(String message) {
        return new WaitlistOrderResponse("FAILURE", message, null);
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WaitlistOrderInfo {
        private Long waitlistId;
        private String orderNumber;
        private LocalDateTime orderTime;
        private BigDecimal totalAmount;
        private LocalDateTime expireTime;
        private Byte orderStatus;
        private String orderStatusText;
        private Integer trainId;
        private String trainNumber;
        private LocalDateTime departureDate;
        private String departureTime;
        private String arrivalTime;
        private String departureStationName;
        private String arrivalStationName;
        private Integer ticketCount;
    }
} 
 