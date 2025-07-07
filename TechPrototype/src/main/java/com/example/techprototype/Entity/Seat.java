package com.example.techprototype.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "seats")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Seat {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_id")
    private Long seatId;
    
    @Column(name = "carriage_id", nullable = false)
    private Long carriageId;
    
    @Column(name = "seat_number", nullable = false, length = 10)
    private String seatNumber;
    
    @Column(name = "seat_type", length = 10)
    private String seatType; // 靠窗, 靠过道, 中间等
    
    @Column(name = "is_available")
    private Boolean isAvailable = true;
} 