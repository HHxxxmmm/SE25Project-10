package com.example.techprototype.Service;

import com.example.techprototype.DTO.BookingResponse;

public interface PaymentService {
    
    /**
     * 支付订单
     */
    BookingResponse payOrder(Long orderId, Long userId);
} 