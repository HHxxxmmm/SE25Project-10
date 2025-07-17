package com.example.techprototype.DAO.Impl;

import com.example.techprototype.DAO.UserDAO;
import com.example.techprototype.Entity.User;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDAOImplTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private TypedQuery<User> typedQuery;

    @InjectMocks
    private UserDAOImpl userDAO;

    private User mockUser;

    @BeforeEach
    void setUp() {
        // 创建模拟用户数据
        mockUser = new User();
        mockUser.setUserId(1L);
        mockUser.setRealName("测试用户");
        mockUser.setPhoneNumber("13800138000");
        mockUser.setEmail("test@example.com");
        mockUser.setPasswordHash("hashed_password");
        mockUser.setAccountStatus((byte) 1);
        mockUser.setRegistrationTime(LocalDateTime.now());
        mockUser.setPassengerId(1L);
        mockUser.setRelatedPassenger(1);
    }

    @Test
    @DisplayName("根据手机号查找用户 - 成功")
    void testFindByPhoneNumberSuccess() {
        // 模拟查询
        when(entityManager.createQuery(anyString(), eq(User.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), anyString())).thenReturn(typedQuery);
        when(typedQuery.getSingleResult()).thenReturn(mockUser);

        // 执行测试
        Optional<User> result = userDAO.findByPhoneNumber("13800138000");

        // 验证结果
        assertTrue(result.isPresent());
        assertEquals(mockUser.getUserId(), result.get().getUserId());
        assertEquals(mockUser.getPhoneNumber(), result.get().getPhoneNumber());
        
        // 验证交互
        verify(entityManager).createQuery(anyString(), eq(User.class));
        verify(typedQuery).setParameter("phoneNumber", "13800138000");
        verify(typedQuery).getSingleResult();
    }

    @Test
    @DisplayName("根据手机号查找用户 - 不存在")
    void testFindByPhoneNumberNotFound() {
        // 模拟查询抛出NoResultException
        when(entityManager.createQuery(anyString(), eq(User.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), anyString())).thenReturn(typedQuery);
        when(typedQuery.getSingleResult()).thenThrow(NoResultException.class);

        // 执行测试
        Optional<User> result = userDAO.findByPhoneNumber("13900139000");

        // 验证结果
        assertFalse(result.isPresent());
        
        // 验证交互
        verify(entityManager).createQuery(anyString(), eq(User.class));
        verify(typedQuery).setParameter("phoneNumber", "13900139000");
        verify(typedQuery).getSingleResult();
    }

    @Test
    @DisplayName("保存用户")
    void testSave() {
        // 执行测试
        User result = userDAO.save(mockUser);

        // 验证结果
        assertNotNull(result);
        assertEquals(mockUser, result);
        
        // 验证交互
        verify(entityManager).persist(mockUser);
        verify(entityManager).flush();
    }

    @Test
    @DisplayName("根据ID查找用户 - 成功")
    void testFindByIdSuccess() {
        // 模拟查询
        when(entityManager.find(User.class, 1L)).thenReturn(mockUser);

        // 执行测试
        Optional<User> result = userDAO.findById(1L);

        // 验证结果
        assertTrue(result.isPresent());
        assertEquals(mockUser.getUserId(), result.get().getUserId());
        
        // 验证交互
        verify(entityManager).find(User.class, 1L);
    }

    @Test
    @DisplayName("根据ID查找用户 - 不存在")
    void testFindByIdNotFound() {
        // 模拟查询返回null
        when(entityManager.find(User.class, 999L)).thenReturn(null);

        // 执行测试
        Optional<User> result = userDAO.findById(999L);

        // 验证结果
        assertFalse(result.isPresent());
        
        // 验证交互
        verify(entityManager).find(User.class, 999L);
    }

    @Test
    @DisplayName("根据邮箱查找用户 - 成功")
    void testFindByEmailSuccess() {
        // 模拟查询
        when(entityManager.createQuery(anyString(), eq(User.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), anyString())).thenReturn(typedQuery);
        when(typedQuery.getSingleResult()).thenReturn(mockUser);

        // 执行测试
        Optional<User> result = userDAO.findByEmail("test@example.com");

        // 验证结果
        assertTrue(result.isPresent());
        assertEquals(mockUser.getEmail(), result.get().getEmail());
        
        // 验证交互
        verify(entityManager).createQuery(anyString(), eq(User.class));
        verify(typedQuery).setParameter("email", "test@example.com");
        verify(typedQuery).getSingleResult();
    }

    @Test
    @DisplayName("根据邮箱查找用户 - 不存在")
    void testFindByEmailNotFound() {
        // 模拟查询抛出NoResultException
        when(entityManager.createQuery(anyString(), eq(User.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), anyString())).thenReturn(typedQuery);
        when(typedQuery.getSingleResult()).thenThrow(NoResultException.class);

        // 执行测试
        Optional<User> result = userDAO.findByEmail("nonexistent@example.com");

        // 验证结果
        assertFalse(result.isPresent());
        
        // 验证交互
        verify(entityManager).createQuery(anyString(), eq(User.class));
        verify(typedQuery).setParameter("email", "nonexistent@example.com");
        verify(typedQuery).getSingleResult();
    }

    @Test
    @DisplayName("根据空邮箱查找用户")
    void testFindByEmptyEmail() {
        // 执行测试
        Optional<User> result = userDAO.findByEmail("");

        // 验证结果
        assertFalse(result.isPresent());
        
        // 验证没有交互
        verify(entityManager, never()).createQuery(anyString(), any());
    }

    @Test
    @DisplayName("更新用户信息")
    void testUpdate() {
        // 执行测试
        userDAO.update(mockUser);

        // 验证交互
        verify(entityManager).merge(mockUser);
        verify(entityManager).flush();
    }

    @Test
    @DisplayName("根据手机号查找用户 - 发生异常")
    void testFindByPhoneNumberException() {
        // 模拟查询时抛出异常
        when(entityManager.createQuery(anyString(), eq(User.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), anyString())).thenReturn(typedQuery);
        when(typedQuery.getSingleResult()).thenThrow(new RuntimeException("Database error"));

        // 执行测试并验证异常
        assertThrows(RuntimeException.class, () -> {
            userDAO.findByPhoneNumber("13800138000");
        });
        
        // 验证交互
        verify(entityManager).createQuery(anyString(), eq(User.class));
        verify(typedQuery).setParameter("phoneNumber", "13800138000");
        verify(typedQuery).getSingleResult();
    }

    @Test
    @DisplayName("根据ID查找用户 - 发生异常")
    void testFindByIdException() {
        // 模拟查询时抛出异常
        when(entityManager.find(User.class, 1L)).thenThrow(new RuntimeException("Database error"));

        // 执行测试并验证异常
        assertThrows(RuntimeException.class, () -> {
            userDAO.findById(1L);
        });
        
        // 验证交互
        verify(entityManager).find(User.class, 1L);
    }

    @Test
    @DisplayName("根据邮箱查找用户 - 发生异常")
    void testFindByEmailException() {
        // 模拟查询时抛出异常
        when(entityManager.createQuery(anyString(), eq(User.class))).thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), anyString())).thenReturn(typedQuery);
        when(typedQuery.getSingleResult()).thenThrow(new RuntimeException("Database error"));

        // 执行测试并验证异常
        assertThrows(RuntimeException.class, () -> {
            userDAO.findByEmail("test@example.com");
        });
        
        // 验证交互
        verify(entityManager).createQuery(anyString(), eq(User.class));
        verify(typedQuery).setParameter("email", "test@example.com");
        verify(typedQuery).getSingleResult();
    }
    
    @Test
    @DisplayName("根据null邮箱查找用户")
    void testFindByNullEmail() {
        // 执行测试
        Optional<User> result = userDAO.findByEmail(null);

        // 验证结果
        assertFalse(result.isPresent());
        
        // 验证没有交互
        verify(entityManager, never()).createQuery(anyString(), any());
    }

    @Test
    @DisplayName("保存用户 - persist发生异常")
    void testSaveException() {
        // 模拟保存时抛出异常
        doThrow(new RuntimeException("Database error")).when(entityManager).persist(mockUser);

        // 执行测试并验证异常
        assertThrows(RuntimeException.class, () -> {
            userDAO.save(mockUser);
        });
        
        // 验证交互
        verify(entityManager).persist(mockUser);
        // 由于异常，不会调用flush
        verify(entityManager, never()).flush();
    }
    
    @Test
    @DisplayName("保存用户 - flush发生异常")
    void testSaveFlushException() {
        // 模拟flush时抛出异常
        doThrow(new RuntimeException("Flush error")).when(entityManager).flush();

        // 执行测试并验证异常
        assertThrows(RuntimeException.class, () -> {
            userDAO.save(mockUser);
        });
        
        // 验证交互
        verify(entityManager).persist(mockUser);
        verify(entityManager).flush();
    }

    @Test
    @DisplayName("更新用户信息 - merge发生异常")
    void testUpdateException() {
        // 模拟更新时抛出异常
        doThrow(new RuntimeException("Database error")).when(entityManager).merge(mockUser);

        // 执行测试并验证异常
        assertThrows(RuntimeException.class, () -> {
            userDAO.update(mockUser);
        });
        
        // 验证交互
        verify(entityManager).merge(mockUser);
        // 由于异常，不会调用flush
        verify(entityManager, never()).flush();
    }
    
    @Test
    @DisplayName("更新用户信息 - flush发生异常")
    void testUpdateFlushException() {
        // 模拟flush时抛出异常
        doThrow(new RuntimeException("Flush error")).when(entityManager).flush();

        // 执行测试并验证异常
        assertThrows(RuntimeException.class, () -> {
            userDAO.update(mockUser);
        });
        
        // 验证交互
        verify(entityManager).merge(mockUser);
        verify(entityManager).flush();
    }
}
