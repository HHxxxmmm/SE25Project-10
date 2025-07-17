package com.example.techprototype.Service.Impl;

import com.example.techprototype.DTO.*;
import com.example.techprototype.Entity.Passenger;
import com.example.techprototype.Entity.User;
import com.example.techprototype.Entity.UserPassengerRelation;
import com.example.techprototype.Repository.PassengerRepository;
import com.example.techprototype.Repository.UserPassengerRelationRepository;
import com.example.techprototype.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PassengerRepository passengerRepository;

    @Mock
    private UserPassengerRelationRepository userPassengerRelationRepository;

    @InjectMocks
    private ProfileServiceImpl profileService;

    private User testUser;
    private Passenger testPassenger;
    private UserPassengerRelation testRelation;
    private final LocalDateTime now = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        // 初始化测试用户数据
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setRealName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPhoneNumber("13800138000");
        testUser.setPasswordHash("$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG");
        testUser.setRegistrationTime(now);
        testUser.setAccountStatus((byte)1);
        testUser.setRelatedPassenger(1);

        // 初始化测试乘客数据
        testPassenger = new Passenger();
        testPassenger.setPassengerId(1L);
        testPassenger.setRealName("Test Passenger");
        testPassenger.setIdCardNumber("310123199001011234");
        testPassenger.setPassengerType((byte)1);
        testPassenger.setPhoneNumber("13800138000");

        // 初始化用户-乘客关系数据
        testRelation = new UserPassengerRelation();
        testRelation.setRelationId(1L);
        testRelation.setUserId(1L);
        testRelation.setPassengerId(1L);
        testRelation.setRelationType((byte)1);
    }

    @Test
    void getUserProfile_Success() {
        // 配置mock行为
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userPassengerRelationRepository.findByUserId(1L))
                .thenReturn(Collections.singletonList(testRelation));
        when(passengerRepository.findById(1L)).thenReturn(Optional.of(testPassenger));

        // 执行测试
        ProfileResponse response = profileService.getUserProfile(1L);

        // 验证结果
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("获取成功", response.getMessage());
        assertNotNull(response.getProfile());
        assertEquals(testUser.getRealName(), response.getProfile().getRealName());
        assertEquals(1, response.getProfile().getLinkedPassengers().size());

        // 验证调用
        verify(userRepository).findById(1L);
        verify(userPassengerRelationRepository).findByUserId(1L);
        verify(passengerRepository).findById(1L);
    }

    @Test
    void getUserProfile_UserNotFound() {
        // 配置mock行为
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // 执行测试
        ProfileResponse response = profileService.getUserProfile(99L);

        // 验证结果
        assertEquals("FAILURE", response.getStatus());
        assertEquals("用户不存在", response.getMessage());
        assertNull(response.getProfile());

        // 验证调用
        verify(userRepository).findById(99L);
        verifyNoInteractions(userPassengerRelationRepository, passengerRepository);
    }

    @Test
    void updateUserProfile_Success() {
        // 准备测试数据
        ProfileRequest request = new ProfileRequest();
        request.setRealName("Updated Name");
        request.setEmail("new@example.com");
        request.setPhoneNumber("13900139000");

        // 配置mock行为
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userPassengerRelationRepository.findByUserId(1L))
                .thenReturn(Collections.emptyList());

        // 执行测试
        ProfileResponse response = profileService.updateUserProfile(1L, request);

        // 验证结果
        assertEquals("SUCCESS", response.getStatus());

        // 验证调用 - findById会被调用两次：一次在updateUserProfile中，一次在getUserProfile中
        verify(userRepository, times(2)).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUserProfile_EmailExists() {
        // 准备测试数据
        ProfileRequest request = new ProfileRequest();
        request.setEmail("existing@example.com");

        // 配置mock行为
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // 执行测试
        ProfileResponse response = profileService.updateUserProfile(1L, request);

        // 验证结果
        assertEquals("FAILURE", response.getStatus());
        assertEquals("邮箱已被其他用户使用", response.getMessage());
    }

    @Test
    void updateUserProfile_PhoneNumberExists() {
        // 准备测试数据
        ProfileRequest request = new ProfileRequest();
        request.setPhoneNumber("13900139000");

        // 配置mock行为
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByPhoneNumber("13900139000")).thenReturn(true);

        // 执行测试
        ProfileResponse response = profileService.updateUserProfile(1L, request);

        // 验证结果
        assertEquals("FAILURE", response.getStatus());
        assertEquals("手机号已被其他用户使用", response.getMessage());
    }

    @Test
    void updateUserProfile_DatabaseError() {
        // 准备测试数据
        ProfileRequest request = new ProfileRequest();
        request.setRealName("Updated Name");

        // 配置mock行为抛出异常
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database error"));

        // 执行测试
        ProfileResponse response = profileService.updateUserProfile(1L, request);

        // 验证结果
        assertEquals("FAILURE", response.getStatus());
        assertTrue(response.getMessage().contains("更新用户个人资料失败"));
    }

    @Test
    void changePassword_Success() {
        // 准备测试数据
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setUserId(1L);
        request.setCurrentPassword("password");
        request.setNewPassword("newpassword");

        // 配置mock行为
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // 执行测试
        ChangePasswordResponse response = profileService.changePassword(request);

        // 验证结果
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("密码修改成功", response.getMessage());

        // 验证调用
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void changePassword_InvalidCurrentPassword() {
        // 准备测试数据
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setUserId(1L);
        request.setCurrentPassword("wrongpassword");
        request.setNewPassword("newpassword");

        // 配置mock行为
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // 执行测试
        ChangePasswordResponse response = profileService.changePassword(request);

        // 验证结果
        assertEquals("FAILURE", response.getStatus());
        assertEquals("当前密码不正确", response.getMessage());

        // 验证调用
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changePassword_WithMd5Password() {
        // 准备测试数据
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setUserId(1L);
        request.setCurrentPassword("password");
        request.setNewPassword("newpassword");

        // 设置用户当前密码为MD5格式
        String md5Password = DigestUtils.md5DigestAsHex("password".getBytes());
        testUser.setPasswordHash(md5Password);

        // 配置mock行为
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // 执行测试
        ChangePasswordResponse response = profileService.changePassword(request);

        // 验证结果
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("密码修改成功", response.getMessage());
    }

    @Test
    void changePassword_WithShortNewPassword() {
        // 准备测试数据
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setUserId(1L);
        request.setCurrentPassword("password");
        request.setNewPassword("12345"); // 5位密码，不满足最小长度要求

        // 配置mock行为
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // 执行测试
        ChangePasswordResponse response = profileService.changePassword(request);

        // 验证结果
        assertEquals("FAILURE", response.getStatus());
        assertEquals("新密码不能为空且长度不能少于6位", response.getMessage());
    }

    @Test
    void updateLastLoginTime_Success() {
        // 准备测试数据
        LastLoginUpdateRequest request = new LastLoginUpdateRequest();
        request.setUserId(1L);
        request.setLoginTime(LocalDateTime.now());

        // 配置mock行为
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // 执行测试
        BasicResponse response = profileService.updateLastLoginTime(request);

        // 验证结果
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("最后登录时间更新成功", response.getMessage());

        // 验证调用
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateLastLoginTime_WithNullTime() {
        // 准备测试数据
        LastLoginUpdateRequest request = new LastLoginUpdateRequest();
        request.setUserId(1L);
        request.setLoginTime(null); // 设置空的登录时间

        // 配置mock行为
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // 执行测试
        BasicResponse response = profileService.updateLastLoginTime(request);

        // 验证结果
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("最后登录时间更新成功", response.getMessage());
    }

    @Test
    void updateLastLoginTime_DatabaseError() {
        // 准备测试数据
        LastLoginUpdateRequest request = new LastLoginUpdateRequest();
        request.setUserId(1L);
        request.setLoginTime(LocalDateTime.now());

        // 配置mock行为抛出异常
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database error"));

        // 执行测试
        BasicResponse response = profileService.updateLastLoginTime(request);

        // 验证结果
        assertEquals("FAILURE", response.getStatus());
        assertTrue(response.getMessage().contains("更新最后登录时间失败"));
    }

    @Test
    void updateAccountStatus_Success() {
        // 准备测试数据
        AccountStatusUpdateRequest request = new AccountStatusUpdateRequest();
        request.setUserId(1L);
        request.setAccountStatus((byte)0);

        // 配置mock行为
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // ���行测试
        BasicResponse response = profileService.updateAccountStatus(request);

        // 验证结果
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("账户状态已更新为：冻结", response.getMessage());

        // 验证调用
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateAccountStatus_InvalidStatus() {
        // 准备测试数据
        AccountStatusUpdateRequest request = new AccountStatusUpdateRequest();
        request.setUserId(1L);
        request.setAccountStatus((byte)2);

        // 配置mock行为
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // 执行测试
        BasicResponse response = profileService.updateAccountStatus(request);

        // 验证结果
        assertEquals("FAILURE", response.getStatus());
        assertEquals("账户状态值无效，必须是0(冻结)或1(正常)", response.getMessage());

        // 验证调用
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateAccountStatus_DatabaseError() {
        // 准备测试数据
        AccountStatusUpdateRequest request = new AccountStatusUpdateRequest();
        request.setUserId(1L);
        request.setAccountStatus((byte)0);

        // 配置mock行为抛出异常
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database error"));

        // 执行测试
        BasicResponse response = profileService.updateAccountStatus(request);

        // 验证结果
        assertEquals("FAILURE", response.getStatus());
        assertTrue(response.getMessage().contains("更新账户状态失败"));
    }

    @Test
    void getUserProfile_DatabaseError() {
        // 配置mock行为抛出异常
        when(userRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));

        // 执行测试
        ProfileResponse response = profileService.getUserProfile(1L);

        // 验证结果
        assertEquals("FAILURE", response.getStatus());
        assertTrue(response.getMessage().contains("获取用户个人资料失败"));
        assertNull(response.getProfile());
    }

    @Test
    void getUserProfile_WithStudentPassenger() {
        // 配置一个学生类型的乘客
        testPassenger.setPassengerType((byte)3); // 学生类型
        testPassenger.setStudentTypeLeft(5); // 设置剩余次数

        // 配置mock行为
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userPassengerRelationRepository.findByUserId(1L))
                .thenReturn(Collections.singletonList(testRelation));
        when(passengerRepository.findById(1L)).thenReturn(Optional.of(testPassenger));

        // 执行测试
        ProfileResponse response = profileService.getUserProfile(1L);

        // 验证结果
        assertEquals("SUCCESS", response.getStatus());
        assertNotNull(response.getProfile());
        assertEquals(1, response.getProfile().getLinkedPassengers().size());
        assertEquals(5, response.getProfile().getLinkedPassengers().get(0).getStudentTypeLeft());
    }

    @Test
    void getUserProfile_InvalidPassengerType() {
        // 配置一个无效类型的乘客
        testPassenger.setPassengerType((byte)99); // 无效类型

        // 配置mock行为
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userPassengerRelationRepository.findByUserId(1L))
                .thenReturn(Collections.singletonList(testRelation));
        when(passengerRepository.findById(1L)).thenReturn(Optional.of(testPassenger));

        // 执行测试
        ProfileResponse response = profileService.getUserProfile(1L);

        // 验证结果
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("未知", response.getProfile().getLinkedPassengers().get(0).getPassengerTypeText());
    }

    @Test
    void getUserProfile_PassengerNotFound() {
        // 配置mock行为
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userPassengerRelationRepository.findByUserId(1L))
                .thenReturn(Collections.singletonList(testRelation));
        when(passengerRepository.findById(1L)).thenReturn(Optional.empty());

        // 执行测试
        ProfileResponse response = profileService.getUserProfile(1L);

        // 验证结果
        assertEquals("SUCCESS", response.getStatus());
        assertTrue(response.getProfile().getLinkedPassengers().isEmpty());
    }

    @Test
    void updateUserProfile_WithNullRequest() {
        // 准备测试数据
        ProfileRequest request = new ProfileRequest();
        // 所有字段都保持为null

        // 配置mock行为
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // 执行测试
        ProfileResponse response = profileService.updateUserProfile(1L, request);

        // 验证结果
        assertEquals("SUCCESS", response.getStatus());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUserProfile_WithEmptyStrings() {
        // 准备测试数据
        ProfileRequest request = new ProfileRequest();
        request.setRealName("");
        request.setEmail("");
        request.setPhoneNumber("");

        // 配置mock行为
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // 执行测试
        ProfileResponse response = profileService.updateUserProfile(1L, request);

        // 验证结果
        assertEquals("SUCCESS", response.getStatus());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void changePassword_WithNullNewPassword() {
        // 准备测试数据
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setUserId(1L);
        request.setCurrentPassword("password");
        request.setNewPassword(null);

        // 配置mock行为
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // 执行测试
        ChangePasswordResponse response = profileService.changePassword(request);

        // 验证结果
        assertEquals("FAILURE", response.getStatus());
        assertEquals("新密码不能为空且长度不能少于6位", response.getMessage());
    }

    @Test
    void changePassword_WithEmptyNewPassword() {
        // 准备测试数据
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setUserId(1L);
        request.setCurrentPassword("password");
        request.setNewPassword("");

        // 配置mock行为
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // 执行测试
        ChangePasswordResponse response = profileService.changePassword(request);

        // 验证结果
        assertEquals("FAILURE", response.getStatus());
        assertEquals("新密码不能为空且长度不能少于6位", response.getMessage());
    }

    @Test
    void updateAccountStatus_UserNotFound() {
        // 准备测试数据
        AccountStatusUpdateRequest request = new AccountStatusUpdateRequest();
        request.setUserId(99L);
        request.setAccountStatus((byte)0);

        // 配置mock行为
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // 执行测试
        BasicResponse response = profileService.updateAccountStatus(request);

        // 验证结果
        assertEquals("FAILURE", response.getStatus());
        assertEquals("用户不存在", response.getMessage());
    }

    @Test
    void updateLastLoginTime_UserNotFound() {
        // 准备测试数据
        LastLoginUpdateRequest request = new LastLoginUpdateRequest();
        request.setUserId(99L);
        request.setLoginTime(LocalDateTime.now());

        // 配置mock行为
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // 执行测试
        BasicResponse response = profileService.updateLastLoginTime(request);

        // 验证结果
        assertEquals("FAILURE", response.getStatus());
        assertEquals("用户不存在", response.getMessage());
    }

    @Test
    void changePassword_RepositoryException() {
        // 准备测试数据
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setUserId(1L);
        request.setCurrentPassword("password");
        request.setNewPassword("newpassword");

        // 配置mock行为
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database error"));

        // 执行测试
        ChangePasswordResponse response = profileService.changePassword(request);

        // 验证结果
        assertEquals("FAILURE", response.getStatus());
        assertTrue(response.getMessage().contains("修改密码失败"));
    }

    @Test
    void updateUserProfile_UserNotFoundShouldReturnFailure() {
        // 准备测试数据
        ProfileRequest request = new ProfileRequest();
        request.setRealName("Test Name");

        // 配置mock行为
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // 执行测试
        ProfileResponse response = profileService.updateUserProfile(99L, request);

        // 验证结果
        assertEquals("FAILURE", response.getStatus());
        assertEquals("用户不存在", response.getMessage());
        verify(userRepository).findById(99L);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void updateUserProfile_SameEmailShouldNotCheckExists() {
        // 准备测试数据
        ProfileRequest request = new ProfileRequest();
        request.setEmail(testUser.getEmail());

        // 配置mock行为
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // 执行测试
        ProfileResponse response = profileService.updateUserProfile(1L, request);

        // 验证结果
        assertEquals("SUCCESS", response.getStatus());
        verify(userRepository, never()).existsByEmail(anyString());
    }

    @Test
    void updateUserProfile_SamePhoneNumberShouldNotCheckExists() {
        // 准备测试数据
        ProfileRequest request = new ProfileRequest();
        request.setPhoneNumber(testUser.getPhoneNumber());

        // 配置mock行为
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // 执行测试
        ProfileResponse response = profileService.updateUserProfile(1L, request);

        // 验证结果
        assertEquals("SUCCESS", response.getStatus());
        verify(userRepository, never()).existsByPhoneNumber(anyString());
    }

    @Test
    void changePassword_UserNotFoundShouldReturnFailure() {
        // 准备测试数据
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setUserId(99L);
        request.setCurrentPassword("password");
        request.setNewPassword("newpassword");

        // 配置mock行为
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // 执行测试
        ChangePasswordResponse response = profileService.changePassword(request);

        // 验证结果
        assertEquals("FAILURE", response.getStatus());
        assertEquals("用户不存在", response.getMessage());
        verify(userRepository, never()).save(any(User.class));
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void updateAccountStatus_SetToNormalShouldShowCorrectMessage() {
        // 准备测试数据
        AccountStatusUpdateRequest request = new AccountStatusUpdateRequest();
        request.setUserId(1L);
        request.setAccountStatus((byte)1);

        // 配置mock行为
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // 执行测试
        BasicResponse response = profileService.updateAccountStatus(request);

        // 验证结果
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("账户状态已更新为：正常", response.getMessage());
    }

    @Test
    void changePassword_WithInvalidMd5Hash() {
        // 准备测试数据
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setUserId(1L);
        request.setCurrentPassword("password");
        request.setNewPassword("newpassword");

        // 设置一个无效的MD5格式密码哈��（33位，不符合32位要求）
        testUser.setPasswordHash("1234567890123456789012345678901234");

        // 配置mock行为
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // 执行测试
        ChangePasswordResponse response = profileService.changePassword(request);

        // 验证结果
        assertEquals("FAILURE", response.getStatus());
        assertEquals("当前密码不正确", response.getMessage());
        verify(userRepository, never()).save(any());
    }
}
