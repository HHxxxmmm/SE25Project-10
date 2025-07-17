package com.example.techprototype.DAO.Impl;

import com.example.techprototype.Entity.UserPassengerRelation;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPassengerRelationDAOImplTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private TypedQuery<UserPassengerRelation> typedQuery;

    @InjectMocks
    private UserPassengerRelationDAOImpl userPassengerRelationDAO;

    private UserPassengerRelation mockRelation;

    @BeforeEach
    void setUp() {
        // 创建模拟关系数据
        mockRelation = new UserPassengerRelation();
        mockRelation.setRelationId(1L);
        mockRelation.setUserId(1L);
        mockRelation.setPassengerId(2L);
        mockRelation.setRelationType((byte) 2);  // 假设2表示亲属关系
        mockRelation.setAlias("父亲");
        mockRelation.setAddedTime(LocalDateTime.now());
    }

    @Test
    @DisplayName("保存用户乘客关系")
    void testSave() {
        // 执行测试
        UserPassengerRelation result = userPassengerRelationDAO.save(mockRelation);

        // 验证结果
        assertNotNull(result);
        assertEquals(mockRelation, result);
        
        // 验证交互
        verify(entityManager).persist(mockRelation);
        verify(entityManager).flush();
    }

    @Test
    @DisplayName("查找用户所有关联乘客 - 成功")
    void testFindAllByUserId() {
        // 准备模拟数据
        UserPassengerRelation relation1 = new UserPassengerRelation();
        relation1.setRelationId(1L);
        relation1.setUserId(1L);
        relation1.setPassengerId(2L);
        
        UserPassengerRelation relation2 = new UserPassengerRelation();
        relation2.setRelationId(2L);
        relation2.setUserId(1L);
        relation2.setPassengerId(3L);
        
        List<UserPassengerRelation> mockList = Arrays.asList(relation1, relation2);
        
        // 模拟查询
        when(entityManager.createQuery(anyString(), eq(UserPassengerRelation.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), anyLong())).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(mockList);

        // 执行测试
        List<UserPassengerRelation> result = userPassengerRelationDAO.findAllByUserId(1L);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getUserId());
        assertEquals(1L, result.get(1).getUserId());
        
        // 验证交互
        verify(entityManager).createQuery(anyString(), eq(UserPassengerRelation.class));
        verify(typedQuery).setParameter("userId", 1L);
        verify(typedQuery).getResultList();
    }

    @Test
    @DisplayName("查找用户所有关联乘客 - 异常情况")
    void testFindAllByUserIdWithException() {
        // 模拟查询抛出异常
        when(entityManager.createQuery(anyString(), eq(UserPassengerRelation.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), anyLong())).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenThrow(new RuntimeException("数据库错误"));

        // 执行测试
        List<UserPassengerRelation> result = userPassengerRelationDAO.findAllByUserId(1L);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        // 验证交互
        verify(entityManager).createQuery(anyString(), eq(UserPassengerRelation.class));
        verify(typedQuery).setParameter("userId", 1L);
        verify(typedQuery).getResultList();
    }

    @Test
    @DisplayName("根据用户ID和乘客ID查找关系 - 成功")
    void testFindByUserIdAndPassengerIdSuccess() {
        // 模拟查询
        when(entityManager.createQuery(anyString(), eq(UserPassengerRelation.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter(eq("userId"), anyLong())).thenReturn(typedQuery);
        when(typedQuery.setParameter(eq("passengerId"), anyLong())).thenReturn(typedQuery);
        when(typedQuery.getSingleResult()).thenReturn(mockRelation);

        // 执行测试
        Optional<UserPassengerRelation> result = userPassengerRelationDAO.findByUserIdAndPassengerId(1L, 2L);

        // 验证结果
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getUserId());
        assertEquals(2L, result.get().getPassengerId());
        
        // 验证交互
        verify(entityManager).createQuery(anyString(), eq(UserPassengerRelation.class));
        verify(typedQuery).setParameter("userId", 1L);
        verify(typedQuery).setParameter("passengerId", 2L);
        verify(typedQuery).getSingleResult();
    }

    @Test
    @DisplayName("根据用户ID和乘客ID查找关系 - 不存在")
    void testFindByUserIdAndPassengerIdNotFound() {
        // 模拟查询抛出NoResultException
        when(entityManager.createQuery(anyString(), eq(UserPassengerRelation.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter(eq("userId"), anyLong())).thenReturn(typedQuery);
        when(typedQuery.setParameter(eq("passengerId"), anyLong())).thenReturn(typedQuery);
        when(typedQuery.getSingleResult()).thenThrow(NoResultException.class);

        // 执行测试
        Optional<UserPassengerRelation> result = userPassengerRelationDAO.findByUserIdAndPassengerId(1L, 999L);

        // 验证结果
        assertFalse(result.isPresent());
        
        // 验证交互
        verify(entityManager).createQuery(anyString(), eq(UserPassengerRelation.class));
        verify(typedQuery).setParameter("userId", 1L);
        verify(typedQuery).setParameter("passengerId", 999L);
        verify(typedQuery).getSingleResult();
    }

    @Test
    @DisplayName("根据用户ID和乘客ID查找关系 - 异常情况")
    void testFindByUserIdAndPassengerIdWithException() {
        // 模拟查询抛出一般异常
        when(entityManager.createQuery(anyString(), eq(UserPassengerRelation.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter(eq("userId"), anyLong())).thenReturn(typedQuery);
        when(typedQuery.setParameter(eq("passengerId"), anyLong())).thenReturn(typedQuery);
        when(typedQuery.getSingleResult()).thenThrow(new RuntimeException("数据库错误"));

        // 执行测试并验证异常被抛出
        assertThrows(RuntimeException.class, () -> {
            userPassengerRelationDAO.findByUserIdAndPassengerId(1L, 2L);
        });
        
        // 验证交互
        verify(entityManager).createQuery(anyString(), eq(UserPassengerRelation.class));
        verify(typedQuery).setParameter("userId", 1L);
        verify(typedQuery).setParameter("passengerId", 2L);
        verify(typedQuery).getSingleResult();
    }
}
