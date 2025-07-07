package com.example.techprototype.Repository;

import com.example.techprototype.Entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    
    List<Ticket> findByOrderId(Long orderId);
    
    List<Ticket> findByOrderIdAndTicketIdIn(Long orderId, List<Long> ticketIds);
    
    @Query("SELECT t FROM Ticket t WHERE t.orderId = :orderId AND t.ticketStatus = 1")
    List<Ticket> findValidTicketsByOrderId(@Param("orderId") Long orderId);
    
    @Query("SELECT t FROM Ticket t WHERE t.orderId != :orderId AND t.ticketStatus = :ticketStatus")
    List<Ticket> findByOrderIdNotAndTicketStatus(@Param("orderId") Long orderId, @Param("ticketStatus") byte ticketStatus);
    
    /**
     * 查询乘客在指定日期和时间段内的有效车票
     * 排除已退票和已改签的车票
     */
    @Query("SELECT t FROM Ticket t WHERE t.passengerId = :passengerId " +
           "AND t.travelDate = :travelDate " +
           "AND t.ticketStatus IN (0, 1, 2) " + // 0-待支付, 1-未使用, 2-已使用
           "AND t.ticketId != :excludeTicketId")
    List<Ticket> findValidTicketsByPassengerAndDate(
            @Param("passengerId") Long passengerId,
            @Param("travelDate") LocalDate travelDate,
            @Param("excludeTicketId") Long excludeTicketId);
    
    /**
     * 查询乘客在指定日期和时间段内的有效车票（不排除任何车票）
     */
    @Query("SELECT t FROM Ticket t WHERE t.passengerId = :passengerId " +
           "AND t.travelDate = :travelDate " +
           "AND t.ticketStatus IN (0, 1, 2)") // 0-待支付, 1-未使用, 2-已使用
    List<Ticket> findValidTicketsByPassengerAndDate(
            @Param("passengerId") Long passengerId,
            @Param("travelDate") LocalDate travelDate);
    
    /**
     * 根据乘客ID和车票状态查询车票
     */
    List<Ticket> findByPassengerIdAndTicketStatus(Long passengerId, byte ticketStatus);
    
    /**
     * 根据乘客ID查询所有车票（按创建时间倒序）
     */
    @Query("SELECT t FROM Ticket t WHERE t.passengerId = :passengerId ORDER BY t.createdTime DESC")
    List<Ticket> findByPassengerIdOrderByCreatedTimeDesc(@Param("passengerId") Long passengerId);
    
    /**
     * 根据乘客ID查询有效车票（待支付、未使用、已使用状态）
     */
    @Query("SELECT t FROM Ticket t WHERE t.passengerId = :passengerId AND t.ticketStatus IN (0, 1, 2) ORDER BY t.createdTime DESC")
    List<Ticket> findValidTicketsByPassengerId(@Param("passengerId") Long passengerId);
    
    /**
     * 根据乘客ID和车票状态查询车票（按创建时间倒序）
     */
    @Query("SELECT t FROM Ticket t WHERE t.passengerId = :passengerId AND t.ticketStatus = :ticketStatus ORDER BY t.createdTime DESC")
    List<Ticket> findByPassengerIdAndTicketStatusOrderByCreatedTimeDesc(
            @Param("passengerId") Long passengerId, 
            @Param("ticketStatus") byte ticketStatus);
    
    /**
     * 根据乘客ID和出发日期范围查询有效车票（按创建时间倒序）
     */
    @Query("SELECT t FROM Ticket t WHERE t.passengerId = :passengerId " +
           "AND t.ticketStatus IN (0, 1, 2) " + // 0-待支付, 1-未使用, 2-已使用
           "AND t.travelDate >= :startDate " +
           "AND t.travelDate <= :endDate " +
           "ORDER BY t.createdTime DESC")
    List<Ticket> findValidTicketsByPassengerAndDateRange(
            @Param("passengerId") Long passengerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
    
    /**
     * 根据乘客ID、车票状态和出发日期范围查询车票（按创建时间倒序）
     */
    @Query("SELECT t FROM Ticket t WHERE t.passengerId = :passengerId " +
           "AND t.ticketStatus = :ticketStatus " +
           "AND t.travelDate >= :startDate " +
           "AND t.travelDate <= :endDate " +
           "ORDER BY t.createdTime DESC")
    List<Ticket> findByPassengerIdAndStatusAndDateRange(
            @Param("passengerId") Long passengerId,
            @Param("ticketStatus") byte ticketStatus,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
} 