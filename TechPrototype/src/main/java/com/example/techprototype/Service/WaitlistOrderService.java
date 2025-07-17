package com.example.techprototype.Service;

import com.example.techprototype.DTO.BookingRequest;
import com.example.techprototype.DTO.BookingResponse;
import com.example.techprototype.DTO.WaitlistOrderResponse;
import com.example.techprototype.DTO.WaitlistOrderDetailResponse;
import java.util.List;

public interface WaitlistOrderService {
    
    /**
     * 创建候补订单
     */
    BookingResponse createWaitlistOrder(BookingRequest request);
    
    /**
     * 获取用户的候补订单列表
     */
    WaitlistOrderResponse getMyWaitlistOrders(Long userId);
    
    /**
     * 获取候补订单详情
     */
    WaitlistOrderDetailResponse getWaitlistOrderDetail(Long userId, Long waitlistId);
    
    /**
     * 支付候补订单
     */
    BookingResponse payWaitlistOrder(Long waitlistId, Long userId);
    
    /**
     * 取消候补订单
     */
    BookingResponse cancelWaitlistOrder(Long waitlistId, Long userId);
    
    /**
     * 处理候补订单兑现
     */
    void processWaitlistFulfillment();
    
    /**
     * 退款候补订单
     */
    BookingResponse refundWaitlistOrder(Long waitlistId, Long userId);
    
    /**
     * 部分退款候补订单项
     */
    BookingResponse refundWaitlistOrderItems(Long waitlistId, Long userId, List<Long> itemIds);
} 
 