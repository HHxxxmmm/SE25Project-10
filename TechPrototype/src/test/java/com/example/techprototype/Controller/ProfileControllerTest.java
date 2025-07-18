package com.example.techprototype.Controller;

import com.example.techprototype.DTO.*;
import com.example.techprototype.Service.ProfileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfileControllerTest {

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private ProfileController profileController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void getUserProfile_Success() {
        // 准备测试数据
        Long userId = 1L;
        ProfileResponse mockResponse = new ProfileResponse();
        mockResponse.setStatus("SUCCESS");
        mockResponse.setMessage("获取成功");

        // 配置mock行为
        when(profileService.getUserProfile(userId)).thenReturn(mockResponse);

        // 执行测试
        ResponseEntity<ProfileResponse> response = profileController.getUserProfile(userId);

        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(profileService).getUserProfile(userId);
    }

    @Test
    void getUserProfile_Failure() {
        // 准备测试数据
        Long userId = 1L;
        ProfileResponse mockResponse = new ProfileResponse();
        mockResponse.setStatus("ERROR");
        mockResponse.setMessage("用户不存在");

        // 配置mock行为
        when(profileService.getUserProfile(userId)).thenReturn(mockResponse);

        // 执行测��
        ResponseEntity<ProfileResponse> response = profileController.getUserProfile(userId);

        // 验证结果
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(profileService).getUserProfile(userId);
    }

    @Test
    void updateUserProfile_Success() {
        // 准备测试数据
        Long userId = 1L;
        ProfileRequest request = new ProfileRequest();
        ProfileResponse mockResponse = new ProfileResponse();
        mockResponse.setStatus("SUCCESS");
        mockResponse.setMessage("更新成功");

        // 配置mock行为
        when(profileService.updateUserProfile(userId, request)).thenReturn(mockResponse);

        // 执行测试
        ResponseEntity<ProfileResponse> response = profileController.updateUserProfile(userId, request);

        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(profileService).updateUserProfile(userId, request);
    }

    @Test
    void updateUserProfile_Failure() {
        // 准备测试数据
        Long userId = 1L;
        ProfileRequest request = new ProfileRequest();
        ProfileResponse mockResponse = new ProfileResponse();
        mockResponse.setStatus("ERROR");
        mockResponse.setMessage("更新失败");

        // 配置mock行为
        when(profileService.updateUserProfile(userId, request)).thenReturn(mockResponse);

        // 执行测试
        ResponseEntity<ProfileResponse> response = profileController.updateUserProfile(userId, request);

        // 验证结果
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(profileService).updateUserProfile(userId, request);
    }

    @Test
    void changePassword_Success() {
        // 准备测试数据
        ChangePasswordRequest request = new ChangePasswordRequest();
        ChangePasswordResponse mockResponse = new ChangePasswordResponse();
        mockResponse.setStatus("SUCCESS");
        mockResponse.setMessage("密码修改成功");

        // 配置mock行为
        when(profileService.changePassword(request)).thenReturn(mockResponse);

        // 执行测试
        ResponseEntity<ChangePasswordResponse> response = profileController.changePassword(request);

        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(profileService).changePassword(request);
    }

    @Test
    void changePassword_Failure() {
        // 准备测试数据
        ChangePasswordRequest request = new ChangePasswordRequest();
        ChangePasswordResponse mockResponse = new ChangePasswordResponse();
        mockResponse.setStatus("ERROR");
        mockResponse.setMessage("原密码错误");

        // 配置mock行为
        when(profileService.changePassword(request)).thenReturn(mockResponse);

        // 执行测试
        ResponseEntity<ChangePasswordResponse> response = profileController.changePassword(request);

        // 验证结果
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(profileService).changePassword(request);
    }

    @Test
    void updateLastLoginTime_Success() {
        // 准备测试数据
        LastLoginUpdateRequest request = new LastLoginUpdateRequest();
        BasicResponse mockResponse = new BasicResponse();
        mockResponse.setStatus("SUCCESS");
        mockResponse.setMessage("登录时间更新成功");

        // 配置mock行为
        when(profileService.updateLastLoginTime(request)).thenReturn(mockResponse);

        // 执行测试
        ResponseEntity<BasicResponse> response = profileController.updateLastLoginTime(request);

        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(profileService).updateLastLoginTime(request);
    }

    @Test
    void updateLastLoginTime_Failure() {
        // 准备测试数据
        LastLoginUpdateRequest request = new LastLoginUpdateRequest();
        BasicResponse mockResponse = new BasicResponse();
        mockResponse.setStatus("ERROR");
        mockResponse.setMessage("更新失败");

        // 配置mock行为
        when(profileService.updateLastLoginTime(request)).thenReturn(mockResponse);

        // 执行测试
        ResponseEntity<BasicResponse> response = profileController.updateLastLoginTime(request);

        // 验证结果
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(profileService).updateLastLoginTime(request);
    }

    @Test
    void updateAccountStatus_Success() {
        // 准备测试数据
        AccountStatusUpdateRequest request = new AccountStatusUpdateRequest();
        BasicResponse mockResponse = new BasicResponse();
        mockResponse.setStatus("SUCCESS");
        mockResponse.setMessage("账户状态更新成功");

        // 配置mock行为
        when(profileService.updateAccountStatus(request)).thenReturn(mockResponse);

        // 执行测试
        ResponseEntity<BasicResponse> response = profileController.updateAccountStatus(request);

        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(profileService).updateAccountStatus(request);
    }

    @Test
    void updateAccountStatus_Failure() {
        // 准备测试数据
        AccountStatusUpdateRequest request = new AccountStatusUpdateRequest();
        BasicResponse mockResponse = new BasicResponse();
        mockResponse.setStatus("ERROR");
        mockResponse.setMessage("无权限进行此操作");

        // 配置mock行为
        when(profileService.updateAccountStatus(request)).thenReturn(mockResponse);

        // 执行测试
        ResponseEntity<BasicResponse> response = profileController.updateAccountStatus(request);

        // 验证结果
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(profileService).updateAccountStatus(request);
    }
}