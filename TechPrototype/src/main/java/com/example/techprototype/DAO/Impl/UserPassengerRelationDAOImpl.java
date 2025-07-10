package com.example.techprototype.DAO.Impl;

import com.example.techprototype.DAO.UserPassengerRelationDAO;
import com.example.techprototype.Entity.UserPassengerRelation;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class UserPassengerRelationDAOImpl implements UserPassengerRelationDAO {

    private static final Logger logger = LoggerFactory.getLogger(UserPassengerRelationDAOImpl.class);
    
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public UserPassengerRelation save(UserPassengerRelation relation) {
        logger.info("保存用户(ID:{})和乘客(ID:{})的关系", relation.getUserId(), relation.getPassengerId());
        try {
            entityManager.persist(relation);
            entityManager.flush();
            logger.info("关系保存成功，ID: {}", relation.getRelationId());
            return relation;
        } catch (Exception e) {
            logger.error("保存用户和乘客关系时发生错误: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public List<UserPassengerRelation> findAllByUserId(Long userId) {
        logger.info("查询用户ID为: {} 的所有关联乘客", userId);
        try {
            TypedQuery<UserPassengerRelation> query = entityManager.createQuery(
                    "SELECT r FROM UserPassengerRelation r WHERE r.userId = :userId", UserPassengerRelation.class);
            query.setParameter("userId", userId);
            return query.getResultList();
        } catch (Exception e) {
            logger.error("查询用户ID为 {} 的关联乘客时发生错误: {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public Optional<UserPassengerRelation> findByUserIdAndPassengerId(Long userId, Long passengerId) {
        logger.info("查询用户ID为: {} 与乘客ID为: {} 的关系", userId, passengerId);
        try {
            TypedQuery<UserPassengerRelation> query = entityManager.createQuery(
                    "SELECT r FROM UserPassengerRelation r WHERE r.userId = :userId AND r.passengerId = :passengerId", 
                    UserPassengerRelation.class);
            query.setParameter("userId", userId);
            query.setParameter("passengerId", passengerId);
            return Optional.ofNullable(query.getSingleResult());
        } catch (NoResultException e) {
            logger.warn("未找到用户ID为 {} 与乘客ID为 {} 的关系", userId, passengerId);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("查询用户与乘客关系时发生错误: {}", e.getMessage());
            throw e;
        }
    }
}
