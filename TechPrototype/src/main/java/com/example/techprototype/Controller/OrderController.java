package com.example.techprototype.Controller;

import com.example.techprototype.DTO.BookingResponse;
import com.example.techprototype.DTO.CancelOrderRequest;
import com.example.techprototype.Service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    /**
     * 取消待支付订单
     */
    @PostMapping("/cancel")
    public ResponseEntity<BookingResponse> cancelOrder(@RequestBody CancelOrderRequest request) {
        BookingResponse response = orderService.cancelOrder(request);
        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    

} 