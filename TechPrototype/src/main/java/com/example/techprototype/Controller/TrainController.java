package com.example.techprototype.Controller;

import com.example.techprototype.Entity.Train;
import com.example.techprototype.Service.TrainService;
import com.example.techprototype.DTO.TrainListDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trains")
public class TrainController {
    @Autowired
    private TrainService trainService;

    @GetMapping("/direct")
    public List<Train> getDirectTrains(@RequestParam Integer startStationId, @RequestParam Integer endStationId) {
        return trainService.findDirectTrains(startStationId, endStationId);
    }

    @GetMapping("/byTime")
    public List<Train> getTrainsByTime(@RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime start,
                                       @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime end) {
        return trainService.findTrainsByDepartureTime(start, end);
    }

    @GetMapping("/transfer")
    public List<Map<String, Object>> getTransferTrains(@RequestParam Integer startStationId, @RequestParam Integer endStationId) {
        return trainService.findTransferTrains(startStationId, endStationId);
    }

    @GetMapping("/list")
    public List<TrainListDTO> getTrainListDTO() {
        return trainService.getAllTrainListDTO();
    }
} 