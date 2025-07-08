package com.example.techprototype.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketInventory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_id")
    private Long inventoryId;
    
    @Column(name = "train_id", nullable = false)
    private Integer trainId;
    
    @Column(name = "departure_stop_id", nullable = false)
    private Long departureStopId;
    
    @Column(name = "arrival_stop_id", nullable = false)
    private Long arrivalStopId;
    
    @Column(name = "travel_date", nullable = false)
    private LocalDate travelDate;
    
    @Column(name = "carriage_type_id", nullable = false)
    private Integer carriageTypeId;
    
    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;
    
    @Column(name = "available_seats", nullable = false)
    private Integer availableSeats;
    
    @Column(name = "cache_version", nullable = false)
    private Long cacheVersion = 0L;
    
    @Column(name = "db_version", nullable = false)
    private Integer dbVersion = 0;
    
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;
} 