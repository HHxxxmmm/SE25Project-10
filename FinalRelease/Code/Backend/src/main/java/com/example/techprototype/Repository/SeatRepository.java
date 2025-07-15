package com.example.techprototype.Repository;

import com.example.techprototype.Entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    
    /**
     * 查找指定车厢的所有座位
     */
    @Query("SELECT s FROM Seat s WHERE s.carriageId = :carriageId")
    List<Seat> findByCarriageId(@Param("carriageId") Long carriageId);
    
    /**
     * 查找指定车次和车厢类型的所有座位
     */
    @Query("SELECT s FROM Seat s WHERE s.carriageId IN (SELECT tc.carriageId FROM TrainCarriage tc WHERE tc.trainId = :trainId AND tc.typeId = :typeId)")
    List<Seat> findByTrainAndType(@Param("trainId") Integer trainId, @Param("typeId") Integer typeId);
    
    /**
     * 根据车厢ID和座位号查找座位
     */
    List<Seat> findByCarriageIdAndSeatNumber(Long carriageId, String seatNumber);
    
    /**
     * 查找指定车厢的可用座位（基于位图查询）
     * 注意：这个方法需要在Service层结合位图逻辑使用
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.carriageId = :carriageId ORDER BY s.seatId")
    List<Seat> findSeatsByCarriageIdForUpdate(@Param("carriageId") Long carriageId);
} 