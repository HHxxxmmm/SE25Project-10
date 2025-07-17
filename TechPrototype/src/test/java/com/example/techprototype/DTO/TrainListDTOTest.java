package com.example.techprototype.DTO;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TrainListDTOTest {

    @Test
    void testGetterAndSetter() {
        TrainListDTO dto = new TrainListDTO();
        dto.setTrain_id("G123");
        dto.setT_station_number(5);
        dto.setT_path(Arrays.asList("北京", "天津", "济南", "南京", "上海"));
        dto.setT_from("北京");
        dto.setT_to("上海");
        dto.setT_start_time("2024-06-01 08:00:00");
        dto.setT_end_time("2024-06-01 12:00:00");
        dto.setSeat(Arrays.asList(1, 2, 3));
        dto.setSeat_number(Arrays.asList(10, 20, 30));
        dto.setSeat_price(Arrays.asList(100, 200, 300));

        assertEquals("G123", dto.getTrain_id());
        assertEquals(5, dto.getT_station_number());
        assertEquals(Arrays.asList("北京", "天津", "济南", "南京", "上海"), dto.getT_path());
        assertEquals("北京", dto.getT_from());
        assertEquals("上海", dto.getT_to());
        assertEquals("2024-06-01 08:00:00", dto.getT_start_time());
        assertEquals("2024-06-01 12:00:00", dto.getT_end_time());
        assertEquals(Arrays.asList(1, 2, 3), dto.getSeat());
        assertEquals(Arrays.asList(10, 20, 30), dto.getSeat_number());
        assertEquals(Arrays.asList(100, 200, 300), dto.getSeat_price());
    }

    @Test
    void testEqualsAndHashCode() {
        TrainListDTO dto1 = new TrainListDTO();
        TrainListDTO dto2 = new TrainListDTO();

        dto1.setTrain_id("G123");
        dto2.setTrain_id("G123");

        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());

        dto2.setTrain_id("G124");
        assertNotEquals(dto1, dto2);
    }

    @Test
    void testToString() {
        TrainListDTO dto = new TrainListDTO();
        dto.setTrain_id("G123");
        String str = dto.toString();
        assertTrue(str.contains("G123"));
        assertTrue(str.contains("TrainListDTO"));
    }
}