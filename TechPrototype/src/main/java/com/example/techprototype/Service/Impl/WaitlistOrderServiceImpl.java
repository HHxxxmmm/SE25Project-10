package com.example.techprototype.Service.Impl;

import com.example.techprototype.DTO.*;
import com.example.techprototype.Entity.*;
import com.example.techprototype.Enums.WaitlistOrderStatus;
import com.example.techprototype.Enums.WaitlistItemStatus;
import com.example.techprototype.Repository.*;
import com.example.techprototype.Service.WaitlistOrderService;
import com.example.techprototype.Service.RedisService;
import com.example.techprototype.Util.TicketNumberGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WaitlistOrderServiceImpl implements WaitlistOrderService {
    
    @Autowired
    private WaitlistOrderRepository waitlistOrderRepository;
    
    @Autowired
    private WaitlistItemRepository waitlistItemRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PassengerRepository passengerRepository;
    
    @Autowired
    private TrainRepository trainRepository;
    
    @Autowired
    private TrainStopRepository trainStopRepository;
    
    @Autowired
    private StationRepository stationRepository;
    
    @Autowired
    private CarriageTypeRepository carriageTypeRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private TicketRepository ticketRepository;
    
    @Autowired
    private TicketInventoryRepository ticketInventoryRepository;
    
    @Autowired
    private RedisService redisService;
    
    @Override
    @Transactional
    public BookingResponse createWaitlistOrder(BookingRequest request) {
        try {
            // 验证用户
            Optional<User> userOpt = userRepository.findById(request.getUserId());
            if (userOpt.isEmpty()) {
                return BookingResponse.failure("用户不存在");
            }
            
            // 生成候补订单号
            String orderNumber = generateWaitlistOrderNumber();
            
            // 计算总金额
            BigDecimal totalAmount = calculateTotalAmount(request);
            
            // 计算过期时间（发车前2小时）
            LocalDateTime expireTime = calculateExpireTime(request.getTravelDate(), request.getTrainId());
            
            // 创建候补订单
            WaitlistOrder waitlistOrder = new WaitlistOrder();
            waitlistOrder.setOrderNumber(orderNumber);
            waitlistOrder.setUserId(request.getUserId());
            waitlistOrder.setOrderTime(LocalDateTime.now());
            waitlistOrder.setTotalAmount(totalAmount);
            waitlistOrder.setExpireTime(expireTime);
            waitlistOrder.setOrderStatus((byte) WaitlistOrderStatus.PENDING_PAYMENT.getCode());
            waitlistOrder.setItemCount(request.getPassengers().size()); // 设置候补订单项数量
            
            waitlistOrderRepository.save(waitlistOrder);
            
            // 创建候补订单项
            List<WaitlistItem> items = new ArrayList<>();
            for (BookingRequest.PassengerInfo passengerInfo : request.getPassengers()) {
                WaitlistItem item = new WaitlistItem();
                item.setWaitlistId(waitlistOrder.getWaitlistId());
                item.setPassengerId(passengerInfo.getPassengerId());
                item.setTrainId(request.getTrainId());
                item.setDepartureStopId(request.getDepartureStopId());
                item.setArrivalStopId(request.getArrivalStopId());
                item.setTravelDate(request.getTravelDate());
                item.setCarriageTypeId(passengerInfo.getCarriageTypeId());
                item.setTicketType(passengerInfo.getTicketType());
                item.setItemStatus((byte) WaitlistItemStatus.PENDING_PAYMENT.getCode());
                item.setCreatedTime(LocalDateTime.now());
                
                // 计算并设置价格
                BigDecimal itemPrice = calculateTicketPrice(
                    request.getTrainId(),
                    request.getDepartureStopId(),
                    request.getArrivalStopId(),
                    request.getTravelDate(),
                    passengerInfo.getCarriageTypeId(),
                    passengerInfo.getTicketType()
                );
                item.setPrice(itemPrice);
                
                items.add(item);
            }
            
            waitlistItemRepository.saveAll(items);
            
            return BookingResponse.successWithMessage("候补订单创建成功", orderNumber, waitlistOrder.getWaitlistId(), totalAmount, LocalDateTime.now());
            
        } catch (Exception e) {
            System.err.println("创建候补订单失败: " + e.getMessage());
            return BookingResponse.failure("创建候补订单失败: " + e.getMessage());
        }
    }
    
    @Override
    public WaitlistOrderResponse getMyWaitlistOrders(Long userId) {
        try {
            List<WaitlistOrder> waitlistOrders = waitlistOrderRepository.findByUserIdOrderByOrderTimeDesc(userId);
            List<WaitlistOrderResponse.WaitlistOrderInfo> orderInfos = convertToWaitlistOrderInfo(waitlistOrders);
            return WaitlistOrderResponse.success(orderInfos);
        } catch (Exception e) {
            System.err.println("获取候补订单失败: " + e.getMessage());
            return WaitlistOrderResponse.failure("获取候补订单失败: " + e.getMessage());
        }
    }
    
    @Override
    public WaitlistOrderDetailResponse getWaitlistOrderDetail(Long userId, Long waitlistId) {
        try {
            Optional<WaitlistOrder> orderOpt = waitlistOrderRepository.findById(waitlistId);
            if (orderOpt.isEmpty()) {
                return WaitlistOrderDetailResponse.failure("候补订单不存在");
            }
            
            WaitlistOrder order = orderOpt.get();
            if (!order.getUserId().equals(userId)) {
                return WaitlistOrderDetailResponse.failure("无权访问此候补订单");
            }
            
            List<WaitlistItem> items = waitlistItemRepository.findByWaitlistId(waitlistId);
            WaitlistOrderDetailResponse.WaitlistOrderDetail detail = convertToWaitlistOrderDetail(order, items);
            
            return WaitlistOrderDetailResponse.success(detail);
        } catch (Exception e) {
            System.err.println("获取候补订单详情失败: " + e.getMessage());
            return WaitlistOrderDetailResponse.failure("获取候补订单详情失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public BookingResponse payWaitlistOrder(Long waitlistId, Long userId) {
        try {
            Optional<WaitlistOrder> orderOpt = waitlistOrderRepository.findById(waitlistId);
            if (orderOpt.isEmpty()) {
                return BookingResponse.failure("候补订单不存在");
            }
            
            WaitlistOrder order = orderOpt.get();
            if (!order.getUserId().equals(userId)) {
                return BookingResponse.failure("无权操作此候补订单");
            }
            
            if (order.getOrderStatus() != WaitlistOrderStatus.PENDING_PAYMENT.getCode()) {
                return BookingResponse.failure("候补订单状态不正确");
            }
            
            // 更新候补订单状态为待兑现
            order.setOrderStatus((byte) WaitlistOrderStatus.PENDING_FULFILLMENT.getCode());
            waitlistOrderRepository.save(order);
            
            // 更新候补订单项状态为待兑现
            List<WaitlistItem> items = waitlistItemRepository.findByWaitlistId(waitlistId);
            for (WaitlistItem item : items) {
                item.setItemStatus((byte) WaitlistItemStatus.PENDING_FULFILLMENT.getCode());
            }
            waitlistItemRepository.saveAll(items);
            
            return BookingResponse.successWithMessage("候补订单支付成功", order.getOrderNumber(), waitlistId, order.getTotalAmount(), LocalDateTime.now());
            
        } catch (Exception e) {
            System.err.println("支付候补订单失败: " + e.getMessage());
            return BookingResponse.failure("支付候补订单失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public BookingResponse cancelWaitlistOrder(Long waitlistId, Long userId) {
        try {
            Optional<WaitlistOrder> orderOpt = waitlistOrderRepository.findById(waitlistId);
            if (orderOpt.isEmpty()) {
                return BookingResponse.failure("候补订单不存在");
            }
            
            WaitlistOrder order = orderOpt.get();
            if (!order.getUserId().equals(userId)) {
                return BookingResponse.failure("无权操作此候补订单");
            }
            
            // 更新候补订单状态为已取消
            order.setOrderStatus((byte) WaitlistOrderStatus.CANCELLED.getCode());
            waitlistOrderRepository.save(order);
            
            // 更新候补订单项状态为已取消
            List<WaitlistItem> items = waitlistItemRepository.findByWaitlistId(waitlistId);
            for (WaitlistItem item : items) {
                item.setItemStatus((byte) WaitlistItemStatus.CANCELLED.getCode());
            }
            waitlistItemRepository.saveAll(items);
            
            return BookingResponse.successWithMessage("候补订单取消成功", order.getOrderNumber(), waitlistId, BigDecimal.ZERO, LocalDateTime.now());
            
        } catch (Exception e) {
            System.err.println("取消候补订单失败: " + e.getMessage());
            return BookingResponse.failure("取消候补订单失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public void processWaitlistFulfillment() {
        try {
            // 查询待兑现的候补订单
            List<WaitlistOrder> pendingOrders = waitlistOrderRepository.findPendingFulfillmentOrders(LocalDateTime.now());
            
            for (WaitlistOrder order : pendingOrders) {
                List<WaitlistItem> pendingItems = waitlistItemRepository.findPendingItemsByWaitlistId(order.getWaitlistId());
                
                for (WaitlistItem item : pendingItems) {
                    // 检查是否有可用库存
                    Optional<TicketInventory> inventory = ticketInventoryRepository.findByKeyWithLock(
                            item.getTrainId(), item.getDepartureStopId(), 
                            item.getArrivalStopId(), item.getTravelDate(), item.getCarriageTypeId());
                    
                    if (inventory.isPresent() && inventory.get().getAvailableSeats() > 0) {
                        // 尝试兑现候补订单项
                        if (fulfillWaitlistItem(item)) {
                            System.out.println("候补订单项兑现成功: " + item.getItemId());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("处理候补订单兑现失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public BookingResponse refundWaitlistOrder(Long waitlistId, Long userId) {
        try {
            Optional<WaitlistOrder> orderOpt = waitlistOrderRepository.findById(waitlistId);
            if (orderOpt.isEmpty()) {
                return BookingResponse.failure("候补订单不存在");
            }
            
            WaitlistOrder order = orderOpt.get();
            if (!order.getUserId().equals(userId)) {
                return BookingResponse.failure("无权操作此候补订单");
            }
            
            // 只有待兑现或已兑现的候补订单才能退款
            if (order.getOrderStatus() != WaitlistOrderStatus.PENDING_FULFILLMENT.getCode() && 
                order.getOrderStatus() != WaitlistOrderStatus.FULFILLED.getCode()) {
                return BookingResponse.failure("候补订单状态不允许退款");
            }
            
            // 获取所有候补订单项
            List<WaitlistItem> allItems = waitlistItemRepository.findByWaitlistId(waitlistId);
            
            if (order.getOrderStatus() == WaitlistOrderStatus.FULFILLED.getCode()) {
                // 已兑现的候补订单退款逻辑
                List<WaitlistItem> fulfilledItems = allItems.stream()
                    .filter(item -> item.getItemStatus() == (byte) WaitlistItemStatus.FULFILLED.getCode())
                    .collect(Collectors.toList());
                
                if (fulfilledItems.isEmpty()) {
                    return BookingResponse.failure("未找到已兑现的候补订单项");
                }
                
                // 查找对应的正式订单（通过候补订单号关联）
                Optional<Order> formalOrderOpt = orderRepository.findByOrderNumber(order.getOrderNumber());
                if (formalOrderOpt.isEmpty()) {
                    return BookingResponse.failure("未找到对应的正式订单");
                }
                
                Order formalOrder = formalOrderOpt.get();
                
                // 获取正式订单下的车票
                List<Ticket> tickets = ticketRepository.findByOrderId(formalOrder.getOrderId());
                
                // 执行退票操作
                for (Ticket ticket : tickets) {
                    // 更新车票状态为已退票
                    ticket.setTicketStatus((byte) 3); // 已退票
                    ticketRepository.save(ticket);
                    
                    // 回滚库存
                    redisService.incrStock(ticket.getTrainId(), ticket.getDepartureStopId(),
                            ticket.getArrivalStopId(), ticket.getTravelDate(), 
                            ticket.getCarriageTypeId(), 1);
                    
                    // 释放座位
                    if (ticket.getSeatNumber() != null && ticket.getCarriageNumber() != null) {
                        // 这里需要调用座位服务释放座位
                        // seatService.releaseSeat(ticket);
                    }
                }
                
                // 更新正式订单状态为已取消
                formalOrder.setOrderStatus((byte) 3); // 已取消
                formalOrder.setTotalAmount(BigDecimal.ZERO);
                orderRepository.save(formalOrder);
                
                // 更新候补订单项状态为已取消
                for (WaitlistItem item : fulfilledItems) {
                    item.setItemStatus((byte) 3); // 已取消
                }
                waitlistItemRepository.saveAll(fulfilledItems);
            } else {
                // 待兑现的候补订单退款逻辑（更简单，因为没有创建正式订单和车票）
                // 直接更新候补订单项状态为已取消
                for (WaitlistItem item : allItems) {
                    item.setItemStatus((byte) 3); // 已取消
                }
                waitlistItemRepository.saveAll(allItems);
            }
            
            // 更新候补订单状态为已取消
            order.setOrderStatus((byte) 3); // 已取消
            waitlistOrderRepository.save(order);
            
            return BookingResponse.successWithMessage("候补订单退款成功", order.getOrderNumber(), waitlistId, order.getTotalAmount(), order.getOrderTime());
            
        } catch (Exception e) {
            System.err.println("退款候补订单失败: " + e.getMessage());
            return BookingResponse.failure("退款候补订单失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public BookingResponse refundWaitlistOrderItems(Long waitlistId, Long userId, List<Long> itemIds) {
        try {
            Optional<WaitlistOrder> orderOpt = waitlistOrderRepository.findById(waitlistId);
            if (orderOpt.isEmpty()) {
                return BookingResponse.failure("候补订单不存在");
            }
            
            WaitlistOrder order = orderOpt.get();
            if (!order.getUserId().equals(userId)) {
                return BookingResponse.failure("无权操作此候补订单");
            }
            
            // 只有待兑现或已兑现的候补订单才能退款
            if (order.getOrderStatus() != WaitlistOrderStatus.PENDING_FULFILLMENT.getCode() && 
                order.getOrderStatus() != WaitlistOrderStatus.FULFILLED.getCode()) {
                return BookingResponse.failure("候补订单状态不允许退款");
            }
            
            // 获取指定的候补订单项
            List<WaitlistItem> selectedItems = waitlistItemRepository.findByItemIdIn(itemIds);
            if (selectedItems.isEmpty()) {
                return BookingResponse.failure("未找到指定的候补订单项");
            }
            
            // 验证所有选中的候补订单项都属于这个候补订单
            for (WaitlistItem item : selectedItems) {
                if (!item.getWaitlistId().equals(waitlistId)) {
                    return BookingResponse.failure("候补订单项不属于此候补订单");
                }
                // 只有待兑现的候补订单项才能退款
                if (item.getItemStatus() != WaitlistItemStatus.PENDING_FULFILLMENT.getCode()) {
                    return BookingResponse.failure("候补订单项状态不允许退款");
                }
            }
            
            BigDecimal refundAmount = BigDecimal.ZERO;
            int refundedCount = 0;
            
            // 更新候补订单项状态为已取消
            for (WaitlistItem item : selectedItems) {
                item.setItemStatus((byte) WaitlistItemStatus.CANCELLED.getCode()); // 已取消
                refundAmount = refundAmount.add(item.getPrice());
                refundedCount++;
            }
            waitlistItemRepository.saveAll(selectedItems);
            
            // 更新候补订单总金额和项数量
            order.setTotalAmount(order.getTotalAmount().subtract(refundAmount));
            order.setItemCount(order.getItemCount() - refundedCount); // 减去退款的数量
            
            // 检查是否所有候补订单项都退了
            List<WaitlistItem> allItems = waitlistItemRepository.findByWaitlistId(waitlistId);
            boolean allCancelled = allItems.stream()
                .allMatch(item -> item.getItemStatus() == WaitlistItemStatus.CANCELLED.getCode());
            
            if (allCancelled) {
                order.setOrderStatus((byte) 3); // 已取消
                order.setItemCount(0); // 所有项都退了
            }
            
            waitlistOrderRepository.save(order);
            
            return BookingResponse.successWithMessage("候补订单项退款成功", order.getOrderNumber(), waitlistId, refundAmount, order.getOrderTime());
            
        } catch (Exception e) {
            System.err.println("退款候补订单项失败: " + e.getMessage());
            return BookingResponse.failure("退款候补订单项失败: " + e.getMessage());
        }
    }
    
    // 私有辅助方法
    private String generateWaitlistOrderNumber() {
        return "WL" + LocalDate.now().toString().replace("-", "") + 
               String.format("%06d", (int)(Math.random() * 1000000));
    }
    
    private BigDecimal calculateTotalAmount(BookingRequest request) {
        BigDecimal total = BigDecimal.ZERO;
        for (BookingRequest.PassengerInfo passengerInfo : request.getPassengers()) {
            BigDecimal ticketPrice = calculateTicketPrice(
                request.getTrainId(),
                request.getDepartureStopId(),
                request.getArrivalStopId(),
                request.getTravelDate(),
                passengerInfo.getCarriageTypeId(),
                passengerInfo.getTicketType()
            );
            total = total.add(ticketPrice);
        }
        return total;
    }
    
    private LocalDateTime calculateExpireTime(LocalDate travelDate, Integer trainId) {
        // 获取发车时间，然后减去2小时
        Optional<Train> trainOpt = trainRepository.findById(trainId);
        if (trainOpt.isPresent()) {
            LocalTime departureTime = trainOpt.get().getDepartureTime();
            return LocalDateTime.of(travelDate, departureTime).minusHours(2);
        }
        return LocalDateTime.now().plusDays(1); // 默认1天后过期
    }
    
    private List<WaitlistOrderResponse.WaitlistOrderInfo> convertToWaitlistOrderInfo(List<WaitlistOrder> orders) {
        List<WaitlistOrderResponse.WaitlistOrderInfo> orderInfos = new ArrayList<>();
        
        for (WaitlistOrder order : orders) {
            List<WaitlistItem> items = waitlistItemRepository.findByWaitlistId(order.getWaitlistId());
            if (items.isEmpty()) continue;
            
            WaitlistItem representativeItem = items.get(0);
            
            WaitlistOrderResponse.WaitlistOrderInfo orderInfo = new WaitlistOrderResponse.WaitlistOrderInfo();
            orderInfo.setWaitlistId(order.getWaitlistId());
            orderInfo.setOrderNumber(order.getOrderNumber());
            orderInfo.setOrderTime(order.getOrderTime());
            orderInfo.setTotalAmount(order.getTotalAmount());
            orderInfo.setExpireTime(order.getExpireTime());
            orderInfo.setOrderStatus(order.getOrderStatus());
            orderInfo.setOrderStatusText(WaitlistOrderStatus.fromCode(order.getOrderStatus()).getDescription());
            orderInfo.setTrainId(representativeItem.getTrainId());
            orderInfo.setTicketCount(order.getItemCount() != null ? order.getItemCount() : items.size());
            
            // 获取车次信息
            Optional<Train> trainOpt = trainRepository.findById(representativeItem.getTrainId());
            if (trainOpt.isPresent()) {
                orderInfo.setTrainNumber(trainOpt.get().getTrainNumber());
            }
            
            // 获取车站信息
            orderInfo.setDepartureStationName(getStationName(representativeItem.getDepartureStopId()));
            orderInfo.setArrivalStationName(getStationName(representativeItem.getArrivalStopId()));
            
            // 设置发车日期
            orderInfo.setDepartureDate(LocalDateTime.of(representativeItem.getTravelDate(), LocalTime.of(0, 0)));
            
            // 获取时间信息
            LocalTime departureTime = getDepartureTime(representativeItem.getTrainId(), representativeItem.getDepartureStopId());
            LocalTime arrivalTime = getArrivalTime(representativeItem.getTrainId(), representativeItem.getArrivalStopId());
            
            if (departureTime != null) {
                orderInfo.setDepartureTime(departureTime.toString());
            }
            if (arrivalTime != null) {
                orderInfo.setArrivalTime(arrivalTime.toString());
            }
            
            orderInfos.add(orderInfo);
        }
        
        return orderInfos;
    }
    
    private WaitlistOrderDetailResponse.WaitlistOrderDetail convertToWaitlistOrderDetail(WaitlistOrder order, List<WaitlistItem> items) {
        WaitlistOrderDetailResponse.WaitlistOrderDetail detail = new WaitlistOrderDetailResponse.WaitlistOrderDetail();
        detail.setWaitlistId(order.getWaitlistId());
        detail.setOrderNumber(order.getOrderNumber());
        detail.setOrderTime(order.getOrderTime());
        detail.setTotalAmount(order.getTotalAmount());
        detail.setExpireTime(order.getExpireTime());
        detail.setOrderStatus(order.getOrderStatus());
        detail.setOrderStatusText(WaitlistOrderStatus.fromCode(order.getOrderStatus()).getDescription());
        detail.setTicketCount(order.getItemCount() != null ? order.getItemCount() : items.size());
        
        // 获取第一个候补订单项的信息作为车次信息
        if (!items.isEmpty()) {
            WaitlistItem representativeItem = items.get(0);
            detail.setTrainId(representativeItem.getTrainId());
            detail.setTravelDate(representativeItem.getTravelDate());
            
            // 获取车次信息
            Optional<Train> trainOpt = trainRepository.findById(representativeItem.getTrainId());
            if (trainOpt.isPresent()) {
                detail.setTrainNumber(trainOpt.get().getTrainNumber());
            }
            
            // 获取车站信息
            detail.setDepartureStation(getStationName(representativeItem.getDepartureStopId()));
            detail.setArrivalStation(getStationName(representativeItem.getArrivalStopId()));
            
            // 获取时间信息
            LocalTime departureTime = getDepartureTime(representativeItem.getTrainId(), representativeItem.getDepartureStopId());
            LocalTime arrivalTime = getArrivalTime(representativeItem.getTrainId(), representativeItem.getArrivalStopId());
            
            if (departureTime != null) {
                detail.setDepartureTime(departureTime.toString());
            }
            if (arrivalTime != null) {
                detail.setArrivalTime(arrivalTime.toString());
            }
        }
        
        List<WaitlistOrderDetailResponse.WaitlistItemInfo> itemInfos = new ArrayList<>();
        for (WaitlistItem item : items) {
            WaitlistOrderDetailResponse.WaitlistItemInfo itemInfo = new WaitlistOrderDetailResponse.WaitlistItemInfo();
            itemInfo.setItemId(item.getItemId());
            itemInfo.setPassengerId(item.getPassengerId());
            itemInfo.setTrainId(item.getTrainId());
            itemInfo.setDepartureStopId(item.getDepartureStopId());
            itemInfo.setArrivalStopId(item.getArrivalStopId());
            itemInfo.setTravelDate(item.getTravelDate());
            itemInfo.setCarriageTypeId(item.getCarriageTypeId());
            itemInfo.setTicketType(item.getTicketType());
            itemInfo.setItemStatus(item.getItemStatus());
            itemInfo.setCreatedTime(item.getCreatedTime());
            
            // 获取乘客信息
            Optional<Passenger> passengerOpt = passengerRepository.findById(item.getPassengerId());
            if (passengerOpt.isPresent()) {
                Passenger passenger = passengerOpt.get();
                itemInfo.setPassengerName(passenger.getRealName());
                itemInfo.setIdCardNumber(passenger.getIdCardNumber());
                itemInfo.setPassengerType(passenger.getPassengerType());
                itemInfo.setPassengerTypeText(getPassengerTypeText(passenger.getPassengerType()));
            }
            
            // 获取车次信息
            Optional<Train> trainOpt = trainRepository.findById(item.getTrainId());
            if (trainOpt.isPresent()) {
                itemInfo.setTrainNumber(trainOpt.get().getTrainNumber());
            }
            
            // 获取车站信息
            itemInfo.setDepartureStationName(getStationName(item.getDepartureStopId()));
            itemInfo.setArrivalStationName(getStationName(item.getArrivalStopId()));
            
            // 获取车厢类型信息
            Optional<CarriageType> carriageTypeOpt = carriageTypeRepository.findById(item.getCarriageTypeId());
            if (carriageTypeOpt.isPresent()) {
                itemInfo.setCarriageTypeName(carriageTypeOpt.get().getTypeName());
            }
            
            // 设置状态文本
            itemInfo.setItemStatusText(WaitlistItemStatus.fromCode(item.getItemStatus()).getDescription());
            
            // 设置票价（从候补订单项中获取实际价格）
            itemInfo.setPrice(item.getPrice());
            
            itemInfos.add(itemInfo);
        }
        
        detail.setItems(itemInfos);
        return detail;
    }
    
    private boolean fulfillWaitlistItem(WaitlistItem item) {
        try {
            // 这里实现具体的兑现逻辑
            // 1. 创建正式订单和车票
            // 2. 更新候补订单项状态
            // 3. 检查候补订单是否全部兑现
            
            // 简化实现，实际需要完整的订单创建逻辑
            item.setItemStatus((byte) WaitlistItemStatus.FULFILLED.getCode());
            waitlistItemRepository.save(item);
            
            return true;
        } catch (Exception e) {
            System.err.println("兑现候补订单项失败: " + e.getMessage());
            return false;
        }
    }
    
    private String getStationName(Long stopId) {
        try {
            Optional<TrainStop> trainStopOpt = trainStopRepository.findByStopId(stopId);
            if (trainStopOpt.isPresent()) {
                Optional<Station> stationOpt = stationRepository.findById(trainStopOpt.get().getStationId());
                if (stationOpt.isPresent()) {
                    return stationOpt.get().getStationName();
                }
            }
        } catch (Exception e) {
            System.err.println("获取车站名称失败: " + e.getMessage());
        }
        return "";
    }
    
    private LocalTime getDepartureTime(Integer trainId, Long stopId) {
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
    
    private LocalTime getArrivalTime(Integer trainId, Long stopId) {
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
    
    private String getPassengerTypeText(Byte passengerType) {
        if (passengerType == null) return "未知";
        switch (passengerType) {
            case 1: return "成人";
            case 2: return "儿童";
            case 3: return "学生";
            case 4: return "残疾";
            case 5: return "军人";
            default: return "未知";
        }
    }
    
    /**
     * 计算票价 - 从库存信息获取基础票价，然后根据票种计算优惠
     */
    private BigDecimal calculateTicketPrice(Integer trainId, Long departureStopId, Long arrivalStopId, 
                                          LocalDate travelDate, Integer carriageTypeId, Byte ticketType) {
        // 从库存信息获取基础票价
        Optional<TicketInventory> inventory = ticketInventoryRepository.findByKeyWithLock(
            trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId);
        
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

