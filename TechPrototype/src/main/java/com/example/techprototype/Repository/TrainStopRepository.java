package com.example.techprototype.Repository;

import com.example.techprototype.Entity.TrainStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TrainStopRepository extends JpaRepository<TrainStop, Long> {
    
    Optional<TrainStop> findByStopId(Long stopId);
    
    /**
     * 根据车次ID和站点ID查询车次停靠信息
     */
    TrainStop findByTrainIdAndStationId(Integer trainId, Integer stationId);
} 