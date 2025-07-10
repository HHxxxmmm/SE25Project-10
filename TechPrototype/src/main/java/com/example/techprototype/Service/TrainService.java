package com.example.techprototype.Service;

import com.example.techprototype.Entity.Train;
import com.example.techprototype.DTO.TrainListDTO;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public interface TrainService {
    List<Train> findDirectTrains(Integer startStationId, Integer endStationId);
    List<Train> findTrainsByDepartureTime(LocalTime start, LocalTime end);
    List<Map<String, Object>> findTransferTrains(Integer startStationId, Integer endStationId);
    List<TrainListDTO> getAllTrainListDTO();
} 