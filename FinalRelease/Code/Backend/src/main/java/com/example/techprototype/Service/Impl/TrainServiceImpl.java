package com.example.techprototype.Service.Impl;

import com.example.techprototype.DAO.TrainDAO;
import com.example.techprototype.DTO.TrainListDTO;
import com.example.techprototype.Entity.Train;
import com.example.techprototype.Entity.TrainStop;
import com.example.techprototype.Entity.Station;
import com.example.techprototype.Entity.TicketInventory;
import com.example.techprototype.Repository.TrainRepository;
import com.example.techprototype.Repository.TrainStopRepository;
import com.example.techprototype.Repository.StationRepository;
import com.example.techprototype.Repository.TicketInventoryRepository;
import com.example.techprototype.Service.TrainService;
import com.example.techprototype.Service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TrainServiceImpl implements TrainService {
    @Autowired
    private TrainDAO trainDAO;
    @Autowired
    private TrainRepository trainRepository;
    @Autowired
    private TrainStopRepository trainStopRepository;
    @Autowired
    private StationRepository stationRepository;
    @Autowired
    private TicketInventoryRepository ticketInventoryRepository;
    @Autowired
    private RedisService redisService;

    @Override
    public List<Train> findDirectTrains(Integer startStationId, Integer endStationId) {
        return trainDAO.findDirectTrains(startStationId, endStationId);
    }

    @Override
    public List<Train> findTrainsByDepartureTime(LocalTime start, LocalTime end) {
        return trainDAO.findByDepartureTimeBetween(start, end);
    }

    @Override
    public List<Map<String, Object>> findTransferTrains(Integer startStationId, Integer endStationId) {
        // 使用DAO层查询中转站
        List<Integer> transferStations = trainDAO.findAllTransferStations(startStationId, endStationId);

        List<Map<String, Object>> result = new ArrayList<>();
        // 性能优化：限制最大中转站数量
        int maxTransferStations = Math.min(transferStations.size(), 10);

        for (int i = 0; i < maxTransferStations; i++) {
            Integer transferStationId = transferStations.get(i);
            result.addAll(trainDAO.findTransferTrains(startStationId, transferStationId, endStationId));
        }
        return result;
    }

    @Override
    public List<TrainListDTO> getAllTrainListDTO() {
        List<Train> trains = trainRepository.findAll();
        List<TrainListDTO> result = new ArrayList<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        // 强制以2025-07-01为标准日期
        LocalDate today = LocalDate.of(2025, 7, 1);
        
        for (Train train : trains) {
            TrainListDTO dto = new TrainListDTO();
            dto.setTrain_id(train.getTrainNumber());
            
            // 查询所有停靠站
            List<TrainStop> stops = trainStopRepository.findByTrainIdOrderBySequenceNumberAsc(train.getTrainId());
            List<String> path = new ArrayList<>();
            for (TrainStop stop : stops) {
                stationRepository.findById(stop.getStationId()).ifPresent(station -> path.add(station.getStationName()));
            }
            dto.setT_path(path);
            
            if (!path.isEmpty()) {
                dto.setT_from(path.get(0));
                dto.setT_to(path.get(path.size() - 1));
                
                // 计算当前区间的站数（从首站到末站）
                if (stops.size() >= 2) {
                    TrainStop firstStop = stops.get(0);
                    TrainStop lastStop = stops.get(stops.size() - 1);
                    int stationCount = lastStop.getSequenceNumber() - firstStop.getSequenceNumber() + 1;
                    dto.setT_station_number(stationCount);
                } else {
                    dto.setT_station_number(path.size());
                }
            } else {
                dto.setT_station_number(0);
            }
            
            dto.setT_start_time(train.getDepartureTime().atDate(today).format(dtf));
            dto.setT_end_time(train.getArrivalTime().atDate(today).format(dtf));
            
            // 获取真实的座位信息（使用首站到末站作为代表性路线）
            List<Integer> seatTypes = new ArrayList<>();
            List<Integer> seatNumbers = new ArrayList<>();
            List<Integer> seatPrices = new ArrayList<>();
            
            if (stops.size() >= 2) {
                TrainStop firstStop = stops.get(0);
                TrainStop lastStop = stops.get(stops.size() - 1);
                
                List<Map<String, Object>> seatInfo = getRealSeatInfo(train.getTrainId(), firstStop.getStopId(), lastStop.getStopId(), today);
                
                for (Map<String, Object> seat : seatInfo) {
                    seatTypes.add((Integer) seat.get("type"));
                    seatNumbers.add((Integer) seat.get("available"));
                    // 正确处理BigDecimal价格，转换为Integer（价格以分为单位）
                    Object priceObj = seat.get("price");
                    Integer price;
                    if (priceObj instanceof BigDecimal) {
                        price = ((BigDecimal) priceObj).intValue();
                    } else if (priceObj instanceof Double) {
                        price = ((Double) priceObj).intValue();
                    } else if (priceObj instanceof Integer) {
                        price = (Integer) priceObj;
                    } else {
                        price = 150; // 默认价格
                    }
                    seatPrices.add(price);
                }
            }
            
            // 如果没有找到真实数据，使用默认数据
            if (seatTypes.isEmpty()) {
                seatTypes = Arrays.asList(1, 3, 4);
                seatNumbers = Arrays.asList(100, 200, 50);
                seatPrices = Arrays.asList(200, 150, 80);
            }
            
            dto.setSeat(seatTypes);
            dto.setSeat_number(seatNumbers);
            dto.setSeat_price(seatPrices);
            
            result.add(dto);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> searchTrainsByStations(String fromStation, String toStation, String travelDate) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        System.out.println("搜索车次 - 出发站: " + fromStation + ", 到达站: " + toStation + ", 日期: " + travelDate);
        
        try {
            // 1. 根据站点名称查找站点ID
            List<Station> fromStations = stationRepository.findByStationNameContainingOrCityContaining(fromStation, fromStation);
            List<Station> toStations = stationRepository.findByStationNameContainingOrCityContaining(toStation, toStation);
            
            System.out.println("找到出发站: " + fromStations.size() + " 个");
            System.out.println("找到到达站: " + toStations.size() + " 个");
            
            if (fromStations.isEmpty() || toStations.isEmpty()) {
                System.out.println("未找到匹配的站点，返回空结果");
                return result;
            }
            
            LocalDate travelDateObj = LocalDate.parse(travelDate);
            
            // 2. 查找包含这些站点的车次
            for (Station fromStationObj : fromStations) {
                for (Station toStationObj : toStations) {
                    System.out.println("检查路线: " + fromStationObj.getStationName() + " -> " + toStationObj.getStationName());
                    
                    // 查找从fromStation到toStation的车次
                    List<TrainStop> fromStops = trainStopRepository.findByStationId(fromStationObj.getStationId());
                    List<TrainStop> toStops = trainStopRepository.findByStationId(toStationObj.getStationId());
                    
                    System.out.println("出发站停靠车次: " + fromStops.size() + " 个");
                    System.out.println("到达站停靠车次: " + toStops.size() + " 个");
                    
                    // 找到同时包含两个站点的车次
                    Set<Integer> commonTrainIds = new HashSet<>();
                    for (TrainStop fromStop : fromStops) {
                        for (TrainStop toStop : toStops) {
                            if (fromStop.getTrainId().equals(toStop.getTrainId()) && 
                                fromStop.getSequenceNumber() < toStop.getSequenceNumber()) {
                                commonTrainIds.add(fromStop.getTrainId());
                            }
                        }
                    }
                    
                    System.out.println("找到符合条件的车次: " + commonTrainIds.size() + " 个");
                    
                    // 3. 为每个车次构建详细信息
                    for (Integer trainId : commonTrainIds) {
                        Train train = trainRepository.findById(trainId).orElse(null);
                        if (train == null) continue;
                        
                        TrainStop fromStop = trainStopRepository.findByTrainIdAndStationId(trainId, fromStationObj.getStationId());
                        TrainStop toStop = trainStopRepository.findByTrainIdAndStationId(trainId, toStationObj.getStationId());
                        
                        if (fromStop == null || toStop == null) continue;
                        
                        System.out.println("构建车次信息: " + train.getTrainNumber());
                        
                        // 构建与TrainListDTO格式一致的数据
                        Map<String, Object> trainInfo = new HashMap<>();
                        trainInfo.put("train_id", train.getTrainNumber());
                        trainInfo.put("trainId", train.getTrainId());
                        trainInfo.put("t_from", fromStationObj.getStationName());
                        trainInfo.put("t_to", toStationObj.getStationName());
                        trainInfo.put("t_start_time", fromStop.getDepartureTime().atDate(travelDateObj).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                        trainInfo.put("t_end_time", toStop.getArrivalTime().atDate(travelDateObj).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                        trainInfo.put("travelDate", travelDate);
                        
                        // 构建途经站点信息
                        List<TrainStop> allStops = trainStopRepository.findByTrainIdOrderBySequenceNumberAsc(trainId);
                        List<String> path = new ArrayList<>();
                        for (TrainStop stop : allStops) {
                            stationRepository.findById(stop.getStationId()).ifPresent(station -> path.add(station.getStationName()));
                        }
                        trainInfo.put("t_path", path);
                        
                        // 计算当前区间的站数（从出发站到到达站）
                        int stationCount = toStop.getSequenceNumber() - fromStop.getSequenceNumber() + 1;
                        trainInfo.put("t_station_number", stationCount);
                        
                        // 从ticket_inventory获取真实票价和从Redis获取库存
                        List<Map<String, Object>> seatInfo = getRealSeatInfo(trainId, fromStop.getStopId(), toStop.getStopId(), travelDateObj);
                        trainInfo.put("seat", seatInfo.stream().map(seat -> seat.get("type")).collect(Collectors.toList()));
                        trainInfo.put("seat_number", seatInfo.stream().map(seat -> seat.get("available")).collect(Collectors.toList()));
                        trainInfo.put("seat_price", seatInfo.stream().map(seat -> seat.get("price")).collect(Collectors.toList()));
                        
                        // 添加库存ID列表
                        List<Long> inventoryIds = seatInfo.stream()
                            .map(seat -> (Long) seat.get("inventoryId"))
                            .filter(id -> id != null)
                            .collect(Collectors.toList());
                        trainInfo.put("inventory_ids", inventoryIds);
                        
                        result.add(trainInfo);
                    }
                }
            }
            
            // 按车次号排序
            result.sort((a, b) -> {
                String trainA = (String) a.get("train_id");
                String trainB = (String) b.get("train_id");
                return trainA.compareTo(trainB);
            });
            
            System.out.println("最终搜索结果: " + result.size() + " 个车次");
            
        } catch (Exception e) {
            System.out.println("搜索车次时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }
    
    private List<Map<String, Object>> getRealSeatInfo(Integer trainId, Long fromStopId, Long toStopId, LocalDate travelDate) {
        List<Map<String, Object>> seatInfo = new ArrayList<>();
        
        try {
            System.out.println("查询座位信息 - 车次ID: " + trainId + ", 出发站ID: " + fromStopId + ", 到达站ID: " + toStopId + ", 日期: " + travelDate);
            
            // 查询ticket_inventory表获取票价信息
            List<TicketInventory> inventories = ticketInventoryRepository.findByTrainAndDate(trainId, travelDate);
            
            System.out.println("找到库存记录: " + inventories.size() + " 条");
            
            for (TicketInventory inventory : inventories) {
                System.out.println("检查库存记录 - 出发站ID: " + inventory.getDepartureStopId() + ", 到达站ID: " + inventory.getArrivalStopId() + 
                                 ", 车厢类型: " + inventory.getCarriageTypeId() + ", 票价: " + inventory.getPrice());
                
                // 只处理匹配的出发站和到达站
                if (inventory.getDepartureStopId().equals(fromStopId) && inventory.getArrivalStopId().equals(toStopId)) {
                    Map<String, Object> seat = new HashMap<>();
                    
                    // 获取席别类型名称
                    String seatTypeName = getSeatTypeName(inventory.getCarriageTypeId());
                    seat.put("type", inventory.getCarriageTypeId());
                    seat.put("typeName", seatTypeName);
                    seat.put("price", inventory.getPrice());
                    seat.put("inventoryId", inventory.getInventoryId()); // 添加库存ID
                    
                    // 从Redis获取库存
                    Optional<Integer> available = redisService.getStock(trainId, fromStopId, toStopId, travelDate, inventory.getCarriageTypeId());
                    int stockCount = available.orElse(inventory.getAvailableSeats()); // 如果Redis没有，使用数据库中的值
                    seat.put("available", stockCount);
                    
                    System.out.println("添加座位信息 - 类型: " + seatTypeName + ", 票价: " + inventory.getPrice() + ", 库存: " + stockCount);
                    seatInfo.add(seat);
                }
            }
            
            if (seatInfo.isEmpty()) {
                System.out.println("未找到匹配的座位信息，返回默认数据");
                // 如果没找到匹配的记录，返回默认数据
                Map<String, Object> defaultSeat = new HashMap<>();
                defaultSeat.put("type", 3);
                defaultSeat.put("typeName", "二等座");
                defaultSeat.put("price", 150.0);
                defaultSeat.put("available", 0);
                seatInfo.add(defaultSeat);
            }
            
        } catch (Exception e) {
            System.out.println("获取座位信息时发生错误: " + e.getMessage());
            e.printStackTrace();
            // 如果出错，返回默认数据
            Map<String, Object> defaultSeat = new HashMap<>();
            defaultSeat.put("type", 3);
            defaultSeat.put("typeName", "二等座");
            defaultSeat.put("price", 150.0);
            defaultSeat.put("available", 0);
            seatInfo.add(defaultSeat);
        }
        
        return seatInfo;
    }
    
    private String getSeatTypeName(Integer carriageTypeId) {
        switch (carriageTypeId) {
            case 1: return "商务座";
            case 2: return "一等座";
            case 3: return "二等座";
            case 4: return "硬座";
            case 5: return "硬卧";
            case 6: return "无座";
            default: return "未知";
        }
    }
}

