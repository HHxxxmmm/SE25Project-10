package com.example.techprototype.Controller;

import com.example.techprototype.DTO.AddPassengerRequest;
import com.example.techprototype.DTO.AddPassengerResponse;
import com.example.techprototype.DTO.BookingResponse;
import com.example.techprototype.DTO.CheckAddPassengerResponse;
import com.example.techprototype.DTO.DeletePassengerRequest;
import com.example.techprototype.Service.PassengerManagementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PassengerManagementControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PassengerManagementService passengerManagementService;

    @InjectMocks
    private PassengerManagementController passengerManagementController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
                .standaloneSetup(passengerManagementController)
                .defaultResponseCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8) // 设置默认响应编码为UTF-8
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("测试检查能否添加乘客 - 允许添加")
    void testCheckCanAddPassenger_Allowed() throws Exception {
        // 准备测试数据
        CheckAddPassengerResponse mockResponse = new CheckAddPassengerResponse();
        mockResponse.setAllowed(true);
        mockResponse.setMessage("可以添加乘客");

        // 设置模拟服务的行为
        when(passengerManagementService.checkCanAddPassenger(anyLong())).thenReturn(mockResponse);

        // 执行请求并验证结果
        mockMvc.perform(get("/api/passenger/check-add")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed", is(true)))
                .andExpect(jsonPath("$.message", is("可以添加乘客")));

        // 验证服务方法被调用
        verify(passengerManagementService).checkCanAddPassenger(1L);
    }

    @Test
    @DisplayName("测试检查能否添加乘客 - 不允许添加")
    void testCheckCanAddPassenger_NotAllowed() throws Exception {
        // 准备测试数据
        CheckAddPassengerResponse mockResponse = new CheckAddPassengerResponse();
        mockResponse.setAllowed(false);
        mockResponse.setMessage("已达乘客上限");

        // 设置模拟服务的行为
        when(passengerManagementService.checkCanAddPassenger(anyLong())).thenReturn(mockResponse);

        // 执行请求并验证结果
        mockMvc.perform(get("/api/passenger/check-add")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed", is(false)))
                .andExpect(jsonPath("$.message", is("已达乘客上限")));

        // 验证服务方法被调用
        verify(passengerManagementService).checkCanAddPassenger(1L);
    }

    @Test
    @DisplayName("测试添加乘客 - 成功")
    void testAddPassenger_Success() throws Exception {
        // 准备请求数据
        AddPassengerRequest request = new AddPassengerRequest();
        request.setUserId(1L);
        request.setRealName("张三");
        request.setIdCardNumber("110101199001010011");
        request.setPhoneNumber("13800138000");

        // 准备响应数据
        AddPassengerResponse mockResponse = new AddPassengerResponse();
        mockResponse.setSuccess(true);
        mockResponse.setMessage("添加乘客成功");
        mockResponse.setRelationId(1L);

        // 设置模拟服务的行为
        when(passengerManagementService.addPassenger(any(AddPassengerRequest.class))).thenReturn(mockResponse);

        // 执行请求并验证结果
        mockMvc.perform(post("/api/passenger/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("添加乘客成功")))
                .andExpect(jsonPath("$.relationId", is(1)));

        // 验证服务方法被调用
        verify(passengerManagementService).addPassenger(any(AddPassengerRequest.class));
    }

    @Test
    @DisplayName("测试添加乘客 - 失败")
    void testAddPassenger_Failure() throws Exception {
        // 准备请求数据
        AddPassengerRequest request = new AddPassengerRequest();
        request.setUserId(1L);
        request.setRealName("张三");
        request.setIdCardNumber("110101199001010011");
        request.setPhoneNumber("13800138000");

        // 准备响应数据
        AddPassengerResponse mockResponse = new AddPassengerResponse();
        mockResponse.setSuccess(false);
        mockResponse.setMessage("该乘客已存在");
        mockResponse.setRelationId(null);

        // 设置模拟服务的行为
        when(passengerManagementService.addPassenger(any(AddPassengerRequest.class))).thenReturn(mockResponse);

        // 执行请求并验证结果
        mockMvc.perform(post("/api/passenger/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("该乘客已存在")))
                .andExpect(jsonPath("$.relationId").doesNotExist());

        // 验证服务方法被调用
        verify(passengerManagementService).addPassenger(any(AddPassengerRequest.class));
    }

    @Test
    @DisplayName("测试刷新用户状态")
    void testRefreshUserStatus() throws Exception {
        // 执行请求并验证结果
        mockMvc.perform(post("/api/passenger/refresh-user/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8"))
                .andExpect(status().isOk());

        // 验证服务方法被调用
        verify(passengerManagementService).refreshUserStatus(1L);
    }

    @Test
    @DisplayName("测试删除乘客 - 成功")
    void testDeletePassenger_Success() throws Exception {
        // 准备请求数据
        DeletePassengerRequest request = new DeletePassengerRequest();
        request.setUserId(1L);
        request.setPassengerId(2L);

        // 准备响应数据
        BookingResponse mockResponse = BookingResponse.successWithMessage(
                "乘客删除成功",
                null,
                null,
                null,
                null
        );

        // 设置模拟服务的行为
        when(passengerManagementService.deletePassenger(any(DeletePassengerRequest.class))).thenReturn(mockResponse);

        // 执行请求并验证结果
        mockMvc.perform(delete("/api/passenger/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.message", is("乘客删除成功")));

        // 验证服务方法被调用
        verify(passengerManagementService).deletePassenger(any(DeletePassengerRequest.class));
    }

    @Test
    @DisplayName("测试删除乘客 - 异常")
    void testDeletePassenger_Exception() throws Exception {
        // 准备请求数据
        DeletePassengerRequest request = new DeletePassengerRequest();
        request.setUserId(1L);
        request.setPassengerId(2L);

        // 设置模拟服务抛出异常
        when(passengerManagementService.deletePassenger(any(DeletePassengerRequest.class)))
                .thenThrow(new RuntimeException("删除乘客失败"));

        // 执行请求并验证结果
        mockMvc.perform(delete("/api/passenger/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("删除乘客失败")));

        // 验证服务方法被调用
        verify(passengerManagementService).deletePassenger(any(DeletePassengerRequest.class));
    }
}