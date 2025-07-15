package com.example.techprototype.Service;

import com.example.techprototype.DAO.TicketInventoryDAO;
import com.example.techprototype.Entity.TicketInventory;
import com.example.techprototype.Service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class InventoryInitService implements CommandLineRunner {
    
    @Autowired
    private TicketInventoryDAO ticketInventoryDAO;
    
    @Autowired
    private RedisService redisService;
    
    @Override
    public void run(String... args) throws Exception {
        System.out.println("开始初始化Redis库存...");
        
        // 直接用数据库数据覆盖Redis
        loadAllInventoryFromDatabase();
        
        System.out.println("Redis库存初始化完成");
    }
    
    /**
     * 从数据库加载所有库存数据到Redis
     */
    private void loadAllInventoryFromDatabase() {
        try {
            // 获取数据库中的所有库存数据
            List<TicketInventory> allInventories = ticketInventoryDAO.findAll();
            
            if (allInventories.isEmpty()) {
                System.out.println("数据库中没有库存数据，创建默认测试数据...");
                createDefaultTestData();
                // 重新获取所有数据
                allInventories = ticketInventoryDAO.findAll();
            }
            
            int loadedCount = 0;
            for (TicketInventory inventory : allInventories) {
                redisService.setStock(
                    inventory.getTrainId(),
                    inventory.getDepartureStopId(),
                    inventory.getArrivalStopId(),
                    inventory.getTravelDate(),
                    inventory.getCarriageTypeId(),
                    inventory.getAvailableSeats()
                );
                loadedCount++;
            }
            
            System.out.println("成功加载 " + loadedCount + " 条库存数据到Redis");
            
        } catch (Exception e) {
            System.err.println("加载库存数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void createDefaultTestData() {
        LocalDate startDate = LocalDate.of(2025, 7, 1);
        LocalDate endDate = LocalDate.of(2025, 7, 31); // 扩展到7月31日
        
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            // 为G101车次创建默认库存数据
            // 车次ID=1, 出发站ID=1(北京西), 到达站ID=2(上海虹桥), 商务座=1, 一等座=2, 二等座=3
            createInventoryRecord(1, 1L, 2L, date, 1, 100, BigDecimal.valueOf(500.0)); // 商务座100张
            createInventoryRecord(1, 1L, 2L, date, 2, 200, BigDecimal.valueOf(300.0)); // 一等座200张
            createInventoryRecord(1, 1L, 2L, date, 3, 500, BigDecimal.valueOf(200.0)); // 二等座500张
            
            // 其他区间组合
            createInventoryRecord(1, 1L, 3L, date, 1, 80, BigDecimal.valueOf(400.0));  // 北京西→南京南 商务座
            createInventoryRecord(1, 1L, 3L, date, 2, 150, BigDecimal.valueOf(250.0)); // 北京西→南京南 一等座
            createInventoryRecord(1, 1L, 3L, date, 3, 400, BigDecimal.valueOf(150.0)); // 北京西→南京南 二等座
            
            createInventoryRecord(1, 3L, 2L, date, 1, 60, BigDecimal.valueOf(300.0));  // 南京南→上海虹桥 商务座
            createInventoryRecord(1, 3L, 2L, date, 2, 120, BigDecimal.valueOf(200.0)); // 南京南→上海虹桥 一等座
            createInventoryRecord(1, 3L, 2L, date, 3, 300, BigDecimal.valueOf(120.0)); // 南京南→上海虹桥 二等座
        }
    }
    
    private void createInventoryRecord(Integer trainId, Long departureStopId, Long arrivalStopId, 
                                     LocalDate travelDate, Integer carriageTypeId, int totalSeats, BigDecimal price) {
        TicketInventory inventory = new TicketInventory();
        inventory.setTrainId(trainId);
        inventory.setDepartureStopId(departureStopId);
        inventory.setArrivalStopId(arrivalStopId);
        inventory.setTravelDate(travelDate);
        inventory.setCarriageTypeId(carriageTypeId);
        inventory.setTotalSeats(totalSeats);
        inventory.setAvailableSeats(totalSeats); // 初始可用座位等于总座位
        inventory.setPrice(price);
        inventory.setCacheVersion(0L);
        inventory.setDbVersion(0);
        
        ticketInventoryDAO.save(inventory);
    }
} 