package com.example.techprototype.Component;

import com.example.techprototype.DTO.OrderMessage;
import com.example.techprototype.Entity.Order;
import com.example.techprototype.Entity.Ticket;
import com.example.techprototype.Entity.TicketInventory;
import com.example.techprototype.Enums.OrderStatus;
import com.example.techprototype.Enums.TicketStatus;
import com.example.techprototype.Repository.OrderRepository;
import com.example.techprototype.Repository.TicketRepository;
import com.example.techprototype.Service.SeatService;
import com.example.techprototype.Service.RedisService;
import com.example.techprototype.DAO.TicketInventoryDAO;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class OrderProcessor {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private TicketRepository ticketRepository;
    
    @Autowired
    private SeatService seatService;
    
    @Autowired
    private TicketInventoryDAO ticketInventoryDAO;
    
    @Autowired
    private RedisService redisService;
    
    @RabbitListener(queues = "order.queue")
    @Transactional
    public void processOrder(OrderMessage orderMessage) {
        System.out.println("收到订单消息: " + orderMessage.getOrderNumber());
        
        // 记录已创建的车票，用于异常时回滚
        List<Ticket> createdTickets = new ArrayList<>();
        Order createdOrder = null;
        
        try {
            // 1. 创建订单
            Order order = new Order();
            order.setOrderNumber(orderMessage.getOrderNumber());
            order.setUserId(orderMessage.getUserId());
            order.setOrderTime(LocalDateTime.now());
            order.setOrderStatus((byte) OrderStatus.PENDING_PAYMENT.getCode());
            
            // 计算总金额
            BigDecimal totalAmount = calculateTotalAmount(orderMessage);
            order.setTotalAmount(totalAmount);
            
            orderRepository.save(order);
            createdOrder = order;
            System.out.println("订单创建成功: " + order.getOrderNumber() + ", ID: " + order.getOrderId());
            
            // 2. 创建车票记录（未支付状态）
            List<Ticket> tickets = new ArrayList<>();
            
            for (OrderMessage.PassengerInfo passengerInfo : orderMessage.getPassengers()) {
                Ticket ticket = new Ticket();
                ticket.setTicketNumber(generateTicketNumber());
                ticket.setOrderId(order.getOrderId());
                ticket.setPassengerId(passengerInfo.getPassengerId());
                ticket.setTrainId(orderMessage.getTrainId());
                ticket.setDepartureStopId(orderMessage.getDepartureStopId());
                ticket.setArrivalStopId(orderMessage.getArrivalStopId());
                ticket.setTravelDate(orderMessage.getTravelDate());
                ticket.setCarriageTypeId(passengerInfo.getCarriageTypeId());
                // 从库存表获取基础票价，然后根据票种计算优惠
                BigDecimal ticketPrice = calculateTicketPrice(
                    orderMessage.getTrainId(),
                    orderMessage.getDepartureStopId(),
                    orderMessage.getArrivalStopId(),
                    orderMessage.getTravelDate(),
                    passengerInfo.getCarriageTypeId(),
                    passengerInfo.getTicketType()
                );
                ticket.setPrice(ticketPrice);
                ticket.setTicketStatus((byte) TicketStatus.PENDING.getCode());
                ticket.setTicketType(passengerInfo.getTicketType());
                ticket.setCreatedTime(LocalDateTime.now());
                
                // 分配座位
                seatService.assignSeat(ticket);
                
                tickets.add(ticket);
            }
            
            ticketRepository.saveAll(tickets);
            createdTickets.addAll(tickets);
            System.out.println("车票创建成功: " + tickets.size() + "张");
            
        } catch (Exception e) {
            System.err.println("订单处理失败: " + e.getMessage());
            e.printStackTrace();
            
            // 回滚库存
            rollbackInventory(orderMessage);
            
            // 清理已创建的数据
            cleanupCreatedData(createdTickets, createdOrder);
            
            // 重新抛出异常，确保事务回滚
            throw new RuntimeException("订单处理失败，已回滚库存: " + e.getMessage(), e);
        }
    }
    
    private BigDecimal calculateTotalAmount(OrderMessage orderMessage) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (OrderMessage.PassengerInfo passengerInfo : orderMessage.getPassengers()) {
            BigDecimal ticketPrice = calculateTicketPrice(
                orderMessage.getTrainId(),
                orderMessage.getDepartureStopId(),
                orderMessage.getArrivalStopId(),
                orderMessage.getTravelDate(),
                passengerInfo.getCarriageTypeId(),
                passengerInfo.getTicketType()
            );
            totalAmount = totalAmount.add(ticketPrice);
        }
        
        return totalAmount;
    }
    
    /**
     * 计算票价 - 从库存信息获取基础票价，然后根据票种计算优惠
     */
    private BigDecimal calculateTicketPrice(Integer trainId, Long departureStopId, Long arrivalStopId, 
                                          LocalDate travelDate, Integer carriageTypeId, Byte ticketType) {
        // 从库存信息获取基础票价
        Optional<TicketInventory> inventory = ticketInventoryDAO.findByKey(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId);
        
        System.out.println("查询库存 - 车次:" + trainId + ", 出发站:" + departureStopId + ", 到达站:" + arrivalStopId + 
                          ", 日期:" + travelDate + ", 车厢类型:" + carriageTypeId + ", 找到:" + inventory.isPresent());
        
        if (inventory.isEmpty()) {
            System.out.println("未找到库存记录，使用默认票价100元");
            return BigDecimal.valueOf(100.0); // 默认票价
        }
        
        BigDecimal basePrice = inventory.get().getPrice();
        System.out.println("找到库存记录，基础票价:" + basePrice + "元");
        
        // 根据票种计算优惠
        BigDecimal finalPrice;
        switch (ticketType) {
            case 1: // 成人票 - 无优惠
                finalPrice = basePrice;
                break;
            case 2: // 儿童票 - 5折
                finalPrice = basePrice.multiply(BigDecimal.valueOf(0.5));
                break;
            case 3: // 学生票 - 8折
                finalPrice = basePrice.multiply(BigDecimal.valueOf(0.8));
                break;
            case 4: // 残疾票 - 5折
                finalPrice = basePrice.multiply(BigDecimal.valueOf(0.5));
                break;
            case 5: // 军人票 - 5折
                finalPrice = basePrice.multiply(BigDecimal.valueOf(0.5));
                break;
            default:
                finalPrice = basePrice;
                break;
        }
        
        System.out.println("票种:" + ticketType + ", 最终票价:" + finalPrice + "元");
        return finalPrice;
    }
    
    private String generateTicketNumber() {
        return "T" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }
    
    /**
     * 回滚库存 - 当订单处理失败时，将Redis中的库存加回
     */
    private void rollbackInventory(OrderMessage orderMessage) {
        try {
            System.out.println("开始回滚库存: " + orderMessage.getOrderNumber());
            
            for (OrderMessage.PassengerInfo passengerInfo : orderMessage.getPassengers()) {
                boolean success = redisService.incrStock(
                    orderMessage.getTrainId(),
                    orderMessage.getDepartureStopId(),
                    orderMessage.getArrivalStopId(),
                    orderMessage.getTravelDate(),
                    passengerInfo.getCarriageTypeId(),
                    1
                );
                
                if (success) {
                    System.out.println("库存回滚成功: 车次" + orderMessage.getTrainId() + 
                                     ", 席别" + passengerInfo.getCarriageTypeId() + 
                                     ", 数量+1");
                } else {
                    System.err.println("库存回滚失败: 车次" + orderMessage.getTrainId() + 
                                     ", 席别" + passengerInfo.getCarriageTypeId());
                }
            }
            
            System.out.println("库存回滚完成: " + orderMessage.getOrderNumber());
        } catch (Exception e) {
            System.err.println("库存回滚异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 清理已创建的数据 - 当订单处理失败时，清理已创建的订单和车票
     */
    private void cleanupCreatedData(List<Ticket> createdTickets, Order createdOrder) {
        try {
            // 删除已创建的车票
            if (!createdTickets.isEmpty()) {
                ticketRepository.deleteAll(createdTickets);
                System.out.println("已删除 " + createdTickets.size() + " 张车票");
            }
            
            // 删除已创建的订单
            if (createdOrder != null) {
                orderRepository.delete(createdOrder);
                System.out.println("已删除订单: " + createdOrder.getOrderNumber());
            }
        } catch (Exception e) {
            System.err.println("清理已创建数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 