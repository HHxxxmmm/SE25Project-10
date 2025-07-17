package com.example.techprototype.Controller;

import com.example.techprototype.DTO.BookingRequest;
import com.example.techprototype.DTO.BookingResponse;
import com.example.techprototype.DTO.WaitlistOrderResponse;
import com.example.techprototype.DTO.WaitlistOrderDetailResponse;
import com.example.techprototype.Service.WaitlistOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/waitlist")
@CrossOrigin(origins = "*")
public class WaitlistOrderController {
    
    @Autowired
    private WaitlistOrderService waitlistOrderService;
    
    /**
     * 创建候补订单
     */
    @PostMapping("/create")
    public ResponseEntity<BookingResponse> createWaitlistOrder(@RequestBody BookingRequest request) {
        BookingResponse response = waitlistOrderService.createWaitlistOrder(request);
        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取我的候补订单
     */
    @GetMapping("/my")
    public ResponseEntity<WaitlistOrderResponse> getMyWaitlistOrders(@RequestParam Long userId) {
        WaitlistOrderResponse response = waitlistOrderService.getMyWaitlistOrders(userId);
        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取候补订单详情
     */
    @GetMapping("/detail")
    public ResponseEntity<WaitlistOrderDetailResponse> getWaitlistOrderDetail(
            @RequestParam Long userId, 
            @RequestParam Long waitlistId) {
        WaitlistOrderDetailResponse response = waitlistOrderService.getWaitlistOrderDetail(userId, waitlistId);
        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 支付候补订单
     */
    @PostMapping("/pay")
    public ResponseEntity<BookingResponse> payWaitlistOrder(@RequestParam Long waitlistId, @RequestParam Long userId) {
        BookingResponse response = waitlistOrderService.payWaitlistOrder(waitlistId, userId);
        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 取消候补订单
     */
    @PostMapping("/cancel")
    public ResponseEntity<BookingResponse> cancelWaitlistOrder(@RequestParam Long waitlistId, @RequestParam Long userId) {
        BookingResponse response = waitlistOrderService.cancelWaitlistOrder(waitlistId, userId);
        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 退款候补订单
     */
    @PostMapping("/refund")
    public ResponseEntity<BookingResponse> refundWaitlistOrder(@RequestParam Long waitlistId, @RequestParam Long userId) {
        BookingResponse response = waitlistOrderService.refundWaitlistOrder(waitlistId, userId);
        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 部分退款候补订单项
     */
    @PostMapping("/refund-items")
    public ResponseEntity<BookingResponse> refundWaitlistOrderItems(@RequestParam Long waitlistId, 
                                                                   @RequestParam Long userId, 
                                                                   @RequestBody List<Long> itemIds) {
        BookingResponse response = waitlistOrderService.refundWaitlistOrderItems(waitlistId, userId, itemIds);
        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
} 
 


