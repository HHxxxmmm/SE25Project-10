package com.example.techprototype.DTO;

import lombok.Data;

@Data
public class CheckAddPassengerResponse {
    private boolean allowed;
    private String message;
    
    public static CheckAddPassengerResponse allowed() {
        CheckAddPassengerResponse response = new CheckAddPassengerResponse();
        response.setAllowed(true);
        response.setMessage("可以添加乘车人");
        return response;
    }
    
    public static CheckAddPassengerResponse notAllowed(String message) {
        CheckAddPassengerResponse response = new CheckAddPassengerResponse();
        response.setAllowed(false);
        response.setMessage(message);
        return response;
    }
} 