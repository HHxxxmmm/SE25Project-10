package com.example.techprototype.Controller;

import com.example.techprototype.DTO.BookingResponse;
import com.example.techprototype.Service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {
    
    @Autowired
    private PaymentService paymentService;
    
    /**
     * 支付订单
     */
    @PostMapping("/pay")
    public ResponseEntity<BookingResponse> payOrder(@RequestParam String orderNumber, @RequestParam Long userId) {
        BookingResponse response = paymentService.payOrder(orderNumber, userId);
        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
} 