package com.example.techprototype.Service;

import com.example.techprototype.DAO.TicketInventoryDAO;
import com.example.techprototype.Entity.TicketInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InventorySyncServiceTest {
    @InjectMocks
    private InventorySyncService inventorySyncService;

    @Mock
    private TicketInventoryDAO ticketInventoryDAO;

    @Mock
    private RedisService redisService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSyncInventoryToDatabase_NoInventories() {
        when(ticketInventoryDAO.findAll()).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> inventorySyncService.syncInventoryToDatabase());
        
        verify(ticketInventoryDAO).findAll();
        verify(redisService, never()).getStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt());
    }

    @Test
    void testSyncInventoryToDatabase_WithInventories_NoChanges() {
        // 创建测试库存数据
        TicketInventory inventory = new TicketInventory();
        inventory.setTrainId(1);
        inventory.setDepartureStopId(1L);
        inventory.setArrivalStopId(2L);
        inventory.setTravelDate(LocalDate.now().plusDays(1));
        inventory.setCarriageTypeId(1);
        inventory.setAvailableSeats(100);
        inventory.setCacheVersion(1L);
        inventory.setDbVersion(1);
        
        List<TicketInventory> inventories = Arrays.asList(inventory);
        
        when(ticketInventoryDAO.findAll()).thenReturn(inventories);
        when(redisService.getStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt()))
            .thenReturn(Optional.of(100)); // Redis库存与数据库库存相同
        doNothing().when(ticketInventoryDAO).save(any(TicketInventory.class));

        assertDoesNotThrow(() -> inventorySyncService.syncInventoryToDatabase());
        
        verify(ticketInventoryDAO).findAll();
        verify(redisService).getStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt());
        verify(ticketInventoryDAO, never()).save(any(TicketInventory.class));
    }

    @Test
    void testSyncInventoryToDatabase_WithInventories_WithChanges() {
        // 创建测试库存数据
        TicketInventory inventory = new TicketInventory();
        inventory.setTrainId(1);
        inventory.setDepartureStopId(1L);
        inventory.setArrivalStopId(2L);
        inventory.setTravelDate(LocalDate.now().plusDays(1));
        inventory.setCarriageTypeId(1);
        inventory.setAvailableSeats(100); // 数据库库存
        inventory.setCacheVersion(1L);
        inventory.setDbVersion(1);
        
        List<TicketInventory> inventories = Arrays.asList(inventory);
        
        when(ticketInventoryDAO.findAll()).thenReturn(inventories);
        when(redisService.getStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt()))
            .thenReturn(Optional.of(80)); // Redis库存与数据库库存不同
        doNothing().when(ticketInventoryDAO).save(any(TicketInventory.class));

        assertDoesNotThrow(() -> inventorySyncService.syncInventoryToDatabase());
        
        verify(ticketInventoryDAO).findAll();
        verify(redisService).getStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt());
        verify(ticketInventoryDAO).save(any(TicketInventory.class));
    }

    @Test
    void testSyncInventoryToDatabase_RedisStockNotPresent() {
        // 创建测试库存数据
        TicketInventory inventory = new TicketInventory();
        inventory.setTrainId(1);
        inventory.setDepartureStopId(1L);
        inventory.setArrivalStopId(2L);
        inventory.setTravelDate(LocalDate.now().plusDays(1));
        inventory.setCarriageTypeId(1);
        inventory.setAvailableSeats(100);
        
        List<TicketInventory> inventories = Arrays.asList(inventory);
        
        when(ticketInventoryDAO.findAll()).thenReturn(inventories);
        when(redisService.getStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt()))
            .thenReturn(Optional.empty()); // Redis中没有库存数据

        assertDoesNotThrow(() -> inventorySyncService.syncInventoryToDatabase());
        
        verify(ticketInventoryDAO).findAll();
        verify(redisService).getStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt());
        verify(ticketInventoryDAO, never()).save(any(TicketInventory.class));
    }

    @Test
    void testSyncInventoryToDatabase_ExceptionHandling() {
        when(ticketInventoryDAO.findAll()).thenThrow(new RuntimeException("Database error"));

        assertDoesNotThrow(() -> inventorySyncService.syncInventoryToDatabase());
        
        verify(ticketInventoryDAO).findAll();
        verify(redisService, never()).getStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt());
    }

    @Test
    void testSyncDatabaseToRedis_WithInventories() {
        // 创建测试库存数据
        TicketInventory inventory1 = new TicketInventory();
        inventory1.setTrainId(1);
        inventory1.setDepartureStopId(1L);
        inventory1.setArrivalStopId(2L);
        inventory1.setTravelDate(LocalDate.now().plusDays(1));
        inventory1.setCarriageTypeId(1);
        inventory1.setAvailableSeats(100);
        
        TicketInventory inventory2 = new TicketInventory();
        inventory2.setTrainId(1);
        inventory2.setDepartureStopId(1L);
        inventory2.setArrivalStopId(2L);
        inventory2.setTravelDate(LocalDate.now().plusDays(1));
        inventory2.setCarriageTypeId(2);
        inventory2.setAvailableSeats(200);
        
        List<TicketInventory> inventories = Arrays.asList(inventory1, inventory2);
        
        when(ticketInventoryDAO.findAll()).thenReturn(inventories);
        doNothing().when(redisService).setStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt(), anyInt());

        assertDoesNotThrow(() -> inventorySyncService.syncDatabaseToRedis());
        
        verify(ticketInventoryDAO).findAll();
        verify(redisService, times(2)).setStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt(), anyInt());
    }

    @Test
    void testSyncDatabaseToRedis_NoInventories() {
        when(ticketInventoryDAO.findAll()).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> inventorySyncService.syncDatabaseToRedis());
        
        verify(ticketInventoryDAO).findAll();
        verify(redisService, never()).setStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt(), anyInt());
    }

    @Test
    void testSyncDatabaseToRedis_ExceptionHandling() {
        when(ticketInventoryDAO.findAll()).thenThrow(new RuntimeException("Database error"));

        assertDoesNotThrow(() -> inventorySyncService.syncDatabaseToRedis());
        
        verify(ticketInventoryDAO).findAll();
        verify(redisService, never()).setStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt(), anyInt());
    }
} 