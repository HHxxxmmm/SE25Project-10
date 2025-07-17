package com.example.techprototype.Service.Impl;

import com.example.techprototype.DTO.TrainListDTO;
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
class TrainServiceImplTest {

    @Autowired
    private TrainServiceImpl trainService;

    @Test
    void findDirectTrains() {
        List<Train> trains = trainService.findDirectTrains(1, 2);
        assertNotNull(trains);
        for (Train train : trains) {
            assertNotNull(train.getTrainId());
            assertNotNull(train.getTrainNumber());
            assertNotNull(train.getDepartureTime());
            assertNotNull(train.getArrivalTime());
        }
        // 边界：无结果
        List<Train> empty = trainService.findDirectTrains(-1, -2);
        assertNotNull(empty);
        assertEquals(0, empty.size());
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
        // 边界：无结果
        List<Train> empty = trainService.findTrainsByDepartureTime(LocalTime.of(0,0), LocalTime.of(0,1));
        assertNotNull(empty);
    }

    @Test
    void findTransferTrains() {
        List<Map<String, Object>> result = trainService.findTransferTrains(1, 2);
        assertNotNull(result);
        for (Map<String, Object> map : result) {
            assertTrue(map.containsKey("t1_train_id") || map.containsKey("t1_train_number") || map.size() > 0);
        }
        // 边界：无结果
        List<Map<String, Object>> empty = trainService.findTransferTrains(-1, -2);
        assertNotNull(empty);
        assertEquals(0, empty.size());
    }

    @Test
    void getAllTrainListDTO() {
        List<TrainListDTO> list = trainService.getAllTrainListDTO();
        assertNotNull(list);
        for (TrainListDTO dto : list) {
            assertNotNull(dto.getTrain_id());
            assertNotNull(dto.getT_from());
            assertNotNull(dto.getT_to());
            assertNotNull(dto.getT_start_time());
            assertNotNull(dto.getT_end_time());
            assertNotNull(dto.getT_path());
            assertNotNull(dto.getSeat());
            assertNotNull(dto.getSeat_number());
            assertNotNull(dto.getSeat_price());
        }
    }

    @Test
    void searchTrainsByStations() {
        List<Map<String, Object>> result = trainService.searchTrainsByStations("北京", "上海", "2025-07-01");
        assertNotNull(result);
        for (Map<String, Object> map : result) {
            assertTrue(map.containsKey("train_id"));
            assertTrue(map.containsKey("t_from"));
            assertTrue(map.containsKey("t_to"));
            assertTrue(map.containsKey("t_start_time"));
            assertTrue(map.containsKey("t_end_time"));
            assertTrue(map.containsKey("t_path"));
            assertTrue(map.containsKey("t_station_number"));
            assertTrue(map.containsKey("seat"));
            assertTrue(map.containsKey("seat_number"));
            assertTrue(map.containsKey("seat_price"));
        }
        // 边界：无结果
        List<Map<String, Object>> empty = trainService.searchTrainsByStations("不存在", "不存在", "2025-07-01");
        assertNotNull(empty);
        assertEquals(0, empty.size());
    }

    @Test
    void testNullAndExceptionCases() {
        // searchTrainsByStations空参数
        List<Map<String, Object>> result = trainService.searchTrainsByStations("", "", "2025-07-01");
        assertNotNull(result);
        // getAllTrainListDTO在无数据时也应返回非null
        // 这里无法直接清空表，仅做接口健壮性测试
        assertDoesNotThrow(() -> trainService.getAllTrainListDTO());
    }
}