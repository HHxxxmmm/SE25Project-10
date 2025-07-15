package com.example.techprototype.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    
    private String orderNumber;
    private Long orderId;
    private BigDecimal totalAmount;
    private LocalDateTime orderTime;
    private String status;
    private String message;
    
    public static BookingResponse success(String orderNumber, Long orderId, BigDecimal totalAmount, LocalDateTime orderTime) {
        return new BookingResponse(orderNumber, orderId, totalAmount, orderTime, "SUCCESS", "购票成功");
    }
    
    public static BookingResponse successWithMessage(String message, String orderNumber, Long orderId, BigDecimal totalAmount, LocalDateTime orderTime) {
        return new BookingResponse(orderNumber, orderId, totalAmount, orderTime, "SUCCESS", message);
    }
    
    public static BookingResponse failure(String message) {
        return new BookingResponse(null, null, null, null, "FAILED", message);
    }
} 