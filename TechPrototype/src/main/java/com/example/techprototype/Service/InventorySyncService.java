package com.example.techprototype.Service;

import com.example.techprototype.DAO.TicketInventoryDAO;
import com.example.techprototype.Entity.TicketInventory;
import com.example.techprototype.Service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class InventorySyncService {
    
    @Autowired
    private TicketInventoryDAO ticketInventoryDAO;
    
    @Autowired
    private RedisService redisService;
    
    /**
     * 每30秒同步一次Redis库存到MySQL
     * Redis作为主数据源，数据库作为备份
     */
    @Scheduled(fixedRate = 30000) // 30秒
    @Transactional
    public void syncInventoryToDatabase() {
        try {
            // 获取所有库存数据
            List<TicketInventory> allInventories = ticketInventoryDAO.findAll();
            
            int syncCount = 0;
            for (TicketInventory inventory : allInventories) {
                // 从Redis获取当前库存
                Optional<Integer> redisStock = redisService.getStock(
                    inventory.getTrainId(),
                    inventory.getDepartureStopId(),
                    inventory.getArrivalStopId(),
                    inventory.getTravelDate(),
                    inventory.getCarriageTypeId()
                );
                
                if (redisStock.isPresent()) {
                    int currentRedisStock = redisStock.get();
                    int currentDbStock = inventory.getAvailableSeats();
                    
                    // 直接同步Redis库存到数据库，Redis作为主数据源
                    if (currentRedisStock != currentDbStock) {
                        inventory.setAvailableSeats(currentRedisStock);
                        inventory.setCacheVersion(inventory.getCacheVersion() + 1);
                        inventory.setDbVersion(inventory.getDbVersion() + 1);
                        ticketInventoryDAO.save(inventory);
                        syncCount++;
                        System.out.println("同步库存到数据库: " + inventory.getTrainId() + ":" + 
                                         inventory.getDepartureStopId() + ":" + inventory.getArrivalStopId() + 
                                         ":" + inventory.getTravelDate() + ":" + inventory.getCarriageTypeId() + 
                                         " 从 " + currentDbStock + " 更新为 " + currentRedisStock);
                    }
                }
            }
            
            if (syncCount > 0) {
                System.out.println("库存同步完成: " + LocalDate.now() + ", 同步了 " + syncCount + " 条记录");
            }
        } catch (Exception e) {
            System.err.println("库存同步失败: " + e.getMessage());
        }
    }
    
    /**
     * 每天凌晨2点同步一次MySQL库存到Redis（作为备份和恢复）
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点
    public void syncDatabaseToRedis() {
        try {
            // 获取所有库存数据
            List<TicketInventory> allInventories = ticketInventoryDAO.findAll();
            
            int backupCount = 0;
            for (TicketInventory inventory : allInventories) {
                redisService.setStock(
                    inventory.getTrainId(),
                    inventory.getDepartureStopId(),
                    inventory.getArrivalStopId(),
                    inventory.getTravelDate(),
                    inventory.getCarriageTypeId(),
                    inventory.getAvailableSeats()
                );
                backupCount++;
            }
            
            System.out.println("Redis库存备份完成: " + LocalDate.now() + ", 备份了 " + backupCount + " 条记录");
        } catch (Exception e) {
            System.err.println("Redis库存备份失败: " + e.getMessage());
        }
    }
} 