package com.example.techprototype.Controller;

import com.example.techprototype.DTO.BookingRequest;
import com.example.techprototype.DTO.BookingResponse;
import com.example.techprototype.DTO.CancelOrderRequest;
import com.example.techprototype.DTO.MyOrderResponse;
import com.example.techprototype.DTO.OrderDetailResponse;
import com.example.techprototype.DTO.RefundPreparationRequest;
import com.example.techprototype.DTO.RefundPreparationResponse;
import com.example.techprototype.Entity.Order;
import com.example.techprototype.Repository.OrderRepository;
import com.example.techprototype.Service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private OrderRepository orderRepository;
    
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
    
    /**
     * 根据订单号获取订单ID
     */
    @GetMapping("/order-id")
    public ResponseEntity<Map<String, Object>> getOrderIdByOrderNumber(@RequestParam String orderNumber) {
        try {
            Optional<Order> order = orderRepository.findByOrderNumber(orderNumber);
            if (order.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("orderId", order.get().getOrderId());
                response.put("orderNumber", order.get().getOrderNumber());
                response.put("status", "SUCCESS");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "NOT_FOUND");
                response.put("message", "订单不存在");
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "ERROR");
            response.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
} 