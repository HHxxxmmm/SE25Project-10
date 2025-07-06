package com.example.techprototype.Service;

import com.example.techprototype.DTO.BookingResponse;
import com.example.techprototype.DTO.CancelOrderRequest;

public interface OrderService {
    
    /**
     * 取消待支付订单
     */
    BookingResponse cancelOrder(CancelOrderRequest request);
} 