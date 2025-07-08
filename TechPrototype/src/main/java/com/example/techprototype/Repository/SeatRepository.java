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
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.carriageId = :carriageId AND s.isAvailable = true ORDER BY s.seatId LIMIT 1")
    Optional<Seat> findAvailableSeatByCarriageId(@Param("carriageId") Long carriageId);
    
    @Query("SELECT s FROM Seat s WHERE s.carriageId = :carriageId AND s.isAvailable = true")
    List<Seat> findAvailableSeatsByCarriageId(@Param("carriageId") Long carriageId);
    
    @Query("SELECT s FROM Seat s WHERE s.carriageId IN (SELECT tc.carriageId FROM TrainCarriage tc WHERE tc.trainId = :trainId AND tc.typeId = :typeId) AND s.isAvailable = true")
    List<Seat> findAvailableSeatsByTrainAndType(@Param("trainId") Integer trainId, @Param("typeId") Integer typeId);
    
    List<Seat> findByCarriageIdAndSeatNumber(Long carriageId, String seatNumber);
} 