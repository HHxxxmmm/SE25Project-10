package com.example.techprototype.Service;

import com.example.techprototype.DTO.BookingResponse;
import com.example.techprototype.DTO.CancelOrderRequest;
import com.example.techprototype.DTO.MyOrderResponse;
import com.example.techprototype.DTO.OrderDetailResponse;
import com.example.techprototype.DTO.RefundPreparationRequest;
import com.example.techprototype.DTO.RefundPreparationResponse;
import com.example.techprototype.Entity.Order;
import com.example.techprototype.DTO.OrderMessage;

import java.time.LocalDate;
import java.util.List;

public interface OrderService {
    
    /**
     * 创建订单
     */
    BookingResponse createOrder(com.example.techprototype.DTO.BookingRequest request);
    
    /**
     * 从消息创建订单
     */
    Order createOrderFromMessage(OrderMessage message);
    
    /**
     * 取消订单
     */
    BookingResponse cancelOrder(CancelOrderRequest request);
    
    /**
     * 获取我的订单
     */
    MyOrderResponse getMyOrders(Long userId);
    
    /**
     * 根据条件获取我的订单
     */
    MyOrderResponse getMyOrdersByConditions(Long userId, String orderNumber, LocalDate startDate, LocalDate endDate, Byte orderStatus, String trainNumber);
    
    /**
     * 获取订单详情
     */
    OrderDetailResponse getOrderDetail(Long userId, Long orderId);
    
    /**
     * 退票准备阶段 - 获取退票信息
     */
    RefundPreparationResponse getRefundPreparation(RefundPreparationRequest request);
} 