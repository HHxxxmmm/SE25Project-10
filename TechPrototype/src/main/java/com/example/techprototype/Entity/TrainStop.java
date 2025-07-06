package com.example.techprototype.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalTime;

@Entity
@Table(name = "train_stops")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainStop {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stop_id")
    private Long stopId;
    
    @Column(name = "train_id", nullable = false)
    private Integer trainId;
    
    @Column(name = "station_id", nullable = false)
    private Integer stationId;
    
    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;
    
    @Column(name = "arrival_time")
    private LocalTime arrivalTime;
    
    @Column(name = "departure_time")
    private LocalTime departureTime;
    
    @Column(name = "stop_minutes")
    private Integer stopMinutes;
    
    @Column(name = "distance_from_start")
    private Integer distanceFromStart; // 距离始发站的距离(公里)
} 