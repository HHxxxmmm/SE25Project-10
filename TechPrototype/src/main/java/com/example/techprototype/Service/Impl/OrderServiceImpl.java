package com.example.techprototype.Service.Impl;

import com.example.techprototype.DAO.TicketInventoryDAO;
import com.example.techprototype.DTO.BookingRequest;
import com.example.techprototype.DTO.BookingResponse;
import com.example.techprototype.DTO.CancelOrderRequest;
import com.example.techprototype.DTO.OrderMessage;
import com.example.techprototype.Entity.Order;
import com.example.techprototype.Entity.Passenger;
import com.example.techprototype.Entity.Ticket;
import com.example.techprototype.Entity.TicketInventory;
import com.example.techprototype.Entity.User;
import com.example.techprototype.Entity.Seat;
import com.example.techprototype.Entity.TrainCarriage;
import com.example.techprototype.Enums.OrderStatus;
import com.example.techprototype.Enums.TicketStatus;
import com.example.techprototype.Repository.OrderRepository;
import com.example.techprototype.Repository.PassengerRepository;
import com.example.techprototype.Repository.TicketRepository;
import com.example.techprototype.Repository.UserRepository;
import com.example.techprototype.Repository.SeatRepository;
import com.example.techprototype.Repository.TrainCarriageRepository;
import com.example.techprototype.Service.OrderService;
import com.example.techprototype.Service.RedisService;
import com.example.techprototype.Service.TicketService;
import com.example.techprototype.Util.TicketNumberGenerator;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrderServiceImpl implements OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private TicketRepository ticketRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PassengerRepository passengerRepository;
    
    @Autowired
    private TicketService ticketService;
    
    @Autowired
    private RedisService redisService;
    
    @Autowired
    private TicketInventoryDAO ticketInventoryDAO;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    /**
     * 创建订单
     */
    public BookingResponse createOrder(BookingRequest request) {
        try {
            // 验证用户和乘客信息
            Optional<User> userOpt = userRepository.findById(request.getUserId());
            if (userOpt.isEmpty()) {
                return BookingResponse.failure("用户不存在");
            }
            
            // 验证乘客信息
            List<Passenger> passengers = new ArrayList<>();
            for (BookingRequest.PassengerInfo passengerInfo : request.getPassengers()) {
                Optional<Passenger> passengerOpt = passengerRepository.findById(passengerInfo.getPassengerId());
                if (passengerOpt.isEmpty()) {
                    return BookingResponse.failure("乘客信息不存在: " + passengerInfo.getPassengerId());
                }
                passengers.add(passengerOpt.get());
            }
            
            // 计算总金额 - 从库存信息获取基础票价，然后根据票种计算
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (BookingRequest.PassengerInfo passengerInfo : request.getPassengers()) {
                BigDecimal ticketPrice = calculateTicketPrice(
                    request.getTrainId(),
                    request.getDepartureStopId(),
                    request.getArrivalStopId(),
                    request.getTravelDate(),
                    request.getCarriageTypeId(),
                    passengerInfo.getTicketType()
                );
                totalAmount = totalAmount.add(ticketPrice);
            }
            
            // 创建订单消息发送到RabbitMQ
            List<OrderMessage.PassengerInfo> passengerInfos = request.getPassengers().stream()
                .map(p -> new OrderMessage.PassengerInfo(p.getPassengerId(), p.getTicketType()))
                .toList();
            
            OrderMessage orderMessage = new OrderMessage(
                request.getUserId(),
                request.getTrainId(),
                request.getDepartureStopId(),
                request.getArrivalStopId(),
                request.getTravelDate(),
                request.getCarriageTypeId(),
                passengerInfos,
                null
            );
            
            // 发送订单消息到RabbitMQ
            rabbitTemplate.convertAndSend("order.exchange", "order.create", orderMessage);
            
            return BookingResponse.success(null, null, totalAmount, LocalDateTime.now());
            
        } catch (Exception e) {
            System.err.println("创建订单失败: " + e.getMessage());
            return BookingResponse.failure("创建订单失败: " + e.getMessage());
        }
    }
    
    /**
     * 从消息创建订单
     */
    @Transactional
    public Order createOrderFromMessage(OrderMessage message) {
        try {
            // 创建订单
            Order order = new Order();
            order.setOrderNumber(redisService.generateOrderNumber());
            order.setUserId(message.getUserId());
            order.setOrderTime(LocalDateTime.now());
            // 计算总金额
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (OrderMessage.PassengerInfo passengerInfo : message.getPassengers()) {
                BigDecimal ticketPrice = calculateTicketPrice(
                    message.getTrainId(),
                    message.getDepartureStopId(),
                    message.getArrivalStopId(),
                    message.getTravelDate(),
                    message.getCarriageTypeId(),
                    passengerInfo.getTicketType()
                );
                totalAmount = totalAmount.add(ticketPrice);
            }
            order.setTotalAmount(totalAmount);
            order.setOrderStatus((byte) 0); // 待支付
            
            order = orderRepository.save(order);
            
            // 创建车票
            List<Ticket> tickets = new ArrayList<>();
            for (OrderMessage.PassengerInfo passengerInfo : message.getPassengers()) {
                Ticket ticket = new Ticket();
                ticket.setTicketNumber(TicketNumberGenerator.generateTicketNumber());
                ticket.setOrderId(order.getOrderId());
                ticket.setPassengerId(passengerInfo.getPassengerId());
                ticket.setTrainId(message.getTrainId());
                ticket.setDepartureStopId(message.getDepartureStopId());
                ticket.setArrivalStopId(message.getArrivalStopId());
                ticket.setTravelDate(message.getTravelDate());
                ticket.setCarriageTypeId(message.getCarriageTypeId());
                ticket.setPrice(calculateTicketPrice(
                    message.getTrainId(),
                    message.getDepartureStopId(),
                    message.getArrivalStopId(),
                    message.getTravelDate(),
                    message.getCarriageTypeId(),
                    passengerInfo.getTicketType()
                ));
                ticket.setTicketStatus((byte) 0); // 待支付
                ticket.setTicketType(passengerInfo.getTicketType());
                
                ticket = ticketRepository.save(ticket);
                tickets.add(ticket);
            }
            
            // 直接操作数据库，不需要Redis缓存
            
            System.out.println("订单创建成功: " + order.getOrderNumber() + ", 车票数量: " + tickets.size());
            
            return order;
            
        } catch (Exception e) {
            System.err.println("创建订单失败: " + e.getMessage());
            throw e;
        }
    }
    
    @Override
    public BookingResponse cancelOrder(CancelOrderRequest request) {
        try {
            // 直接从数据库获取订单
            Optional<Order> orderOpt = orderRepository.findByOrderNumber(request.getOrderNumber());
            
            if (orderOpt.isEmpty()) {
                return BookingResponse.failure("订单不存在");
            }
            
            Order order = orderOpt.get();
            
            // 检查订单状态
            if (order.getOrderStatus() != 0 && order.getOrderStatus() != 1) { // 0-待支付, 1-已支付
                return BookingResponse.failure("订单状态不允许取消");
            }
            
            // 直接从数据库获取车票
            List<Ticket> tickets = ticketRepository.findByOrderId(order.getOrderId());
            
            // 更新车票状态
            for (Ticket ticket : tickets) {
                ticket.setTicketStatus((byte) 3); // 已退票
                ticketRepository.save(ticket);
            }
            
            // 更新订单状态和总价
            order.setOrderStatus((byte) 3); // 已取消
            order.setTotalAmount(BigDecimal.ZERO); // 取消订单总价归零
            orderRepository.save(order);
            
            // 直接操作数据库，不需要Redis缓存
            
            return BookingResponse.success(order.getOrderNumber(), order.getOrderId(), BigDecimal.ZERO, order.getOrderTime());
            
        } catch (Exception e) {
            System.err.println("取消订单失败: " + e.getMessage());
            return BookingResponse.failure("取消订单失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据订单号获取订单
     */
    public Optional<Order> getOrderByNumber(String orderNumber) {
        // 直接从数据库获取
        return orderRepository.findByOrderNumber(orderNumber);
    }
    
    /**
     * 根据用户ID获取订单列表
     */
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findAll().stream()
            .filter(order -> order.getUserId().equals(userId))
            .toList();
    }
    
    /**
     * 计算票价 - 从库存信息获取基础票价，然后根据票种计算优惠
     */
    private BigDecimal calculateTicketPrice(Integer trainId, Long departureStopId, Long arrivalStopId, 
                                          LocalDate travelDate, Integer carriageTypeId, Byte ticketType) {
        // 从库存信息获取基础票价
        Optional<TicketInventory> inventory = ticketInventoryDAO.findByKey(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId);
        if (inventory.isEmpty()) {
            return BigDecimal.valueOf(100.0); // 默认票价
        }
        
        BigDecimal basePrice = inventory.get().getPrice();
        
        // 根据票种计算优惠
        switch (ticketType) {
            case 1: // 成人票 - 无优惠
                return basePrice;
            case 2: // 儿童票 - 5折
                return basePrice.multiply(BigDecimal.valueOf(0.5));
            case 3: // 学生票 - 8折
                return basePrice.multiply(BigDecimal.valueOf(0.8));
            case 4: // 残疾票 - 5折
                return basePrice.multiply(BigDecimal.valueOf(0.5));
            case 5: // 军人票 - 5折
                return basePrice.multiply(BigDecimal.valueOf(0.5));
            default:
                return basePrice;
        }
    }
} 