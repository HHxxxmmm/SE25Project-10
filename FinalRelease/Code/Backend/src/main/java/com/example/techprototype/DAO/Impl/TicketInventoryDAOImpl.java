package com.example.techprototype.DAO.Impl;

import com.example.techprototype.DAO.TicketInventoryDAO;
import com.example.techprototype.Entity.TicketInventory;
import com.example.techprototype.Repository.TicketInventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class TicketInventoryDAOImpl implements TicketInventoryDAO {
    
    @Autowired
    private TicketInventoryRepository ticketInventoryRepository;
    
    @Override
    public Optional<TicketInventory> findByKey(Integer trainId, Long departureStopId, Long arrivalStopId, LocalDate travelDate, Integer carriageTypeId) {
        return ticketInventoryRepository.findByKeyWithLock(trainId, departureStopId, arrivalStopId, travelDate, carriageTypeId);
    }
    
    @Override
    @Transactional
    public boolean updateAvailableSeats(Long inventoryId, Integer availableSeats, Long cacheVersion, Integer dbVersion) {
        Optional<TicketInventory> optional = ticketInventoryRepository.findById(inventoryId);
        if (optional.isPresent()) {
            TicketInventory inventory = optional.get();
            if (inventory.getCacheVersion().equals(cacheVersion) && inventory.getDbVersion().equals(dbVersion)) {
                inventory.setAvailableSeats(availableSeats);
                inventory.setCacheVersion(cacheVersion + 1);
                inventory.setDbVersion(dbVersion + 1);
                ticketInventoryRepository.save(inventory);
                return true;
            }
        }
        return false;
    }
    
    @Override
    public List<TicketInventory> findByTrainAndDate(Integer trainId, LocalDate travelDate) {
        return ticketInventoryRepository.findByTrainAndDate(trainId, travelDate);
    }
    
    @Override
    public List<TicketInventory> findByTrainAndDateAndType(Integer trainId, LocalDate travelDate, Integer carriageTypeId) {
        return ticketInventoryRepository.findByTrainAndDateAndType(trainId, travelDate, carriageTypeId);
    }
    
    @Override
    public void save(TicketInventory ticketInventory) {
        ticketInventoryRepository.save(ticketInventory);
    }
    
    @Override
    public List<TicketInventory> findAll() {
        return ticketInventoryRepository.findAll();
    }
} 