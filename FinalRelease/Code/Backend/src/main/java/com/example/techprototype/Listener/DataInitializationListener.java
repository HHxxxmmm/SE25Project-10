package com.example.techprototype.Listener;

import com.example.techprototype.Event.DataInitializationCompletedEvent;
import com.example.techprototype.Service.InventorySyncService;
import com.example.techprototype.Service.ScheduledTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class DataInitializationListener {
    
    @Autowired
    private InventorySyncService inventorySyncService;
    
    @Autowired
    private ScheduledTaskService scheduledTaskService;
    
    @EventListener
    @Async
    public void handleDataInitializationCompleted(DataInitializationCompletedEvent event) {
        System.out.println("收到数据初始化完成事件，开始启动定时任务...");
        
        // 等待5秒后开始第一次同步
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        
        // 启动第一次库存同步（Redis到数据库）
        System.out.println("开始第一次库存同步...");
        inventorySyncService.syncInventoryToDatabase();
        
        // 启用定时任务
        scheduledTaskService.enableTasks();
        
        System.out.println("所有定时任务已启动");
    }
} 