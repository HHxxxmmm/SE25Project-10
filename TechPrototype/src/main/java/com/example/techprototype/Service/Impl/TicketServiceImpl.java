package com.example.techprototype.Service.Impl;

import com.example.techprototype.DAO.TicketInventoryDAO;
import com.example.techprototype.DTO.*;
import com.example.techprototype.Entity.*;
import com.example.techprototype.Enums.OrderStatus;
import com.example.techprototype.Enums.TicketStatus;
import com.example.techprototype.Repository.OrderRepository;
import com.example.techprototype.Repository.SeatRepository;
import com.example.techprototype.Repository.StationRepository;
import com.example.techprototype.Repository.TicketRepository;
import com.example.techprototype.Repository.TrainCarriageRepository;
import com.example.techprototype.Repository.TrainStopRepository;
import com.example.techprototype.Repository.UserPassengerRelationRepository;
import com.example.techprototype.Service.RedisService;
import com.example.techprototype.Service.TicketService;
import com.example.techprototype.Service.OrderService;
import com.example.techprototype.Service.SeatService;
import com.example.techprototype.Service.TimeConflictService;
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
import java.util.Random;

@Service
public class TicketServiceImpl implements TicketService {
    
    @Autowired
    private RedisService redisService;
    
    @Autowired
    private TicketInventoryDAO ticketInventoryDAO;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private TicketRepository ticketRepository;
    
    @Autowired
    private SeatRepository seatRepository;
    
    @Autowired
    private TrainCarriageRepository trainCarriageRepository;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private StationRepository stationRepository;
    
    @Autowired
    private TrainStopRepository trainStopRepository;
    
    @Autowired
    private UserPassengerRelationRepository userPassengerRelationRepository;
    

    
    @Autowired
    private SeatService seatService;
    
    @Autowired
    private TimeConflictService timeConflictService;
    
    private final Random random = new Random();
    
    @Override
    public BookingResponse bookTickets(BookingRequest request) {
        String lockKey = "booking:" + request.getTrainId() + ":" + request.getTravelDate();
        
        try {
            // 获取分布式锁
            if (!redisService.tryLock(lockKey, 5, 30)) {
                return BookingResponse.failure("系统繁忙，请稍后重试");
            }
            
            // 1. 验证乘客关系
            for (BookingRequest.PassengerInfo passengerInfo : request.getPassengers()) {
                if (!userPassengerRelationRepository.existsByUserIdAndPassengerId(request.getUserId(), passengerInfo.getPassengerId())) {
                    return BookingResponse.failure("乘客ID " + passengerInfo.getPassengerId() + " 与用户无关联关系");
                }
            }
            
            // 2. 检查时间冲突
            for (BookingRequest.PassengerInfo passengerInfo : request.getPassengers()) {
                List<Ticket> conflictTickets = timeConflictService.checkTimeConflict(
                        passengerInfo.getPassengerId(),
                        request.getTravelDate(),
                        request.getTrainId(),
                        request.getDepartureStopId(),
                        request.getArrivalStopId()
                );
                
                if (!conflictTickets.isEmpty()) {
                    String conflictMessage = timeConflictService.generateConflictMessage(conflictTickets);
                    return BookingResponse.failure("乘客ID " + passengerInfo.getPassengerId() + " " + conflictMessage);
                }
            }
            
            // 3. 预减Redis库存
            int totalQuantity = request.getPassengers().size();
            if (!redisService.decrStock(request.getTrainId(), request.getDepartureStopId(), 
                    request.getArrivalStopId(), request.getTravelDate(), request.getCarriageTypeId(), totalQuantity)) {
                return BookingResponse.failure("余票不足");
            }
            
            // 4. 生成订单号
            String orderNumber = redisService.generateOrderNumber();
            
            // 5. 发送订单消息到RabbitMQ
            OrderMessage orderMessage = new OrderMessage();
            orderMessage.setUserId(request.getUserId());
            orderMessage.setTrainId(request.getTrainId());
            orderMessage.setDepartureStopId(request.getDepartureStopId());
            orderMessage.setArrivalStopId(request.getArrivalStopId());
            orderMessage.setTravelDate(request.getTravelDate());
            orderMessage.setCarriageTypeId(request.getCarriageTypeId());
            orderMessage.setOrderNumber(orderNumber);
            
            List<OrderMessage.PassengerInfo> passengerInfos = new ArrayList<>();
            for (BookingRequest.PassengerInfo passengerInfo : request.getPassengers()) {
                OrderMessage.PassengerInfo info = new OrderMessage.PassengerInfo();
                info.setPassengerId(passengerInfo.getPassengerId());
                info.setTicketType(passengerInfo.getTicketType());
                passengerInfos.add(info);
            }
            orderMessage.setPassengers(passengerInfos);
            
            System.out.println("发送订单消息到RabbitMQ: " + orderNumber);
            rabbitTemplate.convertAndSend("order.exchange", "order.create", orderMessage);
            System.out.println("订单消息发送成功: " + orderNumber);
            
            // 6. 返回订单号
            return BookingResponse.success(orderNumber, null, null, LocalDateTime.now());
            
        } finally {
            redisService.unlock(lockKey);
        }
    }
    
    @Override
    @Transactional
    public BookingResponse refundTickets(RefundRequest request) {
        String lockKey = "refund:" + request.getOrderNumber();
        
        try {
            if (!redisService.tryLock(lockKey, 5, 30)) {
                return BookingResponse.failure("系统繁忙，请稍后重试");
            }
            
            // 1. 查找订单
            Optional<Order> orderOpt = orderRepository.findByOrderNumberAndUserId(request.getOrderNumber(), request.getUserId());
            if (!orderOpt.isPresent()) {
                return BookingResponse.failure("订单不存在");
            }
            
            Order order = orderOpt.get();
            if (order.getOrderStatus() != OrderStatus.PAID.getCode()) {
                return BookingResponse.failure("订单状态不允许退票");
            }
            
            // 2. 查找车票
            List<Ticket> tickets = ticketRepository.findByOrderIdAndTicketIdIn(order.getOrderId(), request.getTicketIds());
            if (tickets.isEmpty()) {
                return BookingResponse.failure("车票不存在");
            }
            
            // 3. 检查车票状态
            for (Ticket ticket : tickets) {
                if (ticket.getTicketStatus() != (byte) TicketStatus.UNUSED.getCode()) {
                    return BookingResponse.failure("车票状态不允许退票");
                }
            }
            
            // 4. 更新车票状态
            for (Ticket ticket : tickets) {
                ticket.setTicketStatus((byte) TicketStatus.REFUNDED.getCode());
                ticketRepository.save(ticket);
            }
            
            // 5. 回滚Redis库存
            for (Ticket ticket : tickets) {
                redisService.incrStock(ticket.getTrainId(), ticket.getDepartureStopId(), 
                        ticket.getArrivalStopId(), ticket.getTravelDate(), ticket.getCarriageTypeId(), 1);
            }
            
            // 6. 释放座位
            for (Ticket ticket : tickets) {
                if (ticket.getSeatNumber() != null && ticket.getCarriageNumber() != null) {
                    seatService.releaseSeat(ticket);
                }
            }
            
            // 7. 更新订单总价
            BigDecimal refundedAmount = tickets.stream()
                    .map(Ticket::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            order.setTotalAmount(order.getTotalAmount().subtract(refundedAmount));
            orderRepository.save(order);
            
            // 8. 检查订单是否还有有效车票
            List<Ticket> validTickets = ticketRepository.findValidTicketsByOrderId(order.getOrderId());
            if (validTickets.isEmpty()) {
                // 订单中没有有效车票，将订单状态改为已取消
                order.setOrderStatus((byte) OrderStatus.CANCELLED.getCode());
                orderRepository.save(order);
                return BookingResponse.success("退票成功，订单已取消", null, null, LocalDateTime.now());
            }
            
            return BookingResponse.success("退票成功", null, null, LocalDateTime.now());
            
        } finally {
            redisService.unlock(lockKey);
        }
    }
    
    @Override
    @Transactional
    public BookingResponse changeTickets(ChangeTicketRequest request) {
        String lockKey = "change:" + request.getOriginalOrderNumber();
        
        try {
            if (!redisService.tryLock(lockKey, 5, 30)) {
                return BookingResponse.failure("系统繁忙，请稍后重试");
            }
            
            // 1. 查找原订单
            Optional<Order> originalOrderOpt = orderRepository.findByOrderNumberAndUserId(request.getOriginalOrderNumber(), request.getUserId());
            if (!originalOrderOpt.isPresent()) {
                return BookingResponse.failure("原订单不存在");
            }
            
            Order originalOrder = originalOrderOpt.get();
            if (originalOrder.getOrderStatus() != OrderStatus.PAID.getCode()) {
                return BookingResponse.failure("原订单状态不允许改签");
            }
            
            // 2. 查找要改签的车票
            List<Ticket> originalTickets = ticketRepository.findByOrderIdAndTicketIdIn(originalOrder.getOrderId(), request.getTicketIds());
            if (originalTickets.isEmpty()) {
                return BookingResponse.failure("车票不存在");
            }
            
            // 3. 验证车票状态
            for (Ticket ticket : originalTickets) {
                if (ticket.getTicketStatus() != (byte) TicketStatus.UNUSED.getCode()) {
                    return BookingResponse.failure("车票状态不允许改签");
                }
            }
            
            // 4. 检查时间冲突（排除原票）
            for (Ticket originalTicket : originalTickets) {
                List<Ticket> conflictTickets = timeConflictService.checkTimeConflict(
                        originalTicket.getPassengerId(),
                        request.getNewTravelDate(),
                        request.getNewTrainId(),
                        request.getNewDepartureStopId(),
                        request.getNewArrivalStopId(),
                        originalTicket.getTicketId() // 排除原票
                );
                
                if (!conflictTickets.isEmpty()) {
                    String conflictMessage = timeConflictService.generateConflictMessage(conflictTickets);
                    return BookingResponse.failure("乘客ID " + originalTicket.getPassengerId() + " " + conflictMessage);
                }
            }
            
            // 5. 验证改签的出发站和到达站城市是否与原票一致
            if (!validateChangeTicketCities(originalTickets.get(0), request.getNewDepartureStopId(), request.getNewArrivalStopId())) {
                return BookingResponse.failure("改签的出发站和到达站城市必须与原票一致");
            }
            
            // 6. 检查新车次余票
            int totalQuantity = originalTickets.size();
            if (!redisService.decrStock(request.getNewTrainId(), request.getNewDepartureStopId(), 
                    request.getNewArrivalStopId(), request.getNewTravelDate(), request.getNewCarriageTypeId(), totalQuantity)) {
                return BookingResponse.failure("新车次余票不足");
            }
            
            // 7. 创建新订单
            String newOrderNumber = generateOrderNumber();
            Order newOrder = new Order();
            newOrder.setOrderNumber(newOrderNumber);
            newOrder.setUserId(request.getUserId());
            newOrder.setOrderStatus((byte) OrderStatus.PENDING_PAYMENT.getCode());
            newOrder.setOrderTime(LocalDateTime.now());
            newOrder.setTotalAmount(BigDecimal.ZERO); // 临时设置为0，后面会计算
            
            orderRepository.save(newOrder);
            
            // 8. 生成新票并计算新订单总价
            BigDecimal newOrderTotalAmount = BigDecimal.ZERO;
            for (Ticket oldTicket : originalTickets) {
                // 生成新票
                Ticket newTicket = new Ticket();
                newTicket.setTicketNumber(generateTicketNumber());
                newTicket.setOrderId(newOrder.getOrderId());
                newTicket.setPassengerId(oldTicket.getPassengerId()); // 不允许修改乘车人
                newTicket.setTrainId(request.getNewTrainId());
                newTicket.setDepartureStopId(request.getNewDepartureStopId());
                newTicket.setArrivalStopId(request.getNewArrivalStopId());
                newTicket.setTravelDate(request.getNewTravelDate());
                newTicket.setCarriageTypeId(request.getNewCarriageTypeId());
                newTicket.setTicketType(oldTicket.getTicketType()); // 保持原票种
                
                // 计算新票价格
                BigDecimal newPrice = calculateNewTicketPrice(request.getNewTrainId(), request.getNewDepartureStopId(),
                        request.getNewArrivalStopId(), request.getNewTravelDate(), request.getNewCarriageTypeId(), oldTicket.getTicketType());
                newTicket.setPrice(newPrice);
                newTicket.setTicketStatus((byte) TicketStatus.PENDING.getCode());
                newTicket.setCreatedTime(LocalDateTime.now());
                
                // 分配座位
                seatService.assignSeat(newTicket);
                
                ticketRepository.save(newTicket);
                newOrderTotalAmount = newOrderTotalAmount.add(newPrice);
                
                // 记录改签配对关系：新票ID -> 原票ID:乘客ID
                String mappingKey = "change_mapping:" + newTicket.getTicketId();
                String mappingValue = oldTicket.getTicketId() + ":" + oldTicket.getPassengerId();
                redisService.setChangeMapping(mappingKey, mappingValue);
                
                System.out.println("记录改签配对关系: 新票" + newTicket.getTicketId() + " -> 原票" + oldTicket.getTicketId() + ":乘客" + oldTicket.getPassengerId());
            }
            
            // 9. 更新新订单总价
            newOrder.setTotalAmount(newOrderTotalAmount);
            orderRepository.save(newOrder);
            
            return BookingResponse.success("改签成功，新订单号: " + newOrderNumber + "，请及时支付", newOrder.getOrderId(), newOrder.getTotalAmount(), LocalDateTime.now());
            
        } finally {
            redisService.unlock(lockKey);
        }
    }
    
    private String generateTicketNumber() {
        return "T" + System.currentTimeMillis() + random.nextInt(1000);
    }
    
    private String generateOrderNumber() {
        return "O" + System.currentTimeMillis() + random.nextInt(1000);
    }
    
    private boolean validateStationsInSameCity(Long departureStopId, Long arrivalStopId) {
        try {
            // 这里需要根据实际的数据库结构来验证
            // 简化处理，假设所有站都在同一城市
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean validateChangeTicketCities(Ticket originalTicket, Long newDepartureStopId, Long newArrivalStopId) {
        try {
            // 这里需要根据实际的数据库结构来验证
            // 简化处理，假设改签的站与原票的站在同一城市
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private BigDecimal calculateNewTicketPrice(Integer trainId, Long departureStopId, Long arrivalStopId, 
                                             LocalDate travelDate, Integer carriageTypeId, Byte ticketType) {
        Optional<TicketInventory> inventoryOpt = ticketInventoryDAO.findByKey(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId);
        if (!inventoryOpt.isPresent()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal basePrice = inventoryOpt.get().getPrice();
        
        // 根据票种调整价格
        switch (ticketType) {
            case 1: // 成人票
                return basePrice;
            case 2: // 儿童票
                return basePrice.multiply(BigDecimal.valueOf(0.5));
            case 3: // 学生票
                return basePrice.multiply(BigDecimal.valueOf(0.8));
            case 4: // 残疾票
                return BigDecimal.ZERO;
            case 5: // 军人票
                return BigDecimal.ZERO;
            default:
                return basePrice;
        }
    }

    /**
     * 获取所有库存数据
     */
    public List<TicketInventory> getAllInventory() {
        return ticketInventoryDAO.findAll();
    }
    
    /**
     * 获取指定库存数据
     */
    public Optional<TicketInventory> getInventory(Integer trainId, Long departureStopId, Long arrivalStopId, 
                                                 LocalDate travelDate, Integer carriageTypeId) {
        return ticketInventoryDAO.findByKey(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId);
    }
    
    /**
     * 获取可用座位数
     */
    public Optional<Integer> getAvailableSeats(Integer trainId, Long departureStopId, Long arrivalStopId, 
                                              LocalDate travelDate, Integer carriageTypeId) {
        // 优先从Redis获取库存
        Optional<Integer> redisStock = redisService.getStock(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId);
        
        if (redisStock.isPresent()) {
            return redisStock;
        }
        
        // Redis没有数据，从数据库获取并回填到Redis
        Optional<TicketInventory> inventory = getInventory(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId);
        if (inventory.isPresent()) {
            int availableSeats = inventory.get().getAvailableSeats();
            redisService.setStock(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId, availableSeats);
            return Optional.of(availableSeats);
        }
        
        return Optional.empty();
    }
    
    /**
     * 预留座位
     */
    public boolean reserveSeats(Integer trainId, Long departureStopId, Long arrivalStopId, 
                               LocalDate travelDate, Integer carriageTypeId, int quantity) {
        // 使用Redis原子操作扣减库存
        boolean success = redisService.decrStock(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId, quantity);
        
        if (success) {
            System.out.println("Redis库存扣减成功: " + trainId + ":" + departureStopId + ":" + arrivalStopId + 
                             ":" + travelDate + ":" + carriageTypeId + ", 扣减数量: " + quantity);
        } else {
            System.out.println("Redis库存扣减失败: " + trainId + ":" + departureStopId + ":" + arrivalStopId + 
                             ":" + travelDate + ":" + carriageTypeId + ", 扣减数量: " + quantity);
        }
        
        return success;
    }
    
    /**
     * 释放座位
     */
    public void releaseSeats(Integer trainId, Long departureStopId, Long arrivalStopId, 
                            LocalDate travelDate, Integer carriageTypeId, int quantity) {
        // 使用Redis原子操作释放库存
        redisService.incrStock(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId, quantity);
        System.out.println("Redis库存释放成功: " + trainId + ":" + departureStopId + ":" + arrivalStopId + 
                         ":" + travelDate + ":" + carriageTypeId + ", 释放数量: " + quantity);
    }
    
    /**
     * 获取票价
     */
    public Optional<BigDecimal> getTicketPrice(Integer trainId, Long departureStopId, Long arrivalStopId, 
                                              LocalDate travelDate, Integer carriageTypeId) {
        Optional<TicketInventory> inventory = getInventory(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId);
        return inventory.map(TicketInventory::getPrice);
    }
    
    /**
     * 更新库存
     */
    public void updateInventory(Integer trainId, Long departureStopId, Long arrivalStopId, 
                               LocalDate travelDate, Integer carriageTypeId, int availableSeats) {
        // 更新Redis库存
        redisService.setStock(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId, availableSeats);
        
        // 同时更新数据库
        Optional<TicketInventory> inventory = getInventory(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId);
        if (inventory.isPresent()) {
            TicketInventory inv = inventory.get();
            inv.setAvailableSeats(availableSeats);
            inv.setCacheVersion(inv.getCacheVersion() + 1);
            inv.setDbVersion(inv.getDbVersion() + 1);
            ticketInventoryDAO.save(inv);
        }
    }
} 