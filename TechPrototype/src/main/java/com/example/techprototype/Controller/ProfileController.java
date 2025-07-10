package com.example.techprototype.Controller;

import com.example.techprototype.DTO.*;
import com.example.techprototype.Service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    @Autowired
    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * 获取用户个人资料
     * @param userId 用户ID
     * @return 用户个人资料响应
     */
    @GetMapping("")
    public ResponseEntity<ProfileResponse> getUserProfile(@RequestParam Long userId) {
        ProfileResponse response = profileService.getUserProfile(userId);
        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 更新用户个人资料
     * @param userId 用户ID
     * @param request 更新请求
     * @return 更新后的用户个人资料响应
     */
    @PutMapping("")
    public ResponseEntity<ProfileResponse> updateUserProfile(
            @RequestParam Long userId, 
            @RequestBody ProfileRequest request) {
        ProfileResponse response = profileService.updateUserProfile(userId, request);
        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 修改用户密码
     * @param request 密码修改请求
     * @return 密码修改响应
     */
    @PostMapping("/change-password")
    public ResponseEntity<ChangePasswordResponse> changePassword(@RequestBody ChangePasswordRequest request) {
        ChangePasswordResponse response = profileService.changePassword(request);
        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 更新用户最后登录时间
     * @param request 包含用户ID和登录时间
     * @return 基本响应
     */
    @PostMapping("/update-login-time")
    public ResponseEntity<BasicResponse> updateLastLoginTime(@RequestBody LastLoginUpdateRequest request) {
        BasicResponse response = profileService.updateLastLoginTime(request);
        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 更新用户账户状态（管理员操作）
     * @param request 包含用户ID和账户状态
     * @return 基本响应
     */
    @PostMapping("/update-account-status")
    public ResponseEntity<BasicResponse> updateAccountStatus(@RequestBody AccountStatusUpdateRequest request) {
        BasicResponse response = profileService.updateAccountStatus(request);
        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
} 