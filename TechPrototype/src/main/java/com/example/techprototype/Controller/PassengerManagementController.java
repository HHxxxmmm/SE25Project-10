package com.example.techprototype.Controller;

import com.example.techprototype.DTO.AddPassengerRequest;
import com.example.techprototype.DTO.AddPassengerResponse;
import com.example.techprototype.DTO.CheckAddPassengerResponse;
import com.example.techprototype.Service.PassengerManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/passenger")
public class PassengerManagementController {
    
    @Autowired
    private PassengerManagementService passengerManagementService;
    
    /**
     * 检查是否可以添加乘车人
     */
    @GetMapping("/check-add/{userId}")
    public ResponseEntity<CheckAddPassengerResponse> checkCanAddPassenger(@PathVariable Long userId) {
        CheckAddPassengerResponse response = passengerManagementService.checkCanAddPassenger(userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 添加乘车人
     */
    @PostMapping("/add")
    public ResponseEntity<AddPassengerResponse> addPassenger(@RequestBody AddPassengerRequest request) {
        AddPassengerResponse response = passengerManagementService.addPassenger(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 刷新用户状态（用于测试）
     */
    @PostMapping("/refresh-user/{userId}")
    public ResponseEntity<String> refreshUserStatus(@PathVariable Long userId) {
        passengerManagementService.refreshUserStatus(userId);
        return ResponseEntity.ok("用户状态已刷新");
    }
} 