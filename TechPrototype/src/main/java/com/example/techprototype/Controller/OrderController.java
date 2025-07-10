package com.example.techprototype.Controller;

import com.example.techprototype.DTO.BookingRequest;
import com.example.techprototype.DTO.BookingResponse;
import com.example.techprototype.DTO.CancelOrderRequest;
import com.example.techprototype.DTO.MyOrderResponse;
import com.example.techprototype.DTO.OrderDetailResponse;
import com.example.techprototype.DTO.RefundPreparationRequest;
import com.example.techprototype.DTO.RefundPreparationResponse;
import com.example.techprototype.Service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    /**
     * 创建订单
     */
    @PostMapping("/create")
    public BookingResponse createOrder(@RequestBody BookingRequest request) {
        return orderService.createOrder(request);
    }
    
    /**
     * 取消订单
     */
    @PostMapping("/cancel")
    public BookingResponse cancelOrder(@RequestBody CancelOrderRequest request) {
        return orderService.cancelOrder(request);
    }
    
    /**
     * 获取我的订单
     */
    @GetMapping("/my")
    public MyOrderResponse getMyOrders(@RequestParam Long userId) {
        return orderService.getMyOrders(userId);
    }
    
    /**
     * 根据条件获取我的订单
     */
    @GetMapping("/my/filter")
    public MyOrderResponse getMyOrdersByConditions(
            @RequestParam Long userId,
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Byte orderStatus,
            @RequestParam(required = false) String trainNumber) {
        return orderService.getMyOrdersByConditions(userId, orderNumber, startDate, endDate, orderStatus, trainNumber);
    }
    
    /**
     * 获取订单详情
     */
    @GetMapping("/detail")
    public OrderDetailResponse getOrderDetail(@RequestParam Long userId, @RequestParam Long orderId) {
        return orderService.getOrderDetail(userId, orderId);
    }
    
    /**
     * 退票准备阶段 - 获取退票信息
     */
    @PostMapping("/refund/preparation")
    public RefundPreparationResponse getRefundPreparation(@RequestBody RefundPreparationRequest request) {
        return orderService.getRefundPreparation(request);
    }
} 