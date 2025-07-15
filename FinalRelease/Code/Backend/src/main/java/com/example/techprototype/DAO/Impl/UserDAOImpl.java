package com.example.techprototype.DAO.Impl;

import com.example.techprototype.DAO.UserDAO;
import com.example.techprototype.Entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;

@Repository
public class UserDAOImpl implements UserDAO {

    private static final Logger logger = LoggerFactory.getLogger(UserDAOImpl.class);
    
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<User> findByPhoneNumber(String phoneNumber) {
        logger.info("查询手机号为: {} 的用户", phoneNumber);
        try {
            TypedQuery<User> query = entityManager.createQuery(
                    "SELECT u FROM User u WHERE u.phoneNumber = :phoneNumber", User.class);
            query.setParameter("phoneNumber", phoneNumber);
            return Optional.ofNullable(query.getSingleResult());
        } catch (NoResultException e) {
            logger.warn("未找到手机号为 {} 的用户", phoneNumber);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("查询手机号为 {} 的用户时发生错误: {}", phoneNumber, e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public User save(User user) {
        logger.info("保存用户信息: {}", user.getPhoneNumber());
        try {
            entityManager.persist(user);
            entityManager.flush();
            logger.info("用户保存成功，ID: {}", user.getUserId());
            return user;
        } catch (Exception e) {
            logger.error("保存用户信息时发生错误: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public Optional<User> findById(Long userId) {
        logger.info("查询ID为: {} 的用户", userId);
        try {
            return Optional.ofNullable(entityManager.find(User.class, userId));
        } catch (Exception e) {
            logger.error("查询ID为 {} 的用户时发生错误: {}", userId, e.getMessage());
            throw e;
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        logger.info("查询邮箱为: {} 的用户", email);
        if (email == null || email.trim().isEmpty()) {
            return Optional.empty();
        }
        
        try {
            TypedQuery<User> query = entityManager.createQuery(
                    "SELECT u FROM User u WHERE u.email = :email", User.class);
            query.setParameter("email", email);
            return Optional.ofNullable(query.getSingleResult());
        } catch (NoResultException e) {
            logger.warn("未找到邮箱为 {} 的用户", email);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("查询邮箱为 {} 的用户时发生错误: {}", email, e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public void update(User user) {
        logger.info("更新用户信息: {}", user.getUserId());
        try {
            entityManager.merge(user);
            entityManager.flush();
            logger.info("用户信息更新成功，ID: {}", user.getUserId());
        } catch (Exception e) {
            logger.error("更新用户信息时发生错误: {}", e.getMessage());
            throw e;
        }
    }
}
