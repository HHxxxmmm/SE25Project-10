package com.example.techprototype.Repository;

import com.example.techprototype.Entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    Optional<Order> findByOrderNumber(String orderNumber);
    
    Optional<Order> findByOrderNumberAndUserId(String orderNumber, Long userId);
    
    Optional<Order> findByOrderIdAndUserId(Long orderId, Long userId);
    
    List<Order> findByUserIdOrderByOrderTimeDesc(Long userId);
    
    List<Order> findByUserIdAndOrderNumberContaining(Long userId, String orderNumber);
    
    List<Order> findByUserIdAndOrderStatus(Long userId, Byte orderStatus);
    
    @Query("SELECT DISTINCT o FROM Order o JOIN Ticket t ON o.orderId = t.orderId " +
           "WHERE o.userId = :userId AND t.travelDate BETWEEN :startDate AND :endDate " +
           "ORDER BY o.orderTime DESC")
    List<Order> findByUserIdAndTicketTravelDateBetween(@Param("userId") Long userId, 
                                                       @Param("startDate") LocalDate startDate, 
                                                       @Param("endDate") LocalDate endDate);
    
    /**
     * 查询超时的待支付订单
     */
    @Query("SELECT o FROM Order o WHERE o.orderStatus = 0 AND o.orderTime < :timeoutThreshold")
    List<Order> findTimeoutOrders(@Param("timeoutThreshold") LocalDateTime timeoutThreshold);
} 