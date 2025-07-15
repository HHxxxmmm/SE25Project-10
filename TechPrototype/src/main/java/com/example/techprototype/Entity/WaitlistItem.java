package com.example.techprototype.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "waitlist_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WaitlistItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;
    
    @Column(name = "waitlist_id", nullable = false)
    private Long waitlistId;
    
    @Column(name = "passenger_id", nullable = false)
    private Long passengerId;
    
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
    
    @Column(name = "ticket_type", nullable = false)
    private Byte ticketType;
    
    @Column(name = "item_status", nullable = false)
    private Byte itemStatus = 0; // 0-待支付, 1-待兑现, 2-已兑现, 3-已取消
    
    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;
    
    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;
} 
 