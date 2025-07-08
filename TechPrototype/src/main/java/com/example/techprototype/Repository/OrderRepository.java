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
    
    /**
     * 根据用户ID查询订单（按订单时间倒序）
     */
    @Query("SELECT o FROM Order o WHERE o.userId = :userId ORDER BY o.orderTime DESC")
    List<Order> findByUserIdOrderByOrderTimeDesc(@Param("userId") Long userId);
    
    /**
     * 根据用户ID和订单号查询订单
     */
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.orderNumber LIKE %:orderNumber% ORDER BY o.orderTime DESC")
    List<Order> findByUserIdAndOrderNumberContaining(@Param("userId") Long userId, @Param("orderNumber") String orderNumber);
    
    /**
     * 根据用户ID和订单状态查询订单
     */
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.orderStatus = :orderStatus ORDER BY o.orderTime DESC")
    List<Order> findByUserIdAndOrderStatus(@Param("userId") Long userId, @Param("orderStatus") Byte orderStatus);
    
    /**
     * 根据用户ID和车票出发时间范围查询订单
     */
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.orderId IN (SELECT DISTINCT t.orderId FROM Ticket t WHERE t.travelDate >= :startDate AND t.travelDate <= :endDate) ORDER BY o.orderTime DESC")
    List<Order> findByUserIdAndTicketTravelDateBetween(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
} 