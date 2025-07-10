package com.example.techprototype.DTO;

import lombok.Data;

@Data
public class AddPassengerRequest {
    private Long userId;
    private String realName;
    private String idCardNumber;
    private String phoneNumber;
} 