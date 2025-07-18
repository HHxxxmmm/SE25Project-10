package com.example.techprototype.Service;

import com.example.techprototype.DAO.TicketInventoryDAO;
import com.example.techprototype.Entity.TicketInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InventoryInitServiceTest {
    @InjectMocks
    private InventoryInitService inventoryInitService;

    @Mock
    private TicketInventoryDAO ticketInventoryDAO;

    @Mock
    private RedisService redisService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRun_WithExistingInventory() throws Exception {
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
        
        List<TicketInventory> existingInventories = Arrays.asList(inventory1, inventory2);
        
        when(ticketInventoryDAO.findAll()).thenReturn(existingInventories);
        doNothing().when(redisService).setStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt(), anyInt());

        assertDoesNotThrow(() -> inventoryInitService.run());
        
        verify(ticketInventoryDAO).findAll();
        verify(redisService, times(2)).setStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt(), anyInt());
        verify(ticketInventoryDAO, never()).save(any(TicketInventory.class));
    }

    @Test
    void testRun_WithEmptyInventory() throws Exception {
        // 创建有效的库存数据
        TicketInventory inventory1 = new TicketInventory();
        inventory1.setTrainId(1);
        inventory1.setDepartureStopId(1L);
        inventory1.setArrivalStopId(2L);
        inventory1.setTravelDate(LocalDate.of(2025, 7, 1));
        inventory1.setCarriageTypeId(1);
        inventory1.setAvailableSeats(100);
        
        TicketInventory inventory2 = new TicketInventory();
        inventory2.setTrainId(1);
        inventory2.setDepartureStopId(1L);
        inventory2.setArrivalStopId(2L);
        inventory2.setTravelDate(LocalDate.of(2025, 7, 1));
        inventory2.setCarriageTypeId(2);
        inventory2.setAvailableSeats(200);
        
        // 第一次调用返回空列表，第二次调用返回创建的默认数据
        when(ticketInventoryDAO.findAll())
            .thenReturn(Collections.emptyList())
            .thenReturn(Arrays.asList(inventory1, inventory2));
        doNothing().when(ticketInventoryDAO).save(any(TicketInventory.class));
        doNothing().when(redisService).setStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt(), anyInt());

        assertDoesNotThrow(() -> inventoryInitService.run());
        
        verify(ticketInventoryDAO, times(2)).findAll();
        verify(ticketInventoryDAO, atLeastOnce()).save(any(TicketInventory.class));
        verify(redisService, times(2)).setStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt(), anyInt());
    }

    @Test
    void testRun_ExceptionHandling() throws Exception {
        when(ticketInventoryDAO.findAll()).thenThrow(new RuntimeException("Database error"));

        assertDoesNotThrow(() -> inventoryInitService.run());
        
        verify(ticketInventoryDAO).findAll();
        verify(redisService, never()).setStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt(), anyInt());
    }
} 