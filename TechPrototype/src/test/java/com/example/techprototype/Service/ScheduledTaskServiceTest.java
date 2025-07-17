package com.example.techprototype.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScheduledTaskServiceTest {
    @InjectMocks
    private ScheduledTaskService scheduledTaskService;

    @Mock
    private InventorySyncService inventorySyncService;

    @Mock
    private OrderTimeoutService orderTimeoutService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testEnableTasks() {
        scheduledTaskService.enableTasks();
        
        // 验证任务已启用（通过调用定时任务方法来验证）
        doNothing().when(inventorySyncService).syncInventoryToDatabase();
        doNothing().when(orderTimeoutService).handleTimeoutOrders();
        
        scheduledTaskService.scheduledInventorySync();
        scheduledTaskService.scheduledOrderTimeoutCheck();
        
        verify(inventorySyncService).syncInventoryToDatabase();
        verify(orderTimeoutService).handleTimeoutOrders();
    }

    @Test
    void testScheduledInventorySync_WhenDisabled() {
        // 默认情况下任务是禁用的
        scheduledTaskService.scheduledInventorySync();
        
        verify(inventorySyncService, never()).syncInventoryToDatabase();
    }

    @Test
    void testScheduledOrderTimeoutCheck_WhenDisabled() {
        // 默认情况下任务是禁用的
        scheduledTaskService.scheduledOrderTimeoutCheck();
        
        verify(orderTimeoutService, never()).handleTimeoutOrders();
    }

    @Test
    void testScheduledInventorySync_WhenEnabled() {
        scheduledTaskService.enableTasks();
        
        doNothing().when(inventorySyncService).syncInventoryToDatabase();
        scheduledTaskService.scheduledInventorySync();
        
        verify(inventorySyncService).syncInventoryToDatabase();
    }

    @Test
    void testScheduledOrderTimeoutCheck_WhenEnabled() {
        scheduledTaskService.enableTasks();
        
        doNothing().when(orderTimeoutService).handleTimeoutOrders();
        scheduledTaskService.scheduledOrderTimeoutCheck();
        
        verify(orderTimeoutService).handleTimeoutOrders();
    }
} 