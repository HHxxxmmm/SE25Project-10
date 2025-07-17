package com.example.techprototype.Repository;

import com.example.techprototype.Entity.WaitlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface WaitlistItemRepository extends JpaRepository<WaitlistItem, Long> {
    
    List<WaitlistItem> findByWaitlistId(Long waitlistId);
    
    /**
     * 查询指定条件的待兑现候补订单项
     */
    @Query("SELECT wi FROM WaitlistItem wi WHERE wi.trainId = :trainId AND wi.travelDate = :travelDate " +
           "AND wi.departureStopId = :departureStopId AND wi.arrivalStopId = :arrivalStopId " +
           "AND wi.carriageTypeId = :carriageTypeId AND wi.itemStatus = 1 " +
           "ORDER BY wi.createdTime ASC")
    List<WaitlistItem> findMatchingWaitlistItems(
            @Param("trainId") Integer trainId,
            @Param("travelDate") LocalDate travelDate,
            @Param("departureStopId") Long departureStopId,
            @Param("arrivalStopId") Long arrivalStopId,
            @Param("carriageTypeId") Integer carriageTypeId);
    
    /**
     * 查询候补订单中未兑现的订单项
     */
    @Query("SELECT wi FROM WaitlistItem wi WHERE wi.waitlistId = :waitlistId AND wi.itemStatus = 1")
    List<WaitlistItem> findPendingItemsByWaitlistId(@Param("waitlistId") Long waitlistId);

    @Query("SELECT wi FROM WaitlistItem wi WHERE wi.waitlistId = :waitlistId AND wi.itemStatus IN (1, 2)")
    List<WaitlistItem> findRefundableItemsByWaitlistId(@Param("waitlistId") Long waitlistId);

    List<WaitlistItem> findByItemIdIn(List<Long> itemIds);
} 