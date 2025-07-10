package com.example.techprototype.Service.Impl;

import com.example.techprototype.DTO.PrepareOrderRequest;
import com.example.techprototype.DTO.PrepareOrderResponse;
import com.example.techprototype.Entity.*;
import com.example.techprototype.Repository.*;
import com.example.techprototype.Service.PrepareOrderService;
import com.example.techprototype.Service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PrepareOrderServiceImpl implements PrepareOrderService {
    
    @Autowired
    private TicketInventoryRepository ticketInventoryRepository;
    
    @Autowired
    private TrainRepository trainRepository;
    
    @Autowired
    private StationRepository stationRepository;
    
    @Autowired
    private CarriageTypeRepository carriageTypeRepository;
    
    @Autowired
    private UserPassengerRelationRepository userPassengerRelationRepository;
    
    @Autowired
    private PassengerRepository passengerRepository;
    
    @Autowired
    private TrainStopRepository trainStopRepository;
    
    @Autowired
    private RedisService redisService;
    
    @Override
    public PrepareOrderResponse prepareOrder(PrepareOrderRequest request) {
        // 1. 获取库存信息
        List<TicketInventory> inventories = new ArrayList<>();
        for (Long inventoryId : request.getInventoryIds()) {
            Optional<TicketInventory> inventory = ticketInventoryRepository.findById(inventoryId);
            if (inventory.isPresent()) {
                inventories.add(inventory.get());
            }
        }
        
        if (inventories.isEmpty()) {
            throw new RuntimeException("未找到有效的库存信息");
        }
        
        // 2. 获取车次信息（所有库存应该属于同一车次）
        TicketInventory firstInventory = inventories.get(0);
        Train train = trainRepository.findById(firstInventory.getTrainId())
                .orElseThrow(() -> new RuntimeException("未找到车次信息"));
        
        // 3. 获取出发站和到达站信息（从train_stops表获取）
        TrainStop departureStop = trainStopRepository.findById(firstInventory.getDepartureStopId())
                .orElseThrow(() -> new RuntimeException("未找到出发站信息"));
        TrainStop arrivalStop = trainStopRepository.findById(firstInventory.getArrivalStopId())
                .orElseThrow(() -> new RuntimeException("未找到到达站信息"));
        
        // 4. 获取站点详细信息
        Station departureStation = stationRepository.findById(departureStop.getStationId())
                .orElseThrow(() -> new RuntimeException("未找到出发站详细信息"));
        Station arrivalStation = stationRepository.findById(arrivalStop.getStationId())
                .orElseThrow(() -> new RuntimeException("未找到到达站详细信息"));
        
        // 5. 构建车次信息
        PrepareOrderResponse.TrainInfo trainInfo = new PrepareOrderResponse.TrainInfo();
        trainInfo.setTrainId(train.getTrainId());
        trainInfo.setTrainNumber(train.getTrainNumber());
        trainInfo.setDepartureStation(departureStation.getStationName());
        trainInfo.setArrivalStation(arrivalStation.getStationName());
        trainInfo.setDepartureStopId(departureStop.getStopId());
        trainInfo.setArrivalStopId(arrivalStop.getStopId());
        trainInfo.setDepartureTime(departureStop.getDepartureTime());
        trainInfo.setArrivalTime(arrivalStop.getArrivalTime());
        trainInfo.setTravelDate(firstInventory.getTravelDate());
        
        // 6. 构建席别信息
        List<PrepareOrderResponse.CarriageInfo> carriages = new ArrayList<>();
        for (TicketInventory inventory : inventories) {
            CarriageType carriageType = carriageTypeRepository.findById(inventory.getCarriageTypeId())
                    .orElseThrow(() -> new RuntimeException("未找到席别信息"));
            
            PrepareOrderResponse.CarriageInfo carriageInfo = new PrepareOrderResponse.CarriageInfo();
            carriageInfo.setInventoryId(inventory.getInventoryId());
            carriageInfo.setCarriageTypeId(inventory.getCarriageTypeId());
            carriageInfo.setCarriageTypeName(carriageType.getTypeName());
            carriageInfo.setPrice(inventory.getPrice());
            
            // 从Redis获取库存信息
            Optional<Integer> redisStock = redisService.getStock(
                    inventory.getTrainId(),
                    inventory.getDepartureStopId(),
                    inventory.getArrivalStopId(),
                    inventory.getTravelDate(),
                    inventory.getCarriageTypeId()
            );
            
            if (redisStock.isPresent()) {
                int stock = redisStock.get();
                carriageInfo.setHasStock(stock > 0);
                carriageInfo.setAvailableStock(stock);
            } else {
                // 如果Redis中没有数据，使用数据库中的可用座位数
                carriageInfo.setHasStock(inventory.getAvailableSeats() > 0);
                carriageInfo.setAvailableStock(inventory.getAvailableSeats());
            }
            
            carriages.add(carriageInfo);
        }
        
        // 7. 获取用户关联的乘客信息
        List<UserPassengerRelation> relations = userPassengerRelationRepository.findByUserId(request.getUserId());
        List<PrepareOrderResponse.PassengerInfo> passengers = new ArrayList<>();
        
        for (UserPassengerRelation relation : relations) {
            Passenger passenger = passengerRepository.findById(relation.getPassengerId())
                    .orElseThrow(() -> new RuntimeException("未找到乘客信息"));
            
            PrepareOrderResponse.PassengerInfo passengerInfo = new PrepareOrderResponse.PassengerInfo();
            passengerInfo.setPassengerId(passenger.getPassengerId());
            passengerInfo.setRealName(passenger.getRealName());
            passengerInfo.setIdCardNumber(passenger.getIdCardNumber());
            passengerInfo.setPhoneNumber(passenger.getPhoneNumber());
            passengerInfo.setPassengerType(passenger.getPassengerType());
            passengerInfo.setPassengerTypeName(getPassengerTypeName(passenger.getPassengerType()));
            passengerInfo.setRelationType(relation.getRelationType());
            passengerInfo.setRelationTypeName(getRelationTypeName(relation.getRelationType()));
            
            passengers.add(passengerInfo);
        }
        
        // 8. 构建响应
        PrepareOrderResponse response = new PrepareOrderResponse();
        response.setTrainInfo(trainInfo);
        response.setCarriages(carriages);
        response.setPassengers(passengers);
        
        return response;
    }
    
    /**
     * 获取乘客类型名称
     */
    private String getPassengerTypeName(Byte passengerType) {
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
     * 获取关系类型名称
     */
    private String getRelationTypeName(Byte relationType) {
        switch (relationType) {
            case 1: return "本人";
            case 2: return "亲属";
            case 3: return "其他";
            default: return "未知";
        }
    }
} 