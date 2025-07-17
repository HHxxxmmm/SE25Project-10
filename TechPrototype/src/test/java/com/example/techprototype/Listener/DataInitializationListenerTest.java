package com.example.techprototype.Listener;

import com.example.techprototype.Event.DataInitializationCompletedEvent;
import com.example.techprototype.Service.InventorySyncService;
import com.example.techprototype.Service.ScheduledTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataInitializationListenerTest {
    
    @InjectMocks
    private DataInitializationListener listener;
    
    @Mock
    private InventorySyncService inventorySyncService;
    
    @Mock
    private ScheduledTaskService scheduledTaskService;
    
    private DataInitializationCompletedEvent event;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        event = new DataInitializationCompletedEvent();
    }
    
    @Test
    void testHandleDataInitializationCompleted_Success() throws Exception {
        doNothing().when(inventorySyncService).syncInventoryToDatabase();
        doNothing().when(scheduledTaskService).enableTasks();
        
        assertDoesNotThrow(() -> listener.handleDataInitializationCompleted(event));
        
        // 验证服务调用
        verify(inventorySyncService, timeout(1000)).syncInventoryToDatabase();
        verify(scheduledTaskService, timeout(10)).enableTasks();
    }
    
    @Test
    void testHandleDataInitializationCompleted_WithSource() throws Exception {
        Object source = new Object();
        DataInitializationCompletedEvent eventWithSource = new DataInitializationCompletedEvent(source);
        
        doNothing().when(inventorySyncService).syncInventoryToDatabase();
        doNothing().when(scheduledTaskService).enableTasks();
        
        assertDoesNotThrow(() -> listener.handleDataInitializationCompleted(eventWithSource));
        
        verify(inventorySyncService, timeout(1000)).syncInventoryToDatabase();
        verify(scheduledTaskService, timeout(10)).enableTasks();
    }
    
    @Test
    void testHandleDataInitializationCompleted_InventorySyncException() throws Exception {
        doThrow(new RuntimeException("Sync failed")).when(inventorySyncService).syncInventoryToDatabase();
        doNothing().when(scheduledTaskService).enableTasks();
        
        assertDoesNotThrow(() -> listener.handleDataInitializationCompleted(event));
        
        // 即使库存同步失败，定时任务仍应启动
        verify(scheduledTaskService, timeout(10)).enableTasks();
    }
    
    @Test
    void testHandleDataInitializationCompleted_ScheduledTaskException() throws Exception {
        doNothing().when(inventorySyncService).syncInventoryToDatabase();
        doThrow(new RuntimeException("Task enable failed")).when(scheduledTaskService).enableTasks();
        
        assertDoesNotThrow(() -> listener.handleDataInitializationCompleted(event));
        
        // 库存同步应成功
        verify(inventorySyncService, timeout(1000)).syncInventoryToDatabase();
    }
    
    @Test
    void testHandleDataInitializationCompleted_InterruptedException() throws Exception {
        // 模拟线程中断
        Thread.currentThread().interrupt();
        
        doNothing().when(inventorySyncService).syncInventoryToDatabase();
        doNothing().when(scheduledTaskService).enableTasks();
        
        assertDoesNotThrow(() -> listener.handleDataInitializationCompleted(event));
        
        // 验证线程中断状态被正确处理
        assertTrue(Thread.currentThread().isInterrupted());
        
        // 重置中断状态
        Thread.interrupted();
    }
    
    @Test
    void testHandleDataInitializationCompleted_ServiceOrder() throws Exception {
        doNothing().when(inventorySyncService).syncInventoryToDatabase();
        doNothing().when(scheduledTaskService).enableTasks();
        
        assertDoesNotThrow(() -> listener.handleDataInitializationCompleted(event));
        
        // 验证调用顺序：先同步库存，再启用定时任务
        verify(inventorySyncService, timeout(1000)).syncInventoryToDatabase();
        verify(scheduledTaskService, timeout(10)).enableTasks();
    }
} 
 