package com.example.techprototype.Service.Impl;

import com.example.techprototype.Component.OrderProcessor;
import com.example.techprototype.DAO.TicketInventoryDAO;
import com.example.techprototype.DTO.BookingRequest;
import com.example.techprototype.DTO.BookingResponse;
import com.example.techprototype.DTO.CancelOrderRequest;
import com.example.techprototype.DTO.MyOrderResponse;
import com.example.techprototype.DTO.OrderDetailResponse;
import com.example.techprototype.DTO.OrderMessage;
import com.example.techprototype.Entity.Order;
import com.example.techprototype.Entity.Passenger;
import com.example.techprototype.Entity.Ticket;
import com.example.techprototype.Entity.TicketInventory;
import com.example.techprototype.Entity.User;
import com.example.techprototype.Entity.Seat;
import com.example.techprototype.Entity.TrainCarriage;
import com.example.techprototype.Entity.Train;
import com.example.techprototype.Entity.TrainStop;
import com.example.techprototype.Entity.Station;
import com.example.techprototype.Enums.OrderStatus;
import com.example.techprototype.Enums.TicketStatus;
import com.example.techprototype.Repository.OrderRepository;
import com.example.techprototype.Repository.PassengerRepository;
import com.example.techprototype.Repository.TicketRepository;
import com.example.techprototype.Repository.UserRepository;
import com.example.techprototype.Repository.SeatRepository;
import com.example.techprototype.Repository.TrainCarriageRepository;
import com.example.techprototype.Repository.TrainRepository;
import com.example.techprototype.Repository.TrainStopRepository;
import com.example.techprototype.Repository.StationRepository;
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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    
    @Autowired
    private TrainRepository trainRepository;
    
    @Autowired
    private TrainStopRepository trainStopRepository;
    
    @Autowired
    private StationRepository stationRepository;
    
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
            
            return BookingResponse.successWithMessage("订单创建成功", orderMessage.getOrderNumber(), null, totalAmount, LocalDateTime.now());
            
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
            Optional<Order> orderOpt = orderRepository.findById(request.getOrderId());
            
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
            
            return BookingResponse.successWithMessage("订单取消成功", order.getOrderNumber(), order.getOrderId(), BigDecimal.ZERO, order.getOrderTime());
            
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
    
    @Override
    public MyOrderResponse getMyOrders(Long userId) {
        try {
            // 1. 根据用户ID查询所有订单
            List<Order> orders = orderRepository.findByUserIdOrderByOrderTimeDesc(userId);
            
            // 2. 转换为响应格式
            List<MyOrderResponse.MyOrderInfo> orderInfos = convertToMyOrderInfo(orders);
            
            return MyOrderResponse.success(orderInfos);
            
        } catch (Exception e) {
            System.err.println("获取我的订单失败: " + e.getMessage());
            return MyOrderResponse.failure("获取我的订单失败: " + e.getMessage());
        }
    }
    
    @Override
    public MyOrderResponse getMyOrdersByConditions(Long userId, String orderNumber, LocalDate startDate, LocalDate endDate, Byte orderStatus, String trainNumber) {
        try {
            List<Order> orders = new ArrayList<>();
            
            // 根据条件查询订单
            if (orderNumber != null && !orderNumber.trim().isEmpty()) {
                // 按订单号查询
                orders = orderRepository.findByUserIdAndOrderNumberContaining(userId, orderNumber.trim());
            } else if (orderStatus != null) {
                // 按订单状态查询
                orders = orderRepository.findByUserIdAndOrderStatus(userId, orderStatus);
            } else if (startDate != null && endDate != null) {
                // 按车票出发时间范围查询
                orders = orderRepository.findByUserIdAndTicketTravelDateBetween(userId, startDate, endDate);
            } else {
                // 查询所有订单
                orders = orderRepository.findByUserIdOrderByOrderTimeDesc(userId);
            }
            
            // 转换为响应格式
            List<MyOrderResponse.MyOrderInfo> orderInfos = convertToMyOrderInfo(orders);
            
            // 如果指定了车次号，进行过滤
            if (trainNumber != null && !trainNumber.trim().isEmpty()) {
                orderInfos = orderInfos.stream()
                        .filter(orderInfo -> orderInfo.getTrainNumber() != null && 
                                orderInfo.getTrainNumber().contains(trainNumber.trim()))
                        .collect(Collectors.toList());
            }
            
            return MyOrderResponse.success(orderInfos);
            
        } catch (Exception e) {
            System.err.println("获取我的订单失败: " + e.getMessage());
            return MyOrderResponse.failure("获取我的订单失败: " + e.getMessage());
        }
    }
    
    /**
     * 将Order实体转换为MyOrderInfo
     */
    private List<MyOrderResponse.MyOrderInfo> convertToMyOrderInfo(List<Order> orders) {
        List<MyOrderResponse.MyOrderInfo> orderInfos = new ArrayList<>();
        
        for (Order order : orders) {
            // 获取订单中的任意一张车票作为代表
            List<Ticket> tickets = ticketRepository.findByOrderId(order.getOrderId());
            if (tickets.isEmpty()) {
                continue; // 跳过没有车票的订单
            }
            
            // 使用第一张车票作为代表
            Ticket representativeTicket = tickets.get(0);
            
            // 获取车次信息
            String trainNumber = getTrainNumber(representativeTicket.getTrainId());
            
            // 获取车站信息
            String departureStationName = getStationName(representativeTicket.getDepartureStopId());
            String arrivalStationName = getStationName(representativeTicket.getArrivalStopId());
            String departureCity = getStationCity(representativeTicket.getDepartureStopId());
            String arrivalCity = getStationCity(representativeTicket.getArrivalStopId());
            
            // 获取时间信息
            LocalTime departureTime = getDepartureTime(representativeTicket.getTrainId(), representativeTicket.getDepartureStopId());
            LocalTime arrivalTime = getArrivalTime(representativeTicket.getTrainId(), representativeTicket.getArrivalStopId());
            
            MyOrderResponse.MyOrderInfo orderInfo = new MyOrderResponse.MyOrderInfo();
            orderInfo.setOrderId(order.getOrderId());
            orderInfo.setOrderNumber(order.getOrderNumber());
            orderInfo.setOrderTime(order.getOrderTime());
            orderInfo.setTotalAmount(order.getTotalAmount());
            orderInfo.setOrderStatus(order.getOrderStatus());
            orderInfo.setOrderStatusText(getOrderStatusText(order.getOrderStatus()));
            orderInfo.setPaymentMethod(order.getPaymentMethod());
            orderInfo.setPaymentTime(order.getPaymentTime());
            
            // 车次信息
            orderInfo.setTrainId(representativeTicket.getTrainId());
            orderInfo.setTrainNumber(trainNumber);
            orderInfo.setDepartureDate(representativeTicket.getTravelDate());
            orderInfo.setDepartureTime(departureTime);
            orderInfo.setArrivalTime(arrivalTime);
            orderInfo.setDepartureStationName(departureStationName);
            orderInfo.setDepartureCity(departureCity);
            orderInfo.setArrivalStationName(arrivalStationName);
            orderInfo.setArrivalCity(arrivalCity);
            
            // 车票数量
            orderInfo.setTicketCount(tickets.size());
            
            orderInfos.add(orderInfo);
        }
        
        return orderInfos;
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
     * 获取出发时间
     */
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
    
    /**
     * 获取到达时间
     */
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
    
    @Override
    public OrderDetailResponse getOrderDetail(Long userId, Long orderId) {
        try {
            // 验证订单是否属于该用户
            Optional<Order> orderOpt = orderRepository.findByOrderIdAndUserId(orderId, userId);
            if (orderOpt.isEmpty()) {
                throw new RuntimeException("订单不存在或不属于该用户");
            }
            
            Order order = orderOpt.get();
            
            // 获取订单下的所有车票
            List<Ticket> tickets = ticketRepository.findByOrderId(orderId);
            if (tickets.isEmpty()) {
                throw new RuntimeException("订单中没有车票信息");
            }
            
            // 获取第一个车票的信息作为车次信息（所有车票共享相同信息）
            Ticket firstTicket = tickets.get(0);
            
            // 获取车次信息
            Optional<Train> trainOpt = trainRepository.findById(firstTicket.getTrainId());
            if (trainOpt.isEmpty()) {
                throw new RuntimeException("车次信息不存在");
            }
            Train train = trainOpt.get();
            
            // 获取出发站和到达站信息
            String departureStation = getStationName(firstTicket.getDepartureStopId());
            String arrivalStation = getStationName(firstTicket.getArrivalStopId());
            
            // 获取出发时间和到达时间
            LocalTime departureTime = getDepartureTime(firstTicket.getTrainId(), firstTicket.getDepartureStopId());
            LocalTime arrivalTime = getArrivalTime(firstTicket.getTrainId(), firstTicket.getArrivalStopId());
            
            // 构建车票详情列表
            List<OrderDetailResponse.TicketDetail> ticketDetails = new ArrayList<>();
            for (Ticket ticket : tickets) {
                // 获取乘客信息
                Optional<Passenger> passengerOpt = passengerRepository.findById(ticket.getPassengerId());
                if (passengerOpt.isEmpty()) {
                    continue;
                }
                Passenger passenger = passengerOpt.get();
                
                // 获取席别信息
                String carriageType = getCarriageTypeName(ticket.getCarriageTypeId());
                
                OrderDetailResponse.TicketDetail ticketDetail = new OrderDetailResponse.TicketDetail();
                ticketDetail.setTicketId(ticket.getTicketId());
                ticketDetail.setTicketNumber(ticket.getTicketNumber());
                ticketDetail.setPassengerName(passenger.getRealName());
                ticketDetail.setIdCardNumber(passenger.getIdCardNumber());
                ticketDetail.setPassengerType(passenger.getPassengerType());
                ticketDetail.setTicketType(ticket.getTicketType());
                ticketDetail.setCarriageType(carriageType);
                ticketDetail.setCarriageNumber(ticket.getCarriageNumber());
                ticketDetail.setSeatNumber(ticket.getSeatNumber());
                ticketDetail.setPrice(ticket.getPrice());
                ticketDetail.setTicketStatus(ticket.getTicketStatus());
                
                ticketDetails.add(ticketDetail);
            }
            
            // 构建订单详情响应
            OrderDetailResponse response = new OrderDetailResponse();
            response.setOrderNumber(order.getOrderNumber());
            response.setOrderStatus(order.getOrderStatus());
            response.setOrderTime(order.getOrderTime());
            response.setPaymentTime(order.getPaymentTime());
            response.setPaymentMethod(order.getPaymentMethod());
            response.setTotalAmount(order.getTotalAmount());
            response.setTrainNumber(train.getTrainNumber());
            response.setTravelDate(firstTicket.getTravelDate());
            response.setDepartureTime(departureTime);
            response.setArrivalTime(arrivalTime);
            response.setDepartureStation(departureStation);
            response.setArrivalStation(arrivalStation);
            response.setTickets(ticketDetails);
            
            return response;
            
        } catch (Exception e) {
            System.err.println("获取订单详情失败: " + e.getMessage());
            throw new RuntimeException("获取订单详情失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取席别名称
     */
    private String getCarriageTypeName(Integer carriageTypeId) {
        switch (carriageTypeId) {
            case 1: return "硬座";
            case 2: return "软座";
            case 3: return "硬卧";
            case 4: return "软卧";
            case 5: return "商务座";
            case 6: return "一等座";
            case 7: return "二等座";
            default: return "未知席别";
        }
    }
} 