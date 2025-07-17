package com.example.techprototype.DAO;

import com.example.techprototype.Entity.Train;
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
class TrainDAOTest {

    @Autowired
    private TrainDAO trainDAO;

    @Test
    void testFindDirectTrains() {
        // 假设1和2是存在的station_id
        List<Train> trains = trainDAO.findDirectTrains(1, 2);
        assertNotNull(trains);
        // 可根据实际数据断言数量
        // assertEquals(1, trains.size());
    }

    @Test
    void testFindByDepartureTimeBetween() {
        LocalTime start = LocalTime.of(8, 0);
        LocalTime end = LocalTime.of(12, 0);
        List<Train> trains = trainDAO.findByDepartureTimeBetween(start, end);
        assertNotNull(trains);
        // assertTrue(trains.size() > 0);
    }

    @Test
    void testFindTransferTrains() {
        // 假设1为起点，3为中转，2为终点
        List<Map<String, Object>> result = trainDAO.findTransferTrains(1, 3, 2);
        assertNotNull(result);
        // assertTrue(result.size() > 0);
    }

    @Test
    void testFindAllTransferStations() {
        List<Integer> stations = trainDAO.findAllTransferStations(1, 2);
        assertNotNull(stations);
        // assertTrue(stations.contains(3));
    }
}