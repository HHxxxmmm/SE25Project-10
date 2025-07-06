package com.example.techprototype.Repository;

import com.example.techprototype.Entity.TicketInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketInventoryRepository extends JpaRepository<TicketInventory, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ti FROM TicketInventory ti WHERE ti.trainId = :trainId AND ti.departureStopId = :departureStopId AND ti.arrivalStopId = :arrivalStopId AND ti.travelDate = :travelDate AND ti.carriageTypeId = :carriageTypeId")
    Optional<TicketInventory> findByKeyWithLock(@Param("trainId") Integer trainId, 
                                               @Param("departureStopId") Long departureStopId,
                                               @Param("arrivalStopId") Long arrivalStopId,
                                               @Param("travelDate") LocalDate travelDate,
                                               @Param("carriageTypeId") Integer carriageTypeId);
    
    @Query("SELECT ti FROM TicketInventory ti WHERE ti.trainId = :trainId AND ti.travelDate = :travelDate AND ti.carriageTypeId = :carriageTypeId")
    List<TicketInventory> findByTrainAndDateAndType(@Param("trainId") Integer trainId,
                                                   @Param("travelDate") LocalDate travelDate,
                                                   @Param("carriageTypeId") Integer carriageTypeId);
    
    @Query("SELECT ti FROM TicketInventory ti WHERE ti.trainId = :trainId AND ti.travelDate = :travelDate")
    List<TicketInventory> findByTrainAndDate(@Param("trainId") Integer trainId,
                                            @Param("travelDate") LocalDate travelDate);
} 