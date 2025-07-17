package com.example.techprototype.DAO;

import com.example.techprototype.Entity.TicketInventory;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TicketInventoryDAO {
    
    Optional<TicketInventory> findByKey(Integer trainId, Long departureStopId, Long arrivalStopId, LocalDate travelDate, Integer carriageTypeId);
    
    boolean updateAvailableSeats(Long inventoryId, Integer availableSeats, Long cacheVersion, Integer dbVersion);
    
    List<TicketInventory> findByTrainAndDate(Integer trainId, LocalDate travelDate);
    
    List<TicketInventory> findByTrainAndDateAndType(Integer trainId, LocalDate travelDate, Integer carriageTypeId);
    
    void save(TicketInventory ticketInventory);
    
    List<TicketInventory> findAll();
} 