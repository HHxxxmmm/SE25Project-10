package com.example.techprototype.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {
    private String status;
    private String message;
    private UserProfileData profile;
    private LocalDateTime timestamp;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserProfileData {
        private Long userId;
        private String realName;
        private String email;
        private String phoneNumber;
        private LocalDateTime registrationTime;
        private LocalDateTime lastLoginTime;
        private Byte accountStatus;
        private Integer relatedPassenger;
        private PassengerData linkedPassenger;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PassengerData {
        private Long passengerId;
        private String idCardNumber;
        private String realName;
        private Byte passengerType;
        private String passengerTypeText;
        private String phoneNumber;
        private Integer studentTypeLeft;
    }
    
    public static ProfileResponse success(UserProfileData profile) {
        ProfileResponse response = new ProfileResponse();
        response.setStatus("SUCCESS");
        response.setMessage("获取成功");
        response.setProfile(profile);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
    
    public static ProfileResponse failure(String message) {
        ProfileResponse response = new ProfileResponse();
        response.setStatus("FAILURE");
        response.setMessage(message);
        response.setProfile(null);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
} 