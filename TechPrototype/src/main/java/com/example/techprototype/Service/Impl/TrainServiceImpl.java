package com.example.techprototype.Service.Impl;

import com.example.techprototype.DAO.TrainDAO;
import com.example.techprototype.DTO.TrainListDTO;
import com.example.techprototype.Entity.Train;
import com.example.techprototype.Entity.TrainStop;
import com.example.techprototype.Entity.Station;
import com.example.techprototype.Repository.TrainRepository;
import com.example.techprototype.Repository.TrainStopRepository;
import com.example.techprototype.Repository.StationRepository;
import com.example.techprototype.Service.TrainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class TrainServiceImpl implements TrainService {
    @Autowired
    private TrainDAO trainDAO;
    @Autowired
    private TrainRepository trainRepository;
    @Autowired
    private TrainStopRepository trainStopRepository;
    @Autowired
    private StationRepository stationRepository;

    @Override
    public List<Train> findDirectTrains(Integer startStationId, Integer endStationId) {
        return trainDAO.findDirectTrains(startStationId, endStationId);
    }

    @Override
    public List<Train> findTrainsByDepartureTime(LocalTime start, LocalTime end) {
        return trainDAO.findByDepartureTimeBetween(start, end);
    }

    @Override
    public List<Map<String, Object>> findTransferTrains(Integer startStationId, Integer endStationId) {
        // 使用DAO层查询中转站
        List<Integer> transferStations = trainDAO.findAllTransferStations(startStationId, endStationId);
        
        List<Map<String, Object>> result = new ArrayList<>();
        // 性能优化：限制最大中转站数量
        int maxTransferStations = Math.min(transferStations.size(), 10);
        
        for (int i = 0; i < maxTransferStations; i++) {
            Integer transferStationId = transferStations.get(i);
            result.addAll(trainDAO.findTransferTrains(startStationId, transferStationId, endStationId));
        }
        return result;
    }

    @Override
    public List<TrainListDTO> getAllTrainListDTO() {
        List<Train> trains = trainRepository.findAll();
        List<TrainListDTO> result = new ArrayList<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (Train train : trains) {
            TrainListDTO dto = new TrainListDTO();
            dto.setTrain_id(train.getTrainNumber());
            // 查询所有停靠站
            List<TrainStop> stops = trainStopRepository.findByTrainIdOrderBySequenceNumberAsc(train.getTrainId());
            List<String> path = new ArrayList<>();
            for (TrainStop stop : stops) {
                stationRepository.findById(stop.getStationId()).ifPresent(station -> path.add(station.getStationName()));
            }
            dto.setT_path(path);
            dto.setT_station_number(path.size());
            if (!path.isEmpty()) {
                dto.setT_from(path.get(0));
                dto.setT_to(path.get(path.size() - 1));
            }
            dto.setT_start_time(train.getDepartureTime().atDate(java.time.LocalDate.now()).format(dtf));
            dto.setT_end_time(train.getArrivalTime().atDate(java.time.LocalDate.now()).format(dtf));
            // 席别、余票、价格（此处可根据实际业务补全，暂用mock数据）
            dto.setSeat(Arrays.asList(1, 3, 4));
            dto.setSeat_number(Arrays.asList(100, 200, 50));
            dto.setSeat_price(Arrays.asList(200, 150, 80));
            result.add(dto);
        }
        return result;
    }
} 