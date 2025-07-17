package com.example.techprototype.Repository;

import com.example.techprototype.Entity.WaitlistOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WaitlistOrderRepository extends JpaRepository<WaitlistOrder, Long> {
    
    Optional<WaitlistOrder> findByOrderNumber(String orderNumber);
    
    Optional<WaitlistOrder> findByOrderNumberAndUserId(String orderNumber, Long userId);
    
    List<WaitlistOrder> findByUserIdOrderByOrderTimeDesc(Long userId);
    
    List<WaitlistOrder> findByUserIdAndOrderStatus(Long userId, Byte orderStatus);
    
    /**
     * 查询待兑现的候补订单（按创建时间排序）
     */
    @Query("SELECT wo FROM WaitlistOrder wo WHERE wo.orderStatus = 1 AND wo.expireTime > :now ORDER BY wo.orderTime ASC")
    List<WaitlistOrder> findPendingFulfillmentOrders(@Param("now") LocalDateTime now);
    
    /**
     * 查询指定条件的待兑现候补订单项
     */
    @Query("SELECT wo FROM WaitlistOrder wo JOIN WaitlistItem wi ON wo.waitlistId = wi.waitlistId " +
           "WHERE wo.orderStatus = 1 AND wi.trainId = :trainId AND wi.travelDate = :travelDate " +
           "AND wi.departureStopId = :departureStopId AND wi.arrivalStopId = :arrivalStopId " +
           "AND wi.carriageTypeId = :carriageTypeId AND wi.itemStatus = 1 " +
           "ORDER BY wo.orderTime ASC")
    List<WaitlistOrder> findMatchingWaitlistOrders(
            @Param("trainId") Integer trainId,
            @Param("travelDate") String travelDate,
            @Param("departureStopId") Long departureStopId,
            @Param("arrivalStopId") Long arrivalStopId,
            @Param("carriageTypeId") Integer carriageTypeId);
} 
 