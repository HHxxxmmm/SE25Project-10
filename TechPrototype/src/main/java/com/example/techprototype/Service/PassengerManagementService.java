package com.example.techprototype.Service;

import com.example.techprototype.DTO.AddPassengerRequest;
import com.example.techprototype.DTO.AddPassengerResponse;
import com.example.techprototype.DTO.CheckAddPassengerResponse;

public interface PassengerManagementService {
    
    /**
     * 检查用户是否可以添加乘车人
     */
    CheckAddPassengerResponse checkCanAddPassenger(Long userId);
    
    /**
     * 添加乘车人
     */
    AddPassengerResponse addPassenger(AddPassengerRequest request);
    
    /**
     * 刷新用户状态（用于测试）
     */
    void refreshUserStatus(Long userId);
} 