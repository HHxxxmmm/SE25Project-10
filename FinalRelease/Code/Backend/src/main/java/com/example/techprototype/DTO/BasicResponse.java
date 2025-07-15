package com.example.techprototype.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BasicResponse {
    private String status;
    private String message;
    private LocalDateTime timestamp;

    public static BasicResponse success(String message) {
        BasicResponse response = new BasicResponse();
        response.setStatus("SUCCESS");
        response.setMessage(message);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    public static BasicResponse failure(String message) {
        BasicResponse response = new BasicResponse();
        response.setStatus("FAILURE");
        response.setMessage(message);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
}