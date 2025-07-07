package com.example.techprototype.Repository;

import com.example.techprototype.Entity.Train;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
 
@Repository
public interface TrainRepository extends JpaRepository<Train, Integer> {
} 