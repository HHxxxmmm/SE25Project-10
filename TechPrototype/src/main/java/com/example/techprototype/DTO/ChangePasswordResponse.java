package com.example.techprototype.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordResponse {
    private String status;
    private String message;
    private LocalDateTime timestamp;

    public static ChangePasswordResponse success() {
        ChangePasswordResponse response = new ChangePasswordResponse();
        response.setStatus("SUCCESS");
        response.setMessage("密码修改成功");
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    public static ChangePasswordResponse failure(String message) {
        ChangePasswordResponse response = new ChangePasswordResponse();
        response.setStatus("FAILURE");
        response.setMessage(message);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
}