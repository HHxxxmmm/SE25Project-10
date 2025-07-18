package com.example.techprototype.Service.Impl;

import com.example.techprototype.DTO.*;
import com.example.techprototype.Entity.Passenger;
import com.example.techprototype.Entity.User;
import com.example.techprototype.Entity.UserPassengerRelation;
import com.example.techprototype.Repository.PassengerRepository;
import com.example.techprototype.Repository.UserPassengerRelationRepository;
import com.example.techprototype.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PassengerManagementServiceImplTest {
    @InjectMocks
    private PassengerManagementServiceImpl service;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PassengerRepository passengerRepository;
    @Mock
    private UserPassengerRelationRepository userPassengerRelationRepository;

    private User user;
    private Passenger passenger;
    private UserPassengerRelation relation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = new User();
        user.setUserId(1L);
        user.setRelatedPassenger(1);
        user.setPassengerId(100L);
        passenger = new Passenger();
        passenger.setPassengerId(100L);
        passenger.setRealName("张三");
        passenger.setIdCardNumber("123456789012345678");
        passenger.setPhoneNumber("13900000000");
        relation = new UserPassengerRelation();
        relation.setUserId(1L);
        relation.setPassengerId(100L);
        relation.setRelationId(10L);
    }

    @Test
    void testCheckCanAddPassenger_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        CheckAddPassengerResponse resp = service.checkCanAddPassenger(1L);
        assertTrue(resp.isAllowed());
    }

    @Test
    void testCheckCanAddPassenger_UserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        CheckAddPassengerResponse resp = service.checkCanAddPassenger(1L);
        assertFalse(resp.isAllowed());
        assertEquals("用户不存在", resp.getMessage());
    }

    @Test
    void testCheckCanAddPassenger_MaxLimit() {
        user.setRelatedPassenger(3);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        CheckAddPassengerResponse resp = service.checkCanAddPassenger(1L);
        assertFalse(resp.isAllowed());
        assertEquals("已达到最大乘车人数量限制（3人）", resp.getMessage());
    }

    @Test
    void testAddPassenger_Success() {
        AddPassengerRequest req = new AddPassengerRequest();
        req.setUserId(1L);
        req.setRealName("张三");
        req.setIdCardNumber("123456789012345678");
        req.setPhoneNumber("13900000000");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passengerRepository.findByIdCardNumber("123456789012345678")).thenReturn(Optional.of(passenger));
        when(userPassengerRelationRepository.existsByUserIdAndPassengerId(1L, 100L)).thenReturn(false);
        when(userPassengerRelationRepository.save(any(UserPassengerRelation.class))).thenReturn(relation);
        when(userRepository.save(any(User.class))).thenReturn(user);
        AddPassengerResponse resp = service.addPassenger(req);
        assertTrue(resp.isSuccess());
        assertEquals(10L, resp.getRelationId());
    }

    @Test
    void testAddPassenger_UserNotFound() {
        AddPassengerRequest req = new AddPassengerRequest();
        req.setUserId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        AddPassengerResponse resp = service.addPassenger(req);
        assertFalse(resp.isSuccess());
        assertEquals("用户不存在", resp.getMessage());
    }

    @Test
    void testAddPassenger_MaxLimit() {
        AddPassengerRequest req = new AddPassengerRequest();
        req.setUserId(1L);
        user.setRelatedPassenger(3);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        AddPassengerResponse resp = service.addPassenger(req);
        assertFalse(resp.isSuccess());
        assertEquals("已达到最大乘车人数量限制（3人）", resp.getMessage());
    }

    @Test
    void testAddPassenger_PassengerNotFound() {
        AddPassengerRequest req = new AddPassengerRequest();
        req.setUserId(1L);
        req.setIdCardNumber("notfound");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passengerRepository.findByIdCardNumber("notfound")).thenReturn(Optional.empty());
        AddPassengerResponse resp = service.addPassenger(req);
        assertFalse(resp.isSuccess());
        assertEquals("该乘车人信息无效", resp.getMessage());
    }

    @Test
    void testAddPassenger_InfoNotMatch() {
        AddPassengerRequest req = new AddPassengerRequest();
        req.setUserId(1L);
        req.setRealName("李四");
        req.setIdCardNumber("123456789012345678");
        req.setPhoneNumber("13900000000");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passengerRepository.findByIdCardNumber("123456789012345678")).thenReturn(Optional.of(passenger));
        AddPassengerResponse resp = service.addPassenger(req);
        assertFalse(resp.isSuccess());
        assertEquals("该乘车人信息无效", resp.getMessage());
    }

    @Test
    void testAddPassenger_AlreadyAdded() {
        AddPassengerRequest req = new AddPassengerRequest();
        req.setUserId(1L);
        req.setRealName("张三");
        req.setIdCardNumber("123456789012345678");
        req.setPhoneNumber("13900000000");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passengerRepository.findByIdCardNumber("123456789012345678")).thenReturn(Optional.of(passenger));
        when(userPassengerRelationRepository.existsByUserIdAndPassengerId(1L, 100L)).thenReturn(true);
        AddPassengerResponse resp = service.addPassenger(req);
        assertFalse(resp.isSuccess());
        assertEquals("该乘车人已被添加", resp.getMessage());
    }

    @Test
    void testAddPassenger_NonSelfPassenger() {
        passenger.setPassengerId(200L);
        
        AddPassengerRequest req = new AddPassengerRequest();
        req.setUserId(1L);
        req.setRealName("张三");
        req.setIdCardNumber("123456789012345678");
        req.setPhoneNumber("13900000000");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passengerRepository.findByIdCardNumber("123456789012345678")).thenReturn(Optional.of(passenger));
        when(userPassengerRelationRepository.existsByUserIdAndPassengerId(1L, 200L)).thenReturn(false);
        when(userPassengerRelationRepository.save(any(UserPassengerRelation.class))).thenReturn(relation);
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        AddPassengerResponse resp = service.addPassenger(req);
        assertTrue(resp.isSuccess());
        
        verify(userPassengerRelationRepository).save(argThat(rel -> rel.getRelationType() == 3));
    }

    @Test
    void testAddPassenger_PhoneNumberNull() {
        passenger.setPhoneNumber(null);
        
        AddPassengerRequest req = new AddPassengerRequest();
        req.setUserId(1L);
        req.setRealName("张三");
        req.setIdCardNumber("123456789012345678");
        req.setPhoneNumber("13900000000");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passengerRepository.findByIdCardNumber("123456789012345678")).thenReturn(Optional.of(passenger));
        when(userPassengerRelationRepository.existsByUserIdAndPassengerId(1L, 100L)).thenReturn(false);
        when(userPassengerRelationRepository.save(any(UserPassengerRelation.class))).thenReturn(relation);
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        AddPassengerResponse resp = service.addPassenger(req);
        assertTrue(resp.isSuccess());
    }

    @Test
    void testAddPassenger_RequestPhoneNumberNull() {
        AddPassengerRequest req = new AddPassengerRequest();
        req.setUserId(1L);
        req.setRealName("张三");
        req.setIdCardNumber("123456789012345678");
        req.setPhoneNumber(null);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passengerRepository.findByIdCardNumber("123456789012345678")).thenReturn(Optional.of(passenger));
        when(userPassengerRelationRepository.existsByUserIdAndPassengerId(1L, 100L)).thenReturn(false);
        when(userPassengerRelationRepository.save(any(UserPassengerRelation.class))).thenReturn(relation);
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        AddPassengerResponse resp = service.addPassenger(req);
        assertTrue(resp.isSuccess());
    }

    @Test
    void testAddPassenger_BothPhoneNumbersNull() {
        passenger.setPhoneNumber(null);
        
        AddPassengerRequest req = new AddPassengerRequest();
        req.setUserId(1L);
        req.setRealName("张三");
        req.setIdCardNumber("123456789012345678");
        req.setPhoneNumber(null);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passengerRepository.findByIdCardNumber("123456789012345678")).thenReturn(Optional.of(passenger));
        when(userPassengerRelationRepository.existsByUserIdAndPassengerId(1L, 100L)).thenReturn(false);
        when(userPassengerRelationRepository.save(any(UserPassengerRelation.class))).thenReturn(relation);
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        AddPassengerResponse resp = service.addPassenger(req);
        assertTrue(resp.isSuccess());
    }

    @Test
    void testAddPassenger_IdCardNumberNotMatch() {
        AddPassengerRequest req = new AddPassengerRequest();
        req.setUserId(1L);
        req.setRealName("张三");
        req.setIdCardNumber("different_id_card");
        req.setPhoneNumber("13900000000");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passengerRepository.findByIdCardNumber("different_id_card")).thenReturn(Optional.of(passenger));
        
        AddPassengerResponse resp = service.addPassenger(req);
        assertFalse(resp.isSuccess());
        assertEquals("该乘车人信息无效", resp.getMessage());
    }

    @Test
    void testAddPassenger_PhoneNumberNotMatch() {
        AddPassengerRequest req = new AddPassengerRequest();
        req.setUserId(1L);
        req.setRealName("张三");
        req.setIdCardNumber("123456789012345678");
        req.setPhoneNumber("13800000000");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passengerRepository.findByIdCardNumber("123456789012345678")).thenReturn(Optional.of(passenger));
        
        AddPassengerResponse resp = service.addPassenger(req);
        assertFalse(resp.isSuccess());
        assertEquals("该乘车人信息无效", resp.getMessage());
    }

    @Test
    void testRefreshUserStatus() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userPassengerRelationRepository.countByUserId(1L)).thenReturn(2L);
        when(userRepository.save(any(User.class))).thenReturn(user);
        service.refreshUserStatus(1L);
        verify(userRepository).save(any(User.class));
        assertEquals(2, user.getRelatedPassenger());
    }

    @Test
    void testRefreshUserStatus_UserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        
        service.refreshUserStatus(1L);
        
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testDeletePassenger_Success() {
        DeletePassengerRequest req = new DeletePassengerRequest();
        req.setUserId(1L);
        req.setPassengerId(100L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passengerRepository.findById(100L)).thenReturn(Optional.of(passenger));
        when(userPassengerRelationRepository.findByUserIdAndPassengerId(1L, 100L)).thenReturn(relation);
        doNothing().when(userPassengerRelationRepository).delete(relation);
        when(userRepository.save(any(User.class))).thenReturn(user);
        BookingResponse resp = service.deletePassenger(req);
        assertEquals("SUCCESS", resp.getStatus());
        assertTrue(resp.getMessage().contains("乘客删除成功"));
    }

    @Test
    void testDeletePassenger_UserNotFound() {
        DeletePassengerRequest req = new DeletePassengerRequest();
        req.setUserId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        BookingResponse resp = service.deletePassenger(req);
        assertEquals("FAILED", resp.getStatus());
        assertEquals("用户不存在", resp.getMessage());
    }

    @Test
    void testDeletePassenger_PassengerNotFound() {
        DeletePassengerRequest req = new DeletePassengerRequest();
        req.setUserId(1L);
        req.setPassengerId(100L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passengerRepository.findById(100L)).thenReturn(Optional.empty());
        BookingResponse resp = service.deletePassenger(req);
        assertEquals("FAILED", resp.getStatus());
        assertEquals("乘客不存在", resp.getMessage());
    }

    @Test
    void testDeletePassenger_NoPermission() {
        DeletePassengerRequest req = new DeletePassengerRequest();
        req.setUserId(1L);
        req.setPassengerId(100L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passengerRepository.findById(100L)).thenReturn(Optional.of(passenger));
        when(userPassengerRelationRepository.findByUserIdAndPassengerId(1L, 100L)).thenReturn(null);
        BookingResponse resp = service.deletePassenger(req);
        assertEquals("FAILED", resp.getStatus());
        assertEquals("您没有权限删除该乘客", resp.getMessage());
    }

    @Test
    void testDeletePassenger_Exception() {
        DeletePassengerRequest req = new DeletePassengerRequest();
        req.setUserId(1L);
        req.setPassengerId(100L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passengerRepository.findById(100L)).thenReturn(Optional.of(passenger));
        when(userPassengerRelationRepository.findByUserIdAndPassengerId(1L, 100L)).thenThrow(new RuntimeException("db error"));
        BookingResponse resp = service.deletePassenger(req);
        assertEquals("FAILED", resp.getStatus());
        assertTrue(resp.getMessage().contains("删除乘客失败"));
    }
} 
 