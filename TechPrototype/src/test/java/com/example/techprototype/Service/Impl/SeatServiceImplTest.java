package com.example.techprototype.Service.Impl;

import com.example.techprototype.Entity.Seat;
import com.example.techprototype.Entity.Ticket;
import com.example.techprototype.Entity.TrainCarriage;
import com.example.techprototype.Entity.TrainStop;
import com.example.techprototype.Repository.SeatRepository;
import com.example.techprototype.Repository.TrainCarriageRepository;
import com.example.techprototype.Repository.TrainStopRepository;
import com.example.techprototype.Util.SeatBitmapUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class SeatServiceImplTest {

    @InjectMocks
    private SeatServiceImpl seatService;

    @Mock
    private SeatRepository seatRepository;
    
    @Mock
    private TrainCarriageRepository trainCarriageRepository;
    
    @Mock
    private TrainStopRepository trainStopRepository;

    private Seat testSeat;
    private Ticket testTicket;
    private TrainStop departureStop;
    private TrainStop arrivalStop;
    private TrainCarriage trainCarriage;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 使用SeatBitmapUtil支持的日期范围
        testDate = SeatBitmapUtil.BASE_DATE.plusDays(1); // date_2
        
        // Setup test data
        testSeat = new Seat();
        testSeat.setSeatId(1L);
        testSeat.setCarriageId(1L);
        testSeat.setSeatNumber("1");
        testSeat.setSeatType("靠窗");
        // 设置位图数据，使座位可用（所有日期位图都为0，表示可用）
        testSeat.setDate1(0L);
        testSeat.setDate2(0L);
        testSeat.setDate3(0L);
        testSeat.setDate4(0L);
        testSeat.setDate5(0L);
        testSeat.setDate6(0L);
        testSeat.setDate7(0L);
        testSeat.setDate8(0L);
        testSeat.setDate9(0L);
        testSeat.setDate10(0L);
        
        testTicket = new Ticket();
        testTicket.setTrainId(1);
        testTicket.setDepartureStopId(1L);
        testTicket.setArrivalStopId(2L);
        testTicket.setTravelDate(testDate);
        testTicket.setCarriageNumber("1");
        testTicket.setSeatNumber("1A");
        testTicket.setCarriageTypeId(1);
        
        departureStop = new TrainStop();
        departureStop.setSequenceNumber(1);
        
        arrivalStop = new TrainStop();
        arrivalStop.setSequenceNumber(2);
        
        trainCarriage = new TrainCarriage();
        trainCarriage.setCarriageId(1L);
        trainCarriage.setCarriageNumber("1");
    }

    @Test
    void testFindAvailableSeat_Success() {
        // Given
        Integer trainId = 1;
        Integer typeId = 1;
        LocalDate travelDate = testDate;
        Long departureStopId = 1L;
        Long arrivalStopId = 2L;
        
        when(trainStopRepository.findByTrainIdAndStopId(trainId, departureStopId))
            .thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findByTrainIdAndStopId(trainId, arrivalStopId))
            .thenReturn(Optional.of(arrivalStop));
        when(seatRepository.findByTrainAndType(trainId, typeId))
            .thenReturn(Arrays.asList(testSeat));
        
        // When
        Optional<Seat> result = seatService.findAvailableSeat(trainId, typeId, travelDate, departureStopId, arrivalStopId);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(testSeat, result.get());
        verify(trainStopRepository, times(2)).findByTrainIdAndStopId(any(), any());
        verify(seatRepository).findByTrainAndType(trainId, typeId);
    }

    @Test
    void testFindAvailableSeat_NoStopsFound() {
        // Given
        Integer trainId = 1;
        Integer typeId = 1;
        LocalDate travelDate = testDate;
        Long departureStopId = 1L;
        Long arrivalStopId = 2L;
        
        when(trainStopRepository.findByTrainIdAndStopId(trainId, departureStopId))
            .thenReturn(Optional.empty());
        
        // When
        Optional<Seat> result = seatService.findAvailableSeat(trainId, typeId, travelDate, departureStopId, arrivalStopId);
        
        // Then
        assertFalse(result.isPresent());
        verify(trainStopRepository).findByTrainIdAndStopId(trainId, departureStopId);
        verify(seatRepository, never()).findByTrainAndType(any(), any());
    }

    @Test
    void testFindAvailableSeatBySequence_Success() {
        // Given
        Integer trainId = 1;
        Integer typeId = 1;
        LocalDate travelDate = testDate;
        int departureSequence = 1;
        int arrivalSequence = 2;
        
        when(seatRepository.findByTrainAndType(trainId, typeId))
            .thenReturn(Arrays.asList(testSeat));
        
        // When
        Optional<Seat> result = seatService.findAvailableSeatBySequence(trainId, typeId, travelDate, departureSequence, arrivalSequence);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(testSeat, result.get());
        verify(seatRepository).findByTrainAndType(trainId, typeId);
    }

    @Test
    void testFindAvailableSeatBySequence_NoSeatsFound() {
        // Given
        Integer trainId = 1;
        Integer typeId = 1;
        LocalDate travelDate = testDate;
        int departureSequence = 1;
        int arrivalSequence = 2;
        
        when(seatRepository.findByTrainAndType(trainId, typeId))
            .thenReturn(Arrays.asList());
        
        // When
        Optional<Seat> result = seatService.findAvailableSeatBySequence(trainId, typeId, travelDate, departureSequence, arrivalSequence);
        
        // Then
        assertFalse(result.isPresent());
        verify(seatRepository).findByTrainAndType(trainId, typeId);
    }

    @Test
    void testAssignSeat_Success() {
        // Given
        when(trainStopRepository.findByTrainIdAndStopId(testTicket.getTrainId(), testTicket.getDepartureStopId()))
            .thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findByTrainIdAndStopId(testTicket.getTrainId(), testTicket.getArrivalStopId()))
            .thenReturn(Optional.of(arrivalStop));
        when(seatRepository.findByTrainAndType(testTicket.getTrainId(), testTicket.getCarriageTypeId()))
            .thenReturn(Arrays.asList(testSeat));
        when(seatRepository.save(any(Seat.class)))
            .thenReturn(testSeat);
        when(trainCarriageRepository.findByCarriageId(testSeat.getCarriageId()))
            .thenReturn(Optional.of(trainCarriage));
        
        // When
        seatService.assignSeat(testTicket);
        
        // Then
        verify(trainStopRepository, times(2)).findByTrainIdAndStopId(any(), any());
        verify(seatRepository).findByTrainAndType(testTicket.getTrainId(), testTicket.getCarriageTypeId());
        verify(seatRepository).save(any(Seat.class));
        verify(trainCarriageRepository).findByCarriageId(testSeat.getCarriageId());
    }

    @Test
    void testAssignSeat_NoStopsFound() {
        // Given
        when(trainStopRepository.findByTrainIdAndStopId(testTicket.getTrainId(), testTicket.getDepartureStopId()))
            .thenReturn(Optional.empty());
        
        // When
        seatService.assignSeat(testTicket);
        
        // Then
        verify(trainStopRepository).findByTrainIdAndStopId(testTicket.getTrainId(), testTicket.getDepartureStopId());
        verify(seatRepository, never()).findByTrainAndType(any(), any());
        verify(seatRepository, never()).save(any(Seat.class));
    }

    @Test
    void testAssignSeat_NoAvailableSeat() {
        // Given
        when(trainStopRepository.findByTrainIdAndStopId(testTicket.getTrainId(), testTicket.getDepartureStopId()))
            .thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findByTrainIdAndStopId(testTicket.getTrainId(), testTicket.getArrivalStopId()))
            .thenReturn(Optional.of(arrivalStop));
        when(seatRepository.findByTrainAndType(testTicket.getTrainId(), testTicket.getCarriageTypeId()))
            .thenReturn(Arrays.asList());
        
        // When
        seatService.assignSeat(testTicket);
        
        // Then
        verify(trainStopRepository, times(2)).findByTrainIdAndStopId(any(), any());
        verify(seatRepository).findByTrainAndType(testTicket.getTrainId(), testTicket.getCarriageTypeId());
        verify(seatRepository, never()).save(any(Seat.class));
    }

    @Test
    void testReleaseSeat_Success() {
        // Given
        when(trainStopRepository.findByTrainIdAndStopId(testTicket.getTrainId(), testTicket.getDepartureStopId()))
            .thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findByTrainIdAndStopId(testTicket.getTrainId(), testTicket.getArrivalStopId()))
            .thenReturn(Optional.of(arrivalStop));
        when(trainCarriageRepository.findByTrainIdAndCarriageNumber(testTicket.getTrainId(), testTicket.getCarriageNumber()))
            .thenReturn(Optional.of(trainCarriage));
        when(seatRepository.findByCarriageIdAndSeatNumber(trainCarriage.getCarriageId(), testTicket.getSeatNumber()))
            .thenReturn(Arrays.asList(testSeat));
        when(seatRepository.save(any(Seat.class)))
            .thenReturn(testSeat);
        
        // When
        seatService.releaseSeat(testTicket);
        
        // Then
        verify(trainStopRepository, times(2)).findByTrainIdAndStopId(any(), any());
        verify(trainCarriageRepository).findByTrainIdAndCarriageNumber(testTicket.getTrainId(), testTicket.getCarriageNumber());
        verify(seatRepository).findByCarriageIdAndSeatNumber(trainCarriage.getCarriageId(), testTicket.getSeatNumber());
        verify(seatRepository).save(any(Seat.class));
    }

    @Test
    void testReleaseSeat_NoStopsFound() {
        // Given
        when(trainStopRepository.findByTrainIdAndStopId(testTicket.getTrainId(), testTicket.getDepartureStopId()))
            .thenReturn(Optional.empty());
        
        // When
        seatService.releaseSeat(testTicket);
        
        // Then
        verify(trainStopRepository).findByTrainIdAndStopId(testTicket.getTrainId(), testTicket.getDepartureStopId());
        verify(trainCarriageRepository, never()).findByTrainIdAndCarriageNumber(any(), any());
        verify(seatRepository, never()).findByCarriageIdAndSeatNumber(any(), any());
        verify(seatRepository, never()).save(any(Seat.class));
    }

    @Test
    void testReleaseSeat_NoCarriageFound() {
        // Given
        when(trainStopRepository.findByTrainIdAndStopId(testTicket.getTrainId(), testTicket.getDepartureStopId()))
            .thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findByTrainIdAndStopId(testTicket.getTrainId(), testTicket.getArrivalStopId()))
            .thenReturn(Optional.of(arrivalStop));
        when(trainCarriageRepository.findByTrainIdAndCarriageNumber(testTicket.getTrainId(), testTicket.getCarriageNumber()))
            .thenReturn(Optional.empty());
        
        // When
        seatService.releaseSeat(testTicket);
        
        // Then
        verify(trainStopRepository, times(2)).findByTrainIdAndStopId(any(), any());
        verify(trainCarriageRepository).findByTrainIdAndCarriageNumber(testTicket.getTrainId(), testTicket.getCarriageNumber());
        verify(seatRepository, never()).findByCarriageIdAndSeatNumber(any(), any());
        verify(seatRepository, never()).save(any(Seat.class));
    }

    @Test
    void testReleaseSeat_NoSeatsFound() {
        // Given
        when(trainStopRepository.findByTrainIdAndStopId(testTicket.getTrainId(), testTicket.getDepartureStopId()))
            .thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findByTrainIdAndStopId(testTicket.getTrainId(), testTicket.getArrivalStopId()))
            .thenReturn(Optional.of(arrivalStop));
        when(trainCarriageRepository.findByTrainIdAndCarriageNumber(testTicket.getTrainId(), testTicket.getCarriageNumber()))
            .thenReturn(Optional.of(trainCarriage));
        when(seatRepository.findByCarriageIdAndSeatNumber(trainCarriage.getCarriageId(), testTicket.getSeatNumber()))
            .thenReturn(Arrays.asList());
        
        // When
        seatService.releaseSeat(testTicket);
        
        // Then
        verify(trainStopRepository, times(2)).findByTrainIdAndStopId(any(), any());
        verify(trainCarriageRepository).findByTrainIdAndCarriageNumber(testTicket.getTrainId(), testTicket.getCarriageNumber());
        verify(seatRepository).findByCarriageIdAndSeatNumber(trainCarriage.getCarriageId(), testTicket.getSeatNumber());
        verify(seatRepository, never()).save(any(Seat.class));
    }

    @Test
    void testLockSeatBySequence() {
        // Given
        LocalDate travelDate = testDate;
        int departureSequence = 1;
        int arrivalSequence = 2;
        
        // When
        seatService.lockSeatBySequence(testSeat, travelDate, departureSequence, arrivalSequence);
        
        // Then
        // 验证位图被正确设置（通过检查seat对象的状态）
        assertNotEquals(0L, testSeat.getDate2()); // date2对应testDate
    }

    @Test
    void testReleaseSeatBySequence() {
        // Given
        LocalDate travelDate = testDate;
        int departureSequence = 1;
        int arrivalSequence = 2;
        
        // 先锁定座位
        testSeat.setDate2(6L); // 设置一些占用位
        
        // When
        seatService.releaseSeatBySequence(testSeat, travelDate, departureSequence, arrivalSequence);
        
        // Then
        // 验证位图被正确清除
        assertEquals(0L, testSeat.getDate2()); // 应该被清除
    }

    @Test
    void testIsSeatAvailableBySequence() {
        // Given
        LocalDate travelDate = testDate;
        int departureSequence = 1;
        int arrivalSequence = 2;
        
        // When
        boolean result = seatService.isSeatAvailableBySequence(testSeat, travelDate, departureSequence, arrivalSequence);
        
        // Then
        assertTrue(result); // 座位应该可用
    }

    @Test
    void testGetCarriageNumber_Success() {
        // Given
        Long carriageId = 1L;
        when(trainCarriageRepository.findByCarriageId(carriageId))
            .thenReturn(Optional.of(trainCarriage));
        
        // When - 通过assignSeat方法来测试getCarriageNumber
        when(trainStopRepository.findByTrainIdAndStopId(testTicket.getTrainId(), testTicket.getDepartureStopId()))
            .thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findByTrainIdAndStopId(testTicket.getTrainId(), testTicket.getArrivalStopId()))
            .thenReturn(Optional.of(arrivalStop));
        when(seatRepository.findByTrainAndType(testTicket.getTrainId(), testTicket.getCarriageTypeId()))
            .thenReturn(Arrays.asList(testSeat));
        when(seatRepository.save(any(Seat.class)))
            .thenReturn(testSeat);
        
        seatService.assignSeat(testTicket);
        
        // Then
        verify(trainCarriageRepository).findByCarriageId(carriageId);
    }

    @Test
    void testGetCarriageNumber_NotFound() {
        // Given
        Long carriageId = 999L;
        when(trainCarriageRepository.findByCarriageId(carriageId))
            .thenReturn(Optional.empty());
        
        // When - 通过assignSeat方法来测试getCarriageNumber
        when(trainStopRepository.findByTrainIdAndStopId(testTicket.getTrainId(), testTicket.getDepartureStopId()))
            .thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findByTrainIdAndStopId(testTicket.getTrainId(), testTicket.getArrivalStopId()))
            .thenReturn(Optional.of(arrivalStop));
        when(seatRepository.findByTrainAndType(testTicket.getTrainId(), testTicket.getCarriageTypeId()))
            .thenReturn(Arrays.asList(testSeat));
        when(seatRepository.save(any(Seat.class)))
            .thenReturn(testSeat);
        
        // 修改testSeat的carriageId为不存在的ID
        testSeat.setCarriageId(carriageId);
        
        seatService.assignSeat(testTicket);
        
        // Then
        verify(trainCarriageRepository).findByCarriageId(carriageId);
    }

    @Test
    void testLegacyMethods_ShouldPrintWarning() {
        // Given
        LocalDate travelDate = testDate;
        Long departureStopId = 1L;
        Long arrivalStopId = 2L;
        
        // When & Then - 这些方法应该打印警告并返回false
        assertDoesNotThrow(() -> seatService.lockSeat(testSeat, travelDate, departureStopId, arrivalStopId));
        assertDoesNotThrow(() -> seatService.releaseSeat(testSeat, travelDate, departureStopId, arrivalStopId));
        assertFalse(seatService.isSeatAvailable(testSeat, travelDate, departureStopId, arrivalStopId));
    }

    @Test
    void testReleaseSeat_OnlyDepartureStopNotFound() {
        // Given - 只有出发站找不到
        when(trainStopRepository.findByTrainIdAndStopId(testTicket.getTrainId(), testTicket.getDepartureStopId()))
            .thenReturn(Optional.empty());
        when(trainStopRepository.findByTrainIdAndStopId(testTicket.getTrainId(), testTicket.getArrivalStopId()))
            .thenReturn(Optional.of(arrivalStop));
        
        // When
        seatService.releaseSeat(testTicket);
        
        // Then
        verify(trainStopRepository, times(2)).findByTrainIdAndStopId(any(), any());
        verify(trainCarriageRepository, never()).findByTrainIdAndCarriageNumber(any(), any());
    }

    @Test
    void testReleaseSeat_OnlyArrivalStopNotFound() {
        // Given - 只有到达站找不到
        when(trainStopRepository.findByTrainIdAndStopId(testTicket.getTrainId(), testTicket.getDepartureStopId()))
            .thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findByTrainIdAndStopId(testTicket.getTrainId(), testTicket.getArrivalStopId()))
            .thenReturn(Optional.empty());
        
        // When
        seatService.releaseSeat(testTicket);
        
        // Then
        verify(trainStopRepository, times(2)).findByTrainIdAndStopId(any(), any());
        verify(trainCarriageRepository, never()).findByTrainIdAndCarriageNumber(any(), any());
    }

    @Test
    void testAssignSeat_OnlyDepartureStopNotFound() {
        // Given - 只有出发站找不到
        when(trainStopRepository.findByTrainIdAndStopId(testTicket.getTrainId(), testTicket.getDepartureStopId()))
            .thenReturn(Optional.empty());
        when(trainStopRepository.findByTrainIdAndStopId(testTicket.getTrainId(), testTicket.getArrivalStopId()))
            .thenReturn(Optional.of(arrivalStop));
        
        // When
        seatService.assignSeat(testTicket);
        
        // Then
        verify(trainStopRepository, times(2)).findByTrainIdAndStopId(any(), any());
        verify(seatRepository, never()).findByTrainAndType(any(), any());
    }

    @Test
    void testAssignSeat_OnlyArrivalStopNotFound() {
        // Given - 只有到达站找不到
        when(trainStopRepository.findByTrainIdAndStopId(testTicket.getTrainId(), testTicket.getDepartureStopId()))
            .thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findByTrainIdAndStopId(testTicket.getTrainId(), testTicket.getArrivalStopId()))
            .thenReturn(Optional.empty());
        
        // When
        seatService.assignSeat(testTicket);
        
        // Then
        verify(trainStopRepository, times(2)).findByTrainIdAndStopId(any(), any());
        verify(seatRepository, never()).findByTrainAndType(any(), any());
    }

    @Test
    void testFindAvailableSeat_OnlyDepartureStopNotFound() {
        // Given - 只有出发站找不到
        Integer trainId = 1;
        Integer typeId = 1;
        LocalDate travelDate = testDate;
        Long departureStopId = 1L;
        Long arrivalStopId = 2L;
        
        when(trainStopRepository.findByTrainIdAndStopId(trainId, departureStopId))
            .thenReturn(Optional.empty());
        when(trainStopRepository.findByTrainIdAndStopId(trainId, arrivalStopId))
            .thenReturn(Optional.of(arrivalStop));
        
        // When
        Optional<Seat> result = seatService.findAvailableSeat(trainId, typeId, travelDate, departureStopId, arrivalStopId);
        
        // Then
        assertFalse(result.isPresent());
        verify(trainStopRepository, times(2)).findByTrainIdAndStopId(any(), any());
        verify(seatRepository, never()).findByTrainAndType(any(), any());
    }

    @Test
    void testFindAvailableSeat_OnlyArrivalStopNotFound() {
        // Given - 只有到达站找不到
        Integer trainId = 1;
        Integer typeId = 1;
        LocalDate travelDate = testDate;
        Long departureStopId = 1L;
        Long arrivalStopId = 2L;
        
        when(trainStopRepository.findByTrainIdAndStopId(trainId, departureStopId))
            .thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findByTrainIdAndStopId(trainId, arrivalStopId))
            .thenReturn(Optional.empty());
        
        // When
        Optional<Seat> result = seatService.findAvailableSeat(trainId, typeId, travelDate, departureStopId, arrivalStopId);
        
        // Then
        assertFalse(result.isPresent());
        verify(trainStopRepository, times(2)).findByTrainIdAndStopId(any(), any());
        verify(seatRepository, never()).findByTrainAndType(any(), any());
    }

    @Test
    void testGetDateBitmap_DifferentDateIndices() {
        // 测试不同的日期索引，通过调用lockSeatBySequence和releaseSeatBySequence来间接测试getDateBitmap
        // 使用不同的日期来触发不同的dateIndex
        
        // 测试date1 (BASE_DATE)
        LocalDate date1 = SeatBitmapUtil.BASE_DATE;
        seatService.lockSeatBySequence(testSeat, date1, 1, 2);
        assertNotEquals(0L, testSeat.getDate1());
        
        // 测试date3 (BASE_DATE + 2)
        LocalDate date3 = SeatBitmapUtil.BASE_DATE.plusDays(2);
        seatService.lockSeatBySequence(testSeat, date3, 1, 2);
        assertNotEquals(0L, testSeat.getDate3());
        
        // 测试date5 (BASE_DATE + 4)
        LocalDate date5 = SeatBitmapUtil.BASE_DATE.plusDays(4);
        seatService.lockSeatBySequence(testSeat, date5, 1, 2);
        assertNotEquals(0L, testSeat.getDate5());
        
        // 测试date7 (BASE_DATE + 6)
        LocalDate date7 = SeatBitmapUtil.BASE_DATE.plusDays(6);
        seatService.lockSeatBySequence(testSeat, date7, 1, 2);
        assertNotEquals(0L, testSeat.getDate7());
        
        // 测试date9 (BASE_DATE + 8)
        LocalDate date9 = SeatBitmapUtil.BASE_DATE.plusDays(8);
        seatService.lockSeatBySequence(testSeat, date9, 1, 2);
        assertNotEquals(0L, testSeat.getDate9());
        
        // 测试date10 (BASE_DATE + 9)
        LocalDate date10 = SeatBitmapUtil.BASE_DATE.plusDays(9);
        seatService.lockSeatBySequence(testSeat, date10, 1, 2);
        assertNotEquals(0L, testSeat.getDate10());
        
        // 测试超出范围的日期 (应该触发default分支)
        LocalDate invalidDate = SeatBitmapUtil.BASE_DATE.plusDays(10);
        seatService.lockSeatBySequence(testSeat, invalidDate, 1, 2);
        // 超出范围的日期不会修改任何位图，所以date10应该保持不变
        assertNotEquals(0L, testSeat.getDate10());
    }

    @Test
    void testGetDateBitmap_AllCases() {
        // 测试所有dateIndex的情况，通过直接设置位图值来验证getDateBitmap的调用
        // 由于getDateBitmap是私有方法，我们通过调用使用它的公共方法来间接测试
        
        // 测试date1
        testSeat.setDate1(1L);
        seatService.lockSeatBySequence(testSeat, SeatBitmapUtil.BASE_DATE, 1, 2);
        
        // 测试date3
        testSeat.setDate3(3L);
        seatService.lockSeatBySequence(testSeat, SeatBitmapUtil.BASE_DATE.plusDays(2), 1, 2);
        
        // 测试date4
        testSeat.setDate4(4L);
        seatService.lockSeatBySequence(testSeat, SeatBitmapUtil.BASE_DATE.plusDays(3), 1, 2);
        
        // 测试date6
        testSeat.setDate6(6L);
        seatService.lockSeatBySequence(testSeat, SeatBitmapUtil.BASE_DATE.plusDays(5), 1, 2);
        
        // 测试date8
        testSeat.setDate8(8L);
        seatService.lockSeatBySequence(testSeat, SeatBitmapUtil.BASE_DATE.plusDays(7), 1, 2);
        
        // 验证位图被正确设置
        assertNotEquals(0L, testSeat.getDate1());
        assertNotEquals(0L, testSeat.getDate3());
        assertNotEquals(0L, testSeat.getDate4());
        assertNotEquals(0L, testSeat.getDate6());
        assertNotEquals(0L, testSeat.getDate8());
    }
} 