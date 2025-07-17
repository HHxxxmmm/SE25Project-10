package com.example.techprototype.Service;

import com.example.techprototype.Entity.Order;
import com.example.techprototype.Entity.Ticket;
import com.example.techprototype.Entity.WaitlistOrder;
import com.example.techprototype.Entity.WaitlistItem;
import com.example.techprototype.Enums.OrderStatus;
import com.example.techprototype.Enums.WaitlistOrderStatus;
import com.example.techprototype.Enums.WaitlistItemStatus;
import com.example.techprototype.Repository.OrderRepository;
import com.example.techprototype.Repository.TicketRepository;
import com.example.techprototype.Repository.WaitlistOrderRepository;
import com.example.techprototype.Repository.WaitlistItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderTimeoutService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private TicketRepository ticketRepository;
    
    @Autowired
    private SeatService seatService;
    
    @Autowired
    private RedisService redisService;
    
    @Autowired
    private WaitlistOrderRepository waitlistOrderRepository;
    
    @Autowired
    private WaitlistItemRepository waitlistItemRepository;
    
    /**
     * 每30秒检查一次超时订单
     * 超时时间：15分钟
     */
    @Scheduled(fixedRate = 30000) // 30秒
    @Transactional
    public void handleTimeoutOrders() {
        try {
            // 查找15分钟前创建的待支付订单
            LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(15);
            
            List<Order> timeoutOrders = orderRepository.findTimeoutOrders(timeoutThreshold);
            
            for (Order order : timeoutOrders) {
                handleTimeoutOrder(order);
            }
            
            if (!timeoutOrders.isEmpty()) {
                System.out.println("处理超时订单: " + timeoutOrders.size() + "个");
            }
            
            // 候补订单兑现现在由库存回滚触发，不再需要定时检查
            // processWaitlistFulfillment();
            
        } catch (Exception e) {
            System.err.println("处理超时订单失败: " + e.getMessage());
        }
    }
    
    private void handleTimeoutOrder(Order order) {
        try {
        // 1. 更新订单状态为已取消
        order.setOrderStatus((byte) OrderStatus.CANCELLED.getCode());
        orderRepository.save(order);
        
        // 2. 获取订单下的车票
        List<Ticket> tickets = ticketRepository.findByOrderId(order.getOrderId());
        
            // 3. 更新车票状态为已取消并释放资源
        for (Ticket ticket : tickets) {
                // 更新车票状态为已取消
            ticket.setTicketStatus((byte) 3); // 已退票状态
            ticketRepository.save(ticket);
            
            // 释放座位（使用新的位图管理方式）
                if (ticket.getSeatNumber() != null && ticket.getCarriageNumber() != null) {
            seatService.releaseSeat(ticket);
        }
        
                // 回滚库存
                redisService.incrStock(ticket.getTrainId(), ticket.getDepartureStopId(),
                        ticket.getArrivalStopId(), ticket.getTravelDate(), 
                        ticket.getCarriageTypeId(), 1);
            }
            
            System.out.println("超时订单处理完成: " + order.getOrderNumber() + ", 释放了 " + tickets.size() + " 张车票的座位和库存");
            
        } catch (Exception e) {
            System.err.println("处理超时订单失败: " + order.getOrderNumber() + ", 错误: " + e.getMessage());
        }
    }
    
    /**
     * 处理候补订单兑现
     * 注意：这个方法现在只监测库存回滚，不主动尝试减库存
     */
    private void processWaitlistFulfillment() {
        // 暂时禁用主动兑现逻辑，改为监测库存回滚操作
        // 库存回滚操作会在其他地方触发候补订单兑现
        System.out.println("候补订单兑现：等待库存回滚触发...");
    }
    
    /**
     * 兑现候补订单项
     */
    private boolean fulfillWaitlistItem(WaitlistItem item, WaitlistOrder order) {
        try {
            // 这里实现具体的兑现逻辑
            // 1. 创建正式订单和车票
            // 2. 更新候补订单项状态
            // 3. 检查候补订单是否全部兑现
            
            // 简化实现，实际需要完整的订单创建逻辑
            item.setItemStatus((byte) WaitlistItemStatus.FULFILLED.getCode());
            waitlistItemRepository.save(item);
            
            // 检查候补订单是否全部兑现
            List<WaitlistItem> remainingItems = waitlistItemRepository.findPendingItemsByWaitlistId(order.getWaitlistId());
            if (remainingItems.isEmpty()) {
                order.setOrderStatus((byte) WaitlistOrderStatus.FULFILLED.getCode());
                waitlistOrderRepository.save(order);
            }
            
            return true;
        } catch (Exception e) {
            System.err.println("兑现候补订单项失败: " + e.getMessage());
            return false;
        }

    }
} 