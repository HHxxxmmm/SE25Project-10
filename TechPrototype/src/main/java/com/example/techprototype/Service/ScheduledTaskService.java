package com.example.techprototype.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ScheduledTaskService {
    
    @Autowired
    private InventorySyncService inventorySyncService;
    
    private volatile boolean tasksEnabled = false;
    
    /**
     * 启用定时任务
     */
    public void enableTasks() {
        this.tasksEnabled = true;
        System.out.println("定时任务已启用");
    }
    
    /**
     * 每30秒同步一次Redis库存到数据库
     */
    @Scheduled(fixedRate = 30000) // 30秒
    public void scheduledInventorySync() {
        if (tasksEnabled) {
            inventorySyncService.syncInventoryToDatabase();
        }
    }
} 