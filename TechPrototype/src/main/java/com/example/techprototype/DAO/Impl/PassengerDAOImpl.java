package com.example.techprototype.DAO.Impl;

import com.example.techprototype.DAO.PassengerDAO;
import com.example.techprototype.Entity.Passenger;
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
public class PassengerDAOImpl implements PassengerDAO {

    private static final Logger logger = LoggerFactory.getLogger(PassengerDAOImpl.class);
    
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Passenger> findByIdCardNumber(String idCardNumber) {
        logger.info("查询身份证号为: {} 的乘客", idCardNumber);
        try {
            TypedQuery<Passenger> query = entityManager.createQuery(
                    "SELECT p FROM Passenger p WHERE p.idCardNumber = :idCardNumber", Passenger.class);
            query.setParameter("idCardNumber", idCardNumber);
            return Optional.ofNullable(query.getSingleResult());
        } catch (NoResultException e) {
            logger.warn("未找到身份证号为 {} 的乘客", idCardNumber);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("查询身份证号为 {} 的乘客时发生错误: {}", idCardNumber, e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public Passenger save(Passenger passenger) {
        logger.info("保存乘客信息: {}", passenger.getRealName());
        try {
            entityManager.persist(passenger);
            entityManager.flush();
            logger.info("乘客保存成功，ID: {}", passenger.getPassengerId());
            return passenger;
        } catch (Exception e) {
            logger.error("保存乘客信息时发生错误: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public Optional<Passenger> findById(Long passengerId) {
        logger.info("查询ID为: {} 的乘客", passengerId);
        try {
            return Optional.ofNullable(entityManager.find(Passenger.class, passengerId));
        } catch (Exception e) {
            logger.error("查询ID为 {} 的乘客时发生错误: {}", passengerId, e.getMessage());
            throw e;
        }
    }
}
