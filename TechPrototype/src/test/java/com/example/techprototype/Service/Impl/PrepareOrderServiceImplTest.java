package com.example.techprototype.Service.Impl;

import com.example.techprototype.DTO.PrepareOrderRequest;
import com.example.techprototype.DTO.PrepareOrderResponse;
import com.example.techprototype.Entity.*;
import com.example.techprototype.Repository.*;
import com.example.techprototype.Service.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PrepareOrderServiceImplTest {
    @InjectMocks
    private PrepareOrderServiceImpl service;
    @Mock
    private TicketInventoryRepository ticketInventoryRepository;
    @Mock
    private TrainRepository trainRepository;
    @Mock
    private StationRepository stationRepository;
    @Mock
    private CarriageTypeRepository carriageTypeRepository;
    @Mock
    private UserPassengerRelationRepository userPassengerRelationRepository;
    @Mock
    private PassengerRepository passengerRepository;
    @Mock
    private TrainStopRepository trainStopRepository;
    @Mock
    private RedisService redisService;

    private PrepareOrderRequest request;
    private TicketInventory inventory;
    private Train train;
    private TrainStop departureStop;
    private TrainStop arrivalStop;
    private Station departureStation;
    private Station arrivalStation;
    private CarriageType carriageType;
    private UserPassengerRelation relation;
    private Passenger passenger;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        request = new PrepareOrderRequest();
        request.setUserId(1L);
        request.setInventoryIds(Arrays.asList(1L));
        inventory = new TicketInventory();
        inventory.setInventoryId(1L);
        inventory.setTrainId(1);
        inventory.setDepartureStopId(100L);
        inventory.setArrivalStopId(200L);
        inventory.setTravelDate(LocalDate.of(2025, 1, 1));
        inventory.setCarriageTypeId(1);
        inventory.setPrice(new BigDecimal(100));
        inventory.setAvailableSeats(50);
        train = new Train();
        train.setTrainId(1);
        train.setTrainNumber("G123");
        departureStop = new TrainStop();
        departureStop.setStopId(100L);
        departureStop.setStationId(10);
        departureStop.setDepartureTime(LocalTime.of(8, 0));
        arrivalStop = new TrainStop();
        arrivalStop.setStopId(200L);
        arrivalStop.setStationId(20);
        arrivalStop.setArrivalTime(LocalTime.of(12, 0));
        departureStation = new Station();
        departureStation.setStationId(10);
        departureStation.setStationName("A站");
        arrivalStation = new Station();
        arrivalStation.setStationId(20);
        arrivalStation.setStationName("B站");
        carriageType = new CarriageType();
        carriageType.setTypeId(1);
        carriageType.setTypeName("二等座");
        relation = new UserPassengerRelation();
        relation.setUserId(1L);
        relation.setPassengerId(100L);
        relation.setRelationType((byte) 1);
        passenger = new Passenger();
        passenger.setPassengerId(100L);
        passenger.setRealName("张三");
        passenger.setIdCardNumber("123456789012345678");
        passenger.setPhoneNumber("13900000000");
        passenger.setPassengerType((byte) 1);
    }

    @Test
    void testPrepareOrder_Success() {
        when(ticketInventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(trainRepository.findById(1)).thenReturn(Optional.of(train));
        when(trainStopRepository.findById(100L)).thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findById(200L)).thenReturn(Optional.of(arrivalStop));
        when(stationRepository.findById(10)).thenReturn(Optional.of(departureStation));
        when(stationRepository.findById(20)).thenReturn(Optional.of(arrivalStation));
        when(carriageTypeRepository.findById(1)).thenReturn(Optional.of(carriageType));
        when(redisService.getStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt())).thenReturn(Optional.of(30));
        when(userPassengerRelationRepository.findByUserId(1L)).thenReturn(Collections.singletonList(relation));
        when(passengerRepository.findById(100L)).thenReturn(Optional.of(passenger));
        PrepareOrderResponse response = service.prepareOrder(request);
        assertNotNull(response);
        assertNotNull(response.getTrainInfo());
        assertEquals("G123", response.getTrainInfo().getTrainNumber());
        assertEquals("A站", response.getTrainInfo().getDepartureStation());
        assertEquals("B站", response.getTrainInfo().getArrivalStation());
        assertEquals(1, response.getCarriages().size());
        assertTrue(response.getCarriages().get(0).getHasStock());
        assertEquals(30, response.getCarriages().get(0).getAvailableStock());
        assertEquals(1, response.getPassengers().size());
        assertEquals("张三", response.getPassengers().get(0).getRealName());
        assertEquals("成人", response.getPassengers().get(0).getPassengerTypeName());
        assertEquals("本人", response.getPassengers().get(0).getRelationTypeName());
    }

    @Test
    void testPrepareOrder_NoInventory() {
        when(ticketInventoryRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.prepareOrder(request));
    }

    @Test
    void testPrepareOrder_TrainNotFound() {
        when(ticketInventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(trainRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.prepareOrder(request));
    }

    @Test
    void testPrepareOrder_DepartureStopNotFound() {
        when(ticketInventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(trainRepository.findById(1)).thenReturn(Optional.of(train));
        when(trainStopRepository.findById(100L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.prepareOrder(request));
    }

    @Test
    void testPrepareOrder_ArrivalStopNotFound() {
        when(ticketInventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(trainRepository.findById(1)).thenReturn(Optional.of(train));
        when(trainStopRepository.findById(100L)).thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findById(200L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.prepareOrder(request));
    }

    @Test
    void testPrepareOrder_DepartureStationNotFound() {
        when(ticketInventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(trainRepository.findById(1)).thenReturn(Optional.of(train));
        when(trainStopRepository.findById(100L)).thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findById(200L)).thenReturn(Optional.of(arrivalStop));
        when(stationRepository.findById(10)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.prepareOrder(request));
    }

    @Test
    void testPrepareOrder_ArrivalStationNotFound() {
        when(ticketInventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(trainRepository.findById(1)).thenReturn(Optional.of(train));
        when(trainStopRepository.findById(100L)).thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findById(200L)).thenReturn(Optional.of(arrivalStop));
        when(stationRepository.findById(10)).thenReturn(Optional.of(departureStation));
        when(stationRepository.findById(20)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.prepareOrder(request));
    }

    @Test
    void testPrepareOrder_CarriageTypeNotFound() {
        when(ticketInventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(trainRepository.findById(1)).thenReturn(Optional.of(train));
        when(trainStopRepository.findById(100L)).thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findById(200L)).thenReturn(Optional.of(arrivalStop));
        when(stationRepository.findById(10)).thenReturn(Optional.of(departureStation));
        when(stationRepository.findById(20)).thenReturn(Optional.of(arrivalStation));
        when(carriageTypeRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.prepareOrder(request));
    }

    @Test
    void testPrepareOrder_PassengerNotFound() {
        when(ticketInventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(trainRepository.findById(1)).thenReturn(Optional.of(train));
        when(trainStopRepository.findById(100L)).thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findById(200L)).thenReturn(Optional.of(arrivalStop));
        when(stationRepository.findById(10)).thenReturn(Optional.of(departureStation));
        when(stationRepository.findById(20)).thenReturn(Optional.of(arrivalStation));
        when(carriageTypeRepository.findById(1)).thenReturn(Optional.of(carriageType));
        when(redisService.getStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt())).thenReturn(Optional.of(30));
        when(userPassengerRelationRepository.findByUserId(1L)).thenReturn(Collections.singletonList(relation));
        when(passengerRepository.findById(100L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.prepareOrder(request));
    }

    @Test
    void testPrepareOrder_NoRedisStock() {
        when(ticketInventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(trainRepository.findById(1)).thenReturn(Optional.of(train));
        when(trainStopRepository.findById(100L)).thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findById(200L)).thenReturn(Optional.of(arrivalStop));
        when(stationRepository.findById(10)).thenReturn(Optional.of(departureStation));
        when(stationRepository.findById(20)).thenReturn(Optional.of(arrivalStation));
        when(carriageTypeRepository.findById(1)).thenReturn(Optional.of(carriageType));
        when(redisService.getStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt())).thenReturn(Optional.empty());
        when(userPassengerRelationRepository.findByUserId(1L)).thenReturn(Collections.singletonList(relation));
        when(passengerRepository.findById(100L)).thenReturn(Optional.of(passenger));
        PrepareOrderResponse response = service.prepareOrder(request);
        assertTrue(response.getCarriages().get(0).getHasStock());
        assertEquals(50, response.getCarriages().get(0).getAvailableStock());
    }

    @Test
    void testPrepareOrder_ZeroStock() {
        inventory.setAvailableSeats(0);
        when(ticketInventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(trainRepository.findById(1)).thenReturn(Optional.of(train));
        when(trainStopRepository.findById(100L)).thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findById(200L)).thenReturn(Optional.of(arrivalStop));
        when(stationRepository.findById(10)).thenReturn(Optional.of(departureStation));
        when(stationRepository.findById(20)).thenReturn(Optional.of(arrivalStation));
        when(carriageTypeRepository.findById(1)).thenReturn(Optional.of(carriageType));
        when(redisService.getStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt())).thenReturn(Optional.empty());
        when(userPassengerRelationRepository.findByUserId(1L)).thenReturn(Collections.singletonList(relation));
        when(passengerRepository.findById(100L)).thenReturn(Optional.of(passenger));
        PrepareOrderResponse response = service.prepareOrder(request);
        assertFalse(response.getCarriages().get(0).getHasStock());
        assertEquals(0, response.getCarriages().get(0).getAvailableStock());
    }

    @Test
    void testPrepareOrder_ZeroRedisStock() {
        when(ticketInventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(trainRepository.findById(1)).thenReturn(Optional.of(train));
        when(trainStopRepository.findById(100L)).thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findById(200L)).thenReturn(Optional.of(arrivalStop));
        when(stationRepository.findById(10)).thenReturn(Optional.of(departureStation));
        when(stationRepository.findById(20)).thenReturn(Optional.of(arrivalStation));
        when(carriageTypeRepository.findById(1)).thenReturn(Optional.of(carriageType));
        when(redisService.getStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt())).thenReturn(Optional.of(0));
        when(userPassengerRelationRepository.findByUserId(1L)).thenReturn(Collections.singletonList(relation));
        when(passengerRepository.findById(100L)).thenReturn(Optional.of(passenger));
        
        PrepareOrderResponse response = service.prepareOrder(request);
        assertNotNull(response);
        assertEquals(1, response.getCarriages().size());
        assertFalse(response.getCarriages().get(0).getHasStock());
        assertEquals(0, response.getCarriages().get(0).getAvailableStock());
    }

    @Test
    void testPrepareOrder_DifferentPassengerTypes() {
        when(ticketInventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(trainRepository.findById(1)).thenReturn(Optional.of(train));
        when(trainStopRepository.findById(100L)).thenReturn(Optional.of(departureStop));
        when(trainStopRepository.findById(200L)).thenReturn(Optional.of(arrivalStop));
        when(stationRepository.findById(10)).thenReturn(Optional.of(departureStation));
        when(stationRepository.findById(20)).thenReturn(Optional.of(arrivalStation));
        when(carriageTypeRepository.findById(1)).thenReturn(Optional.of(carriageType));
        when(redisService.getStock(anyInt(), anyLong(), anyLong(), any(LocalDate.class), anyInt())).thenReturn(Optional.of(30));
        
        // 创建不同乘客类型的乘客
        List<UserPassengerRelation> relations = new ArrayList<>();
        List<Passenger> passengers = new ArrayList<>();
        
        // 儿童乘客
        Passenger childPassenger = new Passenger();
        childPassenger.setPassengerId(101L);
        childPassenger.setRealName("李四");
        childPassenger.setPassengerType((byte) 2);
        UserPassengerRelation childRelation = new UserPassengerRelation();
        childRelation.setUserId(1L);
        childRelation.setPassengerId(101L);
        childRelation.setRelationType((byte) 2);
        
        // 学生乘客
        Passenger studentPassenger = new Passenger();
        studentPassenger.setPassengerId(102L);
        studentPassenger.setRealName("王五");
        studentPassenger.setPassengerType((byte) 3);
        UserPassengerRelation studentRelation = new UserPassengerRelation();
        studentRelation.setUserId(1L);
        studentRelation.setPassengerId(102L);
        studentRelation.setRelationType((byte) 3);
        
        // 残疾乘客
        Passenger disabledPassenger = new Passenger();
        disabledPassenger.setPassengerId(103L);
        disabledPassenger.setRealName("赵六");
        disabledPassenger.setPassengerType((byte) 4);
        UserPassengerRelation disabledRelation = new UserPassengerRelation();
        disabledRelation.setUserId(1L);
        disabledRelation.setPassengerId(103L);
        disabledRelation.setRelationType((byte) 1);
        
        // 军人乘客
        Passenger militaryPassenger = new Passenger();
        militaryPassenger.setPassengerId(104L);
        militaryPassenger.setRealName("钱七");
        militaryPassenger.setPassengerType((byte) 5);
        UserPassengerRelation militaryRelation = new UserPassengerRelation();
        militaryRelation.setUserId(1L);
        militaryRelation.setPassengerId(104L);
        militaryRelation.setRelationType((byte) 1);
        
        // 未知类型乘客
        Passenger unknownPassenger = new Passenger();
        unknownPassenger.setPassengerId(105L);
        unknownPassenger.setRealName("孙八");
        unknownPassenger.setPassengerType((byte) 99);
        UserPassengerRelation unknownRelation = new UserPassengerRelation();
        unknownRelation.setUserId(1L);
        unknownRelation.setPassengerId(105L);
        unknownRelation.setRelationType((byte) 99);
        
        relations.add(childRelation);
        relations.add(studentRelation);
        relations.add(disabledRelation);
        relations.add(militaryRelation);
        relations.add(unknownRelation);
        
        when(userPassengerRelationRepository.findByUserId(1L)).thenReturn(relations);
        when(passengerRepository.findById(101L)).thenReturn(Optional.of(childPassenger));
        when(passengerRepository.findById(102L)).thenReturn(Optional.of(studentPassenger));
        when(passengerRepository.findById(103L)).thenReturn(Optional.of(disabledPassenger));
        when(passengerRepository.findById(104L)).thenReturn(Optional.of(militaryPassenger));
        when(passengerRepository.findById(105L)).thenReturn(Optional.of(unknownPassenger));
        
        PrepareOrderResponse response = service.prepareOrder(request);
        assertNotNull(response);
        assertEquals(5, response.getPassengers().size());
        
        // 验证乘客类型名称
        assertEquals("儿童", response.getPassengers().get(0).getPassengerTypeName());
        assertEquals("学生", response.getPassengers().get(1).getPassengerTypeName());
        assertEquals("残疾", response.getPassengers().get(2).getPassengerTypeName());
        assertEquals("军人", response.getPassengers().get(3).getPassengerTypeName());
        assertEquals("未知", response.getPassengers().get(4).getPassengerTypeName());
        
        // 验证关系类型名称
        assertEquals("亲属", response.getPassengers().get(0).getRelationTypeName());
        assertEquals("其他", response.getPassengers().get(1).getRelationTypeName());
        assertEquals("本人", response.getPassengers().get(2).getRelationTypeName());
        assertEquals("本人", response.getPassengers().get(3).getRelationTypeName());
        assertEquals("未知", response.getPassengers().get(4).getRelationTypeName());
    }
} 
 