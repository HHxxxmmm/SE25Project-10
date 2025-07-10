package com.example.techprototype.Service;

import com.example.techprototype.DTO.*;

public interface ProfileService {

    /**
     * 获取用户个人资料
     * @param userId 用户ID
     * @return 个人资料响应，包含用户信息和关联乘客信息
     */
    ProfileResponse getUserProfile(Long userId);

    /**
     * 更新用户个人资料
     * @param userId 用户ID
     * @param request 更新请求
     * @return 更新后的个人资料响应
     */
    ProfileResponse updateUserProfile(Long userId, ProfileRequest request);

    /**
     * 修改用户密码
     * @param request 密码修改请求
     * @return 密码修改响应
     */
    ChangePasswordResponse changePassword(ChangePasswordRequest request);

    /**
     * 更新用户最后登录时间
     * @param request 包含用户ID和登录时间
     * @return 基本响应
     */
    BasicResponse updateLastLoginTime(LastLoginUpdateRequest request);

    /**
     * 更新用户账户状态（管理员操作）
     * @param request 包含用户ID和账户状态
     * @return 基本响应
     */
    BasicResponse updateAccountStatus(AccountStatusUpdateRequest request);
}