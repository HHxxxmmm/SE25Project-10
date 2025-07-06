package com.example.techprototype.Service.Impl;

import com.example.techprototype.DTO.BookingResponse;
import com.example.techprototype.Entity.Order;
import com.example.techprototype.Entity.Ticket;
import com.example.techprototype.Enums.OrderStatus;
import com.example.techprototype.Enums.TicketStatus;
import com.example.techprototype.Repository.OrderRepository;
import com.example.techprototype.Repository.TicketRepository;
import com.example.techprototype.Service.PaymentService;
import com.example.techprototype.Service.RedisService;
import com.example.techprototype.Service.SeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentServiceImpl implements PaymentService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private TicketRepository ticketRepository;
    
    @Autowired
    private RedisService redisService;
    
    @Autowired
    private SeatService seatService;
    
    @Override
    @Transactional
    public BookingResponse payOrder(String orderNumber, Long userId) {
        // 优先从Redis查询订单
        Optional<Order> orderOpt = redisService.getCachedOrder(orderNumber);
        if (!orderOpt.isPresent()) {
            // Redis中没有，从数据库查询
            orderOpt = orderRepository.findByOrderNumberAndUserId(orderNumber, userId);
            if (!orderOpt.isPresent()) {
                return BookingResponse.failure("订单不存在");
            }
            // 缓存到Redis
            redisService.cacheOrder(orderOpt.get());
        }
        
        Order order = orderOpt.get();
        if (order.getOrderStatus() != OrderStatus.PENDING_PAYMENT.getCode()) {
            return BookingResponse.failure("订单状态不正确");
        }
        
        // 更新订单状态
        order.setOrderStatus((byte) OrderStatus.PAID.getCode());
        order.setPaymentTime(LocalDateTime.now());
        order.setPaymentMethod("在线支付");
        orderRepository.save(order);
        
        // 优先从Redis查询车票
        List<Ticket> tickets = redisService.getCachedTickets(order.getOrderId());
        if (tickets.isEmpty()) {
            // Redis中没有，从数据库查询
            tickets = ticketRepository.findByOrderId(order.getOrderId());
            // 缓存到Redis
            for (Ticket ticket : tickets) {
                redisService.cacheTicket(ticket);
            }
        }
        
        // 更新车票状态
        for (Ticket ticket : tickets) {
            ticket.setTicketStatus((byte) TicketStatus.UNUSED.getCode());
            ticketRepository.save(ticket);
        }
        
        // 检查是否是改签订单，如果是则处理原票
        handleChangeTicketPayment(order, tickets);
        
        System.out.println("支付成功: " + orderNumber + ", 订单ID: " + order.getOrderId());
        
        return BookingResponse.success("支付成功", order.getOrderId(), order.getTotalAmount(), LocalDateTime.now());
    }
    
    /**
     * 处理改签支付后的原票更新
     */
    private void handleChangeTicketPayment(Order newOrder, List<Ticket> newTickets) {
        try {
            // 检查是否是改签订单（通过Redis中的改签配对关系来判断）
            for (Ticket newTicket : newTickets) {
                // 查询改签配对关系
                String mappingKey = "change_mapping:" + newTicket.getTicketId();
                Optional<String> mappingValueOpt = redisService.getChangeMapping(mappingKey);
                
                if (mappingValueOpt.isPresent()) {
                    String mappingValue = mappingValueOpt.get();
                    String[] parts = mappingValue.split(":");
                    if (parts.length == 2) {
                        Long originalTicketId = Long.parseLong(parts[0]);
                        Long passengerId = Long.parseLong(parts[1]);
                        
                        System.out.println("发现改签配对关系: 新票" + newTicket.getTicketId() + " -> 原票" + originalTicketId + ":乘客" + passengerId);
                        
                        // 查找原票
                        Optional<Ticket> originalTicketOpt = ticketRepository.findById(originalTicketId);
                        if (originalTicketOpt.isPresent()) {
                            Ticket originalTicket = originalTicketOpt.get();
                            
                            // 验证乘客ID匹配
                            if (originalTicket.getPassengerId().equals(passengerId)) {
                                System.out.println("处理改签原票: " + originalTicket.getTicketId() + 
                                                 ", 乘客ID: " + originalTicket.getPassengerId());
                                
                                // 1. 更新原票状态为已改签
                                originalTicket.setTicketStatus((byte) TicketStatus.CHANGED.getCode());
                                ticketRepository.save(originalTicket);
                                
                                // 2. 回滚原票的库存
                                redisService.incrStock(originalTicket.getTrainId(), originalTicket.getDepartureStopId(),
                                        originalTicket.getArrivalStopId(), originalTicket.getTravelDate(), 
                                        originalTicket.getCarriageTypeId(), 1);
                                
                                // 3. 释放原票的座位
                                if (originalTicket.getSeatNumber() != null && originalTicket.getCarriageNumber() != null) {
                                    seatService.releaseSeat(originalTicket);
                                }
                                
                                // 4. 更新原订单总价
                                Optional<Order> originalOrderOpt = orderRepository.findById(originalTicket.getOrderId());
                                if (originalOrderOpt.isPresent()) {
                                    Order originalOrder = originalOrderOpt.get();
                                    originalOrder.setTotalAmount(originalOrder.getTotalAmount().subtract(originalTicket.getPrice()));
                                    orderRepository.save(originalOrder);
                                    
                                    System.out.println("原订单总价已更新: " + originalOrder.getOrderNumber() + 
                                                     ", 减去金额: " + originalTicket.getPrice());
                                    
                                    // 5. 检查原订单是否还有有效车票
                                    List<Ticket> validTickets = ticketRepository.findValidTicketsByOrderId(originalOrder.getOrderId());
                                    if (validTickets.isEmpty()) {
                                        // 原订单中没有有效车票，将订单状态改为已取消
                                        originalOrder.setOrderStatus((byte) OrderStatus.CANCELLED.getCode());
                                        orderRepository.save(originalOrder);
                                        System.out.println("原订单已取消: " + originalOrder.getOrderNumber());
                                    }
                                }
                                
                                // 6. 删除改签配对关系
                                redisService.deleteChangeMapping(mappingKey);
                                System.out.println("已删除改签配对关系: " + mappingKey);
                            } else {
                                System.err.println("乘客ID不匹配: 期望" + passengerId + ", 实际" + originalTicket.getPassengerId());
                            }
                        } else {
                            System.err.println("未找到原票: " + originalTicketId);
                        }
                    } else {
                        System.err.println("改签配对关系格式错误: " + mappingValue);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("处理改签支付失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    

} 