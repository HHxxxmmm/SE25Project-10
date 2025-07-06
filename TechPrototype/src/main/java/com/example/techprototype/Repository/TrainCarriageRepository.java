package com.example.techprototype.Repository;

import com.example.techprototype.Entity.TrainCarriage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TrainCarriageRepository extends JpaRepository<TrainCarriage, Long> {
    
    Optional<TrainCarriage> findByCarriageId(Long carriageId);
    
    Optional<TrainCarriage> findByCarriageNumber(String carriageNumber);
    
    Optional<TrainCarriage> findByTrainIdAndCarriageNumber(Integer trainId, String carriageNumber);
} 