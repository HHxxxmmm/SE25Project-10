package com.example.techprototype.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.techprototype.Service.TrainService;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
class TrainControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private TrainService trainService;

    @Test
    void testGetTrainListDTO() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/trains/list"))
                .andExpect(status().isOk())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        assertNotNull(json);
        // 可进一步断言json结构
    }

    @Test
    void testGetDirectTrains() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/trains/direct")
                .param("startStationId", "1")
                .param("endStationId", "2"))
                .andExpect(status().isOk())
                .andReturn();
        assertNotNull(result.getResponse().getContentAsString());
    }

    @Test
    void testGetTrainsByTime() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/trains/byTime")
                .param("start", "08:00")
                .param("end", "12:00"))
                .andExpect(status().isOk())
                .andReturn();
        assertNotNull(result.getResponse().getContentAsString());
    }

    @Test
    void testGetTransferTrains() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/trains/transfer")
                .param("startStationId", "1")
                .param("endStationId", "3"))
                .andExpect(status().isOk())
                .andReturn();
        assertNotNull(result.getResponse().getContentAsString());
    }

    @Test
    void testSearchTrains() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/trains/search")
                .param("fromStation", "北京")
                .param("toStation", "上海")
                .param("travelDate", "2024-07-10"))
                .andExpect(status().isOk())
                .andReturn();
        assertNotNull(result.getResponse().getContentAsString());
    }

    @Test
    void testSearchTrains_error() throws Exception {
        // Mock service 抛出异常，确保catch分支被覆盖
        when(trainService.searchTrainsByStations("北京", "上海", "非法时间"))
                .thenThrow(new RuntimeException("mock error"));
        MvcResult result = mockMvc.perform(get("/api/trains/search")
                .param("fromStation", "北京")
                .param("toStation", "上海")
                .param("travelDate", "非法时间"))
                .andExpect(status().isBadRequest())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        assertTrue(json.contains("error"));
    }
}
