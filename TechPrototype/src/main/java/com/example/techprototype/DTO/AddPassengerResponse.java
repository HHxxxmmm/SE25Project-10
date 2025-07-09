package com.example.techprototype.DTO;

import lombok.Data;

@Data
public class AddPassengerResponse {
    private boolean success;
    private String message;
    private Long relationId;
    
    public static AddPassengerResponse success(Long relationId) {
        AddPassengerResponse response = new AddPassengerResponse();
        response.setSuccess(true);
        response.setMessage("乘车人添加成功");
        response.setRelationId(relationId);
        return response;
    }
    
    public static AddPassengerResponse failure(String message) {
        AddPassengerResponse response = new AddPassengerResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
} 