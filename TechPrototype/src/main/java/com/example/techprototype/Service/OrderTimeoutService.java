package com.example.techprototype.Service;

import com.example.techprototype.Entity.Order;
import com.example.techprototype.Entity.Ticket;
import com.example.techprototype.Entity.Seat;
import com.example.techprototype.Entity.TrainCarriage;
import com.example.techprototype.Enums.OrderStatus;
import com.example.techprototype.Repository.OrderRepository;
import com.example.techprototype.Repository.TicketRepository;
import com.example.techprototype.Repository.SeatRepository;
import com.example.techprototype.Repository.TrainCarriageRepository;
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
    private SeatRepository seatRepository;
    
    @Autowired
    private TrainCarriageRepository trainCarriageRepository;
    
    /**
     * 每5分钟检查一次超时订单
     * 超时时间：30分钟
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    @Transactional
    public void handleTimeoutOrders() {
        try {
            // 查找30分钟前创建的待支付订单
            LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(30);
            
            // 这里需要根据实际业务逻辑查询超时订单
            // 简化处理，实际需要添加查询方法
            List<Order> timeoutOrders = findTimeoutOrders(timeoutThreshold);
            
            for (Order order : timeoutOrders) {
                handleTimeoutOrder(order);
            }
            
            if (!timeoutOrders.isEmpty()) {
                System.out.println("处理超时订单: " + timeoutOrders.size() + "个");
            }
        } catch (Exception e) {
            System.err.println("处理超时订单失败: " + e.getMessage());
        }
    }
    
    private List<Order> findTimeoutOrders(LocalDateTime timeoutThreshold) {
        // 这里需要实现查询超时订单的逻辑
        // 简化处理，返回空列表
        return List.of();
    }
    
    private void handleTimeoutOrder(Order order) {
        // 1. 更新订单状态为已取消
        order.setOrderStatus((byte) OrderStatus.CANCELLED.getCode());
        orderRepository.save(order);
        
        // 2. 获取订单下的车票
        List<Ticket> tickets = ticketRepository.findByOrderId(order.getOrderId());
        
        // 3. 更新车票状态为已取消
        for (Ticket ticket : tickets) {
            ticket.setTicketStatus((byte) 3); // 已退票状态
            ticketRepository.save(ticket);
            
            // 释放座位
            if (ticket.getSeatNumber() != null && ticket.getCarriageNumber() != null) {
                releaseSeat(ticket.getTrainId(), ticket.getCarriageNumber(), ticket.getSeatNumber());
            }
        }
        
        System.out.println("超时订单处理完成: " + order.getOrderNumber());
    }
    
    private void releaseSeat(Integer trainId, String carriageNumber, String seatNumber) {
        try {
            // 查找车厢
            java.util.Optional<TrainCarriage> carriageOpt = trainCarriageRepository.findByTrainIdAndCarriageNumber(trainId, carriageNumber);
            if (carriageOpt.isPresent()) {
                TrainCarriage carriage = carriageOpt.get();
                // 查找座位并释放
                List<Seat> seats = seatRepository.findByCarriageIdAndSeatNumber(carriage.getCarriageId(), seatNumber);
                if (!seats.isEmpty()) {
                    Seat seat = seats.get(0);
                    seat.setIsAvailable(true); // 设置为可用
                    seatRepository.save(seat);
                    System.out.println("座位已释放: " + trainId + "-" + carriageNumber + "-" + seatNumber);
                }
            }
        } catch (Exception e) {
            System.err.println("释放座位失败: " + e.getMessage());
        }
    }
} 