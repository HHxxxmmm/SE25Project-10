package com.example.techprototype.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_passenger_relations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPassengerRelation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "relation_id")
    private Long relationId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "passenger_id", nullable = false)
    private Long passengerId;
    
    @Column(name = "relation_type", nullable = false)
    private Byte relationType; // 1-本人, 2-亲属, 3-其他
    
    @Column(name = "alias", length = 50)
    private String alias;
    
    @Column(name = "added_time", nullable = false)
    private LocalDateTime addedTime;
} 