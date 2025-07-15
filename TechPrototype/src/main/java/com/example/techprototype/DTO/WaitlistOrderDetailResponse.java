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
public class WaitlistOrderDetailResponse {
    
    private String status;
    private String message;
    private WaitlistOrderDetail waitlistOrder;
    
    public static WaitlistOrderDetailResponse success(WaitlistOrderDetail waitlistOrder) {
        return new WaitlistOrderDetailResponse("SUCCESS", "获取候补订单详情成功", waitlistOrder);
    }
    
    public static WaitlistOrderDetailResponse failure(String message) {
        return new WaitlistOrderDetailResponse("FAILURE", message, null);
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WaitlistOrderDetail {
        private Long waitlistId;
        private String orderNumber;
        private LocalDateTime orderTime;
        private BigDecimal totalAmount;
        private LocalDateTime expireTime;
        private Byte orderStatus;
        private String orderStatusText;
        private Integer trainId;
        private String trainNumber;
        private LocalDate travelDate;
        private String departureTime;
        private String arrivalTime;
        private String departureStation;
        private String arrivalStation;
        private Integer ticketCount;
        private List<WaitlistItemInfo> items;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WaitlistItemInfo {
        private Long itemId;
        private Long passengerId;
        private String passengerName;
        private String idCardNumber;
        private Byte passengerType;
        private String passengerTypeText;
        private Integer trainId;
        private String trainNumber;
        private Long departureStopId;
        private Long arrivalStopId;
        private String departureStationName;
        private String arrivalStationName;
        private LocalDate travelDate;
        private Integer carriageTypeId;
        private String carriageTypeName;
        private Byte ticketType;
        private String ticketTypeText;
        private Byte itemStatus;
        private String itemStatusText;
        private BigDecimal price;
        private LocalDateTime createdTime;
    }
} 
 