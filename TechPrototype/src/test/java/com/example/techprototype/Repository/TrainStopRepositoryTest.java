package com.example.techprototype.Repository;

import com.example.techprototype.Entity.TrainStop;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class TrainStopRepositoryTest {

    @Autowired
    private TrainStopRepository trainStopRepository;

    private TrainStop stop1, stop2, stop3;

    @BeforeEach
    void setUp() {
        // 构造测试数据
        trainStopRepository.deleteAll();
        stop1 = new TrainStop();
        stop1.setTrainId(1001);
        stop1.setStationId(1);
        stop1.setSequenceNumber(1);
        trainStopRepository.save(stop1);

        stop2 = new TrainStop();
        stop2.setTrainId(1001);
        stop2.setStationId(2);
        stop2.setSequenceNumber(2);
        trainStopRepository.save(stop2);

        stop3 = new TrainStop();
        stop3.setTrainId(1002);
        stop3.setStationId(1);
        stop3.setSequenceNumber(1);
        trainStopRepository.save(stop3);
    }

    @Test
    void findByStopId() {
        Optional<TrainStop> found = trainStopRepository.findByStopId(stop1.getStopId());
        assertTrue(found.isPresent());
        assertEquals(stop1.getTrainId(), found.get().getTrainId());
    }

    @Test
    void findByTrainIdAndStationId() {
        TrainStop found = trainStopRepository.findByTrainIdAndStationId(1001, 2);
        assertNotNull(found);
        assertEquals(stop2.getStationId(), found.getStationId());
    }

    @Test
    void findByTrainIdAndStopId() {
        Optional<TrainStop> found = trainStopRepository.findByTrainIdAndStopId(1001, stop1.getStopId());
        assertTrue(found.isPresent());
        assertEquals(stop1.getStationId(), found.get().getStationId());
    }

    @Test
    void findByTrainIdOrderBySequenceNumberAsc() {
        List<TrainStop> stops = trainStopRepository.findByTrainIdOrderBySequenceNumberAsc(1001);
        assertEquals(2, stops.size());
        assertTrue(stops.get(0).getSequenceNumber() < stops.get(1).getSequenceNumber());
    }

    @Test
    void findByStationId() {
        List<TrainStop> stops = trainStopRepository.findByStationId(1);
        assertEquals(2, stops.size());
        assertTrue(stops.stream().anyMatch(s -> s.getTrainId().equals(1001)));
        assertTrue(stops.stream().anyMatch(s -> s.getTrainId().equals(1002)));
    }
}