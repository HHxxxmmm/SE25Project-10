package com.example.techprototype.Service;

import com.example.techprototype.Entity.Train;
import com.example.techprototype.DTO.TrainListDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class TrainServiceTest {

    @Autowired
    private TrainService trainService;

    @Test
    void findDirectTrains() {
        List<Train> trains = trainService.findDirectTrains(1, 2);
        assertNotNull(trains);
        for (Train train : trains) {
            if (train == null) continue;
            assertNotNull(train.getTrainId());
            assertNotNull(train.getTrainNumber());
        }
    }

    @Test
    void findTrainsByDepartureTime() {
        LocalTime start = LocalTime.of(8, 0);
        LocalTime end = LocalTime.of(12, 0);
        List<Train> trains = trainService.findTrainsByDepartureTime(start, end);
        assertNotNull(trains);
        for (Train train : trains) {
            if (train == null) continue;
            assertNotNull(train.getTrainId());
            assertNotNull(train.getDepartureTime());
        }
    }

    @Test
    void findTransferTrains() {
        List<Map<String, Object>> result = trainService.findTransferTrains(1, 2);
        assertNotNull(result);
        for (Map<String, Object> map : result) {
            if (map == null) continue;
            assertTrue(map.size() > 0);
        }
    }

    @Test
    void getAllTrainListDTO() {
        List<TrainListDTO> list = trainService.getAllTrainListDTO();
        assertNotNull(list);
        for (TrainListDTO dto : list) {
            if (dto == null) continue;
            assertNotNull(dto.getTrain_id());
            assertNotNull(dto.getT_from());
            assertNotNull(dto.getT_to());
        }
    }

    @Test
    void searchTrainsByStations() {
        List<Map<String, Object>> result = trainService.searchTrainsByStations("北京", "上海", "2025-07-01");
        assertNotNull(result);
        for (Map<String, Object> map : result) {
            if (map == null) continue;
            assertTrue(map.containsKey("train_id"));
            assertTrue(map.containsKey("t_from"));
            assertTrue(map.containsKey("t_to"));
        }
    }
}