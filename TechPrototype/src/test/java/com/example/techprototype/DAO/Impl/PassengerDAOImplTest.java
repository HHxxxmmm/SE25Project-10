package com.example.techprototype.DAO.Impl;

import com.example.techprototype.Entity.Passenger;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PassengerDAOImplTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private TypedQuery<Passenger> typedQuery;

    @InjectMocks
    private PassengerDAOImpl passengerDAO;

    private Passenger mockPassenger;

    @BeforeEach
    void setUp() {
        // 创建模拟乘客数据
        mockPassenger = new Passenger();
        mockPassenger.setPassengerId(1L);
        mockPassenger.setRealName("测试乘客");
        mockPassenger.setIdCardNumber("110101199001010011");
        mockPassenger.setPhoneNumber("13800138000");
        mockPassenger.setPassengerType((byte) 1);
    }

    @Test
    @DisplayName("根据身份证号查找乘客 - 成功")
    void testFindByIdCardNumberSuccess() {
        // 模拟查询
        when(entityManager.createQuery(anyString(), eq(Passenger.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), anyString())).thenReturn(typedQuery);
        when(typedQuery.getSingleResult()).thenReturn(mockPassenger);

        // 执行测试
        Optional<Passenger> result = passengerDAO.findByIdCardNumber("110101199001010011");

        // 验证结果
        assertTrue(result.isPresent());
        assertEquals(mockPassenger.getIdCardNumber(), result.get().getIdCardNumber());
        assertEquals(mockPassenger.getRealName(), result.get().getRealName());

        // 验证交互
        verify(entityManager).createQuery(anyString(), eq(Passenger.class));
        verify(typedQuery).setParameter("idCardNumber", "110101199001010011");
        verify(typedQuery).getSingleResult();
    }

    @Test
    @DisplayName("根据身份证号查找乘客 - 不存在")
    void testFindByIdCardNumberNotFound() {
        // 模拟查询抛出NoResultException
        when(entityManager.createQuery(anyString(), eq(Passenger.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), anyString())).thenReturn(typedQuery);
        when(typedQuery.getSingleResult()).thenThrow(NoResultException.class);

        // 执行测试
        Optional<Passenger> result = passengerDAO.findByIdCardNumber("110101199001010022");

        // 验证结果
        assertFalse(result.isPresent());

        // 验证交互
        verify(entityManager).createQuery(anyString(), eq(Passenger.class));
        verify(typedQuery).setParameter("idCardNumber", "110101199001010022");
        verify(typedQuery).getSingleResult();
    }

    @Test
    @DisplayName("保存乘客信息")
    void testSave() {
        // 执行测试
        Passenger result = passengerDAO.save(mockPassenger);

        // 验证结果
        assertNotNull(result);
        assertEquals(mockPassenger, result);

        // 验证交互
        verify(entityManager).persist(mockPassenger);
        verify(entityManager).flush();
    }

    @Test
    @DisplayName("根据ID查找乘客 - 成功")
    void testFindByIdSuccess() {
        // 模拟查询
        when(entityManager.find(Passenger.class, 1L)).thenReturn(mockPassenger);

        // 执行测试
        Optional<Passenger> result = passengerDAO.findById(1L);

        // 验证结果
        assertTrue(result.isPresent());
        assertEquals(mockPassenger.getPassengerId(), result.get().getPassengerId());
        assertEquals(mockPassenger.getRealName(), result.get().getRealName());

        // 验证交互
        verify(entityManager).find(Passenger.class, 1L);
    }

    @Test
    @DisplayName("根据ID查找乘客 - 不存在")
    void testFindByIdNotFound() {
        // 模拟查询返回null
        when(entityManager.find(Passenger.class, 999L)).thenReturn(null);

        // 执行测试
        Optional<Passenger> result = passengerDAO.findById(999L);

        // 验证结果
        assertFalse(result.isPresent());

        // 验证交互
        verify(entityManager).find(Passenger.class, 999L);
    }

    @Test
    @DisplayName("查询乘客时异常处理")
    void testFindByIdCardNumberWithException() {
        // 模拟查询抛出一般异常
        when(entityManager.createQuery(anyString(), eq(Passenger.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), anyString())).thenReturn(typedQuery);
        when(typedQuery.getSingleResult()).thenThrow(new RuntimeException("数据库错误"));

        // 执行测试并验证异常被抛出
        assertThrows(RuntimeException.class, () -> {
            passengerDAO.findByIdCardNumber("110101199001010011");
        });

        // 验证交互
        verify(entityManager).createQuery(anyString(), eq(Passenger.class));
        verify(typedQuery).setParameter("idCardNumber", "110101199001010011");
        verify(typedQuery).getSingleResult();
    }

    @Test
    @DisplayName("根据ID查找乘客 - 异常情况")
    void testFindByIdWithException() {
        // 模拟查询抛出异常
        when(entityManager.find(Passenger.class, 1L)).thenThrow(new RuntimeException("数据库错误"));

        // 执行测试并验证异常被抛出
        assertThrows(RuntimeException.class, () -> {
            passengerDAO.findById(1L);
        });

        // 验证交互
        verify(entityManager).find(Passenger.class, 1L);
    }

    @Test
    @DisplayName("保存乘客信息 - persist异常")
    void testSaveWithPersistException() {
        // 模拟persist操作抛出异常
        doThrow(new RuntimeException("数据库错误")).when(entityManager).persist(mockPassenger);

        // 执行测试并验证异常被抛出
        assertThrows(RuntimeException.class, () -> {
            passengerDAO.save(mockPassenger);
        });

        // 验证交互
        verify(entityManager).persist(mockPassenger);
        // 由于异常，不会调用flush
        verify(entityManager, never()).flush();
    }

    @Test
    @DisplayName("保存乘客信息 - flush异常")
    void testSaveWithFlushException() {
        // 模拟flush操作抛出异常
        doThrow(new RuntimeException("数据库错误")).when(entityManager).flush();

        // 执行测试并验证异常被抛出
        assertThrows(RuntimeException.class, () -> {
            passengerDAO.save(mockPassenger);
        });

        // 验证交互
        verify(entityManager).persist(mockPassenger);
        verify(entityManager).flush();
    }
}