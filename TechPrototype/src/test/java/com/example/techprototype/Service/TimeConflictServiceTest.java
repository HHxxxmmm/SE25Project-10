package com.example.techprototype.Service;

import com.example.techprototype.Entity.Ticket;
import com.example.techprototype.Entity.TrainStop;
import com.example.techprototype.Repository.TicketRepository;
import com.example.techprototype.Repository.TrainStopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TimeConflictServiceTest {
    @InjectMocks
    private TimeConflictService timeConflictService;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TrainStopRepository trainStopRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCheckTimeConflict() {
        // 设置Mock行为
        TrainStop departureStop = new TrainStop();
        departureStop.setDepartureTime(LocalTime.of(8, 0));
        
        TrainStop arrivalStop = new TrainStop();
        arrivalStop.setArrivalTime(LocalTime.of(10, 0));
        
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findByStopId(2L)).thenReturn(Optional.of(arrivalStop));
        when(ticketRepository.findValidTicketsByPassengerAndDate(anyLong(), any(LocalDate.class)))
            .thenReturn(Collections.emptyList());

        List<Ticket> result = timeConflictService.checkTimeConflict(1L, LocalDate.now(), 1, 1L, 2L);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testCheckTimeConflictWithExclude() {
        // 设置Mock行为
        TrainStop departureStop = new TrainStop();
        departureStop.setDepartureTime(LocalTime.of(8, 0));
        
        TrainStop arrivalStop = new TrainStop();
        arrivalStop.setArrivalTime(LocalTime.of(10, 0));
        
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findByStopId(2L)).thenReturn(Optional.of(arrivalStop));
        when(ticketRepository.findValidTicketsByPassengerAndDate(anyLong(), any(LocalDate.class), anyLong()))
            .thenReturn(Collections.emptyList());

        List<Ticket> result = timeConflictService.checkTimeConflict(1L, LocalDate.now(), 1, 1L, 2L, 1L);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testCheckTimeConflict_WithNullDepartureTime() {
        // 设置Mock行为 - 出发时间为null
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.empty());
        when(trainStopRepository.findByStopId(2L)).thenReturn(Optional.empty());

        List<Ticket> result = timeConflictService.checkTimeConflict(1L, LocalDate.now(), 1, 1L, 2L);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(ticketRepository, never()).findValidTicketsByPassengerAndDate(anyLong(), any(LocalDate.class));
    }

    @Test
    void testCheckTimeConflict_WithNullArrivalTime() {
        // 设置Mock行为 - 出发时间有值，到达时间为null
        TrainStop departureStop = new TrainStop();
        departureStop.setDepartureTime(LocalTime.of(8, 0));
        
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findByStopId(2L)).thenReturn(Optional.empty());

        List<Ticket> result = timeConflictService.checkTimeConflict(1L, LocalDate.now(), 1, 1L, 2L);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(ticketRepository, never()).findValidTicketsByPassengerAndDate(anyLong(), any(LocalDate.class));
    }

    @Test
    void testCheckTimeConflict_WithOvernightTicket() {
        // 设置Mock行为 - 跨天车票（到达时间早于出发时间）
        TrainStop departureStop = new TrainStop();
        departureStop.setDepartureTime(LocalTime.of(22, 0)); // 晚上10点出发
        
        TrainStop arrivalStop = new TrainStop();
        arrivalStop.setArrivalTime(LocalTime.of(6, 0)); // 早上6点到达
        
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findByStopId(2L)).thenReturn(Optional.of(arrivalStop));
        when(ticketRepository.findValidTicketsByPassengerAndDate(anyLong(), any(LocalDate.class)))
            .thenReturn(Collections.emptyList());

        List<Ticket> result = timeConflictService.checkTimeConflict(1L, LocalDate.now(), 1, 1L, 2L);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testCheckTimeConflict_WithTimeConflict() {
        // 设置Mock行为
        TrainStop departureStop = new TrainStop();
        departureStop.setDepartureTime(LocalTime.of(8, 0));
        
        TrainStop arrivalStop = new TrainStop();
        arrivalStop.setArrivalTime(LocalTime.of(10, 0));
        
        // 创建冲突的车票
        Ticket conflictTicket = new Ticket();
        conflictTicket.setTicketId(1L);
        conflictTicket.setTrainId(2);
        conflictTicket.setDepartureStopId(3L);
        conflictTicket.setArrivalStopId(4L);
        conflictTicket.setTravelDate(LocalDate.now());
        conflictTicket.setTicketNumber("T001");
        
        TrainStop existingDepartureStop = new TrainStop();
        existingDepartureStop.setDepartureTime(LocalTime.of(9, 0)); // 与新票时间重叠
        
        TrainStop existingArrivalStop = new TrainStop();
        existingArrivalStop.setArrivalTime(LocalTime.of(11, 0));
        
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findByStopId(2L)).thenReturn(Optional.of(arrivalStop));
        when(trainStopRepository.findByStopId(3L)).thenReturn(Optional.of(existingDepartureStop));
        when(trainStopRepository.findByStopId(4L)).thenReturn(Optional.of(existingArrivalStop));
        
        // 只在当天返回冲突车票，前一天返回空列表
        when(ticketRepository.findValidTicketsByPassengerAndDate(anyLong(), eq(LocalDate.now().minusDays(1))))
            .thenReturn(Collections.emptyList());
        when(ticketRepository.findValidTicketsByPassengerAndDate(anyLong(), eq(LocalDate.now())))
            .thenReturn(Arrays.asList(conflictTicket));

        List<Ticket> result = timeConflictService.checkTimeConflict(1L, LocalDate.now(), 1, 1L, 2L);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(conflictTicket, result.get(0));
    }

    @Test
    void testGenerateConflictMessage_EmptyList() {
        String result = timeConflictService.generateConflictMessage(Collections.emptyList());
        assertEquals("", result);
    }

    @Test
    void testGenerateConflictMessage_WithConflicts() {
        // 创建冲突车票
        Ticket conflictTicket = new Ticket();
        conflictTicket.setTicketId(1L);
        conflictTicket.setTrainId(2);
        conflictTicket.setDepartureStopId(3L);
        conflictTicket.setArrivalStopId(4L);
        conflictTicket.setTravelDate(LocalDate.now());
        conflictTicket.setTicketNumber("T001");
        
        TrainStop departureStop = new TrainStop();
        departureStop.setDepartureTime(LocalTime.of(9, 0));
        
        TrainStop arrivalStop = new TrainStop();
        arrivalStop.setArrivalTime(LocalTime.of(11, 0));
        
        when(trainStopRepository.findByStopId(3L)).thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findByStopId(4L)).thenReturn(Optional.of(arrivalStop));
        
        List<Ticket> conflictTickets = Arrays.asList(conflictTicket);
        String result = timeConflictService.generateConflictMessage(conflictTickets);
        
        assertNotNull(result);
        assertTrue(result.contains("乘客在同一时间段内已有其他车票，存在时间冲突"));
        assertTrue(result.contains("T001"));
        assertTrue(result.contains("09:00"));
        assertTrue(result.contains("11:00"));
    }

    @Test
    void testGenerateConflictMessage_WithOvernightTicket() {
        // 创建跨天冲突车票
        Ticket conflictTicket = new Ticket();
        conflictTicket.setTicketId(1L);
        conflictTicket.setTrainId(2);
        conflictTicket.setDepartureStopId(3L);
        conflictTicket.setArrivalStopId(4L);
        conflictTicket.setTravelDate(LocalDate.now());
        conflictTicket.setTicketNumber("T001");
        
        TrainStop departureStop = new TrainStop();
        departureStop.setDepartureTime(LocalTime.of(22, 0)); // 晚上10点出发
        
        TrainStop arrivalStop = new TrainStop();
        arrivalStop.setArrivalTime(LocalTime.of(6, 0)); // 早上6点到达
        
        when(trainStopRepository.findByStopId(3L)).thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findByStopId(4L)).thenReturn(Optional.of(arrivalStop));
        
        List<Ticket> conflictTickets = Arrays.asList(conflictTicket);
        String result = timeConflictService.generateConflictMessage(conflictTickets);
        
        assertNotNull(result);
        assertTrue(result.contains("乘客在同一时间段内已有其他车票，存在时间冲突"));
        assertTrue(result.contains("T001"));
        assertTrue(result.contains("22:00"));
        assertTrue(result.contains("06:00"));
        // 应该显示跨天信息
        assertTrue(result.contains("~"));
    }

    @Test
    void testGenerateConflictMessage_WithNullTimes() {
        // 创建时间信息为null的冲突车票
        Ticket conflictTicket = new Ticket();
        conflictTicket.setTicketId(1L);
        conflictTicket.setTrainId(2);
        conflictTicket.setDepartureStopId(3L);
        conflictTicket.setArrivalStopId(4L);
        conflictTicket.setTravelDate(LocalDate.now());
        conflictTicket.setTicketNumber("T001");
        
        when(trainStopRepository.findByStopId(3L)).thenReturn(Optional.empty());
        when(trainStopRepository.findByStopId(4L)).thenReturn(Optional.empty());
        
        List<Ticket> conflictTickets = Arrays.asList(conflictTicket);
        String result = timeConflictService.generateConflictMessage(conflictTickets);
        
        assertNotNull(result);
        assertTrue(result.contains("乘客在同一时间段内已有其他车票，存在时间冲突"));
        assertTrue(result.contains("T001"));
        assertTrue(result.contains("未知"));
    }

    @Test
    void testCheckTimeConflict_WithNullExistingTimes() {
        // 设置Mock行为
        TrainStop departureStop = new TrainStop();
        departureStop.setDepartureTime(LocalTime.of(8, 0));
        
        TrainStop arrivalStop = new TrainStop();
        arrivalStop.setArrivalTime(LocalTime.of(10, 0));
        
        // 创建现有车票，但时间信息为null
        Ticket existingTicket = new Ticket();
        existingTicket.setTicketId(1L);
        existingTicket.setTrainId(2);
        existingTicket.setDepartureStopId(3L);
        existingTicket.setArrivalStopId(4L);
        existingTicket.setTravelDate(LocalDate.now());
        
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findByStopId(2L)).thenReturn(Optional.of(arrivalStop));
        when(trainStopRepository.findByStopId(3L)).thenReturn(Optional.empty()); // 现有车票出发时间为null
        when(trainStopRepository.findByStopId(4L)).thenReturn(Optional.empty()); // 现有车票到达时间为null
        when(ticketRepository.findValidTicketsByPassengerAndDate(anyLong(), eq(LocalDate.now().minusDays(1))))
            .thenReturn(Collections.emptyList());
        when(ticketRepository.findValidTicketsByPassengerAndDate(anyLong(), eq(LocalDate.now())))
            .thenReturn(Arrays.asList(existingTicket));

        List<Ticket> result = timeConflictService.checkTimeConflict(1L, LocalDate.now(), 1, 1L, 2L);
        assertNotNull(result);
        assertTrue(result.isEmpty()); // 时间信息为null时不应该检测到冲突
    }

    @Test
    void testCheckTimeConflict_WithOvernightExistingTicket() {
        // 设置Mock行为
        TrainStop departureStop = new TrainStop();
        departureStop.setDepartureTime(LocalTime.of(8, 0));
        
        TrainStop arrivalStop = new TrainStop();
        arrivalStop.setArrivalTime(LocalTime.of(10, 0));
        
        // 创建跨天的现有车票，但时间与新票不冲突
        Ticket existingTicket = new Ticket();
        existingTicket.setTicketId(1L);
        existingTicket.setTrainId(2);
        existingTicket.setDepartureStopId(3L);
        existingTicket.setArrivalStopId(4L);
        existingTicket.setTravelDate(LocalDate.now().minusDays(1)); // 前一天的车票
        
        TrainStop existingDepartureStop = new TrainStop();
        existingDepartureStop.setDepartureTime(LocalTime.of(22, 0)); // 晚上10点出发
        
        TrainStop existingArrivalStop = new TrainStop();
        existingArrivalStop.setArrivalTime(LocalTime.of(6, 0)); // 早上6点到达
        
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findByStopId(2L)).thenReturn(Optional.of(arrivalStop));
        when(trainStopRepository.findByStopId(3L)).thenReturn(Optional.of(existingDepartureStop));
        when(trainStopRepository.findByStopId(4L)).thenReturn(Optional.of(existingArrivalStop));
        when(ticketRepository.findValidTicketsByPassengerAndDate(anyLong(), eq(LocalDate.now().minusDays(1))))
            .thenReturn(Arrays.asList(existingTicket)); // 前一天有车票
        when(ticketRepository.findValidTicketsByPassengerAndDate(anyLong(), eq(LocalDate.now())))
            .thenReturn(Collections.emptyList()); // 当天没有车票

        List<Ticket> result = timeConflictService.checkTimeConflict(1L, LocalDate.now(), 1, 1L, 2L);
        assertNotNull(result);
        assertTrue(result.isEmpty()); // 应该没有冲突，因为现有票是前一天的
    }

    @Test
    void testCheckTimeConflict_WithNewTicketDepartureInExistingRange() {
        // 设置Mock行为 - 新票出发时间在现有票时间段内
        TrainStop departureStop = new TrainStop();
        departureStop.setDepartureTime(LocalTime.of(9, 30)); // 新票9:30出发，在现有票时间段内
        
        TrainStop arrivalStop = new TrainStop();
        arrivalStop.setArrivalTime(LocalTime.of(11, 0));
        
        // 创建现有车票
        Ticket existingTicket = new Ticket();
        existingTicket.setTicketId(1L);
        existingTicket.setTrainId(2);
        existingTicket.setDepartureStopId(3L);
        existingTicket.setArrivalStopId(4L);
        existingTicket.setTravelDate(LocalDate.now());
        
        TrainStop existingDepartureStop = new TrainStop();
        existingDepartureStop.setDepartureTime(LocalTime.of(9, 0)); // 现有票9:00出发
        
        TrainStop existingArrivalStop = new TrainStop();
        existingArrivalStop.setArrivalTime(LocalTime.of(10, 0)); // 现有票10:00到达
        
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findByStopId(2L)).thenReturn(Optional.of(arrivalStop));
        when(trainStopRepository.findByStopId(3L)).thenReturn(Optional.of(existingDepartureStop));
        when(trainStopRepository.findByStopId(4L)).thenReturn(Optional.of(existingArrivalStop));
        when(ticketRepository.findValidTicketsByPassengerAndDate(anyLong(), eq(LocalDate.now().minusDays(1))))
            .thenReturn(Collections.emptyList());
        when(ticketRepository.findValidTicketsByPassengerAndDate(anyLong(), eq(LocalDate.now())))
            .thenReturn(Arrays.asList(existingTicket));

        List<Ticket> result = timeConflictService.checkTimeConflict(1L, LocalDate.now(), 1, 1L, 2L);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void testCheckTimeConflict_WithNewTicketContainsExisting() {
        // 设置Mock行为 - 新票时间段完全包含现有票时间段
        TrainStop departureStop = new TrainStop();
        departureStop.setDepartureTime(LocalTime.of(8, 0)); // 新票8:00出发
        
        TrainStop arrivalStop = new TrainStop();
        arrivalStop.setArrivalTime(LocalTime.of(12, 0)); // 新票12:00到达
        
        // 创建现有车票
        Ticket existingTicket = new Ticket();
        existingTicket.setTicketId(1L);
        existingTicket.setTrainId(2);
        existingTicket.setDepartureStopId(3L);
        existingTicket.setArrivalStopId(4L);
        existingTicket.setTravelDate(LocalDate.now());
        
        TrainStop existingDepartureStop = new TrainStop();
        existingDepartureStop.setDepartureTime(LocalTime.of(9, 0)); // 现有票9:00出发
        
        TrainStop existingArrivalStop = new TrainStop();
        existingArrivalStop.setArrivalTime(LocalTime.of(11, 0)); // 现有票11:00到达
        
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findByStopId(2L)).thenReturn(Optional.of(arrivalStop));
        when(trainStopRepository.findByStopId(3L)).thenReturn(Optional.of(existingDepartureStop));
        when(trainStopRepository.findByStopId(4L)).thenReturn(Optional.of(existingArrivalStop));
        when(ticketRepository.findValidTicketsByPassengerAndDate(anyLong(), eq(LocalDate.now().minusDays(1))))
            .thenReturn(Collections.emptyList());
        when(ticketRepository.findValidTicketsByPassengerAndDate(anyLong(), eq(LocalDate.now())))
            .thenReturn(Arrays.asList(existingTicket));

        List<Ticket> result = timeConflictService.checkTimeConflict(1L, LocalDate.now(), 1, 1L, 2L);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void testCheckTimeConflict_WithExactTimeOverlap() {
        // 设置Mock行为 - 新票和现有票时间完全重叠
        TrainStop departureStop = new TrainStop();
        departureStop.setDepartureTime(LocalTime.of(9, 0)); // 新票9:00出发
        
        TrainStop arrivalStop = new TrainStop();
        arrivalStop.setArrivalTime(LocalTime.of(11, 0)); // 新票11:00到达
        
        // 创建现有车票
        Ticket existingTicket = new Ticket();
        existingTicket.setTicketId(1L);
        existingTicket.setTrainId(2);
        existingTicket.setDepartureStopId(3L);
        existingTicket.setArrivalStopId(4L);
        existingTicket.setTravelDate(LocalDate.now());
        
        TrainStop existingDepartureStop = new TrainStop();
        existingDepartureStop.setDepartureTime(LocalTime.of(9, 0)); // 现有票9:00出发
        
        TrainStop existingArrivalStop = new TrainStop();
        existingArrivalStop.setArrivalTime(LocalTime.of(11, 0)); // 现有票11:00到达
        
        when(trainStopRepository.findByStopId(1L)).thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findByStopId(2L)).thenReturn(Optional.of(arrivalStop));
        when(trainStopRepository.findByStopId(3L)).thenReturn(Optional.of(existingDepartureStop));
        when(trainStopRepository.findByStopId(4L)).thenReturn(Optional.of(existingArrivalStop));
        when(ticketRepository.findValidTicketsByPassengerAndDate(anyLong(), eq(LocalDate.now().minusDays(1))))
            .thenReturn(Collections.emptyList());
        when(ticketRepository.findValidTicketsByPassengerAndDate(anyLong(), eq(LocalDate.now())))
            .thenReturn(Arrays.asList(existingTicket));

        List<Ticket> result = timeConflictService.checkTimeConflict(1L, LocalDate.now(), 1, 1L, 2L);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void testGenerateConflictMessage_WithOvernightTicketInMessage() {
        // 创建跨天冲突车票
        Ticket conflictTicket = new Ticket();
        conflictTicket.setTicketId(1L);
        conflictTicket.setTrainId(2);
        conflictTicket.setDepartureStopId(3L);
        conflictTicket.setArrivalStopId(4L);
        conflictTicket.setTravelDate(LocalDate.now());
        conflictTicket.setTicketNumber("T001");
        
        TrainStop departureStop = new TrainStop();
        departureStop.setDepartureTime(LocalTime.of(22, 0)); // 晚上10点出发
        
        TrainStop arrivalStop = new TrainStop();
        arrivalStop.setArrivalTime(LocalTime.of(6, 0)); // 早上6点到达
        
        when(trainStopRepository.findByStopId(3L)).thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findByStopId(4L)).thenReturn(Optional.of(arrivalStop));
        
        List<Ticket> conflictTickets = Arrays.asList(conflictTicket);
        String result = timeConflictService.generateConflictMessage(conflictTickets);
        
        assertNotNull(result);
        assertTrue(result.contains("乘客在同一时间段内已有其他车票，存在时间冲突"));
        assertTrue(result.contains("T001"));
        assertTrue(result.contains("22:00"));
        assertTrue(result.contains("06:00"));
        // 应该显示跨天信息
        assertTrue(result.contains("~"));
    }
} 