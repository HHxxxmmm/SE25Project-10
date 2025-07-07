package com.example.techprototype.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "train_carriages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainCarriage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "carriage_id")
    private Long carriageId;
    
    @Column(name = "train_id", nullable = false)
    private Integer trainId;
    
    @Column(name = "carriage_number", nullable = false, length = 10)
    private String carriageNumber;
    
    @Column(name = "type_id", nullable = false)
    private Integer typeId;
} 