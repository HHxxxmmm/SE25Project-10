package com.example.techprototype.Repository;

import com.example.techprototype.Entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface StationRepository extends JpaRepository<Station, Integer> {
    
    Optional<Station> findByStationId(Integer stationId);
    
    Optional<Station> findByStationName(String stationName);
    
    List<Station> findByStationNameContainingOrCityContaining(String stationName, String city);
} 