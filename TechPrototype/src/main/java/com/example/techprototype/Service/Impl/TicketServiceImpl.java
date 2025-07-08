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
            return BookingResponse.successWithMessage("购票成功", orderNumber, null, null, LocalDateTime.now());
            
        } finally {
            redisService.unlock(lockKey);
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
            orderRepository.save(order);
            
            // 8. 检查订单是否还有有效车票
            List<Ticket> validTickets = ticketRepository.findValidTicketsByOrderId(order.getOrderId());
            if (validTickets.isEmpty()) {
                // 订单中没有有效车票，将订单状态改为已取消
                order.setOrderStatus((byte) OrderStatus.CANCELLED.getCode());
                orderRepository.save(order);
                return BookingResponse.successWithMessage("退票成功，订单已取消", order.getOrderNumber(), null, null, LocalDateTime.now());
            }
            
            return BookingResponse.successWithMessage("退票成功", order.getOrderNumber(), null, null, LocalDateTime.now());
            
        } finally {
            redisService.unlock(lockKey);
        }
    }
    
    @Override
    @Transactional
    public BookingResponse changeTickets(ChangeTicketRequest request) {
        String lockKey = "change:" + request.getOriginalOrderId();
        
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
            
            return BookingResponse.successWithMessage("改签成功", newOrder.getOrderNumber(), newOrder.getOrderId(), newOrder.getTotalAmount(), LocalDateTime.now());
            
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
            
            // 2. 根据乘客ID查询所有有效车票
            List<Ticket> tickets = ticketRepository.findValidTicketsByPassengerId(user.getPassengerId());
            
            // 3. 转换为响应格式
            List<MyTicketResponse.MyTicketInfo> ticketInfos = convertToMyTicketInfo(tickets);
            
            return MyTicketResponse.success(ticketInfos);
            
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
            
            // 2. 根据乘客ID和车票状态查询车票
            List<Ticket> tickets = ticketRepository.findByPassengerIdAndTicketStatusOrderByCreatedTimeDesc(
                    user.getPassengerId(), ticketStatus);
            
            // 3. 转换为响应格式
            List<MyTicketResponse.MyTicketInfo> ticketInfos = convertToMyTicketInfo(tickets);
            
            return MyTicketResponse.success(ticketInfos);
            
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
            
            // 9. 构建车票详情信息
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
            ticketInfo.setCarriageNumber(ticket.getCarriageNumber());
            ticketInfo.setSeatNumber(ticket.getSeatNumber());
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
} 