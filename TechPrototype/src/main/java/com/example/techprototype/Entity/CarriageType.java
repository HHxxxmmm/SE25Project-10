package com.example.techprototype.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "carriage_types")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarriageType {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "type_id")
    private Integer typeId;
    
    @Column(name = "type_name", nullable = false, length = 20)
    private String typeName;
    
    @Column(name = "seat_layout", columnDefinition = "json")
    private String seatLayout;
    
    @Column(name = "seat_count", nullable = false)
    private Integer seatCount;
} 