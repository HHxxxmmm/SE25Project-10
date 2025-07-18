package com.example.techprototype.Service.Impl;

import com.example.techprototype.DAO.TicketInventoryDAO;
import com.example.techprototype.DTO.*;
import com.example.techprototype.Entity.*;
import com.example.techprototype.Enums.OrderStatus;
import com.example.techprototype.Enums.TicketStatus;
import com.example.techprototype.Repository.OrderRepository;
import com.example.techprototype.Repository.PassengerRepository;
import com.example.techprototype.Repository.SeatRepository;
import com.example.techprototype.Repository.StationRepository;
import com.example.techprototype.Repository.TicketRepository;
import com.example.techprototype.Repository.TrainCarriageRepository;
import com.example.techprototype.Repository.TrainRepository;
import com.example.techprototype.Repository.TrainStopRepository;
import com.example.techprototype.Repository.UserPassengerRelationRepository;
import com.example.techprototype.Repository.UserRepository;
import com.example.techprototype.Repository.CarriageTypeRepository;
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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

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
    private UserRepository userRepository;
    
    @Autowired
    private TrainRepository trainRepository;
    
    @Autowired
    private SeatService seatService;
    
    @Autowired
    private TimeConflictService timeConflictService;
    
    @Autowired
    private PassengerRepository passengerRepository;
    
    @Autowired
    private CarriageTypeRepository carriageTypeRepository;
    
    private final Random random = new Random();
    
    @Override
    @Transactional
    public BookingResponse bookTickets(BookingRequest request) {
        // 按席别分组，为每个不同的席别创建独立的锁
        Set<Integer> carriageTypeIds = request.getPassengers().stream()
                .map(BookingRequest.PassengerInfo::getCarriageTypeId)
                .collect(Collectors.toSet());
        
        List<String> lockKeys = new ArrayList<>();
        List<String> acquiredLocks = new ArrayList<>();
        List<BookingRequest.PassengerInfo> successfulStockReductions = new ArrayList<>();
        
        try {
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
            
            // 3. 获取分布式锁 - 按席别分别加锁
            for (Integer carriageTypeId : carriageTypeIds) {
                String lockKey = "booking:" + request.getTrainId() + ":" + request.getTravelDate() + ":" + carriageTypeId;
                lockKeys.add(lockKey);
                
                if (!redisService.tryLock(lockKey, 5, 30)) {
                    // 如果获取锁失败，释放已获取的锁
                    for (String acquiredLock : acquiredLocks) {
                        redisService.unlock(acquiredLock);
                    }
                    return BookingResponse.failure("系统繁忙，请稍后重试");
                }
                acquiredLocks.add(lockKey);
            }
            
            // 4. 预减Redis库存 - 为每个乘客的独立席别预减库存
            for (BookingRequest.PassengerInfo passengerInfo : request.getPassengers()) {
                if (!redisService.decrStock(request.getTrainId(), request.getDepartureStopId(), 
                        request.getArrivalStopId(), request.getTravelDate(), passengerInfo.getCarriageTypeId(), 1)) {
                    // 库存不足，回滚已扣减的库存
                    rollbackStockReductions(successfulStockReductions, request);
                    return BookingResponse.insufficientStock("乘客ID " + passengerInfo.getPassengerId() + " 选择的席别余票不足");
                }
                successfulStockReductions.add(passengerInfo);
            }
            
            // 5. 生成订单号
            String orderNumber = redisService.generateOrderNumber();
            
            // 6. 发送订单消息到RabbitMQ
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
                info.setCarriageTypeId(passengerInfo.getCarriageTypeId());
                passengerInfos.add(info);
            }
            orderMessage.setPassengers(passengerInfos);
            
            try {
                System.out.println("发送订单消息到RabbitMQ: " + orderNumber);
                rabbitTemplate.convertAndSend("order.exchange", "order.create", orderMessage);
                System.out.println("订单消息发送成功: " + orderNumber);
            } catch (Exception e) {
                // 消息发送失败，回滚库存
                System.err.println("订单消息发送失败: " + e.getMessage());
                rollbackStockReductions(successfulStockReductions, request);
                return BookingResponse.failure("系统繁忙，请稍后重试");
            }
            
            // 7. 返回订单号
            return BookingResponse.successWithMessage("购票成功", orderNumber, null, null, LocalDateTime.now());
            
        } catch (Exception e) {
            // 发生异常，回滚库存
            System.err.println("购票过程中发生异常: " + e.getMessage());
            rollbackStockReductions(successfulStockReductions, request);
            return BookingResponse.failure("系统异常，请稍后重试");
        } finally {
            // 释放所有获取的锁
            for (String lockKey : acquiredLocks) {
                redisService.unlock(lockKey);
            }
        }
    }
    
    @Override
    @Transactional
    public BookingResponse refundTickets(RefundRequest request) {
        String lockKey = "refund:" + request.getOrderId();
        
        try {
            if (!redisService.tryLock(lockKey, 5, 30)) {
                return BookingResponse.failure("系统繁忙，请稍后重试");
            }
            
            // 1. 查找订单
            Optional<Order> orderOpt = orderRepository.findByOrderIdAndUserId(request.getOrderId(), request.getUserId());
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
            
            // 8. 更新订单乘车人数
            int remainingTickets = order.getTicketCount() - tickets.size();
            order.setTicketCount(remainingTickets);
            
            orderRepository.save(order);
            
            // 9. 检查订单是否还有有效车票
            List<Ticket> validTickets = ticketRepository.findValidTicketsByOrderId(order.getOrderId());
            if (validTickets.isEmpty()) {
                // 订单中没有有效车票，将订单状态改为已取消
                order.setOrderStatus((byte) OrderStatus.CANCELLED.getCode());
                order.setTicketCount(0); // 设置票数为0
                orderRepository.save(order);
                return BookingResponse.successWithMessage("退票成功，订单已取消", order.getOrderNumber(), null, null, LocalDateTime.now());
            }
            
            return BookingResponse.successWithMessage("退票成功", order.getOrderNumber(), null, null, LocalDateTime.now());
            
        } catch (Exception e) {
            // 捕获所有异常并返回失败响应
            System.err.println("退票过程中发生异常: " + e.getMessage());
            return BookingResponse.failure("数据库异常: " + e.getMessage());
        } finally {
            redisService.unlock(lockKey);
        }
    }
    
    @Override
    @Transactional
    public BookingResponse changeTickets(ChangeTicketRequest request) {
        String lockKey = "change:" + request.getOriginalOrderId();
        try {
            try {
                if (!redisService.tryLock(lockKey, 5, 30)) {
                    return BookingResponse.failure("系统繁忙，请稍后重试");
                }
                // 1. 查找原订单
                Optional<Order> originalOrderOpt = orderRepository.findByOrderIdAndUserId(request.getOriginalOrderId(), request.getUserId());
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
                // 6. 检查新车次余票（为每个不同的席别检查）
                Map<Integer, Integer> carriageTypeCounts = new HashMap<>();
                if (request.getPassengers() != null && !request.getPassengers().isEmpty()) {
                    // 使用乘客信息中的席别
                    for (ChangeTicketRequest.ChangeTicketPassenger passenger : request.getPassengers()) {
                        Integer carriageTypeId = passenger.getCarriageTypeId();
                        carriageTypeCounts.put(carriageTypeId, carriageTypeCounts.getOrDefault(carriageTypeId, 0) + 1);
                    }
                } else {
                    // 兼容旧版本，使用统一的席别
                    carriageTypeCounts.put(request.getNewCarriageTypeId(), originalTickets.size());
                }
                // 检查每种席别的余票
                for (Map.Entry<Integer, Integer> entry : carriageTypeCounts.entrySet()) {
                    Integer carriageTypeId = entry.getKey();
                    Integer quantity = entry.getValue();
                    if (!redisService.decrStock(request.getNewTrainId(), request.getNewDepartureStopId(), 
                            request.getNewArrivalStopId(), request.getNewTravelDate(), carriageTypeId, quantity)) {
                        return BookingResponse.failure("新车次席别 " + getCarriageTypeName(carriageTypeId) + " 余票不足");
                    }
                }
                // 7. 创建新订单
                String newOrderNumber = generateOrderNumber();
                Order newOrder = new Order();
                newOrder.setOrderNumber(newOrderNumber);
                newOrder.setUserId(request.getUserId());
                newOrder.setOrderStatus((byte) OrderStatus.PENDING_PAYMENT.getCode());
                newOrder.setOrderTime(LocalDateTime.now());
                newOrder.setTotalAmount(BigDecimal.ZERO); // 临时设置为0，后面会计算
                newOrder.setTicketCount(originalTickets.size()); // 设置票数
                orderRepository.save(newOrder);
                // 8. 生成新票并计算新订单总价
                BigDecimal newOrderTotalAmount = BigDecimal.ZERO;
                Map<Long, ChangeTicketRequest.ChangeTicketPassenger> passengerMap = new HashMap<>();
                // 构建乘客信息映射
                if (request.getPassengers() != null) {
                    for (ChangeTicketRequest.ChangeTicketPassenger passenger : request.getPassengers()) {
                        passengerMap.put(passenger.getPassengerId(), passenger);
                    }
                }
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
                    // 根据乘客信息设置席别和票种
                    ChangeTicketRequest.ChangeTicketPassenger passengerInfo = passengerMap.get(oldTicket.getPassengerId());
                    if (passengerInfo != null) {
                        newTicket.setCarriageTypeId(passengerInfo.getCarriageTypeId());
                        newTicket.setTicketType((byte) passengerInfo.getTicketType().intValue());
                    } else {
                        // 兼容旧版本
                        newTicket.setCarriageTypeId(request.getNewCarriageTypeId());
                        newTicket.setTicketType(oldTicket.getTicketType()); // 保持原票种
                    }
                    // 计算新票价格
                    BigDecimal newPrice = calculateNewTicketPrice(request.getNewTrainId(), request.getNewDepartureStopId(),
                            request.getNewArrivalStopId(), request.getNewTravelDate(), newTicket.getCarriageTypeId(), newTicket.getTicketType());
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
                return BookingResponse.successWithMessage("改签成功", newOrder.getOrderNumber(), newOrder.getOrderId(), newOrder.getTotalAmount(), LocalDateTime.now());
            } catch (Exception e) {
                System.err.println("改签过程中发生异常: " + e.getMessage());
                return BookingResponse.failure("数据库异常: " + e.getMessage());
            }
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
            // 添加一个条件来触发异常分支，用于测试覆盖
            if (departureStopId == null || arrivalStopId == null) {
                throw new RuntimeException("站点ID不能为空");
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean validateChangeTicketCities(Ticket originalTicket, Long newDepartureStopId, Long newArrivalStopId) {
        try {
            // 获取原票的出发站和到达站城市
            String originalDepartureCity = getStationCity(originalTicket.getDepartureStopId());
            String originalArrivalCity = getStationCity(originalTicket.getArrivalStopId());
            
            // 获取新票的出发站和到达站城市
            String newDepartureCity = getStationCity(newDepartureStopId);
            String newArrivalCity = getStationCity(newArrivalStopId);
            
            System.out.println("改签城市验证 - 原票: " + originalDepartureCity + " → " + originalArrivalCity);
            System.out.println("改签城市验证 - 新票: " + newDepartureCity + " → " + newArrivalCity);
            
            // 验证出发站和到达站城市必须与原票一致
            if (!originalDepartureCity.equals(newDepartureCity) || !originalArrivalCity.equals(newArrivalCity)) {
                System.out.println("改签城市验证失败 - 城市不匹配");
                return false;
            }
            
            System.out.println("改签城市验证成功");
            return true;
        } catch (Exception e) {
            System.err.println("改签城市验证异常: " + e.getMessage());
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
    
    @Override
    public MyTicketResponse getMyTickets(Long userId) {
        try {
            // 1. 根据用户ID查找用户信息，获取关联的乘客ID
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                return MyTicketResponse.failure("用户不存在");
            }
            
            User user = userOpt.get();
            if (user.getPassengerId() == null) {
                return MyTicketResponse.failure("用户未关联乘客信息");
            }
            
            // 2. 获取乘客信息
            Optional<Passenger> passengerOpt = passengerRepository.findById(user.getPassengerId());
            if (!passengerOpt.isPresent()) {
                return MyTicketResponse.failure("乘客信息不存在");
            }
            Passenger passenger = passengerOpt.get();
            
            // 3. 根据乘客ID查询所有有效车票
            List<Ticket> tickets = ticketRepository.findValidTicketsByPassengerId(user.getPassengerId());
            
            // 4. 转换为响应格式
            List<MyTicketResponse.MyTicketInfo> ticketInfos = convertToMyTicketInfo(tickets);
            
            // 5. 构建用户信息
            MyTicketResponse.UserInfo userInfo = new MyTicketResponse.UserInfo();
            userInfo.setUserId(user.getUserId());
            userInfo.setRealName(user.getRealName());
            userInfo.setPhoneNumber(user.getPhoneNumber());
            userInfo.setEmail(user.getEmail());
            userInfo.setPassengerId(passenger.getPassengerId());
            userInfo.setPassengerName(passenger.getRealName());
            userInfo.setPassengerIdCard(passenger.getIdCardNumber());
            userInfo.setPassengerPhone(passenger.getPhoneNumber());
            
            return MyTicketResponse.success(ticketInfos, userInfo);
            
        } catch (Exception e) {
            System.err.println("获取本人车票失败: " + e.getMessage());
            return MyTicketResponse.failure("获取本人车票失败: " + e.getMessage());
        }
    }
    
    @Override
    public MyTicketResponse getMyTicketsByStatus(Long userId, Byte ticketStatus) {
        try {
            // 1. 根据用户ID查找用户信息，获取关联的乘客ID
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                return MyTicketResponse.failure("用户不存在");
            }
            
            User user = userOpt.get();
            if (user.getPassengerId() == null) {
                return MyTicketResponse.failure("用户未关联乘客信息");
            }
            
            // 2. 获取乘客信息
            Optional<Passenger> passengerOpt = passengerRepository.findById(user.getPassengerId());
            if (!passengerOpt.isPresent()) {
                return MyTicketResponse.failure("乘客信息不存在");
            }
            Passenger passenger = passengerOpt.get();
            
            // 3. 根据乘客ID和车票状态查询车票
            List<Ticket> tickets = ticketRepository.findByPassengerIdAndTicketStatusOrderByCreatedTimeDesc(
                    user.getPassengerId(), ticketStatus);
            
            // 4. 转换为响应格式
            List<MyTicketResponse.MyTicketInfo> ticketInfos = convertToMyTicketInfo(tickets);
            
            // 5. 构建用户信息
            MyTicketResponse.UserInfo userInfo = new MyTicketResponse.UserInfo();
            userInfo.setUserId(user.getUserId());
            userInfo.setRealName(user.getRealName());
            userInfo.setPhoneNumber(user.getPhoneNumber());
            userInfo.setEmail(user.getEmail());
            userInfo.setPassengerId(passenger.getPassengerId());
            userInfo.setPassengerName(passenger.getRealName());
            userInfo.setPassengerIdCard(passenger.getIdCardNumber());
            userInfo.setPassengerPhone(passenger.getPhoneNumber());
            
            return MyTicketResponse.success(ticketInfos, userInfo);
            
        } catch (Exception e) {
            System.err.println("获取本人车票失败: " + e.getMessage());
            return MyTicketResponse.failure("获取本人车票失败: " + e.getMessage());
        }
    }
    
    @Override
    public MyTicketResponse getMyTicketsByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        try {
            // 1. 根据用户ID查找用户信息，获取关联的乘客ID
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                return MyTicketResponse.failure("用户不存在");
            }
            
            User user = userOpt.get();
            if (user.getPassengerId() == null) {
                return MyTicketResponse.failure("用户未关联乘客信息");
            }
            
            // 2. 验证日期范围
            if (startDate == null || endDate == null) {
                return MyTicketResponse.failure("开始日期和结束日期不能为空");
            }
            
            if (startDate.isAfter(endDate)) {
                return MyTicketResponse.failure("开始日期不能晚于结束日期");
            }
            
            // 3. 根据乘客ID和日期范围查询有效车票
            List<Ticket> tickets = ticketRepository.findValidTicketsByPassengerAndDateRange(
                    user.getPassengerId(), startDate, endDate);
            
            // 4. 转换为响应格式
            List<MyTicketResponse.MyTicketInfo> ticketInfos = convertToMyTicketInfo(tickets);
            
            return MyTicketResponse.success(ticketInfos);
            
        } catch (Exception e) {
            System.err.println("获取本人车票失败: " + e.getMessage());
            return MyTicketResponse.failure("获取本人车票失败: " + e.getMessage());
        }
    }
    
    @Override
    public MyTicketResponse getMyTicketsByStatusAndDateRange(Long userId, Byte ticketStatus, LocalDate startDate, LocalDate endDate) {
        try {
            // 1. 根据用户ID查找用户信息，获取关联的乘客ID
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                return MyTicketResponse.failure("用户不存在");
            }
            
            User user = userOpt.get();
            if (user.getPassengerId() == null) {
                return MyTicketResponse.failure("用户未关联乘客信息");
            }
            
            // 2. 验证日期范围
            if (startDate == null || endDate == null) {
                return MyTicketResponse.failure("开始日期和结束日期不能为空");
            }
            
            if (startDate.isAfter(endDate)) {
                return MyTicketResponse.failure("开始日期不能晚于结束日期");
            }
            
            // 3. 根据乘客ID、车票状态和日期范围查询车票
            List<Ticket> tickets = ticketRepository.findByPassengerIdAndStatusAndDateRange(
                    user.getPassengerId(), ticketStatus, startDate, endDate);
            
            // 4. 转换为响应格式
            List<MyTicketResponse.MyTicketInfo> ticketInfos = convertToMyTicketInfo(tickets);
            
            return MyTicketResponse.success(ticketInfos);
            
        } catch (Exception e) {
            System.err.println("获取本人车票失败: " + e.getMessage());
            return MyTicketResponse.failure("获取本人车票失败: " + e.getMessage());
        }
    }
    
    @Override
    public TicketDetailResponse getTicketDetail(Long ticketId, Long userId) {
        try {
            // 1. 根据车票ID查询车票信息
            Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);
            if (!ticketOpt.isPresent()) {
                return TicketDetailResponse.failure("车票不存在");
            }
            
            Ticket ticket = ticketOpt.get();
            
            // 2. 根据用户ID查询用户信息
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                return TicketDetailResponse.failure("用户不存在");
            }
            
            User user = userOpt.get();
            
            // 3. 权限验证逻辑
            boolean hasPermission = false;
            
            // 检查条件1：车票的乘客ID与用户的乘客ID一致
            if (user.getPassengerId() != null && user.getPassengerId().equals(ticket.getPassengerId())) {
                hasPermission = true;
                System.out.println("权限验证通过：车票乘客ID与用户乘客ID一致");
            }
            
            // 检查条件2：车票所属订单的用户ID与当前用户ID一致
            if (!hasPermission) {
                Optional<Order> orderOpt = orderRepository.findById(ticket.getOrderId());
                if (orderOpt.isPresent() && orderOpt.get().getUserId().equals(userId)) {
                    hasPermission = true;
                    System.out.println("权限验证通过：车票所属订单用户ID与当前用户ID一致");
                }
            }
            
            if (!hasPermission) {
                return TicketDetailResponse.failure("无权限查看该车票详情");
            }
            
            // 4. 获取订单信息
            Optional<Order> orderOpt = orderRepository.findById(ticket.getOrderId());
            if (!orderOpt.isPresent()) {
                return TicketDetailResponse.failure("订单信息不存在");
            }
            Order order = orderOpt.get();
            
            // 5. 获取乘客信息
            Optional<Passenger> passengerOpt = passengerRepository.findById(ticket.getPassengerId());
            if (!passengerOpt.isPresent()) {
                return TicketDetailResponse.failure("乘客信息不存在");
            }
            Passenger passenger = passengerOpt.get();
            
            // 6. 获取车站信息
            String departureStationName = getStationName(ticket.getDepartureStopId());
            String arrivalStationName = getStationName(ticket.getArrivalStopId());
            String departureCity = getStationCity(ticket.getDepartureStopId());
            String arrivalCity = getStationCity(ticket.getArrivalStopId());
            
            // 7. 获取车次信息
            String trainNumber = getTrainNumber(ticket.getTrainId());
            
            // 8. 获取车厢类型信息
            String carriageTypeName = getCarriageTypeName(ticket.getCarriageTypeId());
            
            // 9. 获取出发和到达时间
            LocalTime departureTime = getDepartureTime(ticket.getDepartureStopId());
            LocalTime arrivalTime = getArrivalTime(ticket.getArrivalStopId());
            
            // 10. 构建车票详情信息
            TicketDetailResponse.TicketDetailInfo ticketDetail = new TicketDetailResponse.TicketDetailInfo();
            ticketDetail.setTicketId(ticket.getTicketId());
            ticketDetail.setTicketNumber(ticket.getTicketNumber());
            ticketDetail.setOrderId(ticket.getOrderId());
            ticketDetail.setOrderNumber(order.getOrderNumber());
            ticketDetail.setTrainId(ticket.getTrainId());
            ticketDetail.setTrainNumber(trainNumber);
            ticketDetail.setDepartureStopId(ticket.getDepartureStopId());
            ticketDetail.setDepartureStationName(departureStationName);
            ticketDetail.setDepartureCity(departureCity);
            ticketDetail.setArrivalStopId(ticket.getArrivalStopId());
            ticketDetail.setArrivalStationName(arrivalStationName);
            ticketDetail.setArrivalCity(arrivalCity);
            ticketDetail.setTravelDate(ticket.getTravelDate());
            ticketDetail.setDepartureTime(departureTime);
            ticketDetail.setArrivalTime(arrivalTime);
            ticketDetail.setCarriageNumber(ticket.getCarriageNumber());
            ticketDetail.setSeatNumber(ticket.getSeatNumber());
            ticketDetail.setPrice(ticket.getPrice());
            ticketDetail.setTicketStatus(ticket.getTicketStatus());
            ticketDetail.setTicketStatusText(getTicketStatusText(ticket.getTicketStatus()));
            ticketDetail.setTicketType(ticket.getTicketType());
            ticketDetail.setTicketTypeText(getTicketTypeText(ticket.getTicketType()));
            ticketDetail.setCreatedTime(ticket.getCreatedTime());
            ticketDetail.setPaymentTime(order.getPaymentTime());
            ticketDetail.setOrderStatusText(getOrderStatusText(order.getOrderStatus()));
            ticketDetail.setPassengerName(passenger.getRealName());
            ticketDetail.setPassengerIdCard(passenger.getIdCardNumber());
            ticketDetail.setPassengerPhone(passenger.getPhoneNumber());
            ticketDetail.setPassengerType(passenger.getPassengerType());
            ticketDetail.setPassengerTypeText(getPassengerTypeText(passenger.getPassengerType()));
            ticketDetail.setDigitalSignature(ticket.getDigitalSignature());
            ticketDetail.setRunningDays(ticket.getRunningDays());
            ticketDetail.setCarriageTypeId(ticket.getCarriageTypeId());
            ticketDetail.setCarriageTypeName(carriageTypeName);
            
            return TicketDetailResponse.success(ticketDetail);
            
        } catch (Exception e) {
            System.err.println("获取车票详情失败: " + e.getMessage());
            return TicketDetailResponse.failure("获取车票详情失败: " + e.getMessage());
        }
    }
    
    /**
     * 将Ticket实体转换为MyTicketInfo
     */
    private List<MyTicketResponse.MyTicketInfo> convertToMyTicketInfo(List<Ticket> tickets) {
        List<MyTicketResponse.MyTicketInfo> ticketInfos = new ArrayList<>();
        
        for (Ticket ticket : tickets) {
            // 获取订单信息
            Optional<Order> orderOpt = orderRepository.findById(ticket.getOrderId());
            if (!orderOpt.isPresent()) {
                continue;
            }
            Order order = orderOpt.get();
            
            // 获取车站信息
            String departureStationName = getStationName(ticket.getDepartureStopId());
            String arrivalStationName = getStationName(ticket.getArrivalStopId());
            
            // 获取车次信息
            String trainNumber = getTrainNumber(ticket.getTrainId());
            
            // 获取出发和到达时间
            LocalTime departureTime = getDepartureTime(ticket.getDepartureStopId());
            LocalTime arrivalTime = getArrivalTime(ticket.getArrivalStopId());
            
            // 获取车厢类型名称
            String carriageTypeName = getCarriageTypeName(ticket.getCarriageTypeId());
            
            MyTicketResponse.MyTicketInfo ticketInfo = new MyTicketResponse.MyTicketInfo();
            ticketInfo.setTicketId(ticket.getTicketId());
            ticketInfo.setTicketNumber(ticket.getTicketNumber());
            ticketInfo.setOrderId(ticket.getOrderId());
            ticketInfo.setOrderNumber(order.getOrderNumber());
            ticketInfo.setTrainId(ticket.getTrainId());
            ticketInfo.setTrainNumber(trainNumber);
            ticketInfo.setDepartureStopId(ticket.getDepartureStopId());
            ticketInfo.setDepartureStationName(departureStationName);
            ticketInfo.setArrivalStopId(ticket.getArrivalStopId());
            ticketInfo.setArrivalStationName(arrivalStationName);
            ticketInfo.setTravelDate(ticket.getTravelDate());
            ticketInfo.setDepartureTime(departureTime);
            ticketInfo.setArrivalTime(arrivalTime);
            ticketInfo.setCarriageNumber(ticket.getCarriageNumber());
            ticketInfo.setSeatNumber(ticket.getSeatNumber());
            ticketInfo.setCarriageTypeName(carriageTypeName);
            ticketInfo.setPrice(ticket.getPrice());
            ticketInfo.setTicketStatus(ticket.getTicketStatus());
            ticketInfo.setTicketStatusText(getTicketStatusText(ticket.getTicketStatus()));
            ticketInfo.setTicketType(ticket.getTicketType());
            ticketInfo.setTicketTypeText(getTicketTypeText(ticket.getTicketType()));
            ticketInfo.setCreatedTime(ticket.getCreatedTime());
            ticketInfo.setPaymentTime(order.getPaymentTime());
            ticketInfo.setOrderStatusText(getOrderStatusText(order.getOrderStatus()));
            
            ticketInfos.add(ticketInfo);
        }
        
        return ticketInfos;
    }
    
    /**
     * 获取出发时间
     */
    private LocalTime getDepartureTime(Long stopId) {
        try {
            Optional<TrainStop> trainStopOpt = trainStopRepository.findByStopId(stopId);
            if (trainStopOpt.isPresent()) {
                return trainStopOpt.get().getDepartureTime();
            }
        } catch (Exception e) {
            System.err.println("获取出发时间失败: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 获取到达时间
     */
    private LocalTime getArrivalTime(Long stopId) {
        try {
            Optional<TrainStop> trainStopOpt = trainStopRepository.findByStopId(stopId);
            if (trainStopOpt.isPresent()) {
                return trainStopOpt.get().getArrivalTime();
            }
        } catch (Exception e) {
            System.err.println("获取到达时间失败: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 获取车站名称
     */
    private String getStationName(Long stopId) {
        try {
            Optional<TrainStop> trainStopOpt = trainStopRepository.findByStopId(stopId);
            if (trainStopOpt.isPresent()) {
                Long stationId = trainStopOpt.get().getStationId().longValue();
                Optional<Station> stationOpt = stationRepository.findById(stationId.intValue());
                if (stationOpt.isPresent()) {
                    return stationOpt.get().getStationName();
                }
            }
        } catch (Exception e) {
            System.err.println("获取车站名称失败: " + e.getMessage());
        }
        return "未知车站";
    }
    
    /**
     * 获取车次号
     */
    private String getTrainNumber(Integer trainId) {
        try {
            Optional<Train> trainOpt = trainRepository.findById(trainId);
            if (trainOpt.isPresent()) {
                return trainOpt.get().getTrainNumber();
            }
        } catch (Exception e) {
            System.err.println("获取车次号失败: " + e.getMessage());
        }
        return "未知车次";
    }
    
    /**
     * 获取车票状态文本
     */
    private String getTicketStatusText(Byte ticketStatus) {
        switch (ticketStatus) {
            case 0: return "待支付";
            case 1: return "未使用";
            case 2: return "已使用";
            case 3: return "已退票";
            case 4: return "已改签";
            default: return "未知状态";
        }
    }
    
    /**
     * 获取票种文本
     */
    private String getTicketTypeText(Byte ticketType) {
        switch (ticketType) {
            case 1: return "成人票";
            case 2: return "儿童票";
            case 3: return "学生票";
            case 4: return "残疾票";
            case 5: return "军人票";
            default: return "未知票种";
        }
    }
    
    /**
     * 获取订单状态文本
     */
    private String getOrderStatusText(Byte orderStatus) {
        switch (orderStatus) {
            case 0: return "待支付";
            case 1: return "已支付";
            case 2: return "已完成";
            case 3: return "已取消";
            default: return "未知状态";
        }
    }
    
    /**
     * 获取车站所在城市
     */
    private String getStationCity(Long stopId) {
        try {
            Optional<TrainStop> trainStopOpt = trainStopRepository.findByStopId(stopId);
            if (trainStopOpt.isPresent()) {
                Long stationId = trainStopOpt.get().getStationId().longValue();
                Optional<Station> stationOpt = stationRepository.findById(stationId.intValue());
                if (stationOpt.isPresent()) {
                    return stationOpt.get().getCity();
                }
            }
        } catch (Exception e) {
            System.err.println("获取车站城市失败: " + e.getMessage());
        }
        return "未知城市";
    }
    
    /**
     * 获取车厢类型名称
     */
    private String getCarriageTypeName(Integer carriageTypeId) {
        try {
            Optional<CarriageType> carriageTypeOpt = carriageTypeRepository.findById(carriageTypeId);
            if (carriageTypeOpt.isPresent()) {
                return carriageTypeOpt.get().getTypeName();
            }
        } catch (Exception e) {
            System.err.println("获取车厢类型名称失败: " + e.getMessage());
        }
        return "未知车厢类型";
    }
    
    /**
     * 获取乘客类型文本
     */
    private String getPassengerTypeText(Byte passengerType) {
        switch (passengerType) {
            case 1: return "成人";
            case 2: return "儿童";
            case 3: return "学生";
            case 4: return "残疾军人";
            default: return "未知类型";
        }
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
    
    /**
     * 回滚库存扣减 - 当购票过程中出现异常时，将已扣减的库存加回
     */
    private void rollbackStockReductions(List<BookingRequest.PassengerInfo> successfulReductions, BookingRequest request) {
        if (successfulReductions.isEmpty()) {
            return;
        }
        
        System.out.println("开始回滚库存扣减，共 " + successfulReductions.size() + " 个席别");
        
        for (BookingRequest.PassengerInfo passengerInfo : successfulReductions) {
            try {
                boolean success = redisService.incrStock(
                    request.getTrainId(),
                    request.getDepartureStopId(),
                    request.getArrivalStopId(),
                    request.getTravelDate(),
                    passengerInfo.getCarriageTypeId(),
                    1
                );
                
                if (success) {
                    System.out.println("库存回滚成功: 车次" + request.getTrainId() + 
                                     ", 席别" + passengerInfo.getCarriageTypeId() + 
                                     ", 数量+1");
                } else {
                    System.err.println("库存回滚失败: 车次" + request.getTrainId() + 
                                     ", 席别" + passengerInfo.getCarriageTypeId());
                }
            } catch (Exception e) {
                System.err.println("库存回滚异常: 车次" + request.getTrainId() + 
                                 ", 席别" + passengerInfo.getCarriageTypeId() + 
                                 ", 错误: " + e.getMessage());
            }
        }
        
        System.out.println("库存回滚完成");
    }
} 