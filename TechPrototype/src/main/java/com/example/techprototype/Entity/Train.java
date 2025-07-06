package com.example.techprototype.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalTime;

@Entity
@Table(name = "trains")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Train {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "train_id")
    private Integer trainId;
    
    @Column(name = "train_number", nullable = false, length = 20, unique = true)
    private String trainNumber;
    
    @Column(name = "train_type", nullable = false, length = 10)
    private String trainType; // G-高铁, D-动车, K-快速, T-特快, Z-直达, C-城际
    
    @Column(name = "start_station_id", nullable = false)
    private Integer startStationId;
    
    @Column(name = "end_station_id", nullable = false)
    private Integer endStationId;
    
    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;
    
    @Column(name = "arrival_time", nullable = false)
    private LocalTime arrivalTime;
    
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;
} 